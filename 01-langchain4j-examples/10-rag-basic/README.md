# 模块 10：RAG 基础（手动实现）

## 模块目标

手动走一遍 RAG（检索增强生成）的完整流程，理解它到底在做什么。

## 核心概念讲解

### 1. RAG 解决什么问题

- 模型不知道你的**私有数据**（公司资料、产品文档……）；
- 模型会**幻觉**——一本正经地编造它不知道的事。

RAG 的思路：先从你的知识库里**检索**出相关内容，再让模型**基于这些内容**回答，从而"有据可依"。

### 2. 三步流程

```
① 索引 Indexing  ：文档 → 切分成片段 → 向量化 → 存入向量库
② 检索 Retrieval ：问题 → 向量化 → 在向量库里找最相似的 Top-K 片段
③ 生成 Generate  ：把检索到的片段 + 问题 拼进提示词 → 模型回答
```

## 代码拆解

```java
// ① 切分 + 向量化 + 入库
List<TextSegment> segments = DocumentSplitters.recursive(80, 20).split(Document.from(knowledge));
for (TextSegment seg : segments) {
    store.add(embeddingModel.embed(seg).content(), seg);
}

// ② 检索 Top-K
Embedding q = embeddingModel.embed(question).content();
List<EmbeddingMatch<TextSegment>> matches = store.search(
        EmbeddingSearchRequest.builder().queryEmbedding(q).maxResults(3).build()).matches();

// ③ 拼接生成
model.chat(system("只根据资料回答，没有就答不知道"),
           user("资料：\n" + context + "\n问题：" + question));
```

## 运行方式

```bash
export DEEPSEEK_API_KEY=sk-xxx
export DASHSCOPE_API_KEY=sk-xxx
mvn -pl 10-rag-basic exec:java -Dexec.mainClass=org.pyliu.learn.ragbasic.Main
```

## 延伸思考

1. `DocumentSplitters.recursive(片段长度, 重叠长度)`：重叠让片段之间不丢上下文。
2. 这里用 `InMemoryEmbeddingStore`（内存），生产要换真正的向量数据库（Milvus / pgvector / Redis 等）。
3. 关键提示词："只根据资料回答，没有就答不知道"能显著抑制幻觉。试着去掉它，对比效果。
