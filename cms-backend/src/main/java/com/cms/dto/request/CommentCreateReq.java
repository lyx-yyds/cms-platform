package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求（CommentCreateReq，触发 AI 初审 7.5）
 */
@Data
public class CommentCreateReq {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过 500 字")
    private String content;

    /** 父评论ID（0=顶级评论） */
    private Long parentId;

    /** 被回复用户ID */
    private Long replyUserId;
}
