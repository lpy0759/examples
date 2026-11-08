package org.pyliu.learn.hello;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 模块 01：Hello World —— 跑通第一个对话。
 *
 * <p>LangChain4j 的三层架构（模型无关性就体现在这里）：
 * <ul>
 *   <li><b>模型层</b>：统一抽象 {@link ChatModel}（文本对话）、{@code EmbeddingModel}（向量）、{@code ImageModel} 等。</li>
 *   <li><b>集成层</b>：各厂商的具体实现，例如 {@link OpenAiChatModel}（本模块用它连 DeepSeek）。</li>
 *   <li><b>工具层</b>：AiServices、记忆、RAG、工具调用等高级能力（后续模块逐一讲解）。</li>
 * </ul>
 *
 * <p>关键点：{@link OpenAiChatModel} 是 OpenAI 兼容客户端，只要把 {@code baseUrl}
 * 指向任意 OpenAI 兼容服务，就能无缝切换到 DeepSeek / 通义千问 / 本地 Ollama 等，
 * <b>业务代码完全不用改</b>。
 */
public class Main {

    public static void main(String[] args) {
        // 1. 从环境变量读取 API Key（不写死在代码里）
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("请先设置环境变量：export DEEPSEEK_API_KEY=sk-xxx");
            System.exit(1);
        }

        // 2. 构建一个 ChatModel：把 OpenAI 兼容客户端指向 DeepSeek
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://maas.qianwenaiapi.com/compatible-mode/v1")   // DeepSeek 的 OpenAI 兼容地址
                .apiKey(apiKey)
                .modelName("qwen3.8-max")            // 对话模型
                // 打开请求/响应日志，方便观察底层发生了什么（默认走 slf4j，DEBUG 级别）
                .logRequests(true)
                .logResponses(true)
                .build();

        // 3. 发送一条消息，拿到回答（chat(String) 是最简单的同步调用）
        String answer = model.chat("请用一句话介绍 LangChain4j 是什么");

        System.out.println("模型回答：" + answer);
    }
}
