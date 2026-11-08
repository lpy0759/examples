package org.pyliu.langgraph4j.parallel;

import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 模块 05：并行（fan-out / fan-in）。
 *
 * <p>一个节点用多条 {@code addEdge} 连出多个下游，就是 <b>fan-out</b>（分发）；
 * 多个节点连到同一个下游，就是 <b>fan-in</b>（汇聚）。框架会等所有分支都完成后才继续。
 *
 * <p>节点返回 {@link CompletableFuture} 时，各分支可真正并发执行（本模块用
 * {@code supplyAsync} 模拟耗时任务，观察三个任务的总耗时 ≈ 最长那个，而不是三者之和）。
 *
 * <pre>
 *                    ┌─→ taskA(800ms) ─┐
 *   START → prepare ─┼─→ taskB(400ms) ─┼─→ merge → END
 *                    └─→ taskC(200ms) ─┘
 * </pre>
 */
public class Main {

    /** 模拟一个耗时的异步任务：睡眠 ms 毫秒后，把结果写入状态。 */
    static AsyncNodeAction<AgentState> task(String name, long ms, String resultKey) {
        return state -> CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("[" + name + "] 开始，耗时 " + ms + "ms");
                Thread.sleep(ms);
                System.out.println("[" + name + "] 完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Map.<String, Object>of(resultKey, name + " 的结果");
        });
    }

    public static void main(String[] args) throws Exception {
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);

        graph.addNode("prepare", (AsyncNodeAction<AgentState>) state -> {
            System.out.println("[prepare] 分发 3 个并行任务");
            return CompletableFuture.completedFuture(Map.of());
        });

        graph.addNode("taskA", task("taskA", 800, "resultA"));
        graph.addNode("taskB", task("taskB", 400, "resultB"));
        graph.addNode("taskC", task("taskC", 200, "resultC"));

        graph.addNode("merge", (AsyncNodeAction<AgentState>) state -> {
            String a = state.<String>value("resultA").orElse("?");
            String b = state.<String>value("resultB").orElse("?");
            String c = state.<String>value("resultC").orElse("?");
            System.out.println("[merge] 三个分支都完成，开始汇聚");
            return CompletableFuture.completedFuture(Map.of("summary", a + " + " + b + " + " + c));
        });

        // fan-out：prepare → 三个任务
        graph.addEdge("prepare", "taskA");
        graph.addEdge("prepare", "taskB");
        graph.addEdge("prepare", "taskC");
        // fan-in：三个任务 → merge
        graph.addEdge("taskA", "merge");
        graph.addEdge("taskB", "merge");
        graph.addEdge("taskC", "merge");

        graph.addEdge(StateGraph.START, "prepare");
        graph.addEdge("merge", StateGraph.END);

        var compiled = graph.compile();

        long start = System.currentTimeMillis();
        AgentState result = compiled
                .invoke(GraphInput.noArgs(), RunnableConfig.empty())
                .orElseThrow();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("最终状态: " + result.data());
        System.out.printf("总耗时 %dms（若串行应约 %dms，说明是并发执行的）%n", elapsed, 800 + 400 + 200);
    }
}
