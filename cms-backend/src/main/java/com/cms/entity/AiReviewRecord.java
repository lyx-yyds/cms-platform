package com.cms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 评论审核记录（ai_review_record 表，4.2.14；文档 7.5 审计与召回分析）
 */
@Data
@TableName("ai_review_record")
public class AiReviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long commentId;

    /** 评论内容快照 */
    private String content;

    /** 机审结果：normal/ad/repeat/abuse/sensitive */
    private String reviewResult;

    /** 违规置信度 */
    private BigDecimal score;

    /** 模型（第四阶段 rule-based-v1；第七阶段替换为 qwen-plus 等） */
    private String model;

    /** 1-机审完成 */
    private Integer status;

    private LocalDateTime createTime;
}
