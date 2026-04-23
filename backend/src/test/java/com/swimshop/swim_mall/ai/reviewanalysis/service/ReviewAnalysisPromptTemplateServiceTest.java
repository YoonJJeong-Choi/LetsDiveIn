package com.swimshop.swim_mall.ai.reviewanalysis.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisRequestDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewItemDto;

class ReviewAnalysisPromptTemplateServiceTest {

    private final ReviewAnalysisPromptTemplateService promptTemplateService = new ReviewAnalysisPromptTemplateService();

    @Test
    void buildSystemPrompt_shouldIncludeCategoryHintForGoggles() {
        ReviewAnalysisRequestDto requestDto = new ReviewAnalysisRequestDto(
                "partner-1",
                null,
                null,
                List.of(),
                20,
                List.of(new ReviewItemDto(1L, 10L, 100L, 3, "고글 김서림이 심하고 물샘이 있어요", LocalDateTime.now())));

        String prompt = promptTemplateService.buildSystemPrompt(requestDto);

        assertTrue(prompt.contains("Domain category hint: GOGGLES"));
        assertTrue(prompt.contains("anti-fog"));
    }

    @Test
    void buildSystemPrompt_shouldFallbackToGeneralWhenNoKeyword() {
        ReviewAnalysisRequestDto requestDto = new ReviewAnalysisRequestDto(
                "partner-1",
                null,
                null,
                List.of(),
                20,
                List.of(new ReviewItemDto(1L, 10L, 100L, 4, "배송이 빨라요", LocalDateTime.now())));

        String prompt = promptTemplateService.buildSystemPrompt(requestDto);

        assertTrue(prompt.contains("Domain category hint: GENERAL"));
    }
}

