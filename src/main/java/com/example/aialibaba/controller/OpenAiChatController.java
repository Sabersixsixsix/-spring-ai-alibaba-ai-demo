package com.example.aialibaba.controller;

import com.example.aialibaba.dao.actorDao;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Slf4j
@RestController
@RequestMapping("/api")
public class OpenAiChatController {
    @Autowired
    @Qualifier("openAiChatClient")
    private ChatClient chatClient;

    @Autowired
    private DeepSeekChatModel deepSeekChatModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private OpenAiImageModel openAiImageModel;
    /**
     * deepseek
     */
    @GetMapping("/deepseek/{msg}")
    String deepSeek(@PathVariable String msg) {
        String response = deepSeekChatModel.call( msg);
        return response;
    }

    /**
     * 拦日志截器
     */
    @GetMapping("/ai/{userInput}")
    String generation(@PathVariable String userInput) {
        SimpleLoggerAdvisor customLogger = new SimpleLoggerAdvisor(
                request -> "Custom request: " + request.prompt().getUserMessage(),
                response -> "Custom response: " + response.getResult(),
                1
        );
        return this.chatClient.prompt()
                .user(userInput)
                .advisors(customLogger)
                .call()
                .content();
    }

    /**
     * 填充为实体类型
     */
    @GetMapping("/actor")
    String getActor() {
        actorDao actor = chatClient.prompt()
                .user("Generate the name and age for a random actor.") // 向 AI 发 prompt
                .call() // 执行调用，获取 AI 返回的结果（JSON 字符串）
                .entity(actorDao.class); // 自动映射为 ActorFilms 对象
        return actor.getName() + " " + actor.getMovies();
    }

    /**
     * 填充为带泛型的复杂类型
     */
    @GetMapping("/films")
    List<actorDao> getFilms() {
        List<actorDao> actorFilms = chatClient.prompt()
                .user("Generate the filmography of 5 movies for Tom Hanks and Bill Murray.")
                .call()
                .entity(new ParameterizedTypeReference<List<actorDao>>() {});
        return actorFilms;
    }

    /**
     * 图片分析
     */
    @GetMapping("/qp")
    String getQP() {
        String response = chatClient.prompt()
                .user(u -> u.text("介绍一下图片内容")
                        .media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("/picture/football.png")))
                .call()
                .content();
        return response;
    }

    /**
     * 嵌入模型，返回嵌入向量
     */
    @GetMapping("/embed")
    public Map embed(@RequestParam(value = "message", defaultValue = "Tell me a joke") String message) {
        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(message));
        return Map.of("embedding", embeddingResponse);
    }

    /**
     * 生成图像
     */
    @GetMapping("/image")
    public String image(@RequestParam(value = "prompt", defaultValue = "A sexy girl upon the sea") String prompt) {
        ImageResponse imageResponse = openAiImageModel.call(
                new ImagePrompt(prompt, OpenAiImageOptions.builder() // 默认model为 dall-e-3
//                        .model(OpenAiImageApi.ImageModel.DALL_E_2.getValue())
                        .responseFormat("url") // url or base
                        .build()
                )
        );
        Image image = imageResponse.getResult().getOutput();
        return String.format("<img src='%s' alt='%s'>", image.getUrl(), prompt);

    }
    /**
     * 生成图像并保存
     */
    @GetMapping("/imagesv")
    public String imagesv(@RequestParam(value = "prompt", defaultValue = "Saber from Fate") String prompt,
                          HttpServletResponse response) {
        ImageResponse imageResponse = openAiImageModel.call(
                new ImagePrompt(prompt, OpenAiImageOptions.builder()
                        .responseFormat("url")
                        .build()
                )
        );
        Image image = imageResponse.getResult().getOutput();
        String imageUrl = image.getUrl();

        // 保存图片到本地
        try {
            URL url = new URL(imageUrl);
            String fileName = UUID.randomUUID().toString() + ".png"; // 生成唯一文件名
            Path filePath = Paths.get("images/" + fileName); // 保存路径

            // 创建目录（如果不存在）
            Files.createDirectories(filePath.getParent());

            // 下载并保存图片
            try (InputStream in = url.openStream();
                 OutputStream out = Files.newOutputStream(filePath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("图片已保存到: " + filePath);

            // 可选：返回保存的图片路径（用于后续访问）
            return String.format("<img src='%s' alt='%s'>", image.getUrl(), prompt);

        } catch (IOException e) {
            e.printStackTrace();
            // 出错时仍返回原始URL
            return String.format("<img src='%s' alt='%s'>", image.getUrl(), prompt);
        }
    }
}
