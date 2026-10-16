package com.cms.service;

import com.cms.service.ai.AiReviewResult;

/**
 * 评论机审编排（文档 7.5）：
 * 异步执行机审 → 落 ai_review_record → 按规则自动流转 cms_comment.status / ai_status。
 */
public interface CommentReviewService {

    /**
     * 异步机审（@Async commentReviewExecutor）；解析失败/超时降级为转人工（不漏审，文档 7.5 第 2 条）
     */
    void reviewAsync(Long commentId);

    /**
     * 同步机审（供联调/测试直接调用）
     */
    AiReviewResult reviewSync(Long commentId);
}
