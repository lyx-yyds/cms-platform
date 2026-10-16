package com.cms.controller.front;

import com.cms.common.Result;
import com.cms.dto.request.CommentCreateReq;
import com.cms.dto.response.CommentResp;
import com.cms.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台评论接口（front/CommentController.java，文档 5.1）
 * 公开：文章评论列表（仅已通过）；登录：发表评论（触发 AI 初审）/ 删除评论
 */
@Tag(name = "评论")
@RestController("frontCommentController")
@RequestMapping
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "文章评论列表（公开，仅展示已通过）")
    @GetMapping("/api/articles/{articleId}/comments")
    public Result<List<CommentResp>> list(@PathVariable Long articleId) {
        return Result.ok(commentService.listApprovedByArticle(articleId));
    }

    @Operation(summary = "发表评论（触发 AI 初审，见 7.5）")
    @PostMapping("/api/articles/{articleId}/comments")
    public Result<Long> create(@PathVariable Long articleId,
                               @Valid @RequestBody CommentCreateReq req,
                               HttpServletRequest request) {
        return Result.ok(commentService.createComment(articleId, req, clientIp(request)));
    }

    @Operation(summary = "删除评论（本人或 ADMIN）")
    @DeleteMapping("/api/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
