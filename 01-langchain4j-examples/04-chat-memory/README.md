# 模块 04：对话记忆（Chat Memory）

## 模块目标

让 LangChain4j **自动**保存并携带对话历史，并学会控制"记住多少"和"按用户隔离"。

## 核心概念讲解

### 1. 为什么要记忆

模块 02 里我们手动拼历史消息；`ChatMemory` 把这个过程自动化：你每次调用，框架自动把历史消息带上，并把新的问答追加进记忆。

### 2. 两种"窗口"策略

| 策略 | 类 | 含义 |
|---|---|---|
| 按条数 | `MessageWindowChatMemory` | 只保留最近 N 条消息 |
| 按 token | `TokenWindowChatMemory` | 只保留最近 N 个 token 的消息（精确控成本） |

模型都有**上下文窗口上限**，历史太长会超限或烧钱，所以要用窗口裁剪。

### 3. 多用户隔离

真实系统有多个用户，记忆不能串。用 `@MemoryId` 标注"以谁的身份记忆"，配合 `ChatMemoryProvider` 按 id 各给一份记忆。

## 代码拆解

```java
// 单份记忆
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
Assistant a = AiServices.builder(Assistant.class)
        .chatModel(model).chatMemory(memory).build();

// token 窗口
ChatMemory m2 = TokenWindowChatMemory.builder()
        .maxTokens(500, new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O)).build();

// 多用户隔离
interface Multi { String chat(@MemoryId String userId, String msg); }
ChatMemoryProvider provider = id -> MessageWindowChatMemory.withMaxMessages(10);
```

## 运行方式

```bash
# 先确保本地 Ollama 已启动并拉取 qwen3.5:9b：ollama pull qwen3.5:9b
mvn -pl 04-chat-memory exec:java -Dexec.mainClass=org.pyliu.learn.memory.Main
```

## 延伸思考

1. 示例里的记忆是**内存实现**，进程重启就丢。生产环境可换成持久化实现（Redis / 数据库），LangChain4j 提供 `ChatMemoryStore` 扩展点。
2. 消息里往往需要保留 system 消息在最前面（`alwaysKeepSystemMessageFirst`），保证人设不丢。
3. token 窗口需要一个"计 token"的估算器，不同模型用不同的 `TokenCountEstimator` 实现。本地 qwen3.5 没有公开 tokenizer，jtokkit 只认 OpenAI 模型名，所以这里借用 GPT-4o 的 O200K 编码做近似计数。
