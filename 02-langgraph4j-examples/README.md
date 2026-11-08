# langgraph4j 系统学习教程

一套**由浅入深、按模块组织、可运行可验证**的 [langgraph4j](https://github.com/langgraph4j/langgraph4j) 学习工程。
langgraph4j 是 [LangGraph](https://www.langchain.com/langgraph)（图编排框架）的 Java 移植，用于把
「大模型调用 + 工具 + 状态流转」组织成一张可控制的**状态图（StateGraph）**。

本工程用 Maven 多模块组织，**每个模块 = 一个独立可运行的 Demo（`Main`）+ 一份中文讲解 README**。
版本统一收敛在父 `pom.xml`，模块间按学习顺序递进，请按编号顺序学习。

## 环境前置条件

| 依赖 | 版本 | 说明 |
|---|---|---|
| JDK | 17+（本机 21） | langgraph4j 1.9.x 编译目标为 17 |
| Maven | 3.9+ | 用于构建与运行 |
| 网络 | 可访问 Maven Central | 国内建议配置阿里云镜像（见下方说明） |

> 本机 `~/.m2/settings.xml` 已配置阿里云镜像 `https://maven.aliyun.com/repository/public`，
> 否则首次拉取依赖可能较慢。

## 统一版本

- **langgraph4j** `1.9.7`（`langgraph4j-bom` 统一管理）
- **LangChain4j** `1.22.0`（注意：BOM 不含它，由父 pom 手动 pin）
- 日志 `slf4j-simple` `2.0.17`

## 模块清单

| # | 模块 | 主题 | 关键 API | LLM | 验证 |
|---|---|---|---|---|---|
| 01 | [hello-graph](01-hello-graph) | 最小图：State / Node / Edge / START / END | `StateGraph`、`addNode`、`addEdge`、`AgentState`、`invoke` | 无 | ✅ 实跑 |
| 02 | [state](02-state) | 状态深入：自定义状态类、通道 / reducer / appender | `AgentState`、`Channels`、`Channel`、`AppenderChannel` | 无 | ✅ 实跑 |
| 03 | [conditional-routing](03-conditional-routing) | 条件路由：按状态选下一节点 | `addConditionalEdges`、`AsyncEdgeAction` | 无 | ✅ 实跑 |
| 04 | [cycles](04-cycles) | 循环：条件边回环 + recursionLimit 防死循环 | `addConditionalEdges` 回边、`recursionLimit` | 无 | ✅ 实跑 |
| 05 | [parallel](05-parallel) | 并行：fan-out / fan-in | 一节点多 `addEdge`、`CompletableFuture` 并发 | 无 | ✅ 实跑 |
| 06 | [streaming](06-streaming) | 流式输出：逐节点观察状态 | `stream`、`AsyncGenerator`、`NodeOutput` | 无 | ✅ 实跑 |
| 07 | [agent](07-agent) | AI Agent：LLM + 工具调用循环 | `ChatModel`、`MessagesState`/appender、工具循环 | Mock / 可选 OpenAI | ✅ mock 实跑；⚠️ 真 API 仅编译 |

## 运行方式

每个模块独立运行（在仓库根目录执行）：

```bash
# 编译并运行单个模块，例如 01：
mvn -pl 01-hello-graph compile exec:java

# 编译全部模块（验证整体可构建）：
mvn clean compile
```

模块 07 默认使用**离线假模型**，无需任何 Key 即可完整跑通 Agent 循环。若要切换到真实 OpenAI：

```bash
export LLM_PROVIDER=openai
export OPENAI_API_KEY=sk-...
export OPENAI_MODEL_NAME=gpt-4o-mini   # 可选，默认 gpt-4o-mini
mvn -pl 07-agent compile exec:java
```

## 验证程度说明（诚实标注）

- ✅ **已实跑验证**：模块 01–06，以及 07 的 mock 模型路径，均已在本机 `mvn ... exec:java` 跑通并核对输出。
- ⚠️ **仅编译验证**：模块 07 切换到真实 OpenAI 的代码（`ModelFactory` 中的 `OpenAiChatModel` 分支）
  已通过编译，但**未实跑**，因为需要有效的 `OPENAI_API_KEY`。使用该分支需自行提供 Key 与网络。

## 学习路径建议

1. **01 → 02**：先理解「图 = 节点 + 边 + 状态」，再理解「状态如何被合并（通道/reducer）」。
2. **03 → 04**：条件边是循环的基础；理解「路由」后，循环只是把回边指向自己。
3. **05 → 06**：并行与流式是「执行方式」的两个维度，都建立在同一张图上。
4. **07**：用所学组合出一个真正的 AI Agent，把 `ChatModel` 抽象接进图里。

每个模块 README 末尾的「延伸思考」会指向更进阶的主题（checkpoint 持久化、human-in-the-loop 中断、
子图、hooks、可视化、MCP 集成等），学完核心后可继续深入。
