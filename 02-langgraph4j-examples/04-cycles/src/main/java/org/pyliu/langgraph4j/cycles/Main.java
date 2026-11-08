package org.pyliu.langgraph4j.cycles;

import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 模块 04：循环（Cycles）。
 *
 * <p>把「条件边」的回边指向自己，就构成了循环。图会一直跑，直到某个条件把它导向 END。
 * 这正是 AI Agent「LLM ⇄ 工具」反复迭代、以及各种「重试直到收敛」场景的基础。
 *
 * <p>为防止写出死循环，langgraph4j 有 {@code recursionLimit}（默认有限值）——
 * 超过这个步数会直接抛异常。所以构建循环时：
 * <ol>
 *   <li>必须有一个能通向 END 的出口条件；</li>
 *   <li>可通过 {@code RunnableConfig.recursionLimit(n)} 调大上限（本例倒计时步数已知，放得很宽松）。</li>
 * </ol>
 *
 * <p>本例：倒计时。{@code count} 从 5 开始，每轮减 1，减到 0 后结束。
 * <pre>
 *   START → decrement ⇄ (count>0 时回 decrement，count==0 时去 END)
 * </pre>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);

        AsyncNodeAction<AgentState> decrement = state -> {
            int count = state.<Integer>value("count").orElse(0);
            int next = count - 1;
            System.out.println("[decrement] count=" + count + " -> " + next);
            return CompletableFuture.completedFuture(Map.of("count", next));
        };

        graph.addNode("decrement", decrement);

        // 条件边：count 仍大于 0 就回到 decrement，否则结束
        AsyncEdgeAction<AgentState> router = state -> {
            int count = state.<Integer>value("count").orElse(0);
            return CompletableFuture.completedFuture(count > 0 ? "again" : "done");
        };
        graph.addConditionalEdges("decrement", router,
                Map.of("again", "decrement", "done", StateGraph.END));

        graph.addEdge(StateGraph.START, "decrement");

        var compiled = graph.compile();

        // 正常倒计时：从 5 减到 0
        System.out.println("===== 倒计时 5 -> 0 =====");
        var config = RunnableConfig.builder().recursionLimit(20).build();
        AgentState result = compiled
                .invoke(GraphInput.args(Map.of("count", 5)), config)
                .orElseThrow();
        System.out.println("最终状态: " + result.data());

        // 演示 recursionLimit 的防护作用：故意把上限设得比实际步数小
        System.out.println("\n===== recursionLimit=2（人为调小，观察异常） =====");
        var tightConfig = RunnableConfig.builder().recursionLimit(2).build();
        try {
            compiled.invoke(GraphInput.args(Map.of("count", 5)), tightConfig).orElseThrow();
        } catch (Exception e) {
            System.out.println("循环被截断，异常类型: " + e.getClass().getSimpleName()
                    + "，消息: " + e.getMessage());
        }
    }
}
