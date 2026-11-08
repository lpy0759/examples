package org.pyliu.langgraph4j.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 模块 07：AI Agent（LLM + 工具调用的循环）。
 *
 * <p>把前面的知识组合起来，做出经典的 ReAct Agent：
 * <pre>
 *   START → agent ─(LLM 决定)─┬─ 要调工具 → tools ─┐
 *              └──────────────┴─ 直接结束 → END ←──┘
 * </pre>
 * 关键点：
 * <ol>
 *   <li>状态里存 {@code List<ChatMessage>}，用 appender 通道累积对话；</li>
 *   <li>{@code agent} 节点调用 LLM，把返回的 {@link AiMessage} 追加进对话；</li>
 *   <li>条件边检查最后一条消息：若它请求调用工具 → 去 {@code tools}，否则 → END；</li>
 *   <li>{@code tools} 节点执行工具，把 {@link ToolExecutionResultMessage} 追加回对话，再回到 agent。</li>
 * </ol>
 *
 * <p>模型默认用离线的 {@link FakeChatModel}（见 {@link ModelFactory} 切换到真实模型）。
 */
public class Main {

    /** 模拟的工具实现：根据工具名返回固定结果。 */
    static String executeTool(ToolExecutionRequest request) {
        return switch (request.name()) {
            case "get_weather" -> "北京：晴朗，25°C（模拟结果）";
            case "get_time" -> "当前时间 10:00（模拟结果）";
            default -> "未知工具: " + request.name();
        };
    }

    public static void main(String[] args) throws Exception {
        ChatModel model = ModelFactory.create();
        System.out.println("使用模型: " + model.getClass().getSimpleName());

        // 状态 schema：messages 用 appender 通道累积对话
        Map<String, Channel<?>> channels = Map.of(
                "messages", Channels.<ChatMessage>appender(ArrayList::new));
        StateGraph<AgentState> graph = new StateGraph<>(channels, AgentState::new);

        // agent 节点：把当前对话交给 LLM，把返回的 AiMessage 追加进状态
        AsyncNodeAction<AgentState> agent = state -> {
            List<ChatMessage> messages = state.<List<ChatMessage>>value("messages").orElse(List.of());
            System.out.println("[agent] 调用 LLM（当前对话 " + messages.size() + " 条）");
            ChatResponse response = model.chat(ChatRequest.builder().messages(messages).build());
            AiMessage ai = response.aiMessage();
            if (ai.hasToolExecutionRequests()) {
                System.out.println("[agent] LLM 请求调用工具: "
                        + ai.toolExecutionRequests().get(0).name());
            } else {
                System.out.println("[agent] LLM 最终回答: " + ai.text());
            }
            return CompletableFuture.completedFuture(Map.of("messages", ai));
        };

        // tools 节点：执行上一条 AiMessage 里的工具请求，返回结果消息
        AsyncNodeAction<AgentState> tools = state -> {
            List<ChatMessage> messages = state.<List<ChatMessage>>value("messages").orElse(List.of());
            AiMessage lastAi = (AiMessage) messages.get(messages.size() - 1);
            List<ChatMessage> results = new ArrayList<>();
            for (ToolExecutionRequest req : lastAi.toolExecutionRequests()) {
                String result = executeTool(req);
                System.out.println("[tools] 执行工具 " + req.name() + " -> " + result);
                results.add(ToolExecutionResultMessage.from(req, result));
            }
            return CompletableFuture.completedFuture(Map.of("messages", results));
        };

        graph.addNode("agent", agent);
        graph.addNode("tools", tools);

        // 条件边：最后一条消息若还在请求工具 → tools，否则结束
        AsyncEdgeAction<AgentState> router = state -> {
            List<ChatMessage> messages = state.<List<ChatMessage>>value("messages").orElse(List.of());
            ChatMessage last = messages.get(messages.size() - 1);
            boolean callTool = last instanceof AiMessage am && am.hasToolExecutionRequests();
            return CompletableFuture.completedFuture(callTool ? "tools" : "end");
        };
        graph.addConditionalEdges("agent", router,
                Map.of("tools", "tools", "end", StateGraph.END));

        graph.addEdge(StateGraph.START, "agent");
        graph.addEdge("tools", "agent"); // 工具执行完回到 agent，形成循环

        var compiled = graph.compile();

        // 初始对话：系统提示 + 用户问题
        List<ChatMessage> seed = List.of(
                new SystemMessage("你是乐于助人的助手，需要查天气时请调用 get_weather 工具。"),
                new UserMessage("北京今天天气怎么样？"));

        // 注意：ChatMessage 未实现 Serializable，这里 disableCloneState 避免状态被
        // Java 序列化克隆；生产环境可改用 langgraph4j-langchain4j 提供的序列化器。
        RunnableConfig config = RunnableConfig.builder()
                .disableCloneState()
                .recursionLimit(10)
                .build();

        System.out.println("\n===== 运行 Agent =====");
        AgentState result = compiled
                .invoke(GraphInput.args(Map.of("messages", seed)), config)
                .orElseThrow();

        System.out.println("\n===== 最终对话 =====");
        List<ChatMessage> messages = result.<List<ChatMessage>>value("messages").orElse(List.of());
        for (ChatMessage m : messages) {
            String who = m instanceof AiMessage ? "AI"
                    : m instanceof UserMessage ? "用户"
                    : m instanceof SystemMessage ? "系统"
                    : "工具";
            System.out.println("[" + who + "] " + m);
        }
    }
}
