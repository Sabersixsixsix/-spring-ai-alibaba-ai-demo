package com.example.aialibaba.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.image.*;
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
@Tag(name = "阿里云对话")
public class AlibabaChatController {

    @Autowired
    @Qualifier("ailbabaChatClient")
    private ChatClient chatClient;

    @Autowired
    @Qualifier("dashScopeImageModel")
    private ImageModel imageModel;

    @Operation(summary = "简单对话")
    @GetMapping("/simple/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "你好，很高兴认识你，能简单介绍一下自己吗？") String query) {
        return chatClient.prompt(query)
                .call()
                .content();
    }

    @Operation(summary = "流式对话")
    @GetMapping(value = "/stream/chat", produces = "text/event-stream;charset=UTF-8")
    Flux<String> stream(@RequestParam(value = "query", defaultValue = "奈斯to秘特油") String query, @RequestParam(value = "mood") String mood ,HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt()
                .system(sp -> sp.param("mood", mood))
                .user(query)
                .stream()
                .content();
    }

    @Operation(summary = "记忆对话，可传入记忆id")
    @GetMapping("/memory/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "我的头有点痛，怎么办")String query,
                             @RequestParam(value = "chat-id", defaultValue = "1") String chatId) {

        return chatClient.prompt(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call().content();
    }

    @Operation(summary = "绘画")
    @GetMapping("/image")
    String getImage(@RequestParam(value = "prompt") String prompt) {
        ImageOptions options = ImageOptionsBuilder.builder()
                .model("wanx2.1-t2i-turbo")
                .build();
        ImagePrompt imagePrompt = new ImagePrompt(prompt, options);
        ImageResponse imageResponse = imageModel.call(imagePrompt);
        return imageResponse.getResult().getOutput().getUrl();
    }

}
