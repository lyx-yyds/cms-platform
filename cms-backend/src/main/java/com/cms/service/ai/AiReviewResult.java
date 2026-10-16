package com.cms.service.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 机审结果（结构化 JSON 契约，文档 7.5：{"result": "...", "score": 0.9, "reason": "..."}）
 */
@Data
@Builder
public class AiReviewResult {

    /** 分类：normal正常 / ad广告 / repeat刷屏 / abuse辱骂 / sensitive敏感 */
    private String result;

    /** 违规置信度（0.00 - 1.00） */
    private BigDecimal score;

    /** 机审原因（供管理页展示与人工复核参考） */
    private String reason;

    /** 使用的模型标识（如 rule-based-v1 / qwen-plus） */
    private String model;
}
