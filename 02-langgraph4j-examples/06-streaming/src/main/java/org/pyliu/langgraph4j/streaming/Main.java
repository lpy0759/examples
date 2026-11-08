package org.pyliu.langgraph4j.streaming;

import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 模块 06：流式输出（Streaming）。
 *
 * <p>{@code invoke} 是「一口气跑到 END，只返回最终状态」。而 {@code stream} 是
 * <b>每执行完一个节点就吐出一次结果</b>，让你能实时观察状态如何一步步变化——
 * 这对长流程、Agent 调试、实时 UI 展示都很有用。
 *
 * <p>返回类型是 {@code AsyncGenerator<NodeOutput<State>>}，其中 {@code NodeOutput} 携带
 * 当前节点名（{@code node()}）与该时刻的状态快照（{@code state()}）。
 * 消费方式两种：
 * <ul>
 *   <li>{@code forEachAsync(consumer)}：异步逐条消费（本例）；</li>
 *   <li>{@code stream().forEach(consumer)}：把它当普通 {@code Stream} 同步消费。</li>
 * </ul>
 *
 * <p>本例：一个三步文本流水线，观察 text 逐步变化。
 */
public class Main {

    public static void main(String[] args) throws Exception {
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);

        AsyncNodeAction<AgentState> toLower = state -> {
            String t = state.<String>value("text").orElse("");
            return CompletableFuture.completedFuture(Map.of("text", t.toLowerCase()));
        };
        AsyncNodeAction<AgentState> addEmoji = state -> {
            String t = state.<String>value("text").orElse("");
            return CompletableFuture.completedFuture(Map.of("text", t + " 🚀"));
        };
        AsyncNodeAction<AgentState> wrap = state -> {
            String t = state.<String>value("text").orElse("");
            return CompletableFuture.completedFuture(Map.of("text", "[" + t + "]"));
        };

        graph.addNode("toLower", toLower);
        graph.addNode("addEmoji", addEmoji);
        graph.addNode("wrap", wrap);

        graph.addEdge(StateGraph.START, "toLower");
        graph.addEdge("toLower", "addEmoji");
        graph.addEdge("addEmoji", "wrap");
        graph.addEdge("wrap", StateGraph.END);

        var compiled = graph.compile();

        System.out.println("===== 流式观察每一步 =====");
        // 把 AsyncGenerator 转成普通 Stream，同步逐个消费（本线程拉取，最简单）
        compiled.stream(GraphInput.args(Map.of("text", "Hello World")), RunnableConfig.empty())
                .stream()
                .forEach(nodeOutput -> {
                    System.out.println("节点 <" + nodeOutput.node() + "> 之后的状态: "
                            + nodeOutput.state().data());
                });

        System.out.println("\n（对比）invoke 只返回最终状态:");
        AgentState finalState = compiled
                .invoke(GraphInput.args(Map.of("text", "Hello World")), RunnableConfig.empty())
                .orElseThrow();
        System.out.println("最终状态: " + finalState.data());
    }
}
