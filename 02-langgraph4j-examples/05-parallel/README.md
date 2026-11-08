# 05 · Parallel —— 并行（fan-out / fan-in）

## 模块目标

学会让图**分叉成多条分支并行执行，再汇聚**。
这是处理「多个独立子任务」的常见结构，例如同时检索多个数据源、同时调用多个工具。

## 核心概念讲解

- **fan-out（分发）**：一个节点用多条 `addEdge` 连出多个下游。
- **fan-in（汇聚）**：多个节点连到同一个下游。

框架会**等所有分支都完成后**，才继续执行汇聚节点。

```java
// fan-out：prepare 同时连出三个任务
graph.addEdge("prepare", "taskA");
graph.addEdge("prepare", "taskB");
graph.addEdge("prepare", "taskC");
// fan-in：三个任务都连到 merge
graph.addEdge("taskA", "merge");
graph.addEdge("taskB", "merge");
graph.addEdge("taskC", "merge");
```

### 何时真正并发？

节点接口 `AsyncNodeAction` 返回 `CompletableFuture`。只要节点把耗时操作放到
`CompletableFuture.supplyAsync(...)` 里返回（而不是 `completedFuture` 前先阻塞 `Thread.sleep`），
各分支就能真正并发。本模块用三个不同耗时的任务验证这一点。

## 代码拆解

```java
static AsyncNodeAction<AgentState> task(String name, long ms, String resultKey) {
    return state -> CompletableFuture.supplyAsync(() -> {
        try {
            System.out.println("[" + name + "] 开始，耗时 " + ms + "ms");
            Thread.sleep(ms);
            System.out.println("[" + name + "] 完成");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return Map.<String, Object>of(resultKey, name + " 的结果");
    });
}
```

图结构：

```
                 ┌─→ taskA(800ms) ─┐
START → prepare ─┼─→ taskB(400ms) ─┼─→ merge → END
                 └─→ taskC(200ms) ─┘
```

## 运行方式

```bash
mvn -pl 05-parallel compile exec:java
```

预期输出（注意完成顺序是 C → B → A，且总耗时 ≈ 800ms 而非三者之和）：

```
[prepare] 分发 3 个并行任务
[taskA] 开始，耗时 800ms
[taskB] 开始，耗时 400ms
[taskC] 开始，耗时 200ms
[taskC] 完成
[taskB] 完成
[taskA] 完成
[merge] 三个分支都完成，开始汇聚
最终状态: {summary=taskA 的结果 + taskB 的结果 + taskC 的结果, ...}
总耗时 835ms（若串行应约 1400ms，说明是并发执行的）
```

## 延伸思考

1. **并行写同一个 key**：如果多个分支写同一个 key，合并规则由通道决定（见 `02-state`）。
   例如都用 `appender` 通道写 `results`，三个分支的结果会自动合并成一条列表——但要注意
   并下发下 reducer 的线程安全性（`AppenderChannel` 默认的 `ReducerDisallowDuplicate` 内部用
   `ArrayList`，串行场景安全；真正的并发写入需谨慎）。
2. **并行节点的执行器**：`RunnableConfig` 提供了 `emitterExecutor(...)` 和
   `addParallelNodeExecutor(name, executor)`，可以自定义并发线程池。
3. 进阶方向：**map-reduce 模式** —— fan-out 到 N 个同构任务，再 fan-in 用 reducer 归并结果，
   是很多批处理图的标准骨架。
