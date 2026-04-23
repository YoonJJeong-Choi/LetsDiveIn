package com.swimshop.swim_mall.ai.reviewanalysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.ai.reviewanalysis.client.OpenAiReviewAnalysisClient;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisRequestDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisResponseDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewItemDto;
import com.swimshop.swim_mall.ai.reviewanalysis.entity.ReviewAnalysisLogEntity;
import com.swimshop.swim_mall.ai.reviewanalysis.repository.ReviewAnalysisLogRepository;

@ExtendWith(MockitoExtension.class)
class ReviewAnalysisServiceTest {

    @Mock
    private OpenAiReviewAnalysisClient openAiReviewAnalysisClient;

    @Mock
    private ReviewAnalysisLogRepository reviewAnalysisLogRepository;

    private ReviewAnalysisService reviewAnalysisService;

    @BeforeEach
    void setUp() {
        reviewAnalysisService = new ReviewAnalysisService(
                openAiReviewAnalysisClient,
                new ObjectMapper(),
                reviewAnalysisLogRepository);
        when(openAiReviewAnalysisClient.getModel()).thenReturn("gpt-4o-mini");
        when(reviewAnalysisLogRepository.save(any(ReviewAnalysisLogEntity.class)))
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
                List.of(new ReviewItemDto(1L, 101L, 10001L, 3, "사이즈가 작아요", LocalDateTime.now())));

        when(openAiReviewAnalysisClient.requestReviewAnalysisJson(any(ReviewAnalysisRequestDto.class)))
                .thenReturn("{"
                        + "\"summary\":\"요약\","
                        + "\"sentiment\":{\"positiveRatio\":1.3,\"negativeRatio\":-0.2,\"neutralRatio\":0.2},"
                        + "\"issues\":[{\"type\":\"사이즈편차\",\"frequency\":-3,\"severity\":\"MEDIUM\",\"evidenceReviewNos\":[1]}],"
                        + "\"actions\":[{\"area\":\"SIZE_GUIDE\",\"recommendation\":\"보강\",\"expectedImpact\":\"HIGH\"}],"
                        + "\"topKeywords\":[{\"keyword\":\"사이즈\",\"weight\":1.5}],"
                        + "\"representativeQuotes\":[{\"reviewNo\":1,\"quote\":\"작아요\"}],"
                        + "\"stats\":{\"count\":-5,\"avgRating\":5.7,\"ratingDist1\":-1,\"ratingDist2\":2,\"ratingDist3\":3,\"ratingDist4\":4,\"ratingDist5\":5},"
                        + "\"alerts\":{\"pii\":false,\"spam\":false,\"policyViolation\":false,\"policyTypes\":[]},"
                        + "\"confidence\":\"HIGH\""
                        + "}");

        ReviewAnalysisResponseDto response = reviewAnalysisService.analyzeForPartner(requestDto);

        assertEquals("요약", response.getSummary());
        assertEquals(1.0d, response.getSentiment().getPositiveRatio());
        assertEquals(0.0d, response.getSentiment().getNegativeRatio());
        assertEquals(0.2d, response.getSentiment().getNeutralRatio());
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
                null,
                null,
                null);

        when(openAiReviewAnalysisClient.requestReviewAnalysisJson(any(ReviewAnalysisRequestDto.class)))
                .thenThrow(new IllegalStateException("OpenAI api key is missing"));

        ReviewAnalysisResponseDto response = reviewAnalysisService.analyzeForPartner(requestDto);

        assertEquals("리뷰 분석 일시 불가", response.getSummary());
        assertEquals(1.0d, response.getSentiment().getNeutralRatio());
        assertEquals(List.of("AI_UNAVAILABLE"), response.getAlerts().getPolicyTypes());
        assertEquals("LOW", response.getConfidence());

        ArgumentCaptor<ReviewAnalysisLogEntity> captor = ArgumentCaptor.forClass(ReviewAnalysisLogEntity.class);
        verify(reviewAnalysisLogRepository).save(captor.capture());
        assertEquals(false, captor.getValue().getSuccess());
        assertNotNull(captor.getValue().getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{"
                    + "\"summary\":\"품질 회귀 테스트 A\","
                    + "\"sentiment\":{\"positiveRatio\":0.6,\"negativeRatio\":0.2,\"neutralRatio\":0.2},"
                    + "\"issues\":[{\"type\":\"사이즈\",\"frequency\":2,\"severity\":\"MEDIUM\",\"evidenceReviewNos\":[1]}],"
                    + "\"actions\":[{\"area\":\"SIZE_GUIDE\",\"recommendation\":\"사이즈 가이드 강화\",\"expectedImpact\":\"MEDIUM\"}],"
                    + "\"topKeywords\":[{\"keyword\":\"사이즈\",\"weight\":0.8}],"
                    + "\"representativeQuotes\":[{\"reviewNo\":1,\"quote\":\"생각보다 작아요\"}],"
                    + "\"stats\":{\"count\":10,\"avgRating\":3.8,\"ratingDist1\":1,\"ratingDist2\":1,\"ratingDist3\":2,\"ratingDist4\":3,\"ratingDist5\":3},"
                    + "\"alerts\":{\"pii\":false,\"spam\":false,\"policyViolation\":false,\"policyTypes\":[]},"
                    + "\"confidence\":\"MEDIUM\""
                    + "}",
            "{"
                    + "\"summary\":\"품질 회귀 테스트 B\","
                    + "\"sentiment\":{\"positiveRatio\":0.4,\"negativeRatio\":0.4,\"neutralRatio\":0.2},"
                    + "\"issues\":[],"
                    + "\"actions\":[{\"area\":\"FAQ\",\"recommendation\":\"반품/교환 FAQ 보강\",\"expectedImpact\":\"LOW\"}],"
                    + "\"topKeywords\":[],"
                    + "\"representativeQuotes\":[],"
                    + "\"stats\":{\"count\":0,\"avgRating\":0.0,\"ratingDist1\":0,\"ratingDist2\":0,\"ratingDist3\":0,\"ratingDist4\":0,\"ratingDist5\":0},"
                    + "\"alerts\":{\"pii\":false,\"spam\":false,\"policyViolation\":false,\"policyTypes\":[]},"
                    + "\"confidence\":\"LOW\""
                    + "}"
    })
    void analyzeForPartner_shouldPassSampleSetRegressionSchema(String aiJson) {
        ReviewAnalysisRequestDto requestDto = new ReviewAnalysisRequestDto(
                "partner-1001",
                null,
                null,
                List.of(),
                20,
                List.of());

        when(openAiReviewAnalysisClient.requestReviewAnalysisJson(any(ReviewAnalysisRequestDto.class)))
                .thenReturn(aiJson);

        ReviewAnalysisResponseDto response = reviewAnalysisService.analyzeForPartner(requestDto);

        assertNotNull(response.getSummary());
        assertNotNull(response.getSentiment());
        assertNotNull(response.getIssues());
        assertNotNull(response.getActions());
        assertNotNull(response.getTopKeywords());
        assertNotNull(response.getRepresentativeQuotes());
        assertNotNull(response.getStats());
        assertNotNull(response.getAlerts());
        assertNotNull(response.getConfidence());
    }
}
