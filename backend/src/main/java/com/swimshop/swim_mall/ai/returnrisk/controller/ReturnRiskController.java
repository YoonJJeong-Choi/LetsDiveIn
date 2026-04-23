package com.swimshop.swim_mall.ai.returnrisk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskScoreRequestDto;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskScoreResponseDto;
import com.swimshop.swim_mall.ai.returnrisk.service.ReturnRiskService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai/return-risk")
@RequiredArgsConstructor
public class ReturnRiskController {

    private final AuthService authService;
    private final ReturnRiskService returnRiskService;

    @PostMapping("/score")
    public ResponseEntity<ApiResponse<ReturnRiskScoreResponseDto>> score(
            HttpSession session,
            @Valid @RequestBody ReturnRiskScoreRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        ReturnRiskScoreResponseDto responseDto = returnRiskService.score(requestDto);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }
}
