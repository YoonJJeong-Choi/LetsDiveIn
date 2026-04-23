package com.swimshop.swim_mall.return_order.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.return_order.dto.ReturnRequestDto;
import com.swimshop.swim_mall.return_order.dto.ReturnResponseDto;
import com.swimshop.swim_mall.return_order.dto.ReturnUpdateRequestDto;
import com.swimshop.swim_mall.return_order.dto.ReturnHistoryDto;
import com.swimshop.swim_mall.return_order.dto.ReturnAiAssistResponseDto;
import com.swimshop.swim_mall.return_order.service.ReturnService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    /**
     * 반품 신청 (고객용)
     * POST /api/returns
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReturnResponseDto>> requestReturn(
            HttpSession session,
            @Valid @RequestBody ReturnRequestDto requestDto) {
        ReturnResponseDto returnDto = returnService.requestReturn(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(returnDto));
    }

    /**
     * 반품 목록 조회 (고객용)
     * GET /api/returns/customer
     */
    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<List<ReturnResponseDto>>> getReturnsByCustomer(HttpSession session) {
        List<ReturnResponseDto> returns = returnService.getReturnsByCustomer(session);
        return ResponseEntity.ok(ApiResponse.success(returns));
    }

    /**
     * 반품 목록 조회 (관리자/파트너용)
     * GET /api/returns
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReturnResponseDto>>> getAllReturns(HttpSession session) {
        List<ReturnResponseDto> returns = returnService.getAllReturns(session);
        return ResponseEntity.ok(ApiResponse.success(returns));
    }

    /**
     * 반품 상세 조회
     * GET /api/returns/{returnNo}
     */
    @GetMapping("/{returnNo}")
    public ResponseEntity<ApiResponse<ReturnResponseDto>> getReturn(
            HttpSession session,
            @PathVariable Long returnNo) {
        ReturnResponseDto returnDto = returnService.getReturn(session, returnNo);
        return ResponseEntity.ok(ApiResponse.success(returnDto));
    }

    /**
     * 반품 승인 (관리자/파트너용)
     * POST /api/returns/{returnNo}/approve
     */
    @PostMapping("/{returnNo}/approve")
    public ResponseEntity<ApiResponse<ReturnResponseDto>> approveReturn(
            HttpSession session,
            @PathVariable Long returnNo) {
        ReturnResponseDto returnDto = returnService.approveReturn(session, returnNo);
        return ResponseEntity.ok(ApiResponse.success(returnDto));
    }

    /**
     * 반품 거절 (관리자/파트너용)
     * POST /api/returns/{returnNo}/reject
     */
    @PostMapping("/{returnNo}/reject")
    public ResponseEntity<ApiResponse<ReturnResponseDto>> rejectReturn(
            HttpSession session,
            @PathVariable Long returnNo,
            @RequestBody(required = false) Map<String, String> requestBody) {
        String rejectionReason = requestBody != null ? requestBody.get("rejectionReason") : null;
        ReturnResponseDto returnDto = returnService.rejectReturn(session, returnNo, rejectionReason);
        return ResponseEntity.ok(ApiResponse.success(returnDto));
    }

    /**
     * 반품 상태 변경 (관리자/파트너용)
     * PATCH /api/returns/{returnNo}
     */
    @PatchMapping("/{returnNo}")
    public ResponseEntity<ApiResponse<ReturnResponseDto>> updateReturnStatus(
            HttpSession session,
            @PathVariable Long returnNo,
            @RequestBody ReturnUpdateRequestDto requestDto) {
        ReturnResponseDto returnDto = returnService.updateReturnStatus(session, returnNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(returnDto));
    }

    /**
     * 반품 변경 이력 조회 (관리자/파트너용)
     * GET /api/returns/{returnNo}/history
     */
    @GetMapping("/{returnNo}/history")
    public ResponseEntity<ApiResponse<List<ReturnHistoryDto>>> getReturnHistory(
            HttpSession session,
            @PathVariable Long returnNo) {
        List<ReturnHistoryDto> history = returnService.getReturnHistory(session, returnNo);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * 관리자 반품 AI 보조 결과 조회 (MVP)
     * GET /api/returns/{returnNo}/ai-assist
     */
    @GetMapping("/{returnNo}/ai-assist")
    public ResponseEntity<ApiResponse<ReturnAiAssistResponseDto>> getAdminAiAssist(
            HttpSession session,
            @PathVariable Long returnNo) {
        ReturnAiAssistResponseDto response = returnService.getAdminAiAssist(session, returnNo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
