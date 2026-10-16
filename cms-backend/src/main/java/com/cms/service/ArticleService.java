package com.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.dto.request.ArticleCreateReq;
import com.cms.dto.request.ArticleUpdateReq;
import com.cms.dto.response.ArticleDetailResp;
import com.cms.dto.response.ArticleResp;

/**
 * 文章服务（文档 7.2 发布流程 / 7.3 数据级权限 / 7.7 浏览量点赞）
 */
public interface ArticleService {

    /** 文章分页（status 传 null 查全部；authorId 传 null 查所有作者） */
    Page<ArticleResp> pageArticles(int page, int size, String keyword, Long categoryId, Integer status, Long authorId);

    /** 文章详情（浏览量 Redis INCR；草稿仅作者或 ADMIN 可见） */
    ArticleDetailResp getArticleDetail(Long id);

    /** 创建文章（XSS 清洗；作者=当前用户） */
    Long createArticle(ArticleCreateReq req);

    /** 修改文章（仅作者或 ADMIN，文档 7.3） */
    void updateArticle(Long id, ArticleUpdateReq req);

    /** 删除文章（逻辑删除；仅作者或 ADMIN） */
    void deleteArticle(Long id);

    /** 点赞（SADD 幂等，首次点赞 DB+1） */
    void likeArticle(Long id);

    /** 取消点赞（SREM，移除成功 DB-1） */
    void unlikeArticle(Long id);
}
