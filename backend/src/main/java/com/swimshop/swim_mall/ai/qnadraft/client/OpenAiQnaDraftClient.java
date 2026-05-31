package com.swimshop.swim_mall.ai.qnadraft.client;

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

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OpenAiQnaDraftClient {

    private final RestClient restClient;
    private final String apiKey;
    @Getter
    private final String model;
    private final int maxOutputTokens;

    public OpenAiQnaDraftClient(
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model:gpt-4o-mini}") String model,
            @Value("${ai.openai.timeout-ms:60000}") int timeoutMs,
            @Value("${ai.qna-draft.openai-max-output-tokens:2048}") int maxOutputTokens) {
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
    void logReadiness() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("OpenAI API 키가 비어 있습니다. QnA AI 답변 초안은 폴백 응답만 반환됩니다.");
        } else {
            log.info("OpenAI QnA Draft 클라이언트 준비됨 (model={}, maxOutputTokens={}).", model, maxOutputTokens);
        }
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String requestDraftJson(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new IllegalStateException("OpenAI api key is missing");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.3);
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
}
