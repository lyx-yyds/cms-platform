package com.cms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cms.common.BusinessException;
import com.cms.common.ResultCode;
import com.cms.dto.request.AiEnhanceReq;
import com.cms.dto.request.AiWriteReq;
import com.cms.dto.response.AiEnhanceResp;
import com.cms.entity.AiGenerationRecord;
import com.cms.entity.User;
import com.cms.mapper.AiGenerationRecordMapper;
import com.cms.mapper.UserMapper;
import com.cms.security.SecurityUtils;
import com.cms.service.AiGenerateService;
import com.cms.service.ai.AiModelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 写作助手实现（文档 7.4）。
 * 模型调用走 Spring AI ChatClient；SSE 流式逐字下发；结果落库 ai_generation_record（成本审计）；
 * 无模型可用 → 2001 AI 生成失败；超限 → 2002。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerateServiceImpl implements AiGenerateService {

    private static final int MAX_CONTENT_CHARS = 4000;

    private final AiModelService aiModelService;
    private final AiGenerationRecordMapper recordMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rate-limit.ai-per-minute:5}")
    private int aiRateLimit;

    @Override
    public SseEmitter write(String type, AiWriteReq req) {
        rateLimit();
        ChatModel model = requireModel();
        String templateName = switch (type) {
            case "continue" -> "continue.st";
            case "polish" -> "polish.st";
            default -> "draft.st";
        };
        String rendered = render(templateName, buildVars(type, req));

        AiGenerationRecord record = new AiGenerationRecord();
        record.setUserId(SecurityUtils.getCurrentUserId());
        record.setArticleId(req.getArticleId());
        record.setType(type);
        record.setPrompt(rendered.length() > 2000 ? rendered.substring(0, 2000) : rendered);
        record.setModel(aiModelService.modelName());
        record.setStatus(0);
        record.setTokensUsed(0);
        recordMapper.insert(record);
        Long recordId = record.getId();

        SseEmitter emitter = new SseEmitter(0L);
        StringBuilder sb = new StringBuilder();
        int[] tokens = {0};

        ChatClient client = ChatClient.builder(model).build();
        Flux<ChatResponse> flux;
        try {
            flux = client.prompt().user(rendered).stream().chatResponse();
        } catch (Exception e) {
            recordMapper.updateById(failRecord(recordId, "模型调用初始化失败: " + e.getMessage()));
            throw new BusinessException(ResultCode.AI_GENERATION_FAILED);
        }

        flux.subscribe(
                resp -> {
                    try {
                        String text = resp.getResult().getOutput().getText();
                        if (text != null) {
                            sb.append(text);
                            emitter.send(text);
                        }
                        tokens[0] += usageTokens(resp);
                    } catch (Exception e) {
                        log.warn("SSE 发送失败: {}", e.getMessage());
                    }
                },
                err -> {
                    log.warn("AI 生成流异常: {}", err.getMessage());
                    try {
                        recordMapper.updateById(failRecord(recordId, "生成中断: " + err.getMessage()));
                    } catch (Exception ignored) {
                    }
                    emitter.completeWithError(err);
                },
                () -> {
                    try {
                        AiGenerationRecord done = new AiGenerationRecord();
                        done.setId(recordId);
                        done.setResult(sb.toString());
                        done.setTokensUsed(tokens[0]);
                        done.setStatus(1);
                        recordMapper.updateById(done);
                    } catch (Exception e) {
                        log.warn("生成记录落库失败: {}", e.getMessage());
                    }
                    emitter.complete();
                }
        );
        return emitter;
    }

    @Override
    public AiEnhanceResp enhance(AiEnhanceReq req) {
        rateLimit();
        ChatModel model = requireModel();
        if (!StringUtils.hasText(req.getTitle()) && !StringUtils.hasText(req.getContent())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "标题与正文至少提供一项");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("title", defaultIfBlank(req.getTitle(), "未命名"));
        vars.put("content", truncate(defaultIfBlank(req.getContent(), "暂无正文"), 2000));
        String rendered = render("enhance.st", vars);

        AiGenerationRecord record = new AiGenerationRecord();
        record.setUserId(SecurityUtils.getCurrentUserId());
        record.setType("seo");
        record.setPrompt(rendered.length() > 2000 ? rendered.substring(0, 2000) : rendered);
        record.setModel(aiModelService.modelName());
        record.setStatus(0);
        record.setTokensUsed(0);
        recordMapper.insert(record);

        try {
            ChatClient client = ChatClient.builder(model).build();
            ChatResponse resp = client.prompt().user(rendered).call().chatResponse();
            String content = resp.getResult().getOutput().getText();
            AiEnhanceResp out = parseEnhance(content);
            record.setId(record.getId());
            record.setResult(content);
            record.setStatus(1);
            record.setTokensUsed(usageTokens(resp));
            recordMapper.updateById(record);
            return out;
        } catch (Exception e) {
            log.warn("AI 摘要生成失败: {}", e.getMessage());
            record.setId(record.getId());
            record.setStatus(2);
            record.setErrorMsg(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 300)) : "未知错误");
            recordMapper.updateById(record);
            throw new BusinessException(ResultCode.AI_GENERATION_FAILED);
        }
    }

    @Override
    public Page<AiGenerationRecord> generations(int page, int size) {
        QueryWrapper<AiGenerationRecord> qw = new QueryWrapper<AiGenerationRecord>()
                .orderByDesc("create_time");
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        if (!isAdmin) {
            qw.eq("user_id", SecurityUtils.getCurrentUserId());
        }
        Page<AiGenerationRecord> result = recordMapper.selectPage(new Page<>(page, size), qw);
        // 管理端全量视图：批量填充用户名
        List<Long> userIds = result.getRecords().stream()
                .map(AiGenerationRecord::getUserId)
                .distinct()
                .toList();
        if (!userIds.isEmpty()) {
            Map<Long, String> nameMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getNickname));
            result.getRecords().forEach(r -> r.setUsername(nameMap.get(r.getUserId())));
        }
        return result;
    }

    @Override
    public AiGenerationRecord getGeneration(Long id) {
        AiGenerationRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "生成记录不存在");
        }
        if (!record.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该记录");
        }
        return record;
    }

    @Override
    public void bindArticle(Long id, Long articleId) {
        AiGenerationRecord record = getGeneration(id);
        AiGenerationRecord patch = new AiGenerationRecord();
        patch.setId(record.getId());
        patch.setArticleId(articleId);
        recordMapper.updateById(patch);
    }

    /* ---------- 私有 ---------- */

    private ChatModel requireModel() {
        ChatModel model = aiModelService.getModel();
        if (model == null) {
            throw new BusinessException(ResultCode.AI_GENERATION_FAILED, "AI 服务未配置（请设置 DASHSCOPE_API_KEY 或启动 Ollama）");
        }
        return model;
    }

    private void rateLimit() {
        Long userId = SecurityUtils.getCurrentUserId();
        String key = "ai:rate:" + userId + ":"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > aiRateLimit) {
            throw new BusinessException(ResultCode.AI_RATE_LIMITED, "AI 调用过于频繁，请稍后再试（每分钟上限 " + aiRateLimit + " 次）");
        }
    }

    private Map<String, Object> buildVars(String type, AiWriteReq req) {
        Map<String, Object> vars = new HashMap<>();
        if ("continue".equals(type) || "polish".equals(type)) {
            vars.put("content", truncate(defaultIfBlank(req.getContent(), ""), MAX_CONTENT_CHARS));
            vars.put("instruction_block", StringUtils.hasText(req.getInstruction())
                    ? "附加要求：" + req.getInstruction() : "");
        } else {
            vars.put("title", defaultIfBlank(req.getTitle(), "未命名标题"));
            vars.put("keywords", defaultIfBlank(req.getKeywords(), "（无）"));
            vars.put("outline_block", StringUtils.hasText(req.getOutline())
                    ? "大纲：\n" + req.getOutline() : "");
        }
        return vars;
    }

    private String render(String templateName, Map<String, Object> vars) {
        try {
            String template = new ClassPathResource("prompts/" + templateName)
                    .getContentAsString(StandardCharsets.UTF_8);
            // 模板变量渲染（{key} 占位符替换；空值替换为空串，保持模板完整性）
            String out = template;
            for (Map.Entry<String, Object> e : vars.entrySet()) {
                out = out.replace("{" + e.getKey() + "}",
                        e.getValue() == null ? "" : String.valueOf(e.getValue()));
            }
            return out;
        } catch (IOException e) {
            throw new IllegalStateException("Prompt 模板加载失败: " + templateName, e);
        }
    }

    private AiGenerationRecord failRecord(Long id, String msg) {
        AiGenerationRecord r = new AiGenerationRecord();
        r.setId(id);
        r.setStatus(2);
        r.setErrorMsg(msg != null ? msg.substring(0, Math.min(msg.length(), 300)) : "生成失败");
        return r;
    }

    private int usageTokens(ChatResponse resp) {
        try {
            var usage = resp.getMetadata().getUsage();
            if (usage == null) {
                return 0;
            }
            int p = usage.getPromptTokens() == null ? 0 : usage.getPromptTokens();
            int c = usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens();
            return p + c;
        } catch (Exception e) {
            return 0;
        }
    }

    private AiEnhanceResp parseEnhance(String resp) {
        if (!StringUtils.hasText(resp)) {
            throw new BusinessException(ResultCode.AI_GENERATION_FAILED, "模型返回为空");
        }
        try {
            String json = resp.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
            }
            JsonNode node = objectMapper.readTree(json);
            AiEnhanceResp out = new AiEnhanceResp();
            out.setSummary(node.path("summary").asText(""));
            out.setSeoTitle(node.path("seoTitle").asText(""));
            out.setSeoDescription(node.path("seoDescription").asText(""));
            out.setSeoKeywords(node.path("seoKeywords").asText(""));
            if (!StringUtils.hasText(out.getSummary()) && !StringUtils.hasText(out.getSeoTitle())) {
                throw new BusinessException(ResultCode.AI_GENERATION_FAILED, "模型返回格式无法解析");
            }
            return out;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.AI_GENERATION_FAILED, "AI 摘要解析失败");
        }
    }

    private String defaultIfBlank(String s, String def) {
        return StringUtils.hasText(s) ? s : def;
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
