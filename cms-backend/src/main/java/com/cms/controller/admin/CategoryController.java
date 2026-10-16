package com.cms.controller.admin;

import com.cms.common.Result;
import com.cms.dto.request.CategoryReq;
import com.cms.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类管理（admin/CategoryController.java，文档 5.1，仅 ADMIN）
 */
@Tag(name = "管理端-分类")
@RestController("adminCategoryController")
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('category:manage')")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CategoryReq req) {
        categoryService.createCategory(req);
        return Result.ok();
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryReq req) {
        categoryService.updateCategory(id, req);
        return Result.ok();
    }

    @Operation(summary = "删除分类（有文章引用时拒绝）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.ok();
    }
}
