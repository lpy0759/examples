# 04 · Cycles —— 循环

## 模块目标

学会用「条件边 + 回边」构造**循环**，直到某个条件满足才走向 END。
这是 AI Agent「LLM ⇄ 工具」反复迭代、以及各种「重试直到收敛」模式的底层结构。

## 核心概念讲解

循环 = 条件边把某个分支**指回上游节点**：

```
START → decrement ⇄ (count>0 时回 decrement；count==0 时去 END)
```

只要存在一条能通向 `END` 的出口，图就是良定义的；框架会反复执行直到进入 END。

### recursionLimit：防死循环的保险丝

如果出口条件写错了（永远不满足），图会无限跑下去。langgraph4j 用 **recursionLimit**
（最大执行步数）兜底，超了就抛异常。默认值有限，构建长循环时需显式调大：

```java
RunnableConfig config = RunnableConfig.builder()
        .recursionLimit(20)   // 调大上限
        .build();
compiled.invoke(GraphInput.args(Map.of("count", 5)), config);
```

本模块还演示了把上限故意调小（`recursionLimit(2)`）会发生什么：框架抛
`IllegalStateException: Maximum number of iterations (2) reached!`（被 `CompletionException` 包装）。

## 代码拆解

```java
AsyncNodeAction<AgentState> decrement = state -> {
    int count = state.<Integer>value("count").orElse(0);
    int next = count - 1;
    System.out.println("[decrement] count=" + count + " -> " + next);
    return CompletableFuture.completedFuture(Map.of("count", next));
};

AsyncEdgeAction<AgentState> router = state ->
    CompletableFuture.completedFuture(
        state.<Integer>value("count").orElse(0) > 0 ? "again" : "done");

graph.addNode("decrement", decrement);
// 关键：分支 "again" 指回自己，形成循环；"done" 直接到 END
graph.addConditionalEdges("decrement", router,
        Map.of("again", "decrement", "done", StateGraph.END));
graph.addEdge(StateGraph.START, "decrement");
```

注意：`decrement` 节点**不再单独** `addEdge("decrement", END)`——它的出口完全由条件边接管。

## 运行方式

```bash
mvn -pl 04-cycles compile exec:java
```

预期输出：

```
===== 倒计时 5 -> 0 =====
[decrement] count=5 -> 4
[decrement] count=4 -> 3
[decrement] count=3 -> 2
[decrement] count=2 -> 1
[decrement] count=1 -> 0
最终状态: {count=0}

===== recursionLimit=2（人为调小，观察异常） =====
[decrement] count=5 -> 4
循环被截断，异常类型: CompletionException，消息: ... Maximum number of iterations (2) reached!
```

## 延伸思考

1. **循环里的状态累积**：循环常配合 appender 通道（见 `02-state`）在每轮往列表里追加记录，
   例如 Agent 把每一轮 LLM 的 `AiMessage` 和工具结果都追加进 `messages`。
2. **recursionLimit 的计数口径**：它统计的是图执行的「超步（super-step）」，而非简单节点数；
   涉及并行分支时会不同，调上限时留足余量。
3. 进阶方向：**human-in-the-loop** 可以看作「循环 + 在某个节点挂起等待人工输入后恢复」
   （`CompileConfig.interruptsBefore/After` + checkpoint 持久化 + `GraphInput.resume()`）。
