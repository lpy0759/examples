# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库中工作时提供指引。

## 这是什么

一个 LangChain4j 学习项目 —— 单仓库、多模块的 Maven 工程，从入门到进阶讲解 LangChain4j。共 14 个编号模块（`01-hello-world` … `14-multimodal`），每个模块都是**自包含的演示**（独立的 `pom.xml` + 可运行的 `Main` + 中文 `README.md`）。模块之间没有业务依赖；父 `pom.xml` 只统一版本和插件配置。

## 常用命令

```bash
# 全量构建（所有模块）
mvn clean install -DskipTests

# 编译单个模块
mvn -pl 01-hello-world compile

# 运行标准模块（主类为 org.pyliu.learn.<包名>.Main）
mvn -pl 01-hello-world exec:java -Dexec.mainClass=org.pyliu.learn.hello.Main

# 运行 Spring Boot 模块
mvn -pl 12-spring-boot spring-boot:run
# 然后访问：curl "http://localhost:8080/chat?q=你好"
```

必需的环境变量（由演示代码读取，绝不硬编码）：

- `DEEPSEEK_API_KEY` —— 聊天模型，模块 01–08、10–13 需要
- `DASHSCOPE_API_KEY` —— 向量/多模态，模块 09–11、14 需要（模块 10–11 两个都要）

## 版本与大坑（API 差异）

LangChain4j 通过父 pom 的 `dev.langchain4j:langchain4j-bom` 锁定在 **1.23.0**。这是 **1.x API，不是网上大量教程里的 0.x API**——不要照抄 0.x 示例。关键的 1.x 差异：

- `ChatLanguageModel` → **`ChatModel`**（`dev.langchain4j.model.chat.ChatModel`）
- 消息类在 `dev.langchain4j.data.message`（`SystemMessage` / `UserMessage` / `AiMessage`）
- 核心接口在 `langchain4j-core`（`dev.langchain4j.model.*`、`dev.langchain4j.rag.*`、`dev.langchain4j.store.embedding.*`）
- `AiServices` 是 `dev.langchain4j.service.AiServices`；`@SystemMessage`/`@UserMessage`/`@V`/`@MemoryId` 在 `dev.langchain4j.service`；`@Tool`/`@P` 在 `dev.langchain4j.agent.tool`；`@Description` 在 `dev.langchain4j.model.output.structured`
- `model.chat(String)` 返回 `String`；`model.chat(ChatMessage...)` / `chat(List<ChatMessage>)` 返回 `ChatResponse`——取文本用 `.aiMessage().text()`

对某个 API 名不确定时，直接查 1.23.0 的 jar 而不要凭记忆：`javap -cp <jars> dev.langchain4j...`（jar 在 `~/.m2`）。

## 模型接线（模型无关）

全部使用 `langchain4j-open-ai`（OpenAI 兼容客户端）+ `baseUrl` 覆盖——绝不用 DeepSeek/Qwen 专用模块：

- 聊天：`OpenAiChatModel.baseUrl("https://api.deepseek.com").modelName("deepseek-chat")`
- 向量：`OpenAiEmbeddingModel.baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1").modelName("text-embedding-v3")`
- 视觉：`OpenAiChatModel.baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1").modelName("qwen-vl-max")`

DeepSeek 没有 embedding 接口，所以向量/视觉走 DashScope（通义千问）。每个模块在自己的私有 `newModel()` / `newEmbeddingModel()` 辅助方法里构建模型并读取环境变量——模块刻意自包含（少量配置重复是有意为之）。

## 约定

- 包名：`org.pyliu.learn.<短名>`，短名为 `hello`、`prompt`、`aiservice`、`memory`、`structured`、`tools`、`agent`、`streaming`、`embedding`、`ragbasic`、`ragadvanced`、`springboot`、`observability`、`multimodal`。入口类是 `Main`，唯独 `12-spring-boot` 用 `DemoApplication` + Spring `@Bean` 装配。
- 每个模块的 `README.md` 遵循固定结构：模块目标 → 核心概念讲解 → 代码拆解 → 运行方式 → 延伸思考。编辑/新增模块时保持一致。
- 父 pom 在 `<build><plugins>` 里给编译器开启了 `-parameters`；这是 `@P`/`@V` 参数绑定和 record 反序列化所必需的。
- 模块 12 引入 `spring-boot-dependencies`（3.4.1），**没有**使用 LangChain4j 的 Spring Boot Starter（其 1.x 版本号是独立的 beta 轨）；它用 `@Bean` 手动装配 `ChatModel` + `AiServices`。`AiConfig` 里说明了这一选择。
