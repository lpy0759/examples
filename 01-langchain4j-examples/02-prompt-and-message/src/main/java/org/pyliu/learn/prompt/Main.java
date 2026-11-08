package org.pyliu.learn.prompt;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模块 02：消息类型与提示词模板。
 *
 * <p>一个"对话"在底层就是一条 <b>消息列表</b>。三种角色：
 * <ul>
 *   <li>{@link SystemMessage}：系统设定（人设、规则），通常放最前面。</li>
 *   <li>{@link UserMessage}：用户说的话。</li>
 *   <li>{@link AiMessage}：模型自己说过的话（多轮时回传给模型）。</li>
 * </ul>
 *
 * <p>{@link PromptTemplate} 则负责把"带占位符的模板 + 变量"拼成最终的提示词。
 */
public class Main {

    public static void main(String[] args) {
        ChatModel model = newModel();

        // 1) 三种消息角色
        demoRoles(model);

        // 2) 多轮对话：把历史消息一起传给模型
        demoMultiTurn(model);

        // 3) 提示词模板：用变量动态生成提示词
        demoTemplate(model);
    }

    private static void demoRoles(ChatModel model) {
        SystemMessage system = new SystemMessage("你是一个只讲重点、回答不超过一句话的助手。");
        UserMessage user = UserMessage.from("什么是面向对象编程？");
        ChatResponse response = model.chat(system, user);
        System.out.println("[角色演示] " + response.aiMessage().text());
    }

    private static void demoMultiTurn(ChatModel model) {
        // 把 system / user / ai 历史消息按顺序放进去，模型就能"记住"上下文
        List<ChatMessage> history = new ArrayList<>();
        history.add(new SystemMessage("你是一个记忆力很好的助手。"));
        history.add(UserMessage.from("你好，我叫小明，今年 30 岁。"));
        history.add(AiMessage.from("你好小明！我记住了。"));
        history.add(UserMessage.from("我叫什么名字？多大年纪？"));

        ChatResponse response = model.chat(history);
        System.out.println("[多轮演示] " + response.aiMessage().text());
    }

    private static void demoTemplate(ChatModel model) {
        // {{text}} 是占位符，apply 时用变量替换
        PromptTemplate template = PromptTemplate.from("请把下面这段英文翻译成中文：\n\"\"\"\n{{text}}\n\"\"\"");
        Prompt prompt = template.apply(Map.of("text", "LangChain4j makes it easy to build LLM applications in Java."));
        String answer = model.chat(prompt.toUserMessage()).aiMessage().text();
        System.out.println("[模板演示] " + answer);
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
