package com.cms.dto.request;

import lombok.Data;

/**
 * AI 摘要 + SEO 请求（文档 5.1 /api/ai/article/enhance）
 */
@Data
public class AiEnhanceReq {

    /** 文章标题 */
    private String title;

    /** 文章正文（用于摘要与 SEO 生成） */
    private String content;
}
