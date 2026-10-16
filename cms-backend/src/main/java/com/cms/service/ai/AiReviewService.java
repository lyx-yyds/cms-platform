package com.cms.service.ai;

/**
 * AI 评论初审服务（文档 7.5）。
 * 第四阶段使用规则实现（RuleBasedAiReviewService）；
 * 第七阶段接入 Spring AI / DashScope 后，以同接口替换实现，调用方不变。
 */
public interface AiReviewService {

    AiReviewResult review(String content);
}
