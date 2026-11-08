# 模块 08：流式输出（Streaming）

## 模块目标

实现"打字机"式的逐字输出，而不是等模型全部生成完才一次性返回。

## 核心概念讲解

### 1. 同步 vs 流式

- **同步** `chat(...)`：阻塞等模型生成完所有 token，一次性拿到完整回答。
- **流式**：模型一边生成、一边把每个 token 推给你，你能立刻看到文字"打出来"，体验更好（ChatGPT 就是这种）。

### 2. 两种用法

1. **底层模型 + 回调**：实现 `StreamingChatResponseHandler`，`onPartialResponse` 每个 token 触发一次。
2. **AiServices + TokenStream**：接口方法返回 `TokenStream`，链式注册回调，更声明式。

## 代码拆解

```java
// 底层流式
model.chat("讲个小故事", new StreamingChatResponseHandler() {
    public void onPartialResponse(String partial) { System.out.print(partial); }
    public void onCompleteResponse(ChatResponse r) { System.out.println("\n完成"); }
    public void onError(Throwable e) { e.printStackTrace(); }
});

// 声明式流式
interface Streamer { TokenStream stream(String msg); }
streamer.stream("...")
        .onPartialResponse(t -> System.out.print(t))
        .onCompleteResponse(r -> System.out.println("\n完成"))
        .onError(Throwable::printStackTrace)
        .start();   // 必须 start 才会真正发起请求
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn -pl 08-streaming exec:java -Dexec.mainClass=org.pyliu.learn.streaming.Main
```

## 延伸思考

1. `TokenStream.onPartialResponse` 收到的是**增量片段**，不是累计全文；要拼接需自己 `StringBuilder` 累加。
2. Web 场景下，流式通常配合 **SSE（Server-Sent Events）** 推给前端，Spring Boot 模块（12）可以这么扩展。
3. 流式还能结合工具调用（`onPartialToolCall`）、RAG（`onRetrieved`）等，观察中间过程。
4. **流式是异步的**：`chat()` / `start()` 立刻返回，token 在后台线程回调。命令行演示里必须用 `CountDownLatch` 阻塞 `main` 线程（或 `Thread.sleep`），否则程序会在回复到达前退出、什么都看不到。Web 场景没有这个问题，因为 Servlet/框架的事件循环本身常驻。
