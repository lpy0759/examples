package org.pyliu.learn.observability;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.TokenUsage;

import java.util.function.Supplier;

/**
 * 模块 13：可观测性与健壮性 —— 日志排查、token 统计、异常处理与重试。
 *
 * <p>模型调用进生产后，三个问题必须能回答：
 * ① 到底给模型发了什么、它回了什么？（日志）② 花了多少 token？（成本）③ 出错了怎么办？（重试）。
 */
public class Main {

    public static void main(String[] args) {
        // 打开请求/响应日志（配合 src/main/resources/simplelogger.properties 的 DEBUG 级别生效）
        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(requireKey())
                .modelName("deepseek-chat")
                .logRequests(true)
                .logResponses(true)
                .build();

        // 用"带重试的调用"包装：遇到瞬时网络/限流错误会自动重试
        ChatResponse response = withRetry(3, () -> model.chat(
                new SystemMessage("你是一个只讲重点的助手。"),
                UserMessage.from("用一句话介绍 Docker")
        ));

        System.out.println("回答：" + response.aiMessage().text());

        // token 统计：用于成本核算与额度监控
        TokenUsage usage = response.tokenUsage();
        System.out.println("输入 token：" + usage.inputTokenCount());
        System.out.println("输出 token：" + usage.outputTokenCount());
        System.out.println("总 token：  " + usage.totalTokenCount());
    }

    /**
     * 简单重试封装：执行失败就重试，最多 maxAttempts 次，失败间隔递增（退避）。
     * 生产环境可用更完善的库（如 Resilience4j），这里展示核心思想。
     */
    private static <T> T withRetry(int maxAttempts, Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    throw e;
                }
                System.out.printf("第 %d 次调用失败（%s），1 秒后重试...%n", attempt, e.getMessage());
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
    }

    private static String requireKey() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置环境变量 DEEPSEEK_API_KEY");
        }
        return apiKey;
    }
}
