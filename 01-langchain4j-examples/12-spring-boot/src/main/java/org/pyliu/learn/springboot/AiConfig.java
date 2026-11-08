package org.pyliu.learn.springboot;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 把 LangChain4j 的对象交给 Spring 容器管理。
 *
 * <p>要点：模型连接信息不写死在代码里，而是放到 application.yml，通过 @Value 注入，
 * 这样换模型/换 Key 只需改配置，无需改代码——和生产实践一致。
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatModel chatModel(@Value("${langchain4j.base-url}") String baseUrl,
                               @Value("${langchain4j.api-key}") String apiKey,
                               @Value("${langchain4j.model-name}") String modelName) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean
    public Assistant assistant(ChatModel chatModel) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .build();
    }
}
