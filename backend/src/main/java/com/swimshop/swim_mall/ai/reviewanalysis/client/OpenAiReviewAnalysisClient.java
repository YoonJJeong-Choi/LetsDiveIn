package com.swimshop.swim_mall.ai.reviewanalysis.client;

import java.time.Duration;
import java.util.LinkedHashMap;
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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OpenAiReviewAnalysisClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;
    private final ReviewAnalysisPromptTemplateService promptTemplateService;

    public OpenAiReviewAnalysisClient(
            ObjectMapper objectMapper,
            ReviewAnalysisPromptTemplateService promptTemplateService,
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model:gpt-4o-mini}") String model,
            @Value("${ai.openai.timeout-ms:60000}") int timeoutMs,
            @Value("${ai.review-analysis.openai-max-output-tokens:4096}") int maxOutputTokens) {
        this.objectMapper = objectMapper;
        this.promptTemplateService = promptTemplateService;
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = Math.max(256, maxOutputTokens);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .requestFactory(requestFactory)
                .build();
    }

    @PostConstruct
    void logOpenAiReadiness() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API 키가 비어 있습니다(ai.openai.api-key / OPENAI_API_KEY). 리뷰 AI 분석은 폴백 응답만 반환됩니다.");
        } else {
            log.info("OpenAI 리뷰 분석 클라이언트 준비됨(model={}, maxOutputTokens={}).", model, maxOutputTokens);
        }
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

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0);
        payload.put("max_tokens", maxOutputTokens);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(
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
