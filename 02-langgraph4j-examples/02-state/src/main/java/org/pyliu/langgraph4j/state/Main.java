package org.pyliu.langgraph4j.state;

import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 模块 02：状态与通道（State & Channel）。
 *
 * <p>本模块讲两件事：
 * <ol>
 *   <li><b>自定义状态类</b>：继承 {@link AgentState}，用类型化 getter 访问字段，替代裸 Map。</li>
 *   <li><b>通道（Channel）</b>：声明每个字段「如何被合并」，两种常见通道——
 *       <ul>
 *         <li>{@code Channels.appender(...)}：追加列表（多节点各写一条，最终累积成列表）；</li>
 *         <li>{@code Channels.base(reducer, default)}：用 reducer 把新值与旧值合并（这里做累加）。</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>图结构：START → stepA → stepB → stepC → END。三个节点都往 {@code steps}、{@code total}
 * 写值，靠通道语义完成「列表追加」与「数值累加」。
 */
public class Main {

    /** 自定义状态类：把字段访问包装成类型安全的方法。 */
    public static class WorkflowState extends AgentState {
        public WorkflowState(Map<String, Object> initData) {
            super(initData);
        }

        public List<String> steps() {
            return this.<List<String>>value("steps").orElse(List.of());
        }

        public int total() {
            return this.<Integer>value("total").orElse(0);
        }
    }

    public static void main(String[] args) throws Exception {
        // 1) 用「通道」声明状态 schema（key -> 合并规则）
        Map<String, Channel<?>> channels = Map.of(
                // appender：每次写入的值被追加进一个 List（默认去重）
                "steps", Channels.<String>appender(ArrayList::new),
                // base + reducer：新值与旧值按 (old, new) -> old + new 合并，默认 0
                "total", Channels.<Integer>base((oldVal, newVal) -> oldVal + newVal, () -> 0)
        );

        // 2) 用「自定义状态工厂」构建图：WorkflowState::new 即「如何从初始 Map 创建状态」
        StateGraph<WorkflowState> graph = new StateGraph<>(channels, WorkflowState::new);

        AsyncNodeAction<WorkflowState> stepA = state -> {
            System.out.println("[stepA] 执行：写 steps=A, total=1");
            return CompletableFuture.completedFuture(Map.<String, Object>of("steps", "A", "total", 1));
        };
        AsyncNodeAction<WorkflowState> stepB = state -> {
            System.out.println("[stepB] 执行：写 steps=B, total=2");
            return CompletableFuture.completedFuture(Map.<String, Object>of("steps", "B", "total", 2));
        };
        AsyncNodeAction<WorkflowState> stepC = state -> {
            System.out.println("[stepC] 执行：写 steps=C, total=3");
            return CompletableFuture.completedFuture(Map.<String, Object>of("steps", "C", "total", 3));
        };

        graph.addNode("stepA", stepA);
        graph.addNode("stepB", stepB);
        graph.addNode("stepC", stepC);

        graph.addEdge(StateGraph.START, "stepA");
        graph.addEdge("stepA", "stepB");
        graph.addEdge("stepB", "stepC");
        graph.addEdge("stepC", StateGraph.END);

        var compiled = graph.compile();

        // 无初始入参：通道的默认值（空列表、0）会作为初始状态
        WorkflowState result = compiled
                .invoke(GraphInput.noArgs(), RunnableConfig.empty())
                .orElseThrow();

        System.out.println("steps 通道（追加）: " + result.steps());   // [A, B, C]
        System.out.println("total 通道（累加）: " + result.total());    // 6
    }
}
