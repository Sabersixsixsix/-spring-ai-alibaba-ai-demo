package com.example.aialibaba.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.memory.jdbc.MysqlChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatClient openAiChatClient(JdbcTemplate jdbcTemplate, @Qualifier("openAiChatModel") OpenAiChatModel chatModel) {
        ChatMemoryRepository chatMemoryRepository = MysqlChatMemoryRepository.mysqlBuilder()
                .jdbcTemplate(jdbcTemplate)
                .build();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .build();
        return ChatClient
                .builder(chatModel)
                .defaultSystem("一你是一个专业的助手，用霸气的语气回答问题。")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                //添加一个日志环绕通知
//                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

@Bean
public ChatClient ailbabaChatClient(JdbcTemplate jdbcTemplate, @Qualifier("dashscopeChatModel") ChatModel chatModel) {
    ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
    // 构造 ChatMemoryRepository 和 ChatMemory
    ChatMemoryRepository chatMemoryRepository = MysqlChatMemoryRepository.mysqlBuilder()
            .jdbcTemplate(jdbcTemplate)
            .build();
    ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .build();
    return chatClientBuilder
//            .defaultSystem("你是一个温柔贴心的助手，今天的心情是{mood}")
            .defaultSystem("你是一个专业的助手，语言简洁")
            .defaultAdvisors(new SimpleLoggerAdvisor())
            // 注册Advisor
//            .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
            .defaultAdvisors(
                    a -> a
                            .param(ChatMemory.CONVERSATION_ID, "1")

            )
            .defaultOptions(
                    DashScopeChatOptions.builder()
                            .withTopP(0.7)
                            .build()
            )
            .build();
}

}
