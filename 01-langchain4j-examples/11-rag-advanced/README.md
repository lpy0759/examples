# 模块 11：RAG 进阶（内置流水线）

## 模块目标

用 LangChain4j 内置的 RAG 流水线，把模块 10 的三步封装成"挂上即用"。

## 核心概念讲解

### 1. RAG 流水线抽象

LangChain4j 把 RAG 拆成一组**可插拔的组件**，由 `RetrievalAugmentor` 统一编排：

| 阶段 | 接口 | 职责 |
|---|---|---|
| 检索 | `ContentRetriever` | 根据 Query 找到相关内容 |
| 聚合 | `ContentAggregator` | 合并/重排多个检索结果 |
| 注入 | `ContentInjector` | 把内容塞进提示词 |

`DefaultRetrievalAugmentor` 用默认的聚合器和注入器即可。

### 2. 挂到 AiServices 上

```java
AiServices.builder(Assistant.class)
        .chatModel(model)
        .retrievalAugmentor(augmentor)   // 挂上 RAG，之后每次调用自动检索+注入
        .build();
```

之后你调用 `assistant.ask(问题)` 时，框架自动完成"检索→注入→生成"，你完全无感知。

## 代码拆解

```java
// ① 入库：EmbeddingStoreIngestor 一行式"切分→向量化→存储"
EmbeddingStoreIngestor.builder()
        .documentSplitter(DocumentSplitters.recursive(80, 20))
        .embeddingModel(embeddingModel)
        .embeddingStore(store).build()
        .ingest(Document.from(knowledge));

// ② 检索器
EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(store).embeddingModel(embeddingModel).maxResults(3).build();

// ③ 增强器
DefaultRetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
        .contentRetriever(retriever).build();
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
export DASHSCOPE_API_KEY=sk-xxx
mvn -pl 11-rag-advanced exec:java -Dexec.mainClass=org.pyliu.learn.ragadvanced.Main
```

## 延伸思考

1. 模块 10 是"原理"，模块 11 是"工程用法"——先懂原理，再用框架，才不会黑盒。
2. 可以在流水线里插 `QueryTransformer`（改写/压缩问题）、`QueryRouter`（路由到不同数据源）等，能力更强。
3. 生产环境把 `InMemoryEmbeddingStore` 换成持久化向量库，`EmbeddingStore` 接口不变，只换实现。
