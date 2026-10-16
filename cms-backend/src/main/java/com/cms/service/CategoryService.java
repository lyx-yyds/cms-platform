package com.cms.service;

import com.cms.dto.request.CategoryReq;
import com.cms.dto.response.CategoryResp;

import java.util.List;

/**
 * 分类服务
 */
public interface CategoryService {

    List<CategoryResp> listCategories();

    void createCategory(CategoryReq req);

    void updateCategory(Long id, CategoryReq req);

    /** 删除分类（存在文章引用时拒绝） */
    void deleteCategory(Long id);
}
