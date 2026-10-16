package com.cms.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 修改文章请求（ArticleUpdateReq）
 */
@Data
public class ArticleUpdateReq {

    private String title;

    private String content;

    private String summary;

    private String coverImage;

    private Long categoryId;

    /** 0-草稿 1-已发布 2-已下架 */
    private Integer status;

    private Boolean isTop;

    private Boolean isRecommend;

    private String seoTitle;

    private String seoDescription;

    private String seoKeywords;

    private List<Long> tagIds;
}
