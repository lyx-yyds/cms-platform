package com.cms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.dto.request.AiEnhanceReq;
import com.cms.dto.request.AiWriteReq;
import com.cms.dto.response.AiEnhanceResp;
import com.cms.entity.AiGenerationRecord;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 写作助手服务（文档 7.4：草稿/续写/润色 SSE 流式 + 摘要 SEO + 生成记录）
 */
public interface AiGenerateService {

    /** 流式生成：draft/continue/polish，返回 SSE 发射器 */
    SseEmitter write(String type, AiWriteReq req);

    /** 摘要 + SEO 同步生成 */
    AiEnhanceResp enhance(AiEnhanceReq req);

    /** 我的生成记录分页 */
    Page<AiGenerationRecord> generations(int page, int size);

    /** 单条生成记录详情（仅本人） */
    AiGenerationRecord getGeneration(Long id);

    /** 将生成记录绑定到已保存的文章（仅本人，回填 article_id） */
    void bindArticle(Long id, Long articleId);
}
