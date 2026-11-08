package org.pyliu.learn.ragadvanced;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * 模块 11：RAG 进阶 —— 使用 LangChain4j 内置 RAG 流水线。
 *
 * <p>模块 10 手动拼了三步。LangChain4j 把这套流程抽象成一条<b>可插拔的流水线</b>：
 * 检索（{@code ContentRetriever}）→ 聚合（{@code ContentAggregator}）→ 注入（{@code ContentInjector}），
 * 由 {@code RetrievalAugmentor} 统一编排，挂到 AiServices 上后<b>自动生效</b>。
 */
public class Main {

    interface Assistant {
        @SystemMessage("请只根据提供的资料回答问题，资料里没有相关信息就回答「不知道」。")
        String ask(String question);
    }

    public static void main(String[] args) {
        String knowledge = """
                我们公司叫「极客科技」，成立于 2020 年，总部在杭州。
                主营产品是智能客服机器人，支持 7x24 小时在线服务。
                创始人是李雷，CTO 是韩梅梅。
                """;

        EmbeddingModel embeddingModel = newEmbeddingModel();
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        // ① 用 EmbeddingStoreIngestor 一行式完成"切分 → 向量化 → 入库"
        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(80, 20))
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build()
                .ingest(Document.from(knowledge));

        // ② 配置检索器（从向量库找最相关片段）
        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .build();

        // ③ 组装 RAG 增强器（检索 + 聚合 + 注入，用默认即可）
        DefaultRetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .build();

        // ④ 挂到 AiServices：之后每次调用都会自动检索并注入上下文
        ChatModel chatModel = newChatModel();
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(augmentor)
                .build();

        System.out.println("Q: 公司的主营产品是什么？\nA: " + assistant.ask("公司的主营产品是什么？"));
        System.out.println("Q: 创始人是谁？\nA: " + assistant.ask("创始人是谁？"));
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
