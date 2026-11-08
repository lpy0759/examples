package org.pyliu.learn.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * 模块 06：工具调用（Function Calling / Tool Calling）。
 *
 * <p>大模型有两大短板：<b>不会精确计算</b>、<b>不知道外部实时数据</b>。
 * "工具调用"让模型在需要时<b>请求执行你写好的 Java 方法</b>，拿到结果再继续回答。
 *
 * <p>工作流程：模型返回一个"工具调用请求"（方法名+参数）→ LangChain4j 执行对应方法
 * → 把返回值回传给模型 → 模型基于结果生成最终回答。这一轮叫一次 round-trip。
 */
public class Main {

    // 工具 1：精确计算（模型不擅长，交给 Java 算）
    static class Calculator {
        @Tool("计算两个数的和")
        double add(@P(name = "a", description = "第一个加数") double a,
                   @P(name = "b", description = "第二个加数") double b) {
            return a + b;
        }

        @Tool("计算两个数的乘积")
        double multiply(@P(name = "a", description = "第一个因数") double a,
                        @P(name = "b", description = "第二个因数") double b) {
            return a * b;
        }
    }

    // 工具 2：查询外部数据（模型完全不知道，必须调用工具）
    static class WeatherService {
        @Tool("查询指定城市的天气")
        String getWeather(@P(name = "city", description = "城市名，例如：北京") String city) {
            // 实际项目里这里应调用真实的天气 API
            return city + " 今天晴，气温 25°C，适合出行。";
        }
    }

    interface Assistant {
        String chat(String message);
    }

    public static void main(String[] args) {
        ChatModel model = newModel();

        // 1) 计算工具：交给 Java 得到精确结果
        Calculator calculator = new Calculator();
        Assistant calcAssistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(calculator)   // 把工具对象注册进去
                .build();
        System.out.println("[计算工具] " + calcAssistant.chat("请帮我算 123456 乘以 789 等于多少？"));

        // 2) 天气工具：模型调用工具"查到"外部数据再回答
        WeatherService weather = new WeatherService();
        Assistant weatherAssistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(weather)
                .build();
        System.out.println("[天气工具] " + weatherAssistant.chat("北京今天天气怎么样？"));
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
