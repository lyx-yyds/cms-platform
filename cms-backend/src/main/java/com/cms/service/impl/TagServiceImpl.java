package com.cms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cms.common.BusinessException;
import com.cms.common.ResultCode;
import com.cms.dto.request.TagReq;
import com.cms.dto.response.TagResp;
import com.cms.entity.Tag;
import com.cms.mapper.TagMapper;
import com.cms.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 标签服务实现
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public List<TagResp> listTags() {
        return tagMapper.selectList(Wrappers.<Tag>lambdaQuery().orderByAsc(Tag::getId))
                .stream()
                .map(t -> TagResp.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .slug(t.getSlug())
                        .articleCount(tagMapper.countArticleByTagId(t.getId()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void createTag(TagReq req) {
        String name = req.getName().trim();
        String slug = normalizeSlug(req.getSlug());
        // 名称/别名唯一性校验（DB 唯一索引兜底，这里翻译成业务提示）
        checkNameUnique(name, null);
        checkSlugUnique(slug, null);
        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(slug);
        tagMapper.insert(tag);
    }

    @Override
    @Transactional
    public void updateTag(Long id, TagReq req) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "标签不存在");
        }
        String name = req.getName().trim();
        String slug = normalizeSlug(req.getSlug());
        checkNameUnique(name, id);
        checkSlugUnique(slug, id);
        existing.setName(name);
        existing.setSlug(slug);
        tagMapper.updateById(existing);
    }

    /** 空别名统一归一化为 null，避免空字符串撞唯一索引 */
    private String normalizeSlug(String slug) {
        return StringUtils.hasText(slug) ? slug.trim() : null;
    }

    private void checkNameUnique(String name, Long excludeId) {
        Long count = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getName, name)
                .ne(excludeId != null, Tag::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标签名称已存在");
        }
    }

    private void checkSlugUnique(String slug, Long excludeId) {
        if (slug == null) {
            return;
        }
        Long count = tagMapper.selectCount(Wrappers.<Tag>lambdaQuery()
                .eq(Tag::getSlug, slug)
                .ne(excludeId != null, Tag::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标签别名已存在");
        }
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Long count = tagMapper.countArticleByTagId(id);
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该标签已被文章使用，无法删除");
        }
        tagMapper.deleteById(id);
    }
}
