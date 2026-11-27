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
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

@Configuration
public class IntelligenceConfig {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceConfig.class);

    @Bean
    public ReviewIntelligence reviewIntelligence(
            ObjectMapper objectMapper,
            @Value("${marlow.ai.mode:heuristic}") String mode,
            @Value("${marlow.ai.openai-api-key:}") String apiKey,
            @Value("${marlow.ai.model:gemini-3.1-flash-lite}") String model,
            @Value("${marlow.ai.base-url:https://generativelanguage.googleapis.com/v1beta/openai}") String baseUrl,
            @Value("${marlow.ai.completions-path:/chat/completions}") String completionsPath,
            @Value("${marlow.ai.min-interval-ms:4500}") long minIntervalMillis) {
        HeuristicReviewIntelligence heuristic = new HeuristicReviewIntelligence();
        if (!"openai".equalsIgnoreCase(mode.trim()) || apiKey == null || apiKey.isBlank()) {
            if ("openai".equalsIgnoreCase(mode.trim())) {
                log.warn("AI mode is on but no API key is set (GEMINI_API_KEY). Using the on-desk reader.");
            } else {
                log.info("Review reading is local (heuristic). Set MARLOW_AI_MODE=openai to use Spring AI.");
            }
            return heuristic;
        }
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl.trim())
                .completionsPath(completionsPath.trim())
                .apiKey(apiKey.trim())
                .build();
        // The library default retries 10 times with waits of minutes, too long for a web request
        RetryTemplate retry = RetryTemplate.builder()
                .maxAttempts(3)
                .retryOn(TransientAiException.class)
                .retryOn(ResourceAccessException.class)
                .fixedBackoff(Duration.ofSeconds(2))
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model(model).temperature(0.2).build())
                .retryTemplate(retry)
                .build();
        ChatClient chatClient = ChatClient.create(chatModel);
        log.info("Review reading is Spring AI via {}", model);
        return new SpringAiReviewIntelligence(chatClient, objectMapper, heuristic, model, minIntervalMillis);
    }
}
