# 07 · AI Agent —— LLM + 工具调用循环

## 模块目标

把前面所有知识组合起来，做出一个真正的 **ReAct Agent**：
LLM 决定要不要调工具，调完工具拿到结果再让 LLM 回答，直到 LLM 不再调工具为止。

```
START → agent ─(LLM 决定)─┬─ 要调工具 → tools ─┐
           └───────────────┴─ 直接结束 → END ←──┘
```

## 核心概念讲解

### 1. LLM 抽象：ChatModel

本模块引入 [LangChain4j](https://docs.langchain4j.dev)（版本 1.22.0）作为 LLM 层。
核心接口是 `dev.langchain4j.model.chat.ChatModel`（**注意**：旧名 `ChatLanguageModel` 在 1.x 已废弃）。

- 调用：`model.chat(ChatRequest.builder().messages(对话列表).build())` → `ChatResponse`。
- 结果：`response.aiMessage()` → `AiMessage`，它可能带**工具调用请求** `toolExecutionRequests()`。
- 消息类型：`SystemMessage` / `UserMessage` / `AiMessage` / `ToolExecutionResultMessage`，
  都在 `dev.langchain4j.data.message` 包下。

### 2. 离线假模型（本模块默认）

1.22.0 的 `langchain4j` 核心里已经没有 `ChatModelMock` 了。为了让教程**无需 API Key 也能实跑**，
本模块自己实现了一个 `FakeChatModel`（见 `FakeChatModel.java`）——实现 `ChatModel` 只需重写
`doChat(ChatRequest)` 一个方法：

- 第一轮（历史里没有工具结果）→ 返回「调用 `get_weather` 工具」的 `AiMessage`；
- 第二轮（已拿到工具结果）→ 返回最终回答。

### 3. Agent 循环的状态与路由

- 状态里存 `List<ChatMessage>`，用 **appender 通道**累积对话（就是 `02-state` 讲的东西，
  也是框架内置 `MessagesState` 的底层机制）。
- `agent` 节点：把当前对话交给 LLM，把返回的 `AiMessage` 追加进状态。
- 条件边：看**最后一条消息**——若它是带工具请求的 `AiMessage` → 去 `tools`，否则 → END。
- `tools` 节点：执行上一条 `AiMessage` 里的每个工具请求，把 `ToolExecutionResultMessage` 追加回对话，
  然后**回到 `agent`** 形成循环。

## 代码拆解

```java
// agent 节点
AsyncNodeAction<AgentState> agent = state -> {
    List<ChatMessage> messages = state.<List<ChatMessage>>value("messages").orElse(List.of());
    ChatResponse response = model.chat(ChatRequest.builder().messages(messages).build());
    return CompletableFuture.completedFuture(Map.of("messages", response.aiMessage()));
};

// 条件边：最后一条消息还在请求工具吗？
AsyncEdgeAction<AgentState> router = state -> {
    ChatMessage last = messages(state).get(messages(state).size() - 1);
    boolean callTool = last instanceof AiMessage am && am.hasToolExecutionRequests();
    return CompletableFuture.completedFuture(callTool ? "tools" : "end");
};
graph.addConditionalEdges("agent", router, Map.of("tools", "tools", "end", StateGraph.END));

graph.addEdge(StateGraph.START, "agent");
graph.addEdge("tools", "agent");   // 工具执行完回到 agent
```

一个容易踩的坑：`ChatMessage` 及其子类**没有实现 `Serializable`**，而 langgraph4j 默认的
状态序列化器是 Java 序列化（`ObjectStreamStateSerializer`）。所以这里用
`RunnableConfig.builder().disableCloneState()` 关闭状态克隆；生产环境更推荐改用
`langgraph4j-langchain4j` 提供的消息序列化器（`JacksonMessagesStateSerializer` 等）。

## 运行方式

默认离线假模型，无需 Key：

```bash
mvn -pl 07-agent compile exec:java
```

预期输出：

```
使用模型: FakeChatModel

===== 运行 Agent =====
[agent] 调用 LLM（当前对话 2 条）
[agent] LLM 请求调用工具: get_weather
[tools] 执行工具 get_weather -> 北京：晴朗，25°C（模拟结果）
[agent] 调用 LLM（当前对话 4 条）
[agent] LLM 最终回答: 根据工具结果：北京今天晴朗，25°C，适合出行。

===== 最终对话 =====
[系统] 你是乐于助人的助手，需要查天气时请调用 get_weather 工具。
[用户] 北京今天天气怎么样？
[AI]   （请求调用 get_weather）
[工具] 北京：晴朗，25°C（模拟结果）
[AI]   根据工具结果：北京今天晴朗，25°C，适合出行。
```

### 切换到真实 OpenAI（⚠️ 需 API Key）

```bash
export LLM_PROVIDER=openai
export OPENAI_API_KEY=sk-...
export OPENAI_MODEL_NAME=gpt-4o-mini   # 可选
mvn -pl 07-agent compile exec:java
```

`ModelFactory.create()` 会按环境变量返回 `OpenAiChatModel`。此分支代码**已通过编译但未实跑**，
需有效的 Key 与网络。

## 延伸思考

1. **真正的工具注册与参数解析**：本模块的 `executeTool` 是硬编码的。生产里工具由
   `ToolSpecification` 声明 + 反射调用（LangChain4j 的 `@Tool` 注解 + `AiServices`），
   或走 MCP（`langgraph4j-mcp` / `langchain4j-mcp`）动态发现工具。
2. **多步 Agent / 状态图更复杂**：真实 Agent 往往有多个节点（规划、检索、反思、总结），
   并用 `Command.gotoNode()` 做更灵活的跳转。
3. **持久化与恢复**：给 `CompileConfig` 配一个 `checkpointSaver`（如 `langgraph4j-sqlite-saver`），
   就能保存每一步状态，实现 human-in-the-loop 中断 / 恢复、多轮会话记忆。
4. **可视化**：配合 `langgraph4j-studio` 可以在图形界面里实时查看图的执行过程。
