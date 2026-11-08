# 模块 12：Spring Boot 集成

## 模块目标

把 LangChain4j 用在一个真实的 Spring Boot Web 项目里，通过 REST 接口对外提供能力。

## 核心概念讲解

### 1. 生产级集成方式

前面的模块都是"一个 `main` 方法跑完就走"；真实项目里，模型和 AiService 应该交给 **Spring 容器**管理，业务代码通过**依赖注入**使用。

### 2. 分层

```
Controller（REST 接口） → Assistant（AiService 接口） → ChatModel（OpenAI 兼容，连 DeepSeek）
```

- `ChatModel` 定义成 `@Bean`，连接信息从 `application.yml` 读（用 `@Value` 注入）。
- `Assistant` 用 `AiServices.builder(...)` 生成后也定义成 `@Bean`。
- `ChatController` 通过构造器注入 `Assistant`，对外暴露 `GET /chat?q=...`。

> 说明：LangChain4j 也提供官方 Spring Boot Starter（`langchain4j-open-ai-spring-boot-starter`，含自动装配和 `@AiService` 注解），
> 但当前 1.x 的 Starter 是独立 beta 版本号，与本项目核心库 1.23.0 不统一。为保持稳定、并让你看清每个 Bean 的由来，
> 本模块用手动 `@Bean` 方式演示——理解了它，再上 Starter 就是"把这里手写的换成注解"而已。

## 代码拆解

```java
// AiConfig：装配 Bean
@Bean ChatModel chatModel(@Value("${langchain4j.base-url}") ...,
                          @Value("${langchain4j.api-key}") ...,
                          @Value("${langchain4j.model-name}") ...) { ... }
@Bean Assistant assistant(ChatModel m) { return AiServices.builder(Assistant.class).chatModel(m).build(); }

// ChatController：对外接口
@GetMapping("/chat")
public String chat(@RequestParam(defaultValue="介绍一下你自己") String q) { return assistant.chat(q); }
```

```yaml
# application.yml
langchain4j:
  base-url: https://api.deepseek.com
  api-key: ${DEEPSEEK_API_KEY}    # 从环境变量读，不硬编码密钥
  model-name: deepseek-chat
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn -pl 12-spring-boot spring-boot:run

# 另开终端测试
curl "http://localhost:8080/chat?q=用一句话介绍SpringBoot"
```

## 延伸思考

1. 换模型只需改 `application.yml` 的 `base-url`/`model-name`，代码零改动。
2. 可给 Controller 加上流式输出（SSE），配合模块 08 的 `TokenStream` 做打字机效果。
3. 官方 Starter 里，`@AiService` 注解 + `langchain4j.open-ai.chat-model.*` 配置能省掉这些手写 Bean，熟悉后推荐迁移。
