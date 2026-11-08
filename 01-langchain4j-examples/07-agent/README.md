# 模块 07：智能体（Agent）

## 模块目标

把 **模型 + 工具 + 记忆** 组合起来，得到一个能自主决策的 Agent。

## 核心概念讲解

### 1. Agent 是什么

普通助手是"一问一答"；Agent 是"给个目标，它自己想办法"——自主决定**调用哪些工具、调几次、何时停止**，最后给出答案。

### 2. ReAct 思想

Agent 的核心是 **ReAct（Reasoning + Acting）**：模型交替进行"思考（下一步做什么）"和"行动（调用工具）"，直到觉得能回答了才停下。

### 3. Agent = 三个要素的组合

| 要素 | 作用 |
|---|---|
| ChatModel | 大脑：推理、决策 |
| Tools | 手脚：能做的事（查时间、查天气、算数） |
| ChatMemory | 记忆：记住上下文，多轮协同 |

## 代码拆解

```java
Agent agent = AiServices.builder(Agent.class)
        .chatModel(model)
        .tools(new AgentTools())                              // 多个工具
        .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
        .build();

agent.answer("现在几点了？北京天气怎么样？");   // 会依次调用"时间""天气"两个工具
agent.answer("那上海呢？");                    // 有记忆，能接住上一轮
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn -pl 07-agent exec:java -Dexec.mainClass=org.pyliu.learn.agent.Main
```

## 延伸思考

1. 工具调用的轮数可用 `.maxToolCallingRoundTrips(n)` 限制，防止模型无限循环。
2. 多个工具可以 `.executeToolsConcurrently()` 并发执行（相互独立时更快）。
3. 更复杂的 Agent（多 Agent 协作、规划）是更前沿的方向，但"模型 + 工具 + 记忆"这个组合是一切的基础。
