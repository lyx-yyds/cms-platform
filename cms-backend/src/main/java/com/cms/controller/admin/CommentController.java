package com.cms.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.Result;
import com.cms.dto.request.CommentAuditReq;
import com.cms.dto.response.CommentResp;
import com.cms.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论管理（admin/CommentController.java，文档 5.1，仅 ADMIN）
 */
@Tag(name = "管理端-评论")
@RestController("adminCommentController")
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('comment:audit')")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "评论分页（含 AI 机审状态/置信度/原因）")
    @GetMapping
    public Result<Page<CommentResp>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer aiStatus,
            @RequestParam(required = false) String keyword) {
        return Result.ok(commentService.pageComments(page, size, articleId, status, aiStatus, keyword));
    }

    @Operation(summary = "人工复核（覆盖机审结果）")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody CommentAuditReq req) {
        commentService.auditComment(id, req.getStatus());
        return Result.ok();
    }
}
