package com.swimshop.swim_mall.ai.reviewanalysis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swimshop.swim_mall.ai.reviewanalysis.client.OpenAiReviewAnalysisClient;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ActionDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.AlertsDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.IssueDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.KeywordDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.QuoteDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisRequestDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisResponseDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.SentimentDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.StatsDto;
import com.swimshop.swim_mall.ai.reviewanalysis.entity.ReviewAnalysisLogEntity;
import com.swimshop.swim_mall.ai.reviewanalysis.repository.ReviewAnalysisLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewAnalysisService {

    private static final List<String> ALLOWED_SEVERITIES = List.of("LOW", "MEDIUM", "HIGH");
    private static final List<String> ALLOWED_IMPACTS = List.of("LOW", "MEDIUM", "HIGH");
    private static final List<String> ALLOWED_AREAS = List.of("DETAIL", "SIZE_GUIDE", "IMAGES", "PACKAGING", "FAQ", "CS_MACRO", "INVENTORY");
    private static final List<String> ALLOWED_CONFIDENCES = List.of("LOW", "MEDIUM", "HIGH");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("sk-[A-Za-z0-9_-]{10,}");
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile("(?i)(authorization\\s*[:=]\\s*)(bearer\\s+)?[^\\s,\\\"]+");
    private static final Pattern SECRET_FIELD_PATTERN = Pattern.compile("(?i)(api[-_]?key|secret|token|password)");

    private final OpenAiReviewAnalysisClient openAiReviewAnalysisClient;
    private final ObjectMapper objectMapper;
    private final ReviewAnalysisLogRepository reviewAnalysisLogRepository;

    public ReviewAnalysisResponseDto analyzeForPartner(ReviewAnalysisRequestDto requestDto) {
        long startTime = System.currentTimeMillis();
        ReviewAnalysisRequestDto safeRequest = toNullSafeRequest(requestDto);
        validateSingleTarget(safeRequest);
        String productNosPayload = toSanitizedJson(safeRequest.getProductNos());
        String requestPayload = toSanitizedJson(safeRequest);
        String model = openAiReviewAnalysisClient.getModel();

        String json;
        try {
            json = requestWithRetry(safeRequest);
        } catch (Exception e) {
            String errorMessage = safeErrorMessage(e);
            log.warn("Review analysis AI call failed. Fallback will be used. reason={}", errorMessage);
            ReviewAnalysisResponseDto fallback = fallbackResponse();
            saveLogBestEffort(safeRequest, productNosPayload, requestPayload, toSanitizedJson(fallback), false, errorMessage, startTime, model);
            return fallback;
        }

        try {
            JsonNode root = parseJson(json);
            ReviewAnalysisResponseDto responseDto = ReviewAnalysisResponseDto.builder()
                    .summary(readRequiredText(root, "summary"))
                    .sentiment(parseSentiment(root.path("sentiment")))
                    .issues(parseIssues(root.path("issues")))
                    .actions(parseActions(root.path("actions")))
                    .topKeywords(parseTopKeywords(root.path("topKeywords")))
                    .representativeQuotes(parseRepresentativeQuotes(root.path("representativeQuotes")))
                    .stats(parseStats(root.path("stats")))
                    .alerts(parseAlerts(root.path("alerts")))
                    .confidence(readAllowedText(root, "confidence", ALLOWED_CONFIDENCES))
                    .build();
            saveLogBestEffort(safeRequest, productNosPayload, requestPayload, toSanitizedJson(responseDto), true, null, startTime, model);
            return responseDto;
        } catch (Exception e) {
            String errorMessage = safeErrorMessage(e);
            log.warn("Review analysis AI parsing failed. Fallback will be used. reason={}", errorMessage);
            ReviewAnalysisResponseDto fallback = fallbackResponse();
            saveLogBestEffort(safeRequest, productNosPayload, requestPayload, toSanitizedJson(fallback), false, errorMessage, startTime, model);
            return fallback;
        }
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse AI JSON", e);
        }
    }

    private SentimentDto parseSentiment(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            throw new IllegalArgumentException("sentiment is required");
        }
        return SentimentDto.builder()
                .positiveRatio(clampRatio(readRequiredDouble(node, "positiveRatio")))
                .negativeRatio(clampRatio(readRequiredDouble(node, "negativeRatio")))
                .neutralRatio(clampRatio(readRequiredDouble(node, "neutralRatio")))
                .build();
    }

    private List<IssueDto> parseIssues(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            throw new IllegalArgumentException("issues is required");
        }
        List<IssueDto> issues = new ArrayList<>();
        for (JsonNode item : node) {
            issues.add(IssueDto.builder()
                    .type(readRequiredText(item, "type"))
                    .frequency(nonNegativeInt(readRequiredInt(item, "frequency")))
                    .severity(readAllowedText(item, "severity", ALLOWED_SEVERITIES))
                    .evidenceReviewNos(readLongArray(item, "evidenceReviewNos"))
                    .build());
        }
        return issues;
    }

    private List<ActionDto> parseActions(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            throw new IllegalArgumentException("actions is required");
        }
        List<ActionDto> actions = new ArrayList<>();
        for (JsonNode item : node) {
            actions.add(ActionDto.builder()
                    .area(readAllowedText(item, "area", ALLOWED_AREAS))
                    .recommendation(readRequiredText(item, "recommendation"))
                    .expectedImpact(readAllowedText(item, "expectedImpact", ALLOWED_IMPACTS))
                    .build());
        }
        return actions;
    }

    private List<KeywordDto> parseTopKeywords(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            throw new IllegalArgumentException("topKeywords is required");
        }
        List<KeywordDto> keywords = new ArrayList<>();
        for (JsonNode item : node) {
            keywords.add(KeywordDto.builder()
                    .keyword(readRequiredText(item, "keyword"))
                    .weight(clampRatio(readRequiredDouble(item, "weight")))
                    .build());
        }
        return keywords;
    }

    private List<QuoteDto> parseRepresentativeQuotes(JsonNode node) {
        if (node == null || node.isNull() || !node.isArray()) {
            throw new IllegalArgumentException("representativeQuotes is required");
        }
        List<QuoteDto> quotes = new ArrayList<>();
        for (JsonNode item : node) {
            quotes.add(QuoteDto.builder()
                    .reviewNo(readRequiredLong(item, "reviewNo"))
                    .quote(readRequiredText(item, "quote"))
                    .build());
        }
        return quotes;
    }

    private StatsDto parseStats(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            throw new IllegalArgumentException("stats is required");
        }
        return StatsDto.builder()
                .count(nonNegativeInt(readRequiredInt(node, "count")))
                .avgRating(clampRating(readRequiredDouble(node, "avgRating")))
                .ratingDist1(nonNegativeInt(readRequiredInt(node, "ratingDist1")))
                .ratingDist2(nonNegativeInt(readRequiredInt(node, "ratingDist2")))
                .ratingDist3(nonNegativeInt(readRequiredInt(node, "ratingDist3")))
                .ratingDist4(nonNegativeInt(readRequiredInt(node, "ratingDist4")))
                .ratingDist5(nonNegativeInt(readRequiredInt(node, "ratingDist5")))
                .build();
    }

    private AlertsDto parseAlerts(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) {
            throw new IllegalArgumentException("alerts is required");
        }
        return AlertsDto.builder()
                .pii(readRequiredBoolean(node, "pii"))
                .spam(readRequiredBoolean(node, "spam"))
                .policyViolation(readRequiredBoolean(node, "policyViolation"))
                .policyTypes(readStringArray(node, "policyTypes"))
                .build();
    }

    private String readRequiredText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.asText();
    }

    private String readAllowedText(JsonNode root, String field, List<String> allowed) {
        String value = readRequiredText(root, field);
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    private double readRequiredDouble(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isNumber()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.asDouble();
    }

    private int readRequiredInt(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isInt()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.asInt();
    }

    private long readRequiredLong(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isNumber()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.asLong();
    }

    private boolean readRequiredBoolean(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isBoolean()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.asBoolean();
    }

    private List<Long> readLongArray(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isArray()) {
            throw new IllegalArgumentException(field + " is required");
        }
        List<Long> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isNumber()) {
                throw new IllegalArgumentException(field + " must contain number");
            }
            values.add(item.asLong());
        }
        return values;
    }

    private List<String> readStringArray(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isArray()) {
            throw new IllegalArgumentException(field + " is required");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException(field + " must contain string");
            }
            values.add(item.asText());
        }
        return values;
    }

    private int nonNegativeInt(int value) {
        return Math.max(0, value);
    }

    private double clampRatio(double value) {
        if (value < 0d) {
            return 0d;
        }
        if (value > 1d) {
            return 1d;
        }
        return value;
    }

    private double clampRating(double value) {
        if (value < 0d) {
            return 0d;
        }
        if (value > 5d) {
            return 5d;
        }
        return value;
    }

    private String requestWithRetry(ReviewAnalysisRequestDto requestDto) {
        final int maxAttempts = 2; // 최초 1회 + 재시도 1회
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return openAiReviewAnalysisClient.requestReviewAnalysisJson(requestDto);
            } catch (Exception e) {
                lastException = e;
                if (attempt == maxAttempts || !isRetryable(e)) {
                    break;
                }
                log.warn("Review analysis AI call retrying. attempt={}/{}", attempt + 1, maxAttempts);
            }
        }

        throw new IllegalStateException("AI request failed after retry", lastException);
    }

    private boolean isRetryable(Exception e) {
        if (e instanceof ResourceAccessException) {
            return true; // 네트워크/타임아웃
        }
        if (e instanceof HttpServerErrorException) {
            return true; // 5xx
        }
        if (e instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError()
                    || responseException.getStatusCode().value() == 429;
        }
        return false;
    }

    private ReviewAnalysisRequestDto toNullSafeRequest(ReviewAnalysisRequestDto requestDto) {
        if (requestDto == null) {
            return new ReviewAnalysisRequestDto(
                    null,
                    null,
                    null,
                    List.of(),
                    100,
                    List.of());
        }

        return new ReviewAnalysisRequestDto(
                requestDto.getPartnerId(),
                requestDto.getFromAt(),
                requestDto.getToAt(),
                requestDto.getProductNos() == null ? List.of() : requestDto.getProductNos(),
                requestDto.getMaxReviews() == null || requestDto.getMaxReviews() < 1 ? 100 : requestDto.getMaxReviews(),
                requestDto.getReviews() == null ? List.of() : requestDto.getReviews());
    }

    private void validateSingleTarget(ReviewAnalysisRequestDto req) {
        // productNos는 0개 또는 1개만 허용
        if (req.getProductNos() != null && req.getProductNos().size() > 1) {
            throw new IllegalArgumentException("Only a single product is allowed for analysis");
        }
        // reviews에 다수 productNo 혼합 금지 (한 상품 내 여러 optionNo는 허용)
        if (req.getReviews() != null && !req.getReviews().isEmpty()) {
            long uniqueProducts = req.getReviews().stream()
                    .map(r -> r.getProductNo())
                    .filter(v -> v != null)
                    .distinct()
                    .count();
            if (uniqueProducts > 1) {
                throw new IllegalArgumentException("Mixed products are not allowed for analysis");
            }
        }
    }

    private ReviewAnalysisResponseDto fallbackResponse() {
        return ReviewAnalysisResponseDto.builder()
                .summary("리뷰 분석 일시 불가")
                .sentiment(SentimentDto.builder()
                        .positiveRatio(0d)
                        .negativeRatio(0d)
                        .neutralRatio(1d)
                        .build())
                .issues(List.of())
                .actions(List.of(
                        ActionDto.builder()
                                .area("FAQ")
                                .recommendation("잠시 후 다시 시도해주세요.")
                                .expectedImpact("LOW")
                                .build()))
                .topKeywords(List.of())
                .representativeQuotes(List.of())
                .stats(StatsDto.builder()
                        .count(0)
                        .avgRating(0d)
                        .ratingDist1(0)
                        .ratingDist2(0)
                        .ratingDist3(0)
                        .ratingDist4(0)
                        .ratingDist5(0)
                        .build())
                .alerts(AlertsDto.builder()
                        .pii(false)
                        .spam(false)
                        .policyViolation(false)
                        .policyTypes(List.of("AI_UNAVAILABLE"))
                        .build())
                .confidence("LOW")
                .build();
    }

    private String safeErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getClass().getSimpleName();
        }
        // 민감정보가 포함될 수 있는 줄바꿈/긴 메시지를 최소화
        String compact = message.replace("\r", " ").replace("\n", " ");
        return e.getClass().getSimpleName() + ": " + compact;
    }

    private void saveLogBestEffort(
            ReviewAnalysisRequestDto requestDto,
            String productNosPayload,
            String requestPayload,
            String responsePayload,
            boolean success,
            String errorMessage,
            long startTime,
            String model) {
        try {
            long latencyMs = Math.max(0L, System.currentTimeMillis() - startTime);
            ReviewAnalysisLogEntity logEntity = ReviewAnalysisLogEntity.builder()
                    .partnerId(requestDto.getPartnerId())
                    .fromAt(requestDto.getFromAt())
                    .toAt(requestDto.getToAt())
                    .productNosPayload(productNosPayload)
                    .requestPayload(requestPayload)
                    .responsePayload(responsePayload)
                    .success(success)
                    .errorMessage(errorMessage == null ? null : truncate(errorMessage, 1000))
                    .latencyMs(latencyMs)
                    .model(model)
                    .build();
            reviewAnalysisLogRepository.save(logEntity);
        } catch (Exception logSaveException) {
            log.warn("Review analysis log save failed. reason={}", safeErrorMessage(logSaveException));
        }
    }

    private String toSanitizedJson(Object value) {
        try {
            JsonNode jsonNode = objectMapper.valueToTree(value);
            sanitizeJsonNode(jsonNode);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            return "{\"error\":\"PAYLOAD_SERIALIZE_FAILED\"}";
        }
    }

    private void sanitizeJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                JsonNode child = objectNode.get(fieldName);
                if (SECRET_FIELD_PATTERN.matcher(fieldName).find()) {
                    objectNode.put(fieldName, "***MASKED***");
                } else if (child.isTextual()) {
                    objectNode.put(fieldName, sanitizeText(child.asText()));
                } else {
                    sanitizeJsonNode(child);
                }
            }
            return;
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                sanitizeJsonNode(child);
            }
        }
    }

    private String sanitizeText(String raw) {
        if (raw == null) {
            return null;
        }
        String masked = API_KEY_PATTERN.matcher(raw).replaceAll("***MASKED***");
        masked = AUTHORIZATION_PATTERN.matcher(masked).replaceAll("$1***MASKED***");
        return masked;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
