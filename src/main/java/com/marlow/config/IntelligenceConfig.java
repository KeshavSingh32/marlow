package com.marlow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marlow.intelligence.HeuristicReviewIntelligence;
import com.marlow.intelligence.ReviewIntelligence;
import com.marlow.intelligence.SpringAiReviewIntelligence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntelligenceConfig {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceConfig.class);

    @Bean
    public ReviewIntelligence reviewIntelligence(
            ObjectMapper objectMapper,
            @Value("${marlow.ai.mode:heuristic}") String mode,
            @Value("${marlow.ai.openai-api-key:}") String apiKey,
            @Value("${marlow.ai.model:gpt-4o-mini}") String model) {
        HeuristicReviewIntelligence heuristic = new HeuristicReviewIntelligence();
        if (!"openai".equalsIgnoreCase(mode.trim()) || apiKey == null || apiKey.isBlank()) {
            if ("openai".equalsIgnoreCase(mode.trim())) {
                log.warn("MARLOW_AI_MODE=openai but OPENAI_API_KEY is empty. Using the on-desk reader.");
            } else {
                log.info("Review reading is local (heuristic). Set MARLOW_AI_MODE=openai to use Spring AI.");
            }
            return heuristic;
        }
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(apiKey.trim()).build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).temperature(0.2).build())
                .build();
        ChatClient chatClient = ChatClient.create(chatModel);
        log.info("Review reading is Spring AI via {}", model);
        return new SpringAiReviewIntelligence(chatClient, objectMapper, heuristic, model);
    }
}
