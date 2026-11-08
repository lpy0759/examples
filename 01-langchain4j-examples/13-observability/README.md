# 模块 13：可观测性与健壮性

## 模块目标

掌握进生产前必备的三件事：**日志排查、token 统计、异常处理与重试**。

## 核心概念讲解

### 1. 日志排查

模型调用是"黑盒"，出问题必须先看清"发了什么、回了什么"。打开：

```java
OpenAiChatModel.builder()
        .logRequests(true)     // 打印完整请求体
        .logResponses(true)    // 打印完整响应体
        .build();
```

注意：这些日志是 **DEBUG 级别**，需要把 slf4j 日志级别调到 debug 才看得到。本模块通过 `src/main/resources/simplelogger.properties` 完成：

```properties
org.slf4j.simpleLogger.defaultLogLevel=debug
```

### 2. Token 统计（成本）

```java
ChatResponse r = model.chat(system, user);
TokenUsage u = r.tokenUsage();
u.inputTokenCount();   // 输入 token
u.outputTokenCount();  // 输出 token
u.totalTokenCount();   // 总计（计费依据）
```

### 3. 异常处理与重试

模型接口是网络调用，会偶发超时、限流。生产需要重试（本模块用一个 `withRetry` 小封装演示核心思想：**失败就退避重试，超过次数才抛错**）。

## 代码拆解

```java
ChatResponse r = withRetry(3, () -> model.chat(system, user));  // 带重试的调用

private static <T> T withRetry(int max, Supplier<T> action) {
    for (int i = 1; ; i++) {
        try { return action.get(); }
        catch (Exception e) {
            if (i >= max) throw e;
            Thread.sleep(1000L * i);   // 退避
        }
    }
}
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn -pl 13-observability exec:java -Dexec.mainClass=org.pyliu.learn.observability.Main
```

## 延伸思考

1. 观察控制台打印的请求/响应 JSON，能直观理解前面所有模块在底层做了什么。
2. 生产建议用更成熟的熔断/重试库（Resilience4j、Spring Retry），并配合监控统计 token 成本。
3. LangChain4j 1.x 的底层 HTTP 客户端也内置重试能力，可通过 `httpClientBuilder` 配置。
