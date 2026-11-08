# 模块 06：工具调用（Function Calling）

## 模块目标

让模型在需要时**调用你写好的 Java 方法**，补足它"不会算、不知道实时数据"的短板。

## 核心概念讲解

### 1. 模型的短板

大模型只会"生成文本"，它：
- **算不准**：复杂计算会出错；
- **不知道外部世界**：实时天气、股价、数据库里的数据，它一概不知。

工具调用（Tool Calling / Function Calling）让模型在需要时**请求执行某个函数**，拿到结果再继续回答。

### 2. 工作流程（一轮 round-trip）

```
用户提问 → 模型决定"我要调用工具 X，参数是..." → LangChain4j 执行方法
        → 结果回传给模型 → 模型基于结果生成最终回答
```

### 3. 关键注解

- `@Tool("描述")`：把一个方法暴露为工具，描述越清晰模型越知道何时调用。
- `@P(name=..., description=...)`：描述参数。

## 代码拆解

```java
class WeatherService {
    @Tool("查询指定城市的天气")
    String getWeather(@P(name = "city", description = "城市名") String city) {
        return city + " 今天晴，25°C";  // 实际中调真实 API
    }
}

Assistant a = AiServices.builder(Assistant.class)
        .chatModel(model)
        .tools(new WeatherService())   // 注册工具
        .build();
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn -pl 06-tools exec:java -Dexec.mainClass=org.pyliu.learn.tools.Main
```

## 延伸思考

1. **观察工具调用**：给模型加 `.logRequests(true).logResponses(true)` 并调高日志级别到 debug（参考模块 13），能在 JSON 里看到 `tool_calls` 字段。
2. 模型可能"幻觉"出不存在的工具名或参数，LangChain4j 提供了 `hallucinatedToolNameStrategy` 等兜底配置。
3. 一个 `@Tool` 方法可以有返回值（同步）或返回 `Future`（异步），还能指定 `returnBehavior`。
