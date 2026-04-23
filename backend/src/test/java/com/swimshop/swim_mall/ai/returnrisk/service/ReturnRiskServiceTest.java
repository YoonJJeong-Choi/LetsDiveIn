package com.swimshop.swim_mall.ai.returnrisk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.ai.returnrisk.client.OpenAiReturnRiskClient;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskLevel;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskScoreRequestDto;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskScoreResponseDto;
import com.swimshop.swim_mall.ai.returnrisk.entity.ReturnRiskLogEntity;
import com.swimshop.swim_mall.ai.returnrisk.repository.ReturnRiskLogRepository;

@ExtendWith(MockitoExtension.class)
class ReturnRiskServiceTest {

    @Mock
    private OpenAiReturnRiskClient openAiReturnRiskClient;

    @Mock
    private ReturnRiskLogRepository returnRiskLogRepository;

    private ReturnRiskService returnRiskService;

    @BeforeEach
    void setUp() {
        returnRiskService = new ReturnRiskService(openAiReturnRiskClient, new ObjectMapper(), returnRiskLogRepository);
        when(returnRiskLogRepository.save(any(ReturnRiskLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(openAiReturnRiskClient.getModel()).thenReturn("gpt-4o-mini");
    }

    @Test
    void score_shouldMapSuccessResponseAndClampScore() {
        ReturnRiskScoreRequestDto requestDto = new ReturnRiskScoreRequestDto(
                101L, 1001L, 0.2d, 0.3d, 2, 10, true);
        when(openAiReturnRiskClient.requestReturnRiskScoreJson(any(ReturnRiskScoreRequestDto.class)))
                .thenReturn("{\"score\":1.2,\"riskLevel\":\"HIGH\",\"badgeText\":\"주의\",\"guideText\":\"사이즈 확인\",\"reasons\":[\"R1\"]}");

        ReturnRiskScoreResponseDto response = returnRiskService.score(requestDto);

        assertEquals(1.0d, response.getScore());
        assertEquals(ReturnRiskLevel.HIGH, response.getRiskLevel());
        assertEquals("주의", response.getBadgeText());
        assertEquals("사이즈 확인", response.getGuideText());
        assertEquals(List.of("R1"), response.getReasons());

        ArgumentCaptor<ReturnRiskLogEntity> captor = ArgumentCaptor.forClass(ReturnRiskLogEntity.class);
        verify(returnRiskLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getSuccess());
        assertEquals("HIGH", captor.getValue().getRiskLevel());
    }

    @Test
    void score_shouldReturnFallbackWhenAiCallFails() {
        ReturnRiskScoreRequestDto requestDto = new ReturnRiskScoreRequestDto(
                101L, 1001L, 0.2d, 0.3d, 2, 10, false);
        when(openAiReturnRiskClient.requestReturnRiskScoreJson(any(ReturnRiskScoreRequestDto.class)))
                .thenThrow(new IllegalStateException("OpenAI api key is missing"));

        ReturnRiskScoreResponseDto response = returnRiskService.score(requestDto);

        assertEquals(0.0d, response.getScore());
        assertEquals(ReturnRiskLevel.UNKNOWN, response.getRiskLevel());
        assertEquals("리스크 분석 일시 불가", response.getBadgeText());
        assertEquals("잠시 후 다시 시도해주세요.", response.getGuideText());
        assertEquals(List.of("AI_UNAVAILABLE"), response.getReasons());

        ArgumentCaptor<ReturnRiskLogEntity> captor = ArgumentCaptor.forClass(ReturnRiskLogEntity.class);
        verify(returnRiskLogRepository).save(captor.capture());
        assertEquals(false, captor.getValue().getSuccess());
        assertNotNull(captor.getValue().getErrorMessage());
    }

    @Test
    void score_shouldRetryOnceWhenServerErrorOccurs() {
        ReturnRiskScoreRequestDto requestDto = new ReturnRiskScoreRequestDto(
                201L, 2001L, 0.1d, 0.2d, 1, 5, true);

        HttpServerErrorException serverError = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                null,
                null);

        when(openAiReturnRiskClient.requestReturnRiskScoreJson(any(ReturnRiskScoreRequestDto.class)))
                .thenThrow(serverError)
                .thenReturn("{\"score\":0.4,\"riskLevel\":\"LOW\",\"badgeText\":\"낮음\",\"guideText\":\"안내\",\"reasons\":[\"R_OK\"]}");

        ReturnRiskScoreResponseDto response = returnRiskService.score(requestDto);

        assertEquals(0.4d, response.getScore());
        assertEquals(ReturnRiskLevel.LOW, response.getRiskLevel());
        verify(openAiReturnRiskClient, times(2)).requestReturnRiskScoreJson(any(ReturnRiskScoreRequestDto.class));
    }

    @Test
    void score_shouldFallbackImmediatelyWhenJsonParsingFails() {
        ReturnRiskScoreRequestDto requestDto = new ReturnRiskScoreRequestDto(
                301L, 3001L, 0.2d, 0.1d, 3, 9, false);

        when(openAiReturnRiskClient.requestReturnRiskScoreJson(any(ReturnRiskScoreRequestDto.class)))
                .thenReturn("NOT_JSON");

        ReturnRiskScoreResponseDto response = returnRiskService.score(requestDto);

        assertEquals(0.0d, response.getScore());
        assertEquals(ReturnRiskLevel.UNKNOWN, response.getRiskLevel());
        assertEquals(List.of("AI_UNAVAILABLE"), response.getReasons());
        verify(openAiReturnRiskClient, times(1)).requestReturnRiskScoreJson(any(ReturnRiskScoreRequestDto.class));
    }
}
