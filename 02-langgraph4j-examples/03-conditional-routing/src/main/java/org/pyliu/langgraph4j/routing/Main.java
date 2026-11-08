package org.pyliu.langgraph4j.routing;

import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 模块 03：条件路由（Conditional Edges）。
 *
 * <p>之前的图都是「一条直线」。真实业务常常需要根据状态决定下一步走哪个分支。
 * {@code addConditionalEdges} 就是干这个的：
 * <pre>
 *   addConditionalEdges("节点A", 路由函数, {分支键 -> 目标节点})
 * </pre>
 * 路由函数（{@link AsyncEdgeAction}）读取当前状态，返回一个「分支键」，
 * 框架再拿这个键去查映射表，决定跳到哪个节点。
 *
 * <p>本例：根据分数 grade（A/B/C）路由到三个不同分支，再各自汇入 END。
 * <pre>
 *   START → classify ─┬─(A)→ excellent → END
 *                     ├─(B)→ pass      → END
 *                     └─(C)→ fail      → END
 * </pre>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);

        // 分类节点：读输入 score，写回 grade
        AsyncNodeAction<AgentState> classify = state -> {
            int score = state.<Integer>value("score").orElse(0);
            String grade = score >= 80 ? "A" : score >= 60 ? "B" : "C";
            System.out.println("[classify] score=" + score + " -> grade=" + grade);
            return CompletableFuture.completedFuture(Map.of("grade", grade));
        };

        AsyncNodeAction<AgentState> excellent = state -> {
            System.out.println("[excellent] 优秀，奖励一杯奶茶 🧋");
            return CompletableFuture.completedFuture(Map.of("comment", "优秀"));
        };
        AsyncNodeAction<AgentState> pass = state -> {
            System.out.println("[pass] 及格，继续加油 💪");
            return CompletableFuture.completedFuture(Map.of("comment", "及格"));
        };
        AsyncNodeAction<AgentState> fail = state -> {
            System.out.println("[fail] 不及格，再接再厉 📚");
            return CompletableFuture.completedFuture(Map.of("comment", "不及格"));
        };

        graph.addNode("classify", classify);
        graph.addNode("excellent", excellent);
        graph.addNode("pass", pass);
        graph.addNode("fail", fail);

        // 条件边：路由函数返回 "A"/"B"/"C"，映射表决定去向
        AsyncEdgeAction<AgentState> router = state -> {
            String grade = state.<String>value("grade").orElse("C");
            return CompletableFuture.completedFuture(grade);
        };
        graph.addConditionalEdges("classify", router,
                Map.of("A", "excellent", "B", "pass", "C", "fail"));

        graph.addEdge(StateGraph.START, "classify");
        graph.addEdge("excellent", StateGraph.END);
        graph.addEdge("pass", StateGraph.END);
        graph.addEdge("fail", StateGraph.END);

        var compiled = graph.compile();

        for (int score : new int[]{92, 75, 40}) {
            System.out.println("\n===== 运行：score=" + score + " =====");
            AgentState result = compiled
                    .invoke(GraphInput.args(Map.of("score", score)), RunnableConfig.empty())
                    .orElseThrow();
            System.out.println("最终状态: " + result.data());
        }
    }
}
