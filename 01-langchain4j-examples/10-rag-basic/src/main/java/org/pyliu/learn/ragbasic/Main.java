package org.pyliu.learn.ragbasic;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/**
 * 模块 10：RAG（检索增强生成）—— 手动实现全流程。
 *
 * <p>RAG 解决的核心问题：模型不知道你的<b>私有数据</b>，还会<b>一本正经地胡说</b>（幻觉）。
 * 做法：先从你的知识库里检索出相关内容，再让模型<b>基于这些内容</b>回答。
 *
 * <p>三步走：① 索引（切分 → 向量化 → 入库）② 检索（问题向量化 → 找最相似片段）③ 生成（拼进提示词）。
 */
public class Main {

    public static void main(String[] args) {
        // 模型不知道的"公司内部资料"
        String knowledge = """
                我们公司叫「极客科技」，成立于 2020 年，总部在杭州。
                主营产品是智能客服机器人，支持 7x24 小时在线服务。
                创始人是李雷，CTO 是韩梅梅。
                公司文化是：简单、开放、极致。
                """;

        // ① 索引阶段：切分 → 向量化 → 存入向量库
        List<TextSegment> segments = DocumentSplitters.recursive(80, 20)
                .split(Document.from(knowledge));
        EmbeddingModel embeddingModel = newEmbeddingModel();
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();
        for (TextSegment seg : segments) {
            store.add(embeddingModel.embed(seg).content(), seg);
        }
        System.out.println("已索引 " + segments.size() + " 个片段\n");

        // ② 检索阶段：问题向量化，找最相似的 3 个片段
        String question = "公司是哪一年成立的？总部在哪？";
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        List<EmbeddingMatch<TextSegment>> matches = store.search(
                        EmbeddingSearchRequest.builder()
                                .queryEmbedding(queryEmbedding)
                                .maxResults(3)
                                .build())
                .matches();

        StringBuilder context = new StringBuilder();
        for (EmbeddingMatch<TextSegment> m : matches) {
            context.append("- ").append(m.embedded().text()).append("\n");
        }
        System.out.println("检索到的最相关片段：\n" + context);

        // ③ 生成阶段：把检索结果拼进提示词，让模型基于资料回答
        ChatModel chatModel = newChatModel();
        String answer = chatModel.chat(
                new SystemMessage("请只根据提供的资料回答问题，资料里没有相关信息就回答「不知道」。"),
                UserMessage.from("资料：\n" + context + "\n问题：" + question)
        ).aiMessage().text();
        System.out.println("模型回答：" + answer);
    }

    private static ChatModel newChatModel() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置环境变量 DEEPSEEK_API_KEY");
        }
        return OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .build();
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
