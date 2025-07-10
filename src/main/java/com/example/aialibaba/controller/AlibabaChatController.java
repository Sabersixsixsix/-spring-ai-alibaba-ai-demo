package com.example.aialibaba.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/alibaba")
public class AlibabaChatController {

    @Autowired
    @Qualifier("ailbabaChatClient")
    private ChatClient chatClient;


    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query) {
        return chatClient.prompt(query)
                .call()
                .content();
    }

    @GetMapping(value = "/stream/chat", produces = "text/event-stream;charset=UTF-8")
    Flux<String> stream(@RequestParam(value = "query", defaultValue = "奈斯to秘特油") String query, @RequestParam(value = "mood") String mood ,HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt()
                .system(sp -> sp.param("mood", mood))
                .user(query)
                .stream()
                .content();
    }

    @GetMapping("/memory/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "我的头有点痛，怎么办")String query,
                             @RequestParam(value = "chat-id", defaultValue = "1") String chatId) {

        return chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().content();
    }

}
