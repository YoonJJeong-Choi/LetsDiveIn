package com.swimshop.swim_mall.ai.returnrisk.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.ai.returnrisk.dto.ReturnRiskLogResponseDto;
import com.swimshop.swim_mall.ai.returnrisk.entity.ReturnRiskLogEntity;
import com.swimshop.swim_mall.ai.returnrisk.repository.ReturnRiskLogRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class ReturnRiskAdminController {

    private final AuthService authService;
    private final ReturnRiskLogRepository returnRiskLogRepository;

    @GetMapping("/return-risk/logs")
    public ResponseEntity<ApiResponse<List<ReturnRiskLogResponseDto>>> getReturnRiskLogs(
            HttpSession session,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        authService.requireRole(session, AccountRole.ADMIN);

        int safeLimit = Math.max(1, Math.min(200, limit == null ? 50 : limit));
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        List<ReturnRiskLogEntity> logs = success == null
                ? returnRiskLogRepository.findAllByOrderByCreatedAtDesc(pageRequest)
                : returnRiskLogRepository.findBySuccessOrderByCreatedAtDesc(success, pageRequest);

        List<ReturnRiskLogResponseDto> response = logs.stream()
                .map(this::toResponseDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private ReturnRiskLogResponseDto toResponseDto(ReturnRiskLogEntity entity) {
        return ReturnRiskLogResponseDto.builder()
                .createdAt(entity.getCreatedAt())
                .productNo(entity.getProductNo())
                .optionNo(entity.getOptionNo())
                .score(entity.getScore() == null ? 0d : entity.getScore())
                .riskLevel(entity.getRiskLevel())
                .success(Boolean.TRUE.equals(entity.getSuccess()))
                .latencyMs(entity.getLatencyMs() == null ? 0L : entity.getLatencyMs())
                .model(entity.getModel())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}
