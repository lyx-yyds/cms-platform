package com.cms.controller.admin;

import com.cms.common.Result;
import com.cms.dto.request.TagReq;
import com.cms.service.TagService;
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
 * 标签管理（admin/TagController.java，仅 ADMIN）
 */
@Tag(name = "管理端-标签")
@RestController("adminTagController")
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('tag:manage')")
public class TagController {

    private final TagService tagService;

    @Operation(summary = "新增标签")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody TagReq req) {
        tagService.createTag(req);
        return Result.ok();
    }

    @Operation(summary = "修改标签")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagReq req) {
        tagService.updateTag(id, req);
        return Result.ok();
    }

    @Operation(summary = "删除标签（有文章关联时拒绝）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.ok();
    }
}
