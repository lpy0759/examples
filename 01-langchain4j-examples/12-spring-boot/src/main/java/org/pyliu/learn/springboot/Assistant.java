package org.pyliu.learn.springboot;

import dev.langchain4j.service.SystemMessage;

/**
 * 声明式 AiService 接口：业务代码只依赖这个接口，不关心底层模型。
 */
public interface Assistant {

    @SystemMessage("你是一个简洁、友好的中文助手，回答尽量不超过三句话。")
    String chat(String message);
}
