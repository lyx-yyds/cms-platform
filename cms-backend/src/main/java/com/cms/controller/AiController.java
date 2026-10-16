package com.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.Result;
import com.cms.dto.request.AiEnhanceReq;
import com.cms.dto.request.AiWriteReq;
import com.cms.dto.response.AiEnhanceResp;
import com.cms.entity.AiGenerationRecord;
import com.cms.service.AiGenerateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 智能接口（AiController.java，文档 5.1 AI 智能接口 / 7.4，创作角色可用：ADMIN/EDITOR/AUTHOR）
 */
@Tag(name = "AI 智能接口")
@RestController("aiController")
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ai:use')")
public class AiController {

    private final AiGenerateService aiGenerateService;

    @Operation(summary = "生成文章草稿（SSE 流式）")
    @PostMapping(value = "/write/draft", produces = "text/event-stream")
    public SseEmitter writeDraft(@RequestBody AiWriteReq req) {
        return aiGenerateService.write("draft", req);
    }

    @Operation(summary = "续写文章（SSE 流式）")
    @PostMapping(value = "/write/continue", produces = "text/event-stream")
    public SseEmitter writeContinue(@RequestBody AiWriteReq req) {
        return aiGenerateService.write("continue", req);
    }

    @Operation(summary = "润色文章（SSE 流式）")
    @PostMapping(value = "/write/polish", produces = "text/event-stream")
    public SseEmitter writePolish(@RequestBody AiWriteReq req) {
        return aiGenerateService.write("polish", req);
    }

    @Operation(summary = "生成摘要与 SEO（同步）")
    @PostMapping("/article/enhance")
    public Result<AiEnhanceResp> enhance(@RequestBody AiEnhanceReq req) {
        return Result.ok(aiGenerateService.enhance(req));
    }

    @Operation(summary = "我的生成记录（分页）")
    @GetMapping("/generations")
    public Result<Page<AiGenerationRecord>> generations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(aiGenerateService.generations(page, size));
    }

    @Operation(summary = "单条生成记录详情（仅本人，用于续编辑保存）")
    @GetMapping("/generations/{id}")
    public Result<AiGenerationRecord> generationDetail(@PathVariable Long id) {
        return Result.ok(aiGenerateService.getGeneration(id));
    }

    @Operation(summary = "绑定生成记录到文章（保存后回填 article_id）")
    @PostMapping("/generations/{id}/bind")
    public Result<Void> bindGeneration(@PathVariable Long id, @RequestParam Long articleId) {
        aiGenerateService.bindArticle(id, articleId);
        return Result.ok();
    }
}
