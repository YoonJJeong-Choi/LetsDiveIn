package com.swimshop.swim_mall.partner.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.partner.dto.PartnerSalesStatisticsDto;
import com.swimshop.swim_mall.partner.dto.PartnerProfileResponseDto;
import com.swimshop.swim_mall.partner.dto.PartnerProfileUpdateRequestDto;
import com.swimshop.swim_mall.partner.dto.PartnerChangeRequestCreateDto;
import com.swimshop.swim_mall.partner.dto.PartnerChangeRequestResponseDto;

import java.time.LocalDate;
import com.swimshop.swim_mall.partner.dto.DeactivationRequestDto;
import com.swimshop.swim_mall.partner.dto.PartnerApplicationRequestDto;
import com.swimshop.swim_mall.partner.dto.PartnerHistoryResponseDto;
import com.swimshop.swim_mall.partner.dto.ReactivationRequestDto;
import com.swimshop.swim_mall.partner.dto.PartnerApplicationResponseDto;
import com.swimshop.swim_mall.partner.dto.ProductCreateRequestDto;
import com.swimshop.swim_mall.partner.dto.ProductUpdateRequestDto;
import com.swimshop.swim_mall.partner.dto.ProductWithOptionsDto;
import com.swimshop.swim_mall.partner.service.PartnerService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 파트너 관련 API 컨트롤러
 */
@RequestMapping("/api/partner")
@RequiredArgsConstructor
@RestController
public class PartnerController {
    
    private final PartnerService partnerService;
    private final AuthService authService;
    
    /**
     * 현재 로그인한 파트너의 상품 목록 조회
     * GET /api/partner/products
     * 
     * @return 상품 목록 (각 상품에 해당 파트너의 옵션 목록 포함)
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductWithOptionsDto>> getMyProducts(HttpSession session) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        List<ProductWithOptionsDto> products = partnerService.getMyProducts(session);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 파트너 상품 상태별 개수 요약
     * GET /api/partner/products/status-counts
     */
    @GetMapping("/products/status-counts")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getMyProductStatusCounts(HttpSession session) {
        // authService.requireRole(session, AccountRole.PARTNER);
        List<ProductWithOptionsDto> products = partnerService.getMyProducts(session);
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        if (products != null) {
            for (ProductWithOptionsDto p : products) {
                String raw = null;
                try {
                    java.lang.reflect.Method m = ProductWithOptionsDto.class.getMethod("getProductActiveStatus");
                    Object v = m.invoke(p);
                    raw = v != null ? v.toString() : null;
                } catch (Exception ignored) {}
                String s = raw == null ? "UNKNOWN" : raw.trim().toUpperCase();
                counts.put(s, counts.getOrDefault(s, 0L) + 1);
            }
        }
        return ResponseEntity.ok(ApiResponse.success(counts));
    }
    
    /**
     * 파트너 상품 등록
     * POST /api/partner/products
     * 
     * @param session 현재 세션
     * @param requestDto 상품 등록 요청 DTO
     * @return 등록된 상품 정보 (옵션 포함)
     */
    @PostMapping("/products")
    public ResponseEntity<ProductWithOptionsDto> createProduct(
            HttpSession session,
            @Valid @RequestBody ProductCreateRequestDto requestDto) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        ProductWithOptionsDto createdProduct = partnerService.createProduct(session, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }
    
