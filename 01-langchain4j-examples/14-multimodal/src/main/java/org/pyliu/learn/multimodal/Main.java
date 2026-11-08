package org.pyliu.learn.multimodal;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 模块 14：多模态（视觉理解）—— 让模型"看懂"图片。
 *
 * <p>普通对话模型只吃文本；视觉模型（Vision Model）还能吃图片。这里用通义千问的
 * {@code qwen-vl-max}（同样走 OpenAI 兼容接口），把图片 URL 作为消息的一部分发过去。
 */
public class Main {

    public static void main(String[] args) {
        // 一张公开图片（维基共享资源上的猫），可换成任意图片 URL
        String imageUrl = "https://pic2.zhimg.com/80/v2-4b1f74b90800d25440314bb897e1e05b_720w.webp";

        ChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(requireKey())
                .modelName("qwen3.8-max")   // 视觉理解模型
                .build();

        // 一条消息里同时带"文字问题 + 图片"
        UserMessage message = UserMessage.from(
                "请用一句话描述这张图片里的内容。",
                ImageContent.from(imageUrl)   // 图片作为 Content 附加到消息里
        );

        String answer = model.chat(message).aiMessage().text();
        System.out.println("回答：" + answer);
    }

    private static String requireKey() {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请先设置环境变量 DASHSCOPE_API_KEY");
        }
        return apiKey;
    }
}
