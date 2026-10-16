package com.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.dto.request.CommentCreateReq;
import com.cms.dto.response.CommentResp;

import java.util.List;

/**
 * 评论服务（文档 5.1 / 7.5）
 */
public interface CommentService {

    /** 管理端评论分页（含机审明细） */
    Page<CommentResp> pageComments(int page, int size, Long articleId, Integer status, Integer aiStatus, String keyword);

    /** 文章评论列表（公开，仅已通过） */
    List<CommentResp> listApprovedByArticle(Long articleId);

    /** 发表评论（status=0 入库 → 异步触发 AI 初审） */
    Long createComment(Long articleId, CommentCreateReq req, String ipAddress);

    /** 删除评论（本人或 ADMIN） */
    void deleteComment(Long id);

    /** 人工复核（ADMIN 覆盖机审结果） */
    void auditComment(Long id, Integer status);
}
