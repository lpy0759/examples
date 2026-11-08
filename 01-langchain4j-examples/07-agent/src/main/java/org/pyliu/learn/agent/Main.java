package org.pyliu.learn.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 模块 07：智能体（Agent）。
 *
 * <p>Agent = 模型 + 工具 + 记忆。模型不再是"一问一答"，而是能<b>自主决定</b>：
 * 该调用哪个工具、调几次、什么时候停，最终给出答案。这正是流行的 ReAct
 * （Reasoning + Acting，推理-行动交替）思想。
 */
public class Main {

    // 一组工具：时间、天气、计算
    static class AgentTools {
        @Tool("获取当前的日期和时间")
        String getCurrentTime() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        @Tool("查询指定城市的天气")
        String getWeather(@P(name = "city", description = "城市名") String city) {
            return city + " 今天多云，气温 22°C。";
        }

        @Tool("计算两个数的和")
        double add(@P(name = "a", description = "加数") double a,
                   @P(name = "b", description = "加数") double b) {
            return a + b;
        }
    }

    interface Agent {
        String answer(String question);
    }

    public static void main(String[] args) {
        ChatModel model = newModel();

        AgentTools tools = new AgentTools();
        Agent agent = AiServices.builder(Agent.class)
                .chatModel(model)
                .tools(tools)                                  // 多个工具
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20)) // 记忆
                .build();

        // 需要"时间"和"天气"两个工具协同
        System.out.println("[Agent] " + agent.answer("现在几点了？北京天气怎么样？"));

        // 有记忆：能接住上一轮的上下文（"上海"是新一轮城市）
        System.out.println("[Agent-记忆] " + agent.answer("那上海呢？"));
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
