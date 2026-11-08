# 03 · Conditional Routing —— 条件路由

## 模块目标

学会用 `addConditionalEdges` 让图**根据当前状态**选择下一步走哪个分支。
这是从「固定流水线」到「动态决策」的关键一步，也是下一模块循环、以及后面 Agent 的基础。

## 核心概念讲解

条件边（conditional edge）需要两样东西：

1. **路由函数**（`AsyncEdgeAction<S>`）：`状态 → CompletableFuture<String>`，返回一个**分支键**。
2. **映射表**（`Map<String, String>`）：`分支键 → 目标节点名`。

框架执行到该节点后，调用路由函数拿到分支键，再查表决定下一个节点。

```java
graph.addConditionalEdges("classify", 路由函数, Map.of("A","excellent", "B","pass", "C","fail"));
```

注意：映射表的目标值**可以是 `StateGraph.END`**，表示该分支直接结束。

## 代码拆解

本例：输入 `score`，`classify` 节点算出 `grade`，再按 `grade` 分流到三个分支：

```java
// 分类节点：读 score，写 grade
AsyncNodeAction<AgentState> classify = state -> {
    int score = state.<Integer>value("score").orElse(0);
    String grade = score >= 80 ? "A" : score >= 60 ? "B" : "C";
    return CompletableFuture.completedFuture(Map.of("grade", grade));
};

// 路由函数：读 grade，返回分支键
AsyncEdgeAction<AgentState> router = state ->
    CompletableFuture.completedFuture(state.<String>value("grade").orElse("C"));

graph.addConditionalEdges("classify", router,
        Map.of("A", "excellent", "B", "pass", "C", "fail"));

graph.addEdge(StateGraph.START, "classify");
graph.addEdge("excellent", StateGraph.END);
graph.addEdge("pass", StateGraph.END);
graph.addEdge("fail", StateGraph.END);
```

图结构：

```
START → classify ─┬─(A)→ excellent → END
                  ├─(B)→ pass      → END
                  └─(C)→ fail      → END
```

## 运行方式

```bash
mvn -pl 03-conditional-routing compile exec:java
```

预期输出（三次不同输入，走不同分支）：

```
===== 运行：score=92 =====
[classify] score=92 -> grade=A
[excellent] 优秀，奖励一杯奶茶 🧋
最终状态: {score=92, comment=优秀, grade=A}
===== 运行：score=75 =====
[classify] score=75 -> grade=B
[pass] 及格，继续加油 💪
...
===== 运行：score=40 =====
[classify] score=40 -> grade=C
[fail] 不及格，再接再厉 📚
...
```

## 延伸思考

1. 路由函数是**纯函数**（只依赖状态）最理想，便于测试与推理。可以把它抽成独立方法并写单元测试。
2. 如果映射表漏了某个分支键（比如路由返回了表里没有的值），框架会报错——写路由时注意覆盖所有分支。
3. 除 `AsyncEdgeAction` 外，还有更强大的 `Command` 路由（`addConditionalEdges(节点, AsyncCommandAction, 映射)`），
   能在路由的同时**直接更新状态 / 指定跳转节点**（`gotoNode`），是 Agent 里常用的高级写法。
