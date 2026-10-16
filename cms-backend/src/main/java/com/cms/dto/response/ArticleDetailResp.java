package com.cms.dto.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章详情（ArticleDetailResp = 列表项 + 正文 + SEO + 点赞状态）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleDetailResp extends ArticleResp {

    /** 文章内容（已清洗 HTML） */
    private String content;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
    /** 当前用户是否已点赞 */
    private Boolean liked;
}
