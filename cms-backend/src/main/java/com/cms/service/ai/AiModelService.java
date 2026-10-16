package com.cms.service.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 模型路由（文档 7.4/7.5：DashScope 主选 / Ollama 兜底）。
 *
 * 当前环境：DashScope starter 不在 Maven Central（依赖注释保留，见附录 B 与 pom.xml），
 * 生效提供方为本地 Ollama（spring.ai.ollama.base-url + model qwen2.5:3b）。
 * 若后续引入 DashScope starter，将同时存在两个 ChatModel Bean，需在此处按优先级选取。
 */
@Component
public class AiModelService {

    private final ObjectProvider<ChatModel> chatModelProvider;

    @Value("${spring.ai.ollama.chat.options.model:qwen2.5:3b}")
    private String ollamaModel;

    public AiModelService(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    /** 当前可用模型；无任何模型提供方时返回 null（上层降级） */
    public ChatModel getModel() {
        return chatModelProvider.getIfUnique();
    }

    /** 当前生效模型名（成本审计记录用） */
    public String modelName() {
        return ollamaModel;
    }

    public boolean available() {
        return getModel() != null;
    }
}
