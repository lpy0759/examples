package org.pyliu.langgraph4j.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 模型工厂：默认用 {@link FakeChatModel}（离线、可复现），
 * 通过环境变量可切换到真实的 OpenAI 模型。
 *
 * <p>切换方式（需要 API Key）：
 * <pre>
 *   export LLM_PROVIDER=openai
 *   export OPENAI_API_KEY=sk-...
 *   export OPENAI_MODEL_NAME=gpt-4o-mini   # 可选，默认 gpt-4o-mini
 * </pre>
 */
public final class ModelFactory {

    private ModelFactory() {
    }

    public static ChatModel create() {
        String provider = System.getenv().getOrDefault("LLM_PROVIDER", "mock");

        if ("openai".equalsIgnoreCase(provider)) {
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "已设置 LLM_PROVIDER=openai，但缺少 OPENAI_API_KEY 环境变量");
            }
            String modelName = System.getenv().getOrDefault("OPENAI_MODEL_NAME", "gpt-4o-mini");
            return OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .build();
        }

        // 默认：离线假模型
        return new FakeChatModel();
    }
}
