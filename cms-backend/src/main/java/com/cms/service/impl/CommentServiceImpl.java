package com.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.BusinessException;
import com.cms.common.Constants;
import com.cms.common.ResultCode;
import com.cms.common.SanitizeUtil;
import com.cms.dto.request.CommentCreateReq;
import com.cms.dto.response.CommentResp;
import com.cms.entity.AiReviewRecord;
import com.cms.entity.Article;
import com.cms.entity.Comment;
import com.cms.mapper.AiReviewRecordMapper;
import com.cms.mapper.ArticleMapper;
import com.cms.mapper.CommentMapper;
import com.cms.security.SecurityUtils;
import com.cms.service.CommentReviewService;
import com.cms.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 评论服务实现（文档 5.1 / 7.3 数据级权限 / 7.5 机审）
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final AiReviewRecordMapper aiReviewRecordMapper;
    private final ArticleMapper articleMapper;
    private final CommentReviewService commentReviewService;

    @Override
    public Page<CommentResp> pageComments(int page, int size, Long articleId, Integer status, Integer aiStatus, String keyword) {
        Page<CommentResp> result = commentMapper.selectCommentPage(new Page<>(page, size), articleId, status, aiStatus, keyword);
        fillAiDetail(result.getRecords());
        return result;
    }

    @Override
    public List<CommentResp> listApprovedByArticle(Long articleId) {
        List<CommentResp> list = commentMapper.selectApprovedByArticle(articleId);
        fillAiDetail(list);
        return list;
    }

    @Override
    @Transactional
    public Long createComment(Long articleId, CommentCreateReq req, String ipAddress) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(SecurityUtils.getCurrentUserId());
        comment.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        comment.setReplyUserId(req.getReplyUserId());
        // 评论内容同样过 XSS 白名单清洗（文档 7.8）
        comment.setContent(SanitizeUtil.sanitizeHtml(req.getContent()));
        comment.setStatus(Constants.COMMENT_STATUS_PENDING);
        comment.setAiStatus(Constants.AI_STATUS_NONE);
        comment.setIpAddress(ipAddress);
        commentMapper.insert(comment);
        // 异步触发 AI 初审（文档 7.5）：
        // 必须等事务提交后再触发，否则异步线程读不到未提交的评论（REPEATABLE READ 隔离级别）
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        commentReviewService.reviewAsync(comment.getId());
                    }
                });
        return comment.getId();
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        Long current = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        if (!isAdmin && !current.equals(comment.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己的评论");
        }
        commentMapper.deleteById(id);
        // 级联清理机审明细（按 comment_id）
        aiReviewRecordMapper.deleteByCommentId(id);
    }

    @Override
    @Transactional
    public void auditComment(Long id, Integer status) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        // 人工复核覆盖机审结果（文档 7.5 第 5 条）：仅覆盖 status，保留 ai_status 机审历史
        comment.setStatus(status);
        commentMapper.updateById(comment);
    }

    /** 填充 AI 机审明细（ai_review_record 最新一条） */
    private void fillAiDetail(List<CommentResp> list) {
        for (CommentResp resp : list) {
            AiReviewRecord record = aiReviewRecordMapper.selectLatestByCommentId(resp.getId());
            if (record != null) {
                resp.setAiResult(record.getReviewResult());
                resp.setAiScore(record.getScore() == null ? null : record.getScore().multiply(java.math.BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP));
                resp.setAiReason(buildReason(record.getReviewResult(), record.getScore()));
            }
        }
    }

    private String buildReason(String result, java.math.BigDecimal score) {
        if (result == null) {
            return null;
        }
        String label = switch (result) {
            case "normal" -> "正常";
            case "ad" -> "广告";
            case "repeat" -> "刷屏";
            case "abuse" -> "辱骂";
            case "sensitive" -> "敏感";
            default -> result;
        };
        return score == null ? label : label + "（违规置信度 " + score.multiply(java.math.BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP) + "%）";
    }
}
