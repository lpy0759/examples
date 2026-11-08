# 02 · State & Channel —— 状态与通道

## 模块目标

讲清两件事：

1. 如何用**自定义状态类**（类型安全的 getter）替代裸 Map；
2. 状态里每个字段「如何被合并」是由**通道（Channel）**决定的——最常见的两种是
   **appender（列表追加）** 和 **base + reducer（旧值新值归并）**。

## 核心概念讲解

### 1. 自定义状态类

默认的 `AgentState` 用 `state.<T>value("key")` 读值，每次都要写 key 字符串、做类型转换，容易出错。
更好的做法是继承 `AgentState`，把字段封装成类型化方法：

```java
public class WorkflowState extends AgentState {
    public WorkflowState(Map<String, Object> initData) { super(initData); }
    public List<String> steps() { return this.<List<String>>value("steps").orElse(List.of()); }
    public int total()      { return this.<Integer>value("total").orElse(0); }
}
```

然后用 `new StateGraph<>(channels, WorkflowState::new)` 告诉框架如何从初始 Map 创建状态。

### 2. Channel（通道）

默认情况下，节点写入同名 key 会「覆盖」旧值。但很多场景需要「合并」：

| 需求 | 通道 | 语义 |
|---|---|---|
| 多节点各写一条，累积成列表 | `Channels.appender(ArrayList::new)` | 新值追加进 List（默认去重） |
| 保留重复项 | `Channels.appenderWithDuplicate(...)` | 追加，不去重 |
| 数值累加 / 自定义合并 | `Channels.base(reducer, default)` | `reducer(旧值, 新值)` 得到新值 |

`reducer` 就是 `BiFunction<T,T,T>`，例如 `(old, new) -> old + new` 做累加。
`default` 是字段的初始值提供者，例如 `() -> 0`。

> 通道在**多个节点写同一个 key**（顺序或并行）时才体现出差别——本模块用三个顺序节点演示，
> 并行场景会在 `05-parallel` 再次出现。

## 代码拆解

```java
Map<String, Channel<?>> channels = Map.of(
        "steps", Channels.<String>appender(ArrayList::new),
        "total", Channels.<Integer>base((a, b) -> a + b, () -> 0));
StateGraph<WorkflowState> graph = new StateGraph<>(channels, WorkflowState::new);
```

三个节点都往 `steps`、`total` 写值：

```java
graph.addNode("stepA", s -> completedFuture(Map.of("steps", "A", "total", 1)));
graph.addNode("stepB", s -> completedFuture(Map.of("steps", "B", "total", 2)));
graph.addNode("stepC", s -> completedFuture(Map.of("steps", "C", "total", 3)));
```

执行后：

- `steps` 通道把三次写入**追加**成 `["A", "B", "C"]`；
- `total` 通道把三次写入**累加**成 `0 + 1 + 2 + 3 = 6`（默认值 0 参与第一轮）。

## 运行方式

```bash
mvn -pl 02-state compile exec:java
```

预期输出：

```
[stepA] 执行：写 steps=A, total=1
[stepB] 执行：写 steps=B, total=2
[stepC] 执行：写 steps=C, total=3
steps 通道（追加）: [A, B, C]
total 通道（累加）: 6
```

## 延伸思考

1. 框架内置了一个 `prebuilt.MessagesState<T>`（本质就是「messages」这个 appender 通道的封装，
   并提供 `messages()` / `lastMessage()` 等便捷方法）。它是 07 模块 Agent 的状态基础。
2. `Channels.base(reducer)` 也支持不给默认值；此时首轮写入的「旧值」为 `null`，reducer 需要自己判空。
3. 通道还有一个高级能力：`AppenderChannel.RemoveIdentifier` 可以从列表里按条件删除元素，
   以及 `MARK_FOR_RESET` / `MARK_FOR_REMOVAL` 特殊标记——用于更精细的状态控制。
