package com.cms.controller.front;

import com.cms.common.Result;
import com.cms.dto.response.CategoryResp;
import com.cms.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台分类接口（公开）
 */
@Tag(name = "前台-分类")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "分类列表（公开）")
    @GetMapping
    public Result<List<CategoryResp>> list() {
        return Result.ok(categoryService.listCategories());
    }
}
