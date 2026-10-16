package com.cms.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Spring AI 评论机审（第七阶段实现，文档 7.5）。
 * 调用真实模型（DashScope/Ollama）分类评论并返回结构化 JSON；
 * 模型不可用 / 调用失败 / 解析失败 → 降级规则机审（保证不漏审）。
 */
@Slf4j
@Service("springAiReviewService")
@Primary
@RequiredArgsConstructor
public class SpringAiReviewService implements AiReviewService {

    private static final Set<String> VALID_RESULTS = Set.of("normal", "ad", "repeat", "abuse", "sensitive");

    private final AiModelService aiModelService;
    private final RuleBasedAiReviewService ruleBasedAiReviewService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.ollama.chat.options.temperature:0.7}")
    private double temperature;

    @Override
    public AiReviewResult review(String content) {
        ChatModel model = aiModelService.getModel();
        if (model == null) {
            return ruleBasedAiReviewService.review(content);
        }
        try {
            ChatClient client = ChatClient.builder(model).build();
            String resp = client.prompt()
                    .system(loadPrompt("review.st"))
                    .user(content)
                    .call()
                    .content();
            AiReviewResult parsed = parseJson(resp);
            if (parsed != null) {
                return parsed;
            }
            log.warn("AI 机审返回无法解析: {}", resp == null ? "null" : resp.substring(0, Math.min(resp.length(), 120)));
        } catch (Exception e) {
            log.warn("AI 机审调用失败，降级规则机审: {}", e.getMessage());
        }
        // 降级：规则机审（model 标注降级来源）
        AiReviewResult fallback = ruleBasedAiReviewService.review(content);
        return AiReviewResult.builder()
                .result(fallback.getResult())
                .score(fallback.getScore())
                .reason(fallback.getReason() + "（模型降级）")
                .model(ruleBasedAiReviewService.modelName())
                .build();
    }

    /** 解析模型返回的 JSON {"result","score","reason"}；非法返回 null */
    private AiReviewResult parseJson(String resp) {
        if (!StringUtils.hasText(resp)) {
            return null;
        }
        try {
            String json = resp.trim();
            // 兼容模型偶尔用 Markdown 代码块包裹 JSON
            if (json.startsWith("```")) {
                json = json.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
            }
            JsonNode node = objectMapper.readTree(json);
            String result = node.path("result").asText("");
            if (!VALID_RESULTS.contains(result)) {
                return null;
            }
            BigDecimal score = BigDecimal.valueOf(node.path("score").asDouble(0.5))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            String reason = node.path("reason").asText("AI 机审");
            return AiReviewResult.builder()
                    .result(result)
                    .score(score)
                    .reason(reason)
                    .model(aiModelService.modelName())
                    .build();
        } catch (Exception e) {
            log.debug("AI 机审 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String loadPrompt(String name) {
        try {
            return new ClassPathResource("prompts/" + name).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Prompt 模板加载失败: " + name, e);
        }
    }
}
