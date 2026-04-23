package com.swimshop.swim_mall.ai.reviewanalysis.client;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisRequestDto;
import com.swimshop.swim_mall.ai.reviewanalysis.service.ReviewAnalysisPromptTemplateService;

@Component
public class OpenAiReviewAnalysisClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ReviewAnalysisPromptTemplateService promptTemplateService;

    public OpenAiReviewAnalysisClient(
            ObjectMapper objectMapper,
            ReviewAnalysisPromptTemplateService promptTemplateService,
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model:gpt-4o-mini}") String model,
            @Value("${ai.openai.timeout-ms:5000}") int timeoutMs) {
        this.objectMapper = objectMapper;
        this.promptTemplateService = promptTemplateService;
        this.apiKey = apiKey;
        this.model = model;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .requestFactory(requestFactory)
                .build();
    }

    public String requestReviewAnalysisJson(ReviewAnalysisRequestDto requestDto) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI api key is missing");
        }

        String inputJson;
        try {
            inputJson = objectMapper.writeValueAsString(requestDto);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize review analysis request", e);
        }

        String systemPrompt = promptTemplateService.buildSystemPrompt(requestDto);
        String userPrompt = "Input JSON: " + inputJson;

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        JsonNode responseRoot = restClient.post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (responseRoot == null) {
            throw new IllegalStateException("OpenAI response is null");
        }

        JsonNode contentNode = responseRoot.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().isBlank()) {
            throw new IllegalStateException("OpenAI content is empty");
        }
        return contentNode.asText();
    }

    public String getModel() {
        return model;
    }
}
