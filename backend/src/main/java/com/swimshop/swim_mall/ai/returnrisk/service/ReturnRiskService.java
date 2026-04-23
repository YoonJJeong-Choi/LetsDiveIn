package com.swimshop.swim_mall.ai.returnrisk.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.ai.returnrisk.client.OpenAiReturnRiskClient;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskLevel;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskScoreRequestDto;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskScoreResponseDto;
import com.swimshop.swim_mall.ai.returnrisk.entity.ReturnRiskLogEntity;
import com.swimshop.swim_mall.ai.returnrisk.repository.ReturnRiskLogRepository;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReturnRiskService {

    private final OpenAiReturnRiskClient openAiReturnRiskClient;
    private final ObjectMapper objectMapper;
    private final ReturnRiskLogRepository returnRiskLogRepository;

    private static final Pattern API_KEY_PATTERN = Pattern.compile("sk-[A-Za-z0-9_-]{10,}");
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile("(?i)(authorization\\s*[:=]\\s*)(bearer\\s+)?[^\\s,\\\"]+");
    private static final Pattern SECRET_FIELD_PATTERN = Pattern.compile("(?i)(api[-_]?key|secret|token|password)");

    public ReturnRiskScoreResponseDto score(ReturnRiskScoreRequestDto requestDto) {
        long startTime = System.currentTimeMillis();
        ReturnRiskScoreRequestDto safeRequest = toNullSafeRequest(requestDto);
        String sanitizedRequestPayload = toSanitizedJson(safeRequest);

        ReturnRiskScoreResponseDto responseDto;
        String responsePayload;
        String errorMessage = null;

        String aiJson;
        try {
            aiJson = requestWithRetry(safeRequest);
        } catch (Exception e) {
            errorMessage = safeErrorMessage(e);
            log.warn("Return risk AI call failed. Fallback will be used. reason={}", errorMessage);
            responseDto = fallbackResponse();
            responsePayload = toSanitizedJson(responseDto);
            saveLogBestEffort(safeRequest, sanitizedRequestPayload, responsePayload, responseDto, false, errorMessage, startTime);
            return responseDto;
        }

        try {
            JsonNode root = parseStrict(aiJson);
            double score = clampScore(readRequiredDouble(root, "score"));
            ReturnRiskLevel riskLevel = ReturnRiskLevel.from(readRequiredText(root, "riskLevel"));
            String badgeText = readRequiredText(root, "badgeText");
            String guideText = readRequiredText(root, "guideText");
            List<String> reasons = readRequiredStringArray(root, "reasons");

            responseDto = ReturnRiskScoreResponseDto.builder()
                    .score(score)
                    .riskLevel(riskLevel)
                    .badgeText(badgeText)
                    .guideText(guideText)
                    .reasons(reasons)
                    .build();
            responsePayload = toSanitizedJson(responseDto);
            saveLogBestEffort(safeRequest, sanitizedRequestPayload, responsePayload, responseDto, true, null, startTime);
            return responseDto;
        } catch (Exception e) {
            // JSON 파싱/필드검증 실패는 재시도 없이 즉시 fallback
            errorMessage = safeErrorMessage(e);
            log.warn("Return risk AI parsing failed. Fallback will be used. reason={}", errorMessage);
            responseDto = fallbackResponse();
            responsePayload = toSanitizedJson(responseDto);
            saveLogBestEffort(safeRequest, sanitizedRequestPayload, responsePayload, responseDto, false, errorMessage, startTime);
            return responseDto;
        }
    }

    private JsonNode parseStrict(String aiJson) {
        try {
            return objectMapper.readTree(aiJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse AI JSON", e);
        }
    }

    private String readRequiredText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.asText();
    }

    private double readRequiredDouble(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isNumber()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return node.asDouble();
    }

    private List<String> readRequiredStringArray(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isArray()) {
            throw new IllegalArgumentException(field + " is required");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual() || item.asText().isBlank()) {
                throw new IllegalArgumentException(field + " must contain non-empty strings");
            }
            values.add(item.asText());
        }
        return values;
    }

    private double clampScore(double score) {
        if (score < 0d) {
            return 0d;
        }
        if (score > 1d) {
            return 1d;
        }
        return score;
    }

    private String requestWithRetry(ReturnRiskScoreRequestDto safeRequest) {
        final int maxAttempts = 2; // 최초 1회 + 재시도 1회
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return openAiReturnRiskClient.requestReturnRiskScoreJson(safeRequest);
            } catch (Exception e) {
                lastException = e;
                if (attempt == maxAttempts || !isRetryable(e)) {
                    break;
                }
                log.warn("Return risk AI call retrying. attempt={}/{}", attempt + 1, maxAttempts);
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
            return responseException.getStatusCode().is5xxServerError();
        }
        return false;
    }

    private ReturnRiskScoreRequestDto toNullSafeRequest(ReturnRiskScoreRequestDto requestDto) {
        if (requestDto == null) {
            return new ReturnRiskScoreRequestDto(
                    null,
                    null,
                    0d,
                    0d,
                    0,
                    0,
                    false);
        }
        return new ReturnRiskScoreRequestDto(
                requestDto.getProductNo(),
                requestDto.getOptionNo(),
                requestDto.getProductReturnRate30d() == null ? 0d : requestDto.getProductReturnRate30d(),
                requestDto.getOptionReturnRate30d() == null ? 0d : requestDto.getOptionReturnRate30d(),
                requestDto.getOptionChangeCount() == null ? 0 : requestDto.getOptionChangeCount(),
                requestDto.getReviewViewCount() == null ? 0 : requestDto.getReviewViewCount(),
                requestDto.getSizeGuideClicked() != null && requestDto.getSizeGuideClicked());
    }

    private ReturnRiskScoreResponseDto fallbackResponse() {
        return ReturnRiskScoreResponseDto.builder()
                .score(0d)
                .riskLevel(ReturnRiskLevel.UNKNOWN)
                .badgeText("리스크 분석 일시 불가")
                .guideText("잠시 후 다시 시도해주세요.")
                .reasons(List.of("AI_UNAVAILABLE"))
                .build();
    }

    private String safeErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getClass().getSimpleName();
        }
        return sanitizeText(e.getClass().getSimpleName() + ": " + message);
    }

    private void saveLogBestEffort(
            ReturnRiskScoreRequestDto safeRequest,
            String sanitizedRequestPayload,
            String sanitizedResponsePayload,
            ReturnRiskScoreResponseDto responseDto,
            boolean success,
            String errorMessage,
            long startTime) {
        try {
            long latencyMs = Math.max(0L, System.currentTimeMillis() - startTime);
            ReturnRiskLogEntity logEntity = ReturnRiskLogEntity.builder()
                    .productNo(safeRequest.getProductNo())
                    .optionNo(safeRequest.getOptionNo())
                    .requestPayload(sanitizedRequestPayload)
                    .responsePayload(sanitizedResponsePayload)
                    .score(responseDto.getScore())
                    .riskLevel(responseDto.getRiskLevel().name())
                    .success(success)
                    .errorMessage(errorMessage == null ? null : truncate(errorMessage, 1000))
                    .latencyMs(latencyMs)
                    .model(openAiReturnRiskClient.getModel())
                    .build();
            returnRiskLogRepository.save(logEntity);
        } catch (Exception logSaveException) {
            // best-effort: 로그 저장 실패가 API 실패로 전파되면 안 됨
            log.warn("Return risk log save failed. reason={}", safeErrorMessage(logSaveException));
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
            node.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode child = entry.getValue();
                if (SECRET_FIELD_PATTERN.matcher(fieldName).find()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(fieldName, "***MASKED***");
                } else if (child.isTextual()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(fieldName, sanitizeText(child.asText()));
                } else {
                    sanitizeJsonNode(child);
                }
            });
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
