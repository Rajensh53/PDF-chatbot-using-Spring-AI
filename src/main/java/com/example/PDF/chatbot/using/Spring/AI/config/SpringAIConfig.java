package com.example.PDF.chatbot.using.Spring.AI.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.groq.GroqChatModel;
import org.springframework.ai.groq.GroqChatOptions;
import org.springframework.ai.groq.api.GroqApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Value("${spring.ai.groq.api-key}")
    private String apiKey;

    @Value("${spring.ai.groq.chat.options.model:llama2-70b-4096}")
    private String model;

    @Value("${spring.ai.groq.chat.options.temperature:0.7}")
    private float temperature;

    @Value("${spring.ai.groq.chat.options.maxTokens:2048}")
    private int maxTokens;

    /**
     * Configure the Groq API client
     */
    @Bean
    public GroqApi groqApi() {
        return new GroqApi(apiKey);
    }

    /**
     * Configure the ChatModel with Groq settings
     */
    @Bean
    public ChatModel chatModel(GroqApi groqApi) {
        GroqChatOptions options = GroqChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
        
        return new GroqChatModel(groqApi, options);
    }

    // No need for ChatClient bean as we're using ChatModel directly
}