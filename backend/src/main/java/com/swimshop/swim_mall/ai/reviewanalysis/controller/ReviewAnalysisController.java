package com.swimshop.swim_mall.ai.reviewanalysis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisRequestDto;
import com.swimshop.swim_mall.ai.reviewanalysis.dto.ReviewAnalysisResponseDto;
import com.swimshop.swim_mall.ai.reviewanalysis.service.ReviewAnalysisService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/review-analysis")
@RequiredArgsConstructor
public class ReviewAnalysisController {

    private final AuthService authService;
    private final ReviewAnalysisService reviewAnalysisService;

    @PostMapping("/partner")
    public ResponseEntity<ApiResponse<ReviewAnalysisResponseDto>> analyzeForPartner(
            HttpSession session,
            @RequestBody ReviewAnalysisRequestDto requestDto) {
        authService.requireRole(session, AccountRole.PARTNER);
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);

        ReviewAnalysisRequestDto requestWithSessionPartner = new ReviewAnalysisRequestDto(
                String.valueOf(currentUser.getSubjectId()),
                requestDto == null ? null : requestDto.getFromAt(),
                requestDto == null ? null : requestDto.getToAt(),
                requestDto == null ? null : requestDto.getProductNos(),
                requestDto == null ? null : requestDto.getMaxReviews(),
                requestDto == null ? null : requestDto.getReviews());

        ReviewAnalysisResponseDto responseDto = reviewAnalysisService.analyzeForPartner(requestWithSessionPartner);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }
}
