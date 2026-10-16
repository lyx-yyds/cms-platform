package com.cms.dto.request;

import lombok.Data;

/**
 * AI 写作请求（draft/continue/polish 通用，文档 5.1）
 */
@Data
public class AiWriteReq {

    /** 关联文章 ID（选填） */
    private Long articleId;

    /** 草稿：标题 */
    private String title;

    /** 草稿：关键词（逗号分隔） */
    private String keywords;

    /** 草稿：大纲（选填，多行） */
    private String outline;

    /** 续写/润色：已有内容 */
    private String content;

    /** 续写/润色：附加指令（选填，如"以更口语化的风格"） */
    private String instruction;
}
