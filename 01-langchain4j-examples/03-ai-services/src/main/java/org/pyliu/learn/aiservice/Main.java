package org.pyliu.learn.aiservice;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 模块 03：AiServices —— LangChain4j 的声明式 API（最核心、最常用）。
 *
 * <p>你只需要定义一个 <b>接口</b>，{@link AiServices} 会通过动态代理在运行时生成实现，
 * 自动完成"拼提示词 → 调模型 → 解析返回"的全过程。这就是"声明式"：你声明"想干什么"，
 * 而不关心"怎么干"。
 */
public class Main {

    // 1) 最朴素的接口：一个方法 = 一次对话
    interface Assistant {
        String chat(String message);
    }

    // 2) @SystemMessage 给整个接口固定"人设"
    interface Translator {
        @SystemMessage("你是一名专业的中英翻译，只输出翻译结果，不要任何解释。")
        String translate(String text);
    }

    // 3) @UserMessage 定义提示词模板，@V 把方法参数填进占位符 {{...}}
    interface Explainer {
        @UserMessage("请用 {{language}} 简单解释：{{topic}}")
        String explain(@V("language") String language, @V("topic") String topic);
    }

    public static void main(String[] args) {
        ChatModel model = newModel();

        // 1) 最基础用法
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .build();
        System.out.println("[基础接口] " + assistant.chat("用一句话介绍什么是人工智能"));

        // 2) 带人设（系统提示词）
        Translator translator = AiServices.builder(Translator.class)
                .chatModel(model)
                .build();
        System.out.println("[人设翻译] " + translator.translate("Hello, how are you doing today?"));

        // 3) 带参数模板
        Explainer explainer = AiServices.builder(Explainer.class)
                .chatModel(model)
                .build();
        System.out.println("[参数模板] " + explainer.explain("中文", "什么是递归"));
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
