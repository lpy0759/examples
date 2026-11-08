# 模块 09：嵌入与向量相似度

## 模块目标

理解 Embedding（向量化）和余弦相似度——这是后面 RAG 模块的地基。

## 核心概念讲解

### 1. 什么是 Embedding

Embedding 把一段文本映射成一组浮点数（向量），例如 1024 维。**语义越相近的文本，向量方向越接近**。"猫"和"小猫"的向量很接近，和"汽车"的向量差很远。

### 2. 余弦相似度

度量两个向量方向有多接近：值在 `[-1, 1]`，越接近 1 表示越相似。RAG 里就是用它从一堆文本里找出"和问题最像"的片段。

### 3. 模型选择

DeepSeek 不提供 embedding 接口，所以本模块和 RAG 用**通义千问 DashScope** 的 `text-embedding-v3`（OpenAI 兼容，仍用 `OpenAiEmbeddingModel`）。

## 代码拆解

```java
EmbeddingModel model = OpenAiEmbeddingModel.builder()
        .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
        .modelName("text-embedding-v3").build();

Embedding cat = model.embed("猫").content();   // Response<Embedding>.content() 取向量
float[] v = cat.vector();                       // 拿到向量数组
cosine(cat, kitten);                            // 手动算余弦相似度
```

## 运行方式

```bash
export DASHSCOPE_API_KEY=sk-xxx
mvn -pl 09-embeddings exec:java -Dexec.mainClass=org.pyliu.learn.embedding.Main
```

## 延伸思考

1. 预期结果：`相似度(猫, 小猫) > 相似度(猫, 汽车)`，说明向量确实编码了语义。
2. 向量维度越高，通常表达能力越强，但存储和计算成本也越高。
3. 真实 RAG 里不会自己手算相似度，而是交给向量数据库（模块 10 的 `EmbeddingStore`）来做近似最近邻检索。
