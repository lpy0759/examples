# 模块 14：多模态（视觉理解）

## 模块目标

让模型"看懂"图片：把图片和文字一起发给视觉模型，让它描述或回答图片内容。

## 核心概念讲解

### 1. 多模态

普通对话模型只吃文本；**视觉模型（Vision Model）**还能吃图片。本模块用通义千问的 `qwen-vl-max`（OpenAI 兼容接口，故仍用 `OpenAiChatModel`）。

### 2. 消息里"附加"图片

一条消息可以同时包含文字 + 图片：图片用 `ImageContent` 表示，作为 `Content` 附加到 `UserMessage`。

```java
UserMessage message = UserMessage.from(
        "请用一句话描述这张图片里的内容。",   // 文字问题
        ImageContent.from(imageUrl));          // 图片（URL）
```

## 代码拆解

```java
ChatModel model = OpenAiChatModel.builder()
        .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
        .modelName("qwen-vl-max")   // 视觉模型
        .build();

String answer = model.chat(message).aiMessage().text();
```

## 运行方式

```bash
export DASHSCOPE_API_KEY=sk-xxx
mvn -pl 14-multimodal exec:java -Dexec.mainClass=org.pyliu.learn.multimodal.Main
```

> 示例图片是维基共享资源上的一张猫的照片，可换成任意可访问的图片 URL（或改用 `ImageContent` 的 base64 构造方式）。

## 延伸思考

1. 视觉模型可做：OCR（识别图中文字）、图片问答、商品图分析、截图理解等。
2. 除图片外，`Content` 还有音频等其他类型，多模态能力在持续扩展。
3. 结合 AiServices + 结构化输出（模块 05），可以"看发票图片 → 抽出金额/日期"等实用功能。
