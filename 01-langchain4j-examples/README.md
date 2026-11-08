# LangChain4j 学习项目

一个从零开始、由浅入深学习 [LangChain4j](https://github.com/langchain4j/langchain4j) 的 Maven 多模块工程。
每个模块都是一个**可独立编译运行**的子项目，内含 `Main` 示例代码 + 中文 `README.md` 讲解。

## 前置条件

- **JDK 21**（本项目用 `maven-compiler-plugin` 指定 `release=21`）
- **Maven 3.9+**
- **API Key**（二选一或都准备）：
  - `DEEPSEEK_API_KEY`：聊天模型（DeepSeek），模块 01–08、10–13 需要
  - `DASHSCOPE_API_KEY`：向量/多模态（通义千问 DashScope），模块 09–11、14 需要

```bash
export DEEPSEEK_API_KEY=sk-你的DeepSeek密钥
export DASHSCOPE_API_KEY=sk-你的DashScope密钥
```

> 说明：聊天用 DeepSeek（OpenAI 兼容接口），embedding / 多模态用通义千问 DashScope（因为 DeepSeek 不提供 embedding 接口）。

## 模块清单（难度渐进）

| 模块 | 主题 | 需要的 Key | 一句话 |
|---|---|---|---|
| [01-hello-world](01-hello-world/README.md) | 快速上手 | DeepSeek | 跑通第一个对话，理解模型无关性 |
| [02-prompt-and-message](02-prompt-and-message/README.md) | 消息与提示词 | DeepSeek | 三种消息角色 + 提示词模板 |
| [03-ai-services](03-ai-services/README.md) | 声明式 API | DeepSeek | 接口定义 → 自动实现（核心） |
| [04-chat-memory](04-chat-memory/README.md) | 对话记忆 | DeepSeek | 让模型记住上下文、按用户隔离 |
| [05-structured-output](05-structured-output/README.md) | 结构化输出 | DeepSeek | 返回强类型对象而非字符串 |
| [06-tools](06-tools/README.md) | 工具调用 | DeepSeek | 让模型调用你的 Java 方法 |
| [07-agent](07-agent/README.md) | 智能体 | DeepSeek | 工具 + 记忆组合的自主 Agent |
| [08-streaming](08-streaming/README.md) | 流式输出 | DeepSeek | 逐字输出 |
| [09-embeddings](09-embeddings/README.md) | 嵌入与向量 | DashScope | 语义向量 + 余弦相似度 |
| [10-rag-basic](10-rag-basic/README.md) | RAG 基础 | 两者 | 手动实现检索增强生成 |
| [11-rag-advanced](11-rag-advanced/README.md) | RAG 进阶 | 两者 | 内置 RAG 流水线 |
| [12-spring-boot](12-spring-boot/README.md) | Spring Boot 集成 | DeepSeek | 真实 Web 项目集成 |
| [13-observability](13-observability/README.md) | 可观测性 | DeepSeek | 日志 / token / 重试 |
| [14-multimodal](14-multimodal/README.md) | 多模态 | DashScope | 图像理解 |

## 如何运行

每个模块一个命令（模块自包含，无需先构建父工程）：

```bash
# 示例：运行模块 01
mvn -pl 01-hello-world exec:java -Dexec.mainClass=org.pyliu.learn.hello.Main
```

Spring Boot 模块（12）用：

```bash
mvn -pl 12-spring-boot spring-boot:run
# 然后访问：curl "http://localhost:8080/chat?q=你好"
```

## 目录结构

```
langchain4j-learining/          # 父 POM：统一 BOM 版本 + 插件 + 子模块列表
├── pom.xml
├── 01-hello-world/             # 每个模块 = pom.xml + src/main/java(示例) + README.md(讲解)
├── 02-prompt-and-message/
├── ...
└── 14-multimodal/
```

## 技术栈

- LangChain4j **1.23.0**（通过 `dev.langchain4j:langchain4j-bom` 统一管理版本）
- 模型接入：`langchain4j-open-ai`（OpenAI 兼容客户端，`baseUrl` 指向 DeepSeek / DashScope）
- 日志：`slf4j-simple`（Spring Boot 模块用自带 Logback）

> 换模型只需改 `baseUrl` + `modelName` + `apiKey`，业务代码一行不用改——这正是 LangChain4j 模型无关性的价值。
