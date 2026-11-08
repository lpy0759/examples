package org.pyliu.langgraph4j.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;

/**
 * 一个确定性的「假 LLM」，实现了 LangChain4j 的 {@link ChatModel} 接口。
 *
 * <p>它不调用任何外部服务，而是根据对话历史返回固定结果：
 * <ul>
 *   <li>还没执行过工具时 → 返回一个「调用 get_weather 工具」的 {@link AiMessage}；</li>
 *   <li>已经拿到工具结果时 → 返回一段最终回答。</li>
 * </ul>
 *
 * <p>这样就能在完全离线的情况下，真实走一遍「LLM → 工具 → LLM → 结束」的 Agent 循环。
 * 实现 {@link ChatModel} 只需重写 {@link #doChat(ChatRequest)} 一个方法。
 */
public class FakeChatModel implements ChatModel {

    @Override
    public ChatResponse doChat(ChatRequest request) {
        List<ChatMessage> messages = request.messages();

        // 判断历史里是否已经有工具执行结果：有 → 该给最终回答了
        boolean hasToolResult = messages.stream()
                .anyMatch(m -> m instanceof ToolExecutionResultMessage);

        if (!hasToolResult) {
            // 第一轮：返回一个工具调用请求
            ToolExecutionRequest toolCall = ToolExecutionRequest.builder()
                    .id("call_1")
                    .name("get_weather")
                    .arguments("{\"city\":\"北京\"}")
                    .build();
            return ChatResponse.builder()
                    .aiMessage(AiMessage.from(toolCall))
                    .build();
        }

        // 第二轮：基于工具结果给出最终回答
        return ChatResponse.builder()
                .aiMessage(AiMessage.from("根据工具结果：北京今天晴朗，25°C，适合出行。"))
                .build();
    }
}
