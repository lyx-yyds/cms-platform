package com.cms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cms.common.BusinessException;
import com.cms.common.ResultCode;
import com.cms.dto.request.CategoryReq;
import com.cms.dto.response.CategoryResp;
import com.cms.entity.Category;
import com.cms.mapper.ArticleMapper;
import com.cms.mapper.CategoryMapper;
import com.cms.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分类服务实现
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    @Override
    public List<CategoryResp> listCategories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId))
                .stream()
                .map(c -> CategoryResp.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .slug(c.getSlug())
                        .description(c.getDescription())
                        .sortOrder(c.getSortOrder())
                        .status(c.getStatus())
                        .articleCount(articleMapper.selectCount(Wrappers.<com.cms.entity.Article>lambdaQuery()
                                .eq(com.cms.entity.Article::getCategoryId, c.getId())))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void createCategory(CategoryReq req) {
        Category category = new Category();
        category.setName(req.getName().trim());
        category.setSlug(req.getSlug());
        category.setDescription(req.getDescription());
        category.setParentId(req.getParentId());
        category.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        category.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        categoryMapper.insert(category);
    }

    @Override
    @Transactional
    public void updateCategory(Long id, CategoryReq req) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        existing.setName(req.getName().trim());
        if (req.getSlug() != null) {
            existing.setSlug(req.getSlug());
        }
        if (req.getDescription() != null) {
            existing.setDescription(req.getDescription());
        }
        if (req.getParentId() != null) {
            existing.setParentId(req.getParentId());
        }
        if (req.getSortOrder() != null) {
            existing.setSortOrder(req.getSortOrder());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }
        categoryMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Long count = articleMapper.selectCount(Wrappers.<com.cms.entity.Article>lambdaQuery()
                .eq(com.cms.entity.Article::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该分类下仍有文章，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
