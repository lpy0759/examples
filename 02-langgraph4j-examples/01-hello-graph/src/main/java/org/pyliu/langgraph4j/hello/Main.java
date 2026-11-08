package org.pyliu.langgraph4j.hello;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 模块 01：最小可运行图。
 *
 * <p>核心概念：
 * <ul>
 *   <li><b>State（状态）</b>：在节点之间流动的、可变的共享数据。默认用 {@link AgentState}。</li>
 *   <li><b>Node（节点）</b>：处理状态的一个步骤。它读取当前状态，返回一段「状态增量」（Map），框架会自动合并。</li>
 *   <li><b>Edge（边）</b>：节点之间的连线，决定执行顺序。</li>
 *   <li><b>START / END</b>：虚拟的入口与出口节点。图从 START 出发，走到 END 结束。</li>
 * </ul>
 *
 * <p>本模块的图：START → greet → farewell → END
 */
public class Main {

    public static void main(String[] args) throws Exception {
        // 1) 构建图：用 AgentState::new 告诉框架「如何创建一份初始状态」
        StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);

        // 2) 定义节点。节点是一个返回「状态增量」的函数（这里用异步接口 AsyncNodeAction，
        //    通过 completedFuture 包装一个立即完成的 Future）。
        AsyncNodeAction<AgentState> greet = state -> {
            String name = state.<String>value("name").orElse("world");   // 读状态，缺省 world
            System.out.println("[greet] 执行：生成问候语");
            // 返回的 Map 是「状态增量」，会被合并进全局状态
            return CompletableFuture.completedFuture(Map.of("greeting", "你好，" + name + "！"));
        };

        AsyncNodeAction<AgentState> farewell = state -> {
            String greeting = state.<String>value("greeting").orElse("你好！"); // 读上一个节点写入的 greeting
            System.out.println("[farewell] 执行：生成告别语");
            return CompletableFuture.completedFuture(Map.of("farewell", greeting + " 再见！"));
        };

        graph.addNode("greet", greet);
        graph.addNode("farewell", farewell);

        // 3) 定义边。START 和 END 是图内置的常量。
        graph.addEdge(StateGraph.START, "greet");
        graph.addEdge("greet", "farewell");
        graph.addEdge("farewell", StateGraph.END);

        // 4) 编译：把图定义固化成一个可执行的 CompiledGraph
        CompiledGraph<AgentState> compiled = graph.compile();

        // 5) 执行：invoke 传入初始状态（这里是 name），同步跑到 END，返回最终状态。
        //    GraphInput.args(...) 包装初始状态，RunnableConfig.empty() 表示用默认运行配置。
        AgentState result = compiled
                .invoke(GraphInput.args(Map.of("name", "LangGraph4j")), RunnableConfig.empty())
                .orElseThrow();

        System.out.println("最终状态: " + result.data());
    }
}