    /**
     * 파트너 상품 수정
     * PUT /api/partner/products/{productNo}
     * 
     * @param session 현재 세션
     * @param productNo 수정할 상품 번호
     * @param requestDto 상품 수정 요청 DTO
     * @return 수정된 상품 정보 (옵션 포함)
     */
    @PutMapping("/products/{productNo}")
    public ResponseEntity<ProductWithOptionsDto> updateProduct(
            HttpSession session,
            @PathVariable Long productNo,
            @Valid @RequestBody ProductUpdateRequestDto requestDto) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        ProductWithOptionsDto updatedProduct = partnerService.updateProduct(session, productNo, requestDto);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * 파트너 상품 삭제 (소프트 삭제)
     * DELETE /api/partner/products/{productNo}
     * 
     * @param session 현재 세션
     * @param productNo 삭제할 상품 번호
     * @return 204 No Content (성공)
     */
    @DeleteMapping("/products/{productNo}")
    public ResponseEntity<Void> deleteProduct(
            HttpSession session,
            @PathVariable Long productNo) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        partnerService.deleteProduct(session, productNo);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    /**
     * 파트너 상품 재활성화 (INACTIVE → ACTIVE)
     * 파트너가 직접 비활성화한 상품만 즉시 재활성화 가능 (거절 사유가 없는 경우)
     * 거절 사유가 있는 경우는 수정 후 재신청 필요
     * PATCH /api/partner/products/{productNo}/reactivate
     * 
     * @param session 현재 세션
     * @param productNo 재활성화할 상품 번호
     * @return 재활성화된 상품 정보 (옵션 포함)
     */
    @PatchMapping("/products/{productNo}/reactivate")
    public ResponseEntity<ProductWithOptionsDto> reactivateProduct(
            HttpSession session,
            @PathVariable Long productNo) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        ProductWithOptionsDto reactivatedProduct = partnerService.reactivateProduct(session, productNo);
        return ResponseEntity.ok(reactivatedProduct);
    }

    /**
     * 파트너 상품 수정 신청 취소 (PENDING_UPDATE → ACTIVE)
     * 파트너가 수정 신청을 취소하여 원래 상태로 복구
     * PATCH /api/partner/products/{productNo}/cancel-update
     * 
     * @param session 현재 세션
     * @param productNo 취소할 상품 번호
     * @return 취소된 상품 정보 (옵션 포함)
     */
    @PatchMapping("/products/{productNo}/cancel-update")
    public ResponseEntity<ProductWithOptionsDto> cancelProductUpdate(
            HttpSession session,
            @PathVariable Long productNo) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        ProductWithOptionsDto cancelledProduct = partnerService.cancelProductUpdate(session, productNo);
        return ResponseEntity.ok(cancelledProduct);
    }

    /**
     * 파트너 입점 신청
     * POST /api/partner/apply
     * 비회원/회원 모두 신청 가능
     * 
     * @param requestDto 파트너 입점 신청 정보
     * @return 파트너 신청 정보
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<PartnerApplicationResponseDto>> applyForPartnership(
            @Valid @RequestBody PartnerApplicationRequestDto requestDto) {
        PartnerApplicationResponseDto response = partnerService.applyForPartnership(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 파트너 휴업 신청
     * POST /api/partner/deactivation/request
     * 로그인한 파트너만 신청 가능 (APPROVED 상태만)
     * 
     * @param session 현재 세션
     * @param requestDto 휴업 신청 정보
     * @return 성공 메시지
     */
    @PostMapping("/deactivation/request")
    public ResponseEntity<ApiResponse<Void>> requestDeactivation(
            HttpSession session,
            @Valid @RequestBody DeactivationRequestDto requestDto) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        partnerService.requestDeactivation(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 파트너 재활성화 신청
     * POST /api/partner/reactivation/request
     * 로그인한 파트너만 신청 가능 (INACTIVE 상태만)
     * 
     * @param session 현재 세션
     * @param requestDto 재활성화 신청 정보
     * @return 성공 메시지
     */
    @PostMapping("/reactivation/request")
    public ResponseEntity<ApiResponse<Void>> requestReactivation(
            HttpSession session,
            @Valid @RequestBody ReactivationRequestDto requestDto) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        partnerService.requestReactivation(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 파트너 자신의 정보 조회
     * GET /api/partner/me
     * 로그인한 파트너만 자신의 정보를 조회할 수 있음
     * 
     * @param session 현재 세션
     * @return 파트너 정보
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PartnerApplicationResponseDto>> getMyPartnerInfo(
            HttpSession session) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        PartnerApplicationResponseDto partnerInfo = partnerService.getMyPartnerInfo(session);
        return ResponseEntity.ok(ApiResponse.success(partnerInfo));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PartnerProfileResponseDto>> getMyProfile(HttpSession session) {
        PartnerProfileResponseDto profile = partnerService.getMyProfile(session);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<PartnerProfileResponseDto>> updateMyProfile(
            HttpSession session,
            @RequestBody PartnerProfileUpdateRequestDto requestDto) {
        PartnerProfileResponseDto profile = partnerService.updateMyProfile(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/profile-change-requests")
    public ResponseEntity<ApiResponse<PartnerChangeRequestResponseDto>> createProfileChangeRequest(
            HttpSession session,
            @RequestBody PartnerChangeRequestCreateDto requestDto) {
        PartnerChangeRequestResponseDto response = partnerService.createChangeRequest(session, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/profile-change-requests")
    public ResponseEntity<ApiResponse<List<PartnerChangeRequestResponseDto>>> getMyProfileChangeRequests(
            HttpSession session) {
        List<PartnerChangeRequestResponseDto> list = partnerService.getMyChangeRequests(session);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 파트너 자신의 이력 조회
     * GET /api/partner/history?actionType=APPLICATION|APPROVAL|REJECTION|...
     * 로그인한 파트너만 자신의 이력을 조회할 수 있음
     * 
     * @param session 현재 세션
     * @param actionType 액션 타입 필터 (선택)
     * @return 파트너 이력 목록
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PartnerHistoryResponseDto>>> getMyHistory(
            HttpSession session,
            @RequestParam(required = false) String actionType) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        List<PartnerHistoryResponseDto> history = partnerService.getMyHistory(session, actionType);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    /**
     * 파트너 자신의 매출 통계 조회
     * GET /api/partner/sales/statistics?startDate=2024-01-01&endDate=2024-01-31
     * 로그인한 파트너만 자신의 매출을 조회할 수 있음
     * 배송 완료되고 구매 확정된 주문상품만 집계
     * 
     * @param session 현재 세션
     * @param startDate 시작 날짜 (선택, 기본값: 30일 전)
     * @param endDate 종료 날짜 (선택, 기본값: 오늘)
     * @return 파트너 매출 통계
     */
    @GetMapping("/sales/statistics")
    public ResponseEntity<ApiResponse<PartnerSalesStatisticsDto>> getMySalesStatistics(
            HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // TODO: 프로젝트 완성 후 아래 주석 해제하여 PARTNER만 접근 가능하도록 제한
        // authService.requireRole(session, AccountRole.PARTNER);
        
        PartnerSalesStatisticsDto statistics = partnerService.getMySalesStatistics(session, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
