# 模块 03：AiServices（声明式 API）

## 模块目标

掌握 LangChain4j 最核心、最常用的能力：**只定义接口，框架自动生成实现**。

## 核心概念讲解

### 1. 声明式 vs 命令式

- **命令式**（模块 01/02）：手动拼消息 → 手动调 `model.chat()` → 手动解析返回。啰嗦、易错。
- **声明式**（本模块）：定义一个接口，写清楚"要什么"，`AiServices` 用**动态代理**在运行时生成实现，自动完成"拼提示词 → 调模型 → 解析返回"。

### 2. 三个常用注解

- `@SystemMessage`：给整个接口固定人设（系统提示词）。
- `@UserMessage`：定义用户消息模板，可用 `{{占位符}}`。
- `@V("名字")`：把方法参数绑定到模板里的占位符。

## 代码拆解

```java
interface Translator {
    @SystemMessage("你是一名专业的中英翻译，只输出译文。")
    String translate(String text);
}

Translator t = AiServices.builder(Translator.class)
        .chatModel(model)          // 注入模型
        .build();
String s = t.translate("Hello");   // 像调用普通接口一样调用
```

带参数模板：

```java
interface Explainer {
    @UserMessage("请用 {{language}} 简单解释：{{topic}}")
    String explain(@V("language") String language, @V("topic") String topic);
}
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn -pl 03-ai-services exec:java -Dexec.mainClass=org.pyliu.learn.aiservice.Main
```

## 延伸思考

1. `AiServices.builder(...)` 上还能挂很多东西：`.chatMemory(...)`（记忆）、`.tools(...)`（工具）、`.retrievalAugmentor(...)`（RAG）——后续模块都是在往这个 builder 上"加料"。
2. 接口方法的返回值可以是 `String`，也可以是**自定义对象 / enum / List**——这就是模块 05 的结构化输出。
3. 把业务逻辑写成接口，还有个好处：**可测试、可 mock**，业务代码不依赖任何厂商。
