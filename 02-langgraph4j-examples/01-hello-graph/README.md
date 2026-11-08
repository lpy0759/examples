# 01 · Hello Graph —— 最小可运行图

## 模块目标

跑通 langgraph4j 的最小闭环，建立四个核心概念：**状态（State）、节点（Node）、边（Edge）、入口/出口（START/END）**。
看完本模块，你要能自己搭一张「节点按边依次执行、状态在节点间流转」的图。

## 核心概念讲解

一张图（`StateGraph`）由三样东西组成：

- **State（状态）**：在节点之间流动的共享数据，本质是一个 `Map<String,Object>`。
  langgraph4j 默认提供 `AgentState` 来包装它。每个节点读它、并返回一小段「增量」。
- **Node（节点）**：一个处理步骤，即一个函数 `状态 → 状态增量`。
  它读当前状态，返回一个 `Map<String,Object>`，框架会把这个 Map **合并**回全局状态。
- **Edge（边）**：节点之间的连线，决定执行顺序。

还有两个虚拟节点：

- `StateGraph.START`：入口，图从这里出发。
- `StateGraph.END`：出口，走到这里图结束。

> ⚠️ 版本注意：langgraph4j 1.9.x 里**没有** `setEntryPoint` / `setFinishPoint` 方法
> （旧教程常见写法）。入口/出口直接用 `addEdge(START, "节点")` 和 `addEdge("节点", END)` 表达。

## 代码拆解

完整代码见 `src/main/java/org/pyliu/langgraph4j/hello/Main.java`，关键步骤：

```java
// 1) 构建图：AgentState::new 告诉框架「如何创建一份初始状态」
StateGraph<AgentState> graph = new StateGraph<>(AgentState::new);

// 2) 定义节点：返回「状态增量」，这里用异步接口 AsyncNodeAction
AsyncNodeAction<AgentState> greet = state -> {
    String name = state.<String>value("name").orElse("world");
    return CompletableFuture.completedFuture(Map.of("greeting", "你好，" + name + "！"));
};
graph.addNode("greet", greet);

// 3) 定义边
graph.addEdge(StateGraph.START, "greet");
graph.addEdge("greet", "farewell");
graph.addEdge("farewell", StateGraph.END);

// 4) 编译
CompiledGraph<AgentState> compiled = graph.compile();

// 5) 执行：GraphInput.args 包装初始状态，RunnableConfig.empty() 用默认配置
AgentState result = compiled
        .invoke(GraphInput.args(Map.of("name", "LangGraph4j")), RunnableConfig.empty())
        .orElseThrow();
```

几个值得注意的点：

- 节点接口 `AsyncNodeAction<S>` 是 `S → CompletableFuture<Map<String,Object>>`。
  这里的例子用 `completedFuture` 包装一个立即完成的 Future；后面的模块会看到真正的异步。
- `state.<String>value("name")` 读状态（泛型指定返回类型），`.orElse(...)` 提供缺省值。
  （`value(key, 默认值)` 这个便捷重载在 1.9.x 已标记 `@Deprecated(forRemoval=true)`，不要再用。）
- `invoke` 是「同步跑到 END 并返回最终状态」；`invoke(Map)` 同样已废弃，改用 `invoke(GraphInput, RunnableConfig)`。

## 运行方式

```bash
mvn -pl 01-hello-graph compile exec:java
```

预期输出：

```
[greet] 执行：生成问候语
[farewell] 执行：生成告别语
最终状态: {name=LangGraph4j, greeting=你好，LangGraph4j！, farewell=你好，LangGraph4j！ 再见！}
```

## 延伸思考

1. 节点返回的 Map 如果和已有状态有**同名 key**，会发生什么？—— 默认会「覆盖」。
   但覆盖行为其实是由「通道（Channel）」决定的，这正是下一模块 `02-state` 要讲的内容。
2. 如果某条边指向一个不存在的节点，`compile()` 阶段会不会报错？可以动手试试。
3. 想「可视化」这张图？框架提供了 `graph.getGraph(GraphRepresentation.Type.MERMAID)`
   可以输出 Mermaid 描述，配合 `DiagramGenerator` 生成图。留作进阶探索。
