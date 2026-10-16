package com.cms.dto.response;

import lombok.Data;

/**
 * AI 摘要 + SEO 响应（文档 7.4：摘要与 SEO 自动生成）
 */
@Data
public class AiEnhanceResp {

    private String summary;
    private String seoTitle;
    private String seoDescription;
    private String seoKeywords;
}
