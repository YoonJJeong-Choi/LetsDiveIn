package com.swimshop.swim_mall.ai.reviewanalysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.ai.common.AiDailyLimitService;
import com.swimshop.swim_mall.ai.reviewanalysis.client.OpenAiReviewAnalysisClient;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisRequestDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisResponseDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewItemDto;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.ai.reviewanalysis.entity.ReviewAnalysisLogEntity;
import com.swimshop.swim_mall.ai.reviewanalysis.repository.ReviewAnalysisLogRepository;

@ExtendWith(MockitoExtension.class)
class ReviewAnalysisServiceTest {

    @Mock
    private OpenAiReviewAnalysisClient openAiReviewAnalysisClient;

    @Mock
    private ReviewAnalysisLogRepository reviewAnalysisLogRepository;

    @Mock
    private AiDailyLimitService aiDailyLimitService;

    private ReviewAnalysisService reviewAnalysisService;

    @BeforeEach
    void setUp() {
        reviewAnalysisService = new ReviewAnalysisService(
                openAiReviewAnalysisClient,
                new ObjectMapper(),
                reviewAnalysisLogRepository,
                aiDailyLimitService);
        ReflectionTestUtils.setField(reviewAnalysisService, "minimumRequiredReviews", 3);
        lenient().when(openAiReviewAnalysisClient.getModel()).thenReturn("gpt-4o-mini");
        lenient().when(reviewAnalysisLogRepository.save(any(ReviewAnalysisLogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void analyzeForPartner_shouldMapSuccessResponseAndClampValues() {
        ReviewAnalysisRequestDto requestDto = new ReviewAnalysisRequestDto(
                "partner-1001",
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now(),
                List.of(101L),
                20,
                List.of(
                        new ReviewItemDto(1L, 101L, 10001L, 3, "사이즈가 작아요", LocalDateTime.now()),
                        new ReviewItemDto(2L, 101L, 10002L, 4, "색상은 마음에 들어요", LocalDateTime.now()),
                        new ReviewItemDto(3L, 101L, 10003L, 2, "배송이 조금 늦었어요", LocalDateTime.now())));

        when(openAiReviewAnalysisClient.requestReviewAnalysisJson(any(ReviewAnalysisRequestDto.class)))
                .thenReturn("{"
                        + "\"summary\":\"요약\","
                        + "\"issues\":[{\"type\":\"사이즈편차\",\"frequency\":-3,\"severity\":\"MEDIUM\",\"evidenceReviewNos\":[1]}],"
                        + "\"actions\":[{\"area\":\"SIZE_GUIDE\",\"recommendation\":\"보강\"}],"
                        + "\"topKeywords\":[{\"keyword\":\"사이즈\",\"weight\":1.5}],"
                        + "\"representativeQuotes\":[{\"reviewNo\":1,\"quote\":\"작아요\"}],"
                        + "\"stats\":{\"count\":-5,\"avgRating\":5.7,\"ratingDist1\":-1,\"ratingDist2\":2,\"ratingDist3\":3,\"ratingDist4\":4,\"ratingDist5\":5},"
                        + "\"alerts\":{\"pii\":false,\"spam\":false,\"policyViolation\":false,\"policyTypes\":[]}}");

        ReviewAnalysisResponseDto response = reviewAnalysisService.analyzeForPartner(requestDto);

        assertEquals("요약", response.getSummary());
        assertEquals(0, response.getIssues().get(0).getFrequency());
        assertEquals(1.0d, response.getTopKeywords().get(0).getWeight());
        assertEquals(0, response.getStats().getCount());
        assertEquals(5.0d, response.getStats().getAvgRating());
        assertEquals(0, response.getStats().getRatingDist1());

        ArgumentCaptor<ReviewAnalysisLogEntity> captor = ArgumentCaptor.forClass(ReviewAnalysisLogEntity.class);
        verify(reviewAnalysisLogRepository).save(captor.capture());
        assertTrue(captor.getValue().getSuccess());
    }

    @Test
    void analyzeForPartner_shouldReturnFallbackWhenAiCallFails() {
        ReviewAnalysisRequestDto requestDto = new ReviewAnalysisRequestDto(
                "partner-1001",
                null,
                null,
                List.of(101L),
                20,
                List.of(
                        new ReviewItemDto(1L, 101L, 10001L, 3, "사이즈가 작아요", LocalDateTime.now()),
                        new ReviewItemDto(2L, 101L, 10002L, 4, "색상은 마음에 들어요", LocalDateTime.now()),
                        new ReviewItemDto(3L, 101L, 10003L, 2, "배송이 조금 늦었어요", LocalDateTime.now())));

        when(openAiReviewAnalysisClient.requestReviewAnalysisJson(any(ReviewAnalysisRequestDto.class)))
                .thenThrow(new IllegalStateException("OpenAI api key is missing"));

        ReviewAnalysisResponseDto response = reviewAnalysisService.analyzeForPartner(requestDto);

        assertEquals("리뷰 분석 일시 불가", response.getSummary());
        assertEquals(List.of("AI_UNAVAILABLE"), response.getAlerts().getPolicyTypes());

        ArgumentCaptor<ReviewAnalysisLogEntity> captor = ArgumentCaptor.forClass(ReviewAnalysisLogEntity.class);
        verify(reviewAnalysisLogRepository).save(captor.capture());
        assertEquals(false, captor.getValue().getSuccess());
        assertNotNull(captor.getValue().getErrorMessage());
    }

    @Test
    void analyzeForPartner_shouldThrowBusinessExceptionWhenReviewsAreInsufficient() {
        ReviewAnalysisRequestDto requestDto = new ReviewAnalysisRequestDto(
                "partner-1001",
                null,
                null,
                List.of(101L),
                20,
                List.of(
                        new ReviewItemDto(1L, 101L, 10001L, 3, "사이즈가 작아요", LocalDateTime.now()),
                        new ReviewItemDto(2L, 101L, 10002L, 4, "색상은 마음에 들어요", LocalDateTime.now())));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reviewAnalysisService.analyzeForPartner(requestDto));
        assertEquals("리뷰 AI 분석은 최소 3건 이상 필요합니다. (현재 2건)", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{"
                    + "\"summary\":\"품질 회귀 테스트 A\","
                    + "\"issues\":[{\"type\":\"사이즈\",\"frequency\":2,\"severity\":\"MEDIUM\",\"evidenceReviewNos\":[1]}],"
                    + "\"actions\":[{\"area\":\"SIZE_GUIDE\",\"recommendation\":\"사이즈 가이드 강화\"}],"
                    + "\"topKeywords\":[{\"keyword\":\"사이즈\",\"weight\":0.8}],"
                    + "\"representativeQuotes\":[{\"reviewNo\":1,\"quote\":\"생각보다 작아요\"}],"
                    + "\"stats\":{\"count\":10,\"avgRating\":3.8,\"ratingDist1\":1,\"ratingDist2\":1,\"ratingDist3\":2,\"ratingDist4\":3,\"ratingDist5\":3},"
                    + "\"alerts\":{\"pii\":false,\"spam\":false,\"policyViolation\":false,\"policyTypes\":[]}}",
            "{"
                    + "\"summary\":\"품질 회귀 테스트 B\","
                    + "\"issues\":[],"
                    + "\"actions\":[{\"area\":\"FAQ\",\"recommendation\":\"반품/교환 FAQ 보강\"}],"
                    + "\"topKeywords\":[],"
                    + "\"representativeQuotes\":[],"
                    + "\"stats\":{\"count\":0,\"avgRating\":0.0,\"ratingDist1\":0,\"ratingDist2\":0,\"ratingDist3\":0,\"ratingDist4\":0,\"ratingDist5\":0},"
                    + "\"alerts\":{\"pii\":false,\"spam\":false,\"policyViolation\":false,\"policyTypes\":[]}}"
    })
    void analyzeForPartner_shouldPassSampleSetRegressionSchema(String aiJson) {
        ReviewAnalysisRequestDto requestDto = new ReviewAnalysisRequestDto(
                "partner-1001",
                null,
                null,
                List.of(),
                20,
                List.of(
                        new ReviewItemDto(1L, 101L, 10001L, 3, "생각보다 작아요", LocalDateTime.now()),
                        new ReviewItemDto(2L, 101L, 10002L, 4, "배송은 빨랐어요", LocalDateTime.now()),
                        new ReviewItemDto(3L, 101L, 10003L, 5, "재구매 의사 있어요", LocalDateTime.now())));

        when(openAiReviewAnalysisClient.requestReviewAnalysisJson(any(ReviewAnalysisRequestDto.class)))
                .thenReturn(aiJson);

        ReviewAnalysisResponseDto response = reviewAnalysisService.analyzeForPartner(requestDto);

        assertNotNull(response.getSummary());
        assertNotNull(response.getIssues());
        assertNotNull(response.getActions());
        assertNotNull(response.getTopKeywords());
        assertNotNull(response.getRepresentativeQuotes());
        assertNotNull(response.getStats());
        assertNotNull(response.getAlerts());
    }
}
