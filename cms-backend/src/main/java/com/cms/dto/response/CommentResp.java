package com.cms.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评论响应（CommentResp，含 AI 机审明细，文档 7.5）
 */
@Data
public class CommentResp {

    private Long id;
    private Long articleId;
    private String articleTitle;
    private Long userId;
    private String userNickname;
    private Long parentId;
    private Long replyUserId;
    private String replyNickname;
    private String content;
    /** 0-待审核 1-已通过 2-已拒绝 */
    private Integer status;
    /** AI 机审状态：0-未机审 1-机审通过 2-转人工 3-机审拒绝 */
    private Integer aiStatus;
    private String ipAddress;
    private LocalDateTime createTime;

    // ===== AI 机审明细（来自 ai_review_record）=====
    /** 机审结果：normal/ad/repeat/abuse/sensitive */
    private String aiResult;
    /** 违规置信度（0-100） */
    private BigDecimal aiScore;
    /** 机审原因 */
    private String aiReason;
}
