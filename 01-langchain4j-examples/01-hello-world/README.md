# 模块 01：Hello World

## 模块目标

跑通第一个 LangChain4j 程序：用一句自然语言问模型，拿到回答。同时理解 LangChain4j 最重要的一个设计思想——**模型无关性**。

## 核心概念讲解

### 1. LangChain4j 是什么？

LangChain4j 是 Java 生态下的 LLM 应用开发框架，对标 Python 的 LangChain。它要解决的问题：**如何把"调用大模型"这件事，变成 Java 里可控、可测试、可组合的代码**，而不仅仅是发一个 HTTP 请求。

### 2. 三层架构（模型无关性）

```
┌─────────────────────────────────────────┐
│  工具层：AiServices / 记忆 / RAG / 工具调用 │  ← 业务直接面对的高级 API
├─────────────────────────────────────────┤
│  集成层：OpenAI / Anthropic / Ollama ...  │  ← 各厂商实现（本模块用 langchain4j-open-ai 连 DeepSeek）
├─────────────────────────────────────────┤
│  模型层：ChatModel / EmbeddingModel / ... │  ← 统一抽象接口
└─────────────────────────────────────────┘
```

- **模型层**：`ChatModel` 是对"对话模型"的统一抽象，定义好 `chat(...)` 等方法。
- **集成层**：`OpenAiChatModel` 是 OpenAI 兼容实现。因为 DeepSeek、通义千问、本地 Ollama 都提供 **OpenAI 兼容接口**，所以只要改 `baseUrl` + `modelName`，就能在它们之间随意切换。
- **工具层**：`AiServices`、`ChatMemory`、RAG、工具调用等，后续模块逐步展开。

> **模型无关性是本项目的核心前提**：后面 14 个模块里，除 `baseUrl`/`modelName`/`apiKey` 外，业务代码几乎不依赖具体厂商，换模型只需改配置。

### 3. 为什么用 DeepSeek？

DeepSeek 提供标准 OpenAI 兼容接口（`https://api.deepseek.com`），对话模型为 `deepseek-chat`。国内直连、成本低、效果稳定，很适合学习。

## 代码拆解

```java
ChatModel model = OpenAiChatModel.builder()
        .baseUrl("https://api.deepseek.com")   // 关键：把 OpenAI 客户端指向 DeepSeek
        .apiKey(apiKey)
        .modelName("deepseek-chat")
        .logRequests(true)                     // 观察底层请求/响应
        .logResponses(true)
        .build();

String answer = model.chat("请用一句话介绍 LangChain4j 是什么");
```

- `ChatModel` 是接口，`OpenAiChatModel` 是具体实现——**面向接口编程**。
- `chat(String)` 是最简单的同步调用：一条用户消息进去，一条回答出来。
- `logRequests/logResponses` 打开后，控制台会打印真实请求体/响应体，非常有助于理解底层。

## 运行方式

```bash
# 1. 设置 API Key
export DEEPSEEK_API_KEY=sk-你的DeepSeek密钥

# 2. 运行本模块
mvn -pl 01-hello-world exec:java -Dexec.mainClass=org.pyliu.learn.hello.Main
```

预期输出类似：`模型回答：LangChain4j 是一个面向 Java 的...`

## 延伸思考

1. `chat(String)` 一次只发一条消息。如果想让模型"记住"之前的对话，需要自己把历史消息也传进去——这正是模块 02（消息类型）和模块 04（记忆）要解决的。
2. 试着把 `baseUrl` 换成 `https://dashscope.aliyuncs.com/compatible-mode/v1`、`modelName` 换成 `qwen-plus`，你会发现**业务代码一行没改**就能换厂商——这就是模型无关性的价值。
3. 打开 `logRequests(true)` 后，观察控制台打印的 JSON：`messages` 数组里那条消息的 `role` 是 `user`。下一模块会讲到 `system` / `user` / `assistant` 三种角色。
