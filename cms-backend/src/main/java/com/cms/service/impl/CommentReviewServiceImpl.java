package com.cms.service.impl;

import com.cms.common.Constants;
import com.cms.entity.AiReviewRecord;
import com.cms.entity.Comment;
import com.cms.mapper.AiReviewRecordMapper;
import com.cms.mapper.CommentMapper;
import com.cms.service.CommentReviewService;
import com.cms.service.ai.AiReviewResult;
import com.cms.service.ai.AiReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 评论机审编排实现（文档 7.5）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentReviewServiceImpl implements CommentReviewService {

    private final CommentMapper commentMapper;
    private final AiReviewRecordMapper aiReviewRecordMapper;
    private final AiReviewService aiReviewService;

    @Override
    @Async("commentReviewExecutor")
    public void reviewAsync(Long commentId) {
        try {
            reviewSync(commentId);
        } catch (Exception e) {
            log.warn("评论机审异步执行失败，降级转人工 commentId={}: {}", commentId, e.getMessage());
            downgradeToManual(commentId);
        }
    }

    /** 评论机审同步执行 */
    @Override
    @Transactional
    public AiReviewResult reviewSync(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在: " + commentId);
        }
        // 1. 调用机审（第四阶段规则实现；第七阶段替换为真实模型）
        AiReviewResult result = aiReviewService.review(comment.getContent());

        // 2. 落 ai_review_record（内容快照 + 结果 + 置信度，支持审计与召回分析）
        AiReviewRecord record = new AiReviewRecord();
        record.setCommentId(commentId);
        record.setContent(comment.getContent());
        record.setReviewResult(result.getResult());
        record.setScore(result.getScore());
        record.setModel(result.getModel());
        record.setStatus(1);
        aiReviewRecordMapper.insert(record);

        // 3. 自动流转（文档 7.5 第 4 条）
        applyFlow(comment, result);
        return result;
    }

    /**
     * 自动流转规则：
     * - normal（置信度高）→ status=1 已通过，前端可见
     * - repeat → 转人工（status=0）
     * - ad/abuse/sensitive：score ≥ 0.9 → status=2 拒绝；否则 status=0 转人工
     */
    private void applyFlow(Comment comment, AiReviewResult result) {
        String r = result.getResult();
        BigDecimal score = result.getScore();
        if ("normal".equals(r)) {
            // score 统一为违规置信度：低分（<0.5）才自动通过；label=normal 但违规置信度偏高（模型自相矛盾）→ 转人工兜底
            if (score != null && score.compareTo(new BigDecimal("0.5")) >= 0) {
                comment.setStatus(Constants.COMMENT_STATUS_PENDING);
                comment.setAiStatus(Constants.AI_STATUS_REVIEW);
            } else {
                comment.setStatus(Constants.COMMENT_STATUS_PASSED);
                comment.setAiStatus(Constants.AI_STATUS_PASS);
            }
        } else if ("repeat".equals(r)) {
            comment.setStatus(Constants.COMMENT_STATUS_PENDING);
            comment.setAiStatus(Constants.AI_STATUS_REVIEW);
        } else if (score != null && score.compareTo(new BigDecimal("0.9")) >= 0) {
            comment.setStatus(Constants.COMMENT_STATUS_REJECTED);
            comment.setAiStatus(Constants.AI_STATUS_REJECT);
        } else {
            comment.setStatus(Constants.COMMENT_STATUS_PENDING);
            comment.setAiStatus(Constants.AI_STATUS_REVIEW);
        }
        commentMapper.updateById(comment);
    }

    /** 异常降级：保持待审核、转人工（文档 7.5 第 2 条：解析失败/超时 → status=0） */
    private void downgradeToManual(Long commentId) {
        try {
            Comment comment = commentMapper.selectById(commentId);
            if (comment != null) {
                comment.setStatus(Constants.COMMENT_STATUS_PENDING);
                comment.setAiStatus(Constants.AI_STATUS_REVIEW);
                commentMapper.updateById(comment);
            }
        } catch (Exception ex) {
            log.error("评论降级转人工失败 commentId={}", commentId, ex);
        }
    }
}
