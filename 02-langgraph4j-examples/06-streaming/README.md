# 06 · Streaming —— 流式输出

## 模块目标

学会用 `stream` **逐节点观察状态变化**，而不是像 `invoke` 那样只拿最终结果。
这对长流程调试、实时 UI 展示、Agent 过程可视化都很关键。

## 核心概念讲解

- `invoke`：一口气跑到 END，返回 `Optional<State>`（只有最终状态）。
- `stream`：每执行完一个节点就**吐出一个 `NodeOutput`**，实时看到状态如何一步步演化。

返回类型是 `org.bsc.async.AsyncGenerator<NodeOutput<State>>`，其中 `NodeOutput` 携带：

- `node()`：刚执行完的节点名（注意还会看到 `__START__` 和 `__END__`）。
- `state()`：该时刻的状态快照。

### 两种消费方式

```java
// 方式一：同步消费（最简单，本线程逐个拉取）—— 推荐用于演示 / 简单脚本
compiled.stream(input, config).stream()
        .forEach(no -> System.out.println(no.node() + " -> " + no.state().data()));

// 方式二：异步消费
compiled.stream(input, config)
        .forEachAsync(no -> { ... })
        .join();
```

> 提示：`forEachAsync` 会在后台线程消费，**简单命令行 Demo 里进程可能因此不退出**
> （需要一个非守护线程兜底，或显式关闭）。本模块用 `.stream()` 同步方式，进程能干净结束。

`streamMode` 有两种：`StreamMode.VALUES`（默认，逐节点输出）与 `StreamMode.SNAPSHOTS`
（输出带 checkpoint 信息的状态快照，配合持久化使用），通过
`RunnableConfig.builder().streamMode(...)` 设置。

## 代码拆解

一个三步文本流水线：

```java
graph.addNode("toLower",  s -> completedFuture(Map.of("text", s.<String>value("text").orElse("").toLowerCase())));
graph.addNode("addEmoji", s -> completedFuture(Map.of("text", s.<String>value("text").orElse("") + " 🚀")));
graph.addNode("wrap",     s -> completedFuture(Map.of("text", "[" + s.<String>value("text").orElse("") + "]")));

compiled.stream(GraphInput.args(Map.of("text", "Hello World")), RunnableConfig.empty())
        .stream()
        .forEach(no -> System.out.println("节点 <" + no.node() + "> 之后的状态: " + no.state().data()));
```

## 运行方式

```bash
mvn -pl 06-streaming compile exec:java
```

预期输出：

```
===== 流式观察每一步 =====
节点 <__START__> 之后的状态: {text=Hello World}
节点 <toLower> 之后的状态: {text=hello world}
节点 <addEmoji> 之后的状态: {text=hello world 🚀}
节点 <wrap> 之后的状态: {text=[hello world 🚀]}
节点 <__END__> 之后的状态: {text=[hello world 🚀]}

（对比）invoke 只返回最终状态:
最终状态: {text=[hello world 🚀]}
```

## 延伸思考

1. **流式 + LLM**：真正的价值在 Agent 场景——LLM 节点边生成 token 边往外吐，前端可以逐字渲染。
   langgraph4j 里 LLM 的 `StreamingChatModel` 能把 token 流嵌入到图的流里。
2. **`streamSnapshots`**：`compiled.streamSnapshots(input, config)` 输出 `StateSnapshot`
   （含 checkpointId、nextNodeId 等），配合 checkpoint 持久化可做断点续跑。
3. **取消**：`stream(...)` 返回的 `AsyncGenerator.Cancellable` 支持 `cancel(...)`，
   可在用户中断时停止执行。
