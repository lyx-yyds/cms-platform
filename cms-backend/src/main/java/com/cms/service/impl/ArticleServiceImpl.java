package com.cms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.BusinessException;
import com.cms.common.Constants;
import com.cms.common.ResultCode;
import com.cms.common.SanitizeUtil;
import com.cms.dto.request.ArticleCreateReq;
import com.cms.dto.request.ArticleUpdateReq;
import com.cms.dto.response.ArticleDetailResp;
import com.cms.dto.response.ArticleResp;
import com.cms.entity.Article;
import com.cms.mapper.ArticleMapper;
import com.cms.mapper.CategoryMapper;
import com.cms.mapper.TagMapper;
import com.cms.security.SecurityUtils;
import com.cms.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章服务实现（文档 7.2 / 7.3 / 7.7）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Page<ArticleResp> pageArticles(int page, int size, String keyword, Long categoryId, Integer status, Long authorId) {
        Page<ArticleResp> p = new Page<>(page, size);
        Page<ArticleResp> result = articleMapper.selectArticlePage(p, keyword, categoryId, status, authorId);
        // 批量填充标签
        for (ArticleResp item : result.getRecords()) {
            item.setTags(tagMapper.selectTagNamesByArticleId(item.getId()));
        }
        return result;
    }

    @Override
    public ArticleDetailResp getArticleDetail(Long id) {
        ArticleDetailResp detail = (ArticleDetailResp) articleMapper.selectArticleDetail(id);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        // 数据级可见性：已发布公开；草稿/下架仅作者或 ADMIN（文档 7.3）
        if (detail.getStatus() != Constants.ARTICLE_STATUS_PUBLISHED) {
            Long current = currentUserIdQuietly();
            boolean isAuthor = current != null && current.equals(detail.getAuthorId());
            boolean isAdmin = current != null && SecurityUtils.hasRole("ADMIN");
            if (!isAuthor && !isAdmin) {
                throw new BusinessException(ResultCode.FORBIDDEN, "文章未发布");
            }
        }
        // 浏览量：Redis INCR 原子自增（文档 7.7.1）；展示 = DB 值 + Redis 增量
        Long redisDelta = stringRedisTemplate.opsForValue().increment(Constants.REDIS_KEY_ARTICLE_VIEWS + id);
        detail.setViewCount(detail.getViewCount() + redisDelta.intValue());
        // 当前用户点赞状态
        Long current = currentUserIdQuietly();
        if (current != null) {
            Boolean liked = stringRedisTemplate.opsForSet().isMember(Constants.REDIS_KEY_ARTICLE_LIKED + id, String.valueOf(current));
            detail.setLiked(Boolean.TRUE.equals(liked));
        } else {
            detail.setLiked(false);
        }
        detail.setTags(tagMapper.selectTagNamesByArticleId(id));
        return detail;
    }

    @Override
    @Transactional
    public Long createArticle(ArticleCreateReq req) {
        Article article = new Article();
        article.setTitle(req.getTitle().trim());
        String cleanContent = SanitizeUtil.sanitizeHtml(req.getContent());
        article.setContent(cleanContent);
        String summary = StringUtils.hasText(req.getSummary())
                ? SanitizeUtil.sanitizeHtml(req.getSummary())
                : SanitizeUtil.plainText(cleanContent, 200);
        article.setSummary(summary);
        article.setCoverImage(req.getCoverImage());
        article.setCategoryId(req.getCategoryId());
        article.setAuthorId(SecurityUtils.getCurrentUserId());
        article.setStatus(req.getStatus() == null ? Constants.ARTICLE_STATUS_DRAFT : req.getStatus());
        article.setIsTop(Boolean.TRUE.equals(req.getIsTop()));
        article.setIsRecommend(Boolean.TRUE.equals(req.getIsRecommend()));
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setPublishTime(article.getStatus() == Constants.ARTICLE_STATUS_PUBLISHED ? LocalDateTime.now() : null);
        articleMapper.insert(article);
        bindTags(article.getId(), req.getTagIds());
        return article.getId();
    }

    @Override
    @Transactional
    public void updateArticle(Long id, ArticleUpdateReq req) {
        Article existing = articleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        checkAuthorOrAdmin(existing.getAuthorId());
        if (StringUtils.hasText(req.getTitle())) {
            existing.setTitle(req.getTitle().trim());
        }
        if (req.getContent() != null) {
            String clean = SanitizeUtil.sanitizeHtml(req.getContent());
            existing.setContent(clean);
            if (!StringUtils.hasText(req.getSummary())) {
                existing.setSummary(SanitizeUtil.plainText(clean, 200));
            }
        }
        if (req.getSummary() != null) {
            existing.setSummary(SanitizeUtil.sanitizeHtml(req.getSummary()));
        }
        if (req.getCoverImage() != null) {
            existing.setCoverImage(req.getCoverImage());
        }
        if (req.getCategoryId() != null) {
            existing.setCategoryId(req.getCategoryId());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        if (req.getIsTop() != null) {
            existing.setIsTop(req.getIsTop());
        }
        if (req.getIsRecommend() != null) {
            existing.setIsRecommend(req.getIsRecommend());
        }
        if (req.getSeoTitle() != null) {
            existing.setSeoTitle(req.getSeoTitle());
        }
        if (req.getSeoDescription() != null) {
            existing.setSeoDescription(req.getSeoDescription());
        }
        if (req.getSeoKeywords() != null) {
            existing.setSeoKeywords(req.getSeoKeywords());
        }
        // 首次发布时落 publishTime；取消发布则清空
        if (existing.getStatus() == Constants.ARTICLE_STATUS_PUBLISHED && existing.getPublishTime() == null) {
            existing.setPublishTime(LocalDateTime.now());
        } else if (existing.getStatus() != Constants.ARTICLE_STATUS_PUBLISHED) {
            existing.setPublishTime(null);
        }
        articleMapper.updateById(existing);
        if (req.getTagIds() != null) {
            tagMapper.deleteArticleTags(id);
            bindTags(id, req.getTagIds());
        }
    }

    @Override
    @Transactional
    public void deleteArticle(Long id) {
        Article existing = articleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        checkAuthorOrAdmin(existing.getAuthorId());
        // MP 逻辑删除：is_deleted = 1（文档 7.10）
        articleMapper.deleteById(id);
        tagMapper.deleteArticleTags(id);
    }

    @Override
    public void likeArticle(Long id) {
        ensureArticleExists(id);
        Long current = SecurityUtils.getCurrentUserId();
        Long added = stringRedisTemplate.opsForSet().add(Constants.REDIS_KEY_ARTICLE_LIKED + id, String.valueOf(current));
        if (added != null && added == 1) {
            articleMapper.increaseLikeCount(id);
        }
    }

    @Override
    public void unlikeArticle(Long id) {
        ensureArticleExists(id);
        Long current = SecurityUtils.getCurrentUserId();
        Long removed = stringRedisTemplate.opsForSet().remove(Constants.REDIS_KEY_ARTICLE_LIKED + id, String.valueOf(current));
        if (removed != null && removed == 1) {
            articleMapper.decreaseLikeCount(id);
        }
    }

    // ===== 私有方法 =====

    private void ensureArticleExists(Long id) {
        if (articleMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
    }

    /** 数据级权限：仅作者本人或 ADMIN（文档 7.3） */
    private void checkAuthorOrAdmin(Long authorId) {
        Long current = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        if (!isAdmin && !current.equals(authorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己的文章");
        }
    }

    /** 未登录返回 null（详情页浏览量/点赞状态对游客开放） */
    private Long currentUserIdQuietly() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (BusinessException e) {
            return null;
        }
    }

    private void bindTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            if (tagMapper.selectById(tagId) != null) {
                tagMapper.insertArticleTag(articleId, tagId);
            }
        }
    }
}
