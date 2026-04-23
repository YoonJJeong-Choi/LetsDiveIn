package com.swimshop.swim_mall.return_order.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.common.enums.ReturnReasonType;

@Component
public class OpenAiReturnAssistVisionClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public OpenAiReturnAssistVisionClient(
            ObjectMapper objectMapper,
            @Value("${ai.openai.api-key:}") String apiKey,
            @Value("${ai.openai.model:gpt-4o-mini}") String model,
            @Value("${ai.openai.timeout-ms:5000}") int timeoutMs,
            @Value("${ai.openai.max-tokens:256}") int maxTokens) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .requestFactory(requestFactory)
                .build();
    }

    public VisionAssistResult requestVisionAssist(
            List<String> imageUrls,
            ReturnReasonType reasonType,
            String returnReason) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI api key is missing");
        }

        List<String> safeImageUrls = imageUrls == null
                ? List.of()
                : imageUrls.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(url -> !url.isBlank())
                        .limit(6)
                        .collect(Collectors.toList());

        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of(
                "type", "text",
                "text", buildUserPrompt(reasonType, returnReason, safeImageUrls.size())));
        for (String imageUrl : safeImageUrls) {
            userContent.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of(
                            "url", imageUrl,
                            "detail", "low")));
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0,
                "max_tokens", maxTokens,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", buildSystemPrompt()),
                        Map.of("role", "user", "content", userContent)));

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

        return parseVisionResult(contentNode.asText());
    }

    public String getModel() {
        return model;
    }

    private VisionAssistResult parseVisionResult(String contentJson) {
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            return new VisionAssistResult(
                    readStringList(root, "evidenceTags"),
                    readStringList(root, "evidenceGaps"),
                    readStringList(root, "recommendedActions"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenAI vision result", e);
        }
    }

    private List<String> readStringList(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && item.isTextual()) {
                String value = item.asText().trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private String buildSystemPrompt() {
        return "관리자 반품 이미지 심사 도우미다. "
                + "다음 JSON만 반환하라: "
                + "{\"evidenceTags\":[\"...\"],\"evidenceGaps\":[\"...\"],\"recommendedActions\":[\"...\"]}. "
                + "추가 키/설명/마크다운은 금지한다. "
                + "evidenceTags,evidenceGaps 값은 UPPERCASE_SNAKE_CASE를 사용하라. "
                + "recommendedActions는 한국어 문장 1~3개로 작성하라.";
    }

    private String buildUserPrompt(ReturnReasonType reasonType, String returnReason, int imageCount) {
        String safeReason = returnReason == null ? "" : returnReason.trim();
        return "사유유형: " + (reasonType != null ? reasonType.name() : "UNKNOWN")
                + "\n사유텍스트: " + safeReason
                + "\n첨부이미지수: " + imageCount;
    }

    public record VisionAssistResult(
            List<String> evidenceTags,
            List<String> evidenceGaps,
            List<String> recommendedActions) {
    }
}
