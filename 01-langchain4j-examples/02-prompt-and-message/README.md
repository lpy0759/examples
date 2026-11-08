# 模块 02：消息类型与提示词模板

## 模块目标

理解"一次对话"在底层到底是什么——**一条消息列表**，并掌握三种消息角色和提示词模板。

## 核心概念讲解

### 1. 三种消息角色

调用大模型时，输入不是一句话，而是一串按顺序排列的消息：

| 角色 | 类 | 含义 |
|---|---|---|
| system | `SystemMessage` | 系统设定（人设、规则），放最前面 |
| user | `UserMessage` | 用户说的话 |
| assistant | `AiMessage` | 模型自己说过的话（多轮时回传） |

### 2. 多轮对话的本质

大模型本身是**无状态**的：它不记得上一轮。所谓"多轮对话"，就是每次都把**全部历史消息**重新发给它。本模块演示了这一点。

### 3. 提示词模板

`PromptTemplate` 用 `{{变量}}` 占位符写模板，`apply(变量)` 时再填值，避免字符串拼接。

## 代码拆解

```java
// 三种角色 + 一次对话
SystemMessage system = new SystemMessage("你是一个只讲重点的助手。");
UserMessage user = UserMessage.from("什么是面向对象编程？");
ChatResponse response = model.chat(system, user);   // 返回 ChatResponse

// 多轮：手动拼历史消息列表
List<ChatMessage> history = new ArrayList<>();
history.add(new SystemMessage("..."));
history.add(UserMessage.from("我叫小明"));
history.add(AiMessage.from("你好小明！"));
history.add(UserMessage.from("我叫什么？"));
model.chat(history);

// 模板
PromptTemplate.from("翻译：\n{{text}}").apply(Map.of("text", "..."));
```

- `model.chat(...)` 有多个重载：`chat(String)`、`chat(ChatMessage...)`、`chat(List<ChatMessage>)`。
- `ChatResponse.aiMessage().text()` 取出回答文本。

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
mvn -pl 02-prompt-and-message exec:java -Dexec.mainClass=org.pyliu.learn.prompt.Main
```

## 延伸思考

1. 手动拼历史消息很容易出错（忘记哪条、越堆越长）。模块 04 的 `ChatMemory` 会帮你自动管理。
2. 打开 `logRequests(true)`，观察底层 JSON 里 `messages` 数组的 `role` 字段，能直观看到这三种角色。
3. 多轮消息越长，token 消耗越多、成本越高——这引出了模块 04 里的"窗口"概念。
