package com.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体（cms_comment 表，4.2.10；ai_status 机审状态见 7.5）
 */
@Data
@TableName("cms_comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long userId;

    /** 父评论ID（0=顶级评论） */
    private Long parentId;

    /** 被回复用户ID */
    private Long replyUserId;

    private String content;

    /** 0-待审核 1-已通过 2-已拒绝 */
    private Integer status;

    /** AI 机审状态：0-未机审 1-机审通过 2-转人工 3-机审拒绝 */
    private Integer aiStatus;

    private String ipAddress;

    private LocalDateTime createTime;
}
