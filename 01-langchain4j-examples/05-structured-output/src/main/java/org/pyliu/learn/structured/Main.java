package org.pyliu.learn.structured;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;

import java.util.List;

/**
 * 模块 05：结构化输出（Structured Outputs）。
 *
 * <p>让模型直接返回<b>强类型</b>的 Java 对象，而不是让你手动解析字符串。
 * 底层原理：LangChain4j 根据返回类型生成 JSON Schema，要求模型按 Schema 输出 JSON，
 * 再用 Jackson 反序列化成你的对象。
 *
 * <p>常见返回类型：自定义 POJO / record、枚举 enum、List、Map 等。
 */
public class Main {

    // record 的每个字段用 @Description 说明含义，模型会按说明填值
    record Person(
            @Description("姓名") String name,
            @Description("年龄") int age,
            @Description("所在城市") String city
    ) {}

    enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

    interface Extractor {
        Person extractPerson(String text);

        Sentiment classify(String text);

        List<String> extractKeywords(String text);
    }

    public static void main(String[] args) {
        ChatModel model = newModel();
        Extractor extractor = AiServices.builder(Extractor.class)
                .chatModel(model)
                .build();

        Person person = extractor.extractPerson("张三今年 30 岁，目前住在杭州。");
        System.out.println("[自定义对象] " + person);

        Sentiment sentiment = extractor.classify("这家餐厅的菜太难吃了，服务也很差！");
        System.out.println("[枚举分类] " + sentiment);

        List<String> keywords = extractor.extractKeywords(
                "LangChain4j 是 Java 生态下的 LLM 应用开发框架，支持 RAG、工具调用、记忆等能力。");
        System.out.println("[关键词列表] " + keywords);
    }

    private static ChatModel newModel() {
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
}
