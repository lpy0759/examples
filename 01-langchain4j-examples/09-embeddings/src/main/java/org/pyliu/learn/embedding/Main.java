package org.pyliu.learn.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * 模块 09：嵌入（Embedding）与向量相似度 —— RAG 的基石。
 *
 * <p>Embedding 把一段文本映射成一组浮点数（向量），语义越相近的文本，向量方向越接近。
 * 用<b>余弦相似度</b>（0~1，越大越像）可以度量两个文本的语义距离。
 *
 * <p>注意：DeepSeek 不提供 embedding 接口，本模块及后续 RAG 使用<b>通义千问 DashScope</b> 的
 * {@code text-embedding-v3}，它同样是 OpenAI 兼容接口，所以仍用 {@link OpenAiEmbeddingModel}。
 */
public class Main {

    public static void main(String[] args) {
        EmbeddingModel model = newEmbeddingModel();

        // 把四组词变成向量
        Embedding cat = model.embed("猫").content();
        Embedding kitten = model.embed("小猫").content();
        Embedding dog = model.embed("狗").content();
        Embedding car = model.embed("汽车").content();

        System.out.println("向量维度：" + cat.dimension());
        System.out.printf("相似度(猫, 小猫) = %.3f%n", cosine(cat, kitten));
        System.out.printf("相似度(猫, 狗)   = %.3f%n", cosine(cat, dog));
        System.out.printf("相似度(猫, 汽车) = %.3f%n", cosine(cat, car));
        // 预期：(猫,小猫) 明显高于 (猫,汽车)，说明向量确实捕捉到了语义
    }

    // 余弦相似度：两个向量夹角的余弦值，越接近 1 表示越相似
    private static double cosine(Embedding a, Embedding b) {
        float[] x = a.vector();
        float[] y = b.vector();
        double dot = 0, nx = 0, ny = 0;
        for (int i = 0; i < x.length; i++) {
            dot += (double) x[i] * y[i];
            nx += (double) x[i] * x[i];
            ny += (double) y[i] * y[i];
        }
        return dot / (Math.sqrt(nx) * Math.sqrt(ny));
    }

    private static EmbeddingModel newEmbeddingModel() {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置环境变量 DASHSCOPE_API_KEY");
        }
        return OpenAiEmbeddingModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(apiKey)
                .modelName("text-embedding-v3")
                .build();
    }
}
