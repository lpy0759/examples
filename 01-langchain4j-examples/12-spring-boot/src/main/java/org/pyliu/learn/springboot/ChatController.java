package org.pyliu.learn.springboot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 一个简单的 REST 接口：GET /chat?q=... 会调用 AiService 并返回回答。
 */
@RestController
public class ChatController {

    private final Assistant assistant;

    public ChatController(Assistant assistant) {
        this.assistant = assistant;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(defaultValue = "介绍一下你自己") String q) {
        return assistant.chat(q);
    }
}
