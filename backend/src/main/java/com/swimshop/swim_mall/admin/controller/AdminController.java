package com.swimshop.swim_mall.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.admin.dto.AdminBootstrapRequestDto;
import com.swimshop.swim_mall.admin.dto.AdminDashboardQueueDto;
import com.swimshop.swim_mall.admin.dto.AdminDashboardHealthDto;
import com.swimshop.swim_mall.admin.dto.AdminDashboardAnalyticsDto;
import com.swimshop.swim_mall.admin.dto.AdminDashboardInsightsDto;
import com.swimshop.swim_mall.admin.dto.AdminAiUsageOverviewDto;
import com.swimshop.swim_mall.admin.dto.PartnerChangeRequestRejectDto;
import com.swimshop.swim_mall.admin.dto.PartnerRejectionRequestDto;
import com.swimshop.swim_mall.admin.service.AdminService;
import com.swimshop.swim_mall.admin.service.AdminAiOpsService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.PartnerChangeRequestStatus;
import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.inventory.dto.InventoryResponseDto;
import com.swimshop.swim_mall.inventory.service.InventoryService;
import com.swimshop.swim_mall.partner.dto.PartnerApplicationResponseDto;
import com.swimshop.swim_mall.partner.dto.PartnerChangeRequestResponseDto;
import com.swimshop.swim_mall.partner.dto.PartnerHistoryResponseDto;
import com.swimshop.swim_mall.product.dto.ProductListDto;
import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.settlement.dto.AdminSettlementResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementCreateRequestDto;
import com.swimshop.swim_mall.settlement.dto.SettlementDetailResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementListResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementDashboardDto;
import com.swimshop.swim_mall.settlement.dto.SettlementHistoryDto;
import com.swimshop.swim_mall.settlement.service.SettlementService;
import com.swimshop.swim_mall.settlement.service.SettlementExportService;
import com.swimshop.swim_mall.admin.dto.CustomerListResponseDto;
import com.swimshop.swim_mall.admin.dto.CustomerDetailResponseDto;
import com.swimshop.swim_mall.admin.dto.CustomerStatisticsDto;
import com.swimshop.swim_mall.admin.dto.AdminCustomerUpdateRequestDto;
import com.swimshop.swim_mall.admin.dto.CustomerGradeDto;
import com.swimshop.swim_mall.admin.dto.CustomerGradeRequestDto;
import jakarta.validation.Valid;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 관리자 계정: 최초 1회 부트스트랩만 제공.
 * 파트너 승인/거절 기능 제공.
 */
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RestController
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;
    private final InventoryService inventoryService;
    private final SettlementService settlementService;
    private final SettlementExportService settlementExportService;
    private final AdminAiOpsService adminAiOpsService;

    /**
     * 최초 관리자 1명 생성. Admin이 한 명도 없을 때만 성공.
     * 이후에는 호출 불가 (이미 관리자 존재 시 400).
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<String> bootstrapFirstAdmin(@RequestBody AdminBootstrapRequestDto dto) {
        adminService.bootstrapFirstAdmin(dto);
        return ResponseEntity.ok("최초 관리자 계정이 생성되었습니다. 해당 이메일/비밀번호로 로그인하세요.");
    }

    /**
     * 관리자 대시보드 처리 큐 집계
     * GET /api/admin/dashboard/queue
     */
    @GetMapping("/dashboard/queue")
    public ResponseEntity<ApiResponse<AdminDashboardQueueDto>> getAdminDashboardQueue(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        AdminDashboardQueueDto dto = adminService.getAdminDashboardQueue();
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * 관리자 대시보드 전체 헬스(요약)
     * GET /api/admin/dashboard/health
     */
    @GetMapping("/dashboard/health")
    public ResponseEntity<ApiResponse<AdminDashboardHealthDto>> getAdminDashboardHealth(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        AdminDashboardHealthDto dto = adminService.getAdminDashboardHealth();
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * 관리자 대시보드 — 추이·랭킹·최근 주문/반품·정산 요약
     * GET /api/admin/dashboard/insights?days=30 (days: 14~30)
     */
    @GetMapping("/dashboard/insights")
    public ResponseEntity<ApiResponse<AdminDashboardInsightsDto>> getAdminDashboardInsights(
            HttpSession session,
            @RequestParam(defaultValue = "30") int days
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        AdminDashboardInsightsDto dto = adminService.getAdminDashboardInsights(days);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * 관리자 분석 대시보드 — 긴 추이·파트너·시간대·이행·리스크 집계
     * GET /api/admin/dashboard/analytics?trendDays=30 (trendDays: 7~90)
     */
    @GetMapping("/dashboard/analytics")
    public ResponseEntity<ApiResponse<AdminDashboardAnalyticsDto>> getAdminDashboardAnalytics(
            HttpSession session,
            @RequestParam(defaultValue = "30") int trendDays
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        AdminDashboardAnalyticsDto dto = adminService.getAdminDashboardAnalytics(trendDays);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/ai/overview")
    public ResponseEntity<ApiResponse<AdminAiUsageOverviewDto>> getAiOverview(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        return ResponseEntity.ok(ApiResponse.success(adminAiOpsService.getOverview()));
    }

    /**
     * 파트너 신청 목록 조회 (PENDING 상태만)
     * GET /api/admin/partners/pending
     * 관리자만 접근 가능
     */
    @GetMapping("/partners/pending")
    public ResponseEntity<ApiResponse<List<PartnerApplicationResponseDto>>> getPendingPartners(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        List<PartnerApplicationResponseDto> pendingPartners = adminService.getPendingPartners();
        return ResponseEntity.ok(ApiResponse.success(pendingPartners));
    }

    /**
     * 파트너 승인
     * PATCH /api/admin/partners/{partnerId}/approve
     * 관리자만 접근 가능
     */
    @PatchMapping("/partners/{partnerId}/approve")
    public ResponseEntity<ApiResponse<PartnerApplicationResponseDto>> approvePartner(
            HttpSession session,
            @PathVariable Long partnerId) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        PartnerApplicationResponseDto approvedPartner = adminService.approvePartner(partnerId, session);
        return ResponseEntity.ok(ApiResponse.success(approvedPartner));
    }

    /**
     * 파트너 거절
     * PATCH /api/admin/partners/{partnerId}/reject
     * 관리자만 접근 가능
     */
    @PatchMapping("/partners/{partnerId}/reject")
    public ResponseEntity<ApiResponse<String>> rejectPartner(
            HttpSession session,
            @PathVariable Long partnerId,
            @Valid @RequestBody PartnerRejectionRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        adminService.rejectPartner(partnerId, requestDto.getRejectionReason(), session);
        return ResponseEntity.ok(ApiResponse.success("파트너 신청이 거절되었습니다."));
    }

    /**
     * 파트너 목록 조회 (상태별 필터링 및 휴업/재활성화 신청 필터 가능)
     * GET /api/admin/partners?status=PENDING|APPROVED|REJECTED|INACTIVE&hasDeactivationRequest=true&hasReactivationRequest=true
     * 관리자만 접근 가능
     * 
     * 필터는 AND 조건으로 작동합니다:
     * - status: 파트너 상태 필터 (PENDING, APPROVED, REJECTED, INACTIVE)
     * - hasDeactivationRequest: 휴업 신청이 있는 파트너만 조회 (true)
     * - hasReactivationRequest: 재활성화 신청이 있는 파트너만 조회 (true)
     * 
     * 예시:
     * - /api/admin/partners?status=APPROVED&hasDeactivationRequest=true
     *   → "승인 · 운영 중" 상태이면서 휴업 신청이 있는 파트너
     * - /api/admin/partners?status=INACTIVE&hasReactivationRequest=true
     *   → "승인 · 비활성" 상태이면서 재활성화 신청이 있는 파트너
     */
    @GetMapping("/partners")
    public ResponseEntity<ApiResponse<List<PartnerApplicationResponseDto>>> getAllPartners(
            HttpSession session,
            @RequestParam(required = false) PartnerStatus status,
            @RequestParam(required = false) Boolean hasDeactivationRequest,
            @RequestParam(required = false) Boolean hasReactivationRequest) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        List<PartnerApplicationResponseDto> partners = adminService.getAllPartners(
                status, hasDeactivationRequest, hasReactivationRequest);
        return ResponseEntity.ok(ApiResponse.success(partners));
    }

    /**
     * 파트너 상세 조회
     * GET /api/admin/partners/{partnerId}
     * 관리자만 접근 가능
     */
    @GetMapping("/partners/{partnerId}")
    public ResponseEntity<ApiResponse<PartnerApplicationResponseDto>> getPartnerDetail(
            HttpSession session,
            @PathVariable Long partnerId) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        PartnerApplicationResponseDto partner = adminService.getPartnerDetail(partnerId);
        return ResponseEntity.ok(ApiResponse.success(partner));
    }

    /**
     * 파트너 비활성화 (APPROVED → INACTIVE)
     * PATCH /api/admin/partners/{partnerId}/deactivate
     * 관리자만 접근 가능
     */
    @PatchMapping("/partners/{partnerId}/deactivate")
    public ResponseEntity<ApiResponse<PartnerApplicationResponseDto>> deactivatePartner(
            HttpSession session,
            @PathVariable Long partnerId) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        PartnerApplicationResponseDto deactivatedPartner = adminService.deactivatePartner(partnerId, session);
        return ResponseEntity.ok(ApiResponse.success(deactivatedPartner));
    }

    /**
     * 파트너 재활성화 (INACTIVE → APPROVED)
     * PATCH /api/admin/partners/{partnerId}/activate
     * 관리자만 접근 가능
     * 재활성화 신청이 있는 경우에도 이 API로 승인 처리
     */
    @PatchMapping("/partners/{partnerId}/activate")
    public ResponseEntity<ApiResponse<PartnerApplicationResponseDto>> activatePartner(
            HttpSession session,
            @PathVariable Long partnerId) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        PartnerApplicationResponseDto activatedPartner = adminService.activatePartner(partnerId, session);
        return ResponseEntity.ok(ApiResponse.success(activatedPartner));
    }

    /**
     * 재활성화 신청 거절
     * PATCH /api/admin/partners/{partnerId}/reactivation-request/reject
     * 관리자만 접근 가능
     * 기존 파트너 관리 페이지에서 사용
     */
    @PatchMapping("/partners/{partnerId}/reactivation-request/reject")
    public ResponseEntity<ApiResponse<String>> rejectReactivationRequest(
            HttpSession session,
            @PathVariable Long partnerId,
            @Valid @RequestBody PartnerRejectionRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        adminService.rejectReactivationRequest(partnerId, requestDto.getRejectionReason(), session);
        return ResponseEntity.ok(ApiResponse.success("재활성화 신청이 거절되었습니다."));
    }

    /**
     * 휴업 신청 승인
     * PATCH /api/admin/partners/{partnerId}/deactivation-request/approve
     * 관리자만 접근 가능
     */
    @PatchMapping("/partners/{partnerId}/deactivation-request/approve")
    public ResponseEntity<ApiResponse<PartnerApplicationResponseDto>> approveDeactivationRequest(
            HttpSession session,
            @PathVariable Long partnerId) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        PartnerApplicationResponseDto partner = adminService.approveDeactivationRequest(partnerId, session);
        return ResponseEntity.ok(ApiResponse.success(partner));
    }

    /**
     * 휴업 신청 거절
     * PATCH /api/admin/partners/{partnerId}/deactivation-request/reject
     * 관리자만 접근 가능
     */
    @PatchMapping("/partners/{partnerId}/deactivation-request/reject")
    public ResponseEntity<ApiResponse<String>> rejectDeactivationRequest(
            HttpSession session,
            @PathVariable Long partnerId,
            @Valid @RequestBody PartnerRejectionRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        adminService.rejectDeactivationRequest(partnerId, requestDto.getRejectionReason(), session);
        return ResponseEntity.ok(ApiResponse.success("휴업 신청이 거절되었습니다."));
    }

    /**
     * 파트너 이력 조회
     * GET /api/admin/partners/{partnerId}/history?actionType=APPLICATION|APPROVAL|REJECTION|...
     * 관리자만 접근 가능
     * 
     * @param session 현재 세션
     * @param partnerId 파트너 ID
     * @param actionType 액션 타입 필터 (선택)
     * @return 파트너 이력 목록
     */
    @GetMapping("/partners/{partnerId}/history")
    public ResponseEntity<ApiResponse<List<PartnerHistoryResponseDto>>> getPartnerHistory(
            HttpSession session,
            @PathVariable Long partnerId,
            @RequestParam(required = false) String actionType) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        List<PartnerHistoryResponseDto> history = adminService.getPartnerHistory(partnerId, actionType);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/partners/change-requests")
    public ResponseEntity<ApiResponse<List<PartnerChangeRequestResponseDto>>> getPartnerChangeRequests(
            HttpSession session,
            @RequestParam(required = false) PartnerChangeRequestStatus status) {
        authService.requireRole(session, AccountRole.ADMIN);
        return ResponseEntity.ok(ApiResponse.success(adminService.getPartnerChangeRequests(session, status)));
    }

    @PatchMapping("/partners/change-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<PartnerChangeRequestResponseDto>> approvePartnerChangeRequest(
            HttpSession session,
            @PathVariable Long requestId) {
        authService.requireRole(session, AccountRole.ADMIN);
        return ResponseEntity.ok(ApiResponse.success(adminService.approvePartnerChangeRequest(session, requestId)));
    }

    @PatchMapping("/partners/change-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<PartnerChangeRequestResponseDto>> rejectPartnerChangeRequest(
            HttpSession session,
            @PathVariable Long requestId,
            @Valid @RequestBody PartnerChangeRequestRejectDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        return ResponseEntity.ok(ApiResponse.success(
                adminService.rejectPartnerChangeRequest(session, requestId, requestDto.getRejectReason())
        ));
    }

    /**
     * 상품 목록 조회 (상태별 필터링 가능)
     * GET /api/admin/products?status=PENDING|ACTIVE|REJECTED|INACTIVE
     * 관리자만 접근 가능
     * 
     * 사용 예시:
     * - 승인 대기 중인 상품: /api/admin/products?status=PENDING
     * - 활성 상품: /api/admin/products?status=ACTIVE
     * - 전체 상품: /api/admin/products
     * 
     * @param session 현재 세션
     * @param status 필터링할 상태 (선택, null이면 전체)
     * @return 상품 목록
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getAllProducts(
            HttpSession session,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        com.swimshop.swim_mall.common.enums.ActiveStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = com.swimshop.swim_mall.common.enums.ActiveStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new com.swimshop.swim_mall.common.error.BusinessException(
                    com.swimshop.swim_mall.common.error.ErrorCode.INVALID_REQUEST, 
                    "유효하지 않은 상태값입니다: " + status);
            }
        }
        
        List<ProductListDto> products = adminService.getAllProducts(statusEnum);
        int total = products != null ? products.size() : 0;
        int p = page != null ? page : 0; // 0-based 입력
        int s = size != null ? size : 10;
        int from = Math.max(0, Math.min(p * s, total));
        int to = Math.max(from, Math.min(from + s, total));
        List<ProductListDto> slice = products != null ? products.subList(from, to) : java.util.List.of();
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("products", slice);
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", p); // 0-based 그대로
        meta.put("size", s);
        meta.put("total", total);
        body.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /**
     * 상품 승인
     * PATCH /api/admin/products/{productNo}/approve
     * 관리자만 접근 가능
     * 
     * @param session 현재 세션
     * @param productNo 상품 번호
     * @return 승인된 상품 정보
     */
    @PatchMapping("/products/{productNo}/approve")
    public ResponseEntity<ApiResponse<ProductListDto>> approveProduct(
            HttpSession session,
            @PathVariable Long productNo) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        ProductListDto product = adminService.approveProduct(productNo, session);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * 상품 거절
     * PATCH /api/admin/products/{productNo}/reject
     * 관리자만 접근 가능
     * 
     * @param session 현재 세션
     * @param productNo 상품 번호
     * @param requestDto 거절 사유
     * @return 거절된 상품 정보
     */
    @PatchMapping("/products/{productNo}/reject")
    public ResponseEntity<ApiResponse<ProductListDto>> rejectProduct(
            HttpSession session,
            @PathVariable Long productNo,
            @Valid @RequestBody PartnerRejectionRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        ProductListDto product = adminService.rejectProduct(productNo, requestDto.getRejectionReason(), session);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * 전체 재고 목록 조회 (관리자용 - 모든 파트너)
     * GET /api/admin/inventory
     * 관리자만 접근 가능
     * 
     * @param session 현재 세션
     * @return 전체 재고 목록
     */
    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<List<InventoryResponseDto>>> getAllInventories(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        List<InventoryResponseDto> inventories = inventoryService.getAllInventories();
        return ResponseEntity.ok(ApiResponse.success(inventories));
    }

    /**
     * 특정 파트너의 재고 목록 조회 (관리자용)
     * GET /api/admin/inventory/partner/{partnerId}
     * 관리자만 접근 가능
     * 
     * @param session 현재 세션
     * @param partnerId 파트너 ID
     * @return 파트너의 재고 목록
     */
    @GetMapping("/inventory/partner/{partnerId}")
    public ResponseEntity<ApiResponse<List<InventoryResponseDto>>> getInventoriesByPartner(
            HttpSession session,
            @PathVariable Long partnerId) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        List<InventoryResponseDto> inventories = inventoryService.getInventoriesByPartnerId(partnerId);
        return ResponseEntity.ok(ApiResponse.success(inventories));
    }

    /**
     * 상품 상태별 개수 요약 (관리자용)
     * GET /api/admin/products/status-counts
     * - 승인 대기(PENDING), 수정 승인 대기(PENDING_UPDATE), 활성(ACTIVE), 거절(REJECTED), 비활성(INACTIVE)
     */
    @GetMapping("/products/status-counts")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getProductStatusCounts(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        List<ProductListDto> products = adminService.getAllProducts(null);
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        if (products != null) {
            for (ProductListDto p : products) {
                String raw = null;
                try {
                    raw = (String) ProductListDto.class.getDeclaredField("productActiveStatus").get(p);
                } catch (Exception ignored) {
                    // fallback via getter if available
                    try {
                        java.lang.reflect.Method m = ProductListDto.class.getMethod("getProductActiveStatus");
                        Object v = m.invoke(p);
                        raw = v != null ? v.toString() : null;
                    } catch (Exception ignored2) {}
                }
                String s = raw == null ? "UNKNOWN" : raw.trim().toUpperCase();
                counts.put(s, counts.getOrDefault(s, 0L) + 1);
            }
        }
        return ResponseEntity.ok(ApiResponse.success(counts));
    }
    
    /**
     * 관리자 정산 대상 목록 조회
     * GET /api/admin/settlement
     * 
     * @param session HTTP 세션
     * @param partnerId 파트너 ID (선택, null이면 전체 파트너)
     * @param startDate 정산 기간 시작일 (선택, 형식: yyyy-MM-dd)
     * @param endDate 정산 기간 종료일 (선택, 형식: yyyy-MM-dd)
     * @param status 필터 상태 (선택, "SETTLEMENT_READY": 정산 가능만, "ALL": 전체, 기본값: "ALL")
     * @return 정산 대상 목록 및 합계 정보
     */
    @GetMapping("/settlement")
    public ResponseEntity<ApiResponse<AdminSettlementResponseDto>> getSettlementItems(
            HttpSession session,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate endDate,
            @RequestParam(required = false, defaultValue = "ALL") 
            String status
    ) {
        AdminSettlementResponseDto response = settlementService.getAdminSettlementItems(
                session, 
                partnerId,
                startDate, 
                endDate, 
                status
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 정산 생성 (관리자용)
     * POST /api/admin/settlement
     * 
     * @param session HTTP 세션
     * @param request 정산 생성 요청
     * @return 생성된 정산 정보
     */
    /**
     * 정산 생성 (관리자용)
     * POST /api/admin/settlement
     * 
     * @param session HTTP 세션
     * @param request 정산 생성 요청
     * @return 생성된 정산 정보
     */
    @PostMapping("/settlement")
    public ResponseEntity<ApiResponse<SettlementDetailResponseDto>> createSettlement(
            HttpSession session,
            @Valid @RequestBody SettlementCreateRequestDto request
    ) {
        SettlementDetailResponseDto response = settlementService.createSettlement(session, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 정산 상태 변경 (관리자용)
     * PATCH /api/admin/settlement/{settlementId}/status
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @param status 변경할 상태 (PENDING, COMPLETED, CANCELLED)
     * @param paidDate 정산 지급일 (선택, 형식: yyyy-MM-dd)
     * @return 변경된 정산 정보
     */
    @PatchMapping("/settlement/{settlementId}/status")
    public ResponseEntity<ApiResponse<SettlementDetailResponseDto>> updateSettlementStatus(
            HttpSession session,
            @PathVariable Long settlementId,
            @RequestParam SettlementStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidDate
    ) {
        SettlementDetailResponseDto response = settlementService.updateSettlementStatus(session, settlementId, status, paidDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 생성된 정산 목록 조회 (관리자용)
     * GET /api/admin/settlements
     * 
     * @param session HTTP 세션
     * @param partnerId 파트너 ID (선택, null이면 전체 파트너)
     * @param status 정산 상태 필터 (선택, null이면 전체)
     * @return 생성된 정산 목록
     */
    @GetMapping("/settlements")
    public ResponseEntity<ApiResponse<SettlementListResponseDto>> getSettlementList(
            HttpSession session,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) String status
    ) {
        SettlementStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = SettlementStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 유효하지 않은 상태값이거나 제거된 PROCESSING 상태인 경우 null로 처리
                statusEnum = null;
            }
        }
        SettlementListResponseDto response = settlementService.getSettlementList(session, partnerId, statusEnum);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 정산 대시보드 조회 (관리자용)
     * GET /api/admin/settlements/dashboard
     * 
     * @param session HTTP 세션
     * @return 정산 대시보드 데이터 (전체 현황, 파트너별 순위, 월별 현황)
     */
    @GetMapping("/settlements/dashboard")
    public ResponseEntity<ApiResponse<SettlementDashboardDto>> getSettlementDashboard(
            HttpSession session
    ) {
        SettlementDashboardDto response = settlementService.getSettlementDashboard(session);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 정산 목록 Excel 다운로드 (관리자용)
     * GET /api/admin/settlements/excel?partnerId={partnerId}&status={status}
     * 
     * ⚠️ 중요: 이 엔드포인트는 /settlements/{settlementId}보다 먼저 정의되어야 합니다.
     * 그렇지 않으면 "excel"이 settlementId로 파싱되어 NumberFormatException이 발생합니다.
     * 
     * @param session HTTP 세션
     * @param partnerId 파트너 ID (선택)
     * @param status 정산 상태 (선택)
     * @return Excel 파일
     */
    @GetMapping("/settlements/excel")
    public ResponseEntity<byte[]> downloadSettlementExcel(
            HttpSession session,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) String status
    ) throws java.io.IOException {
        byte[] excelBytes = settlementExportService.generateSettlementExcel(session, partnerId, status);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "settlements.xlsx");
        headers.setContentLength(excelBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
    
    /**
     * 정산서 PDF 다운로드 (관리자용)
     * GET /api/admin/settlements/{settlementId}/pdf
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return PDF 파일
     */
    @GetMapping("/settlements/{settlementId}/pdf")
    public ResponseEntity<byte[]> downloadSettlementPdf(
            HttpSession session,
            @PathVariable Long settlementId
    ) throws java.io.IOException {
        byte[] pdfBytes = settlementExportService.generateSettlementPdf(session, settlementId);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            "settlement_" + settlementId + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    
    /**
     * 정산 상세 조회 (관리자용)
     * GET /api/admin/settlements/{settlementId}
     * 
     * ⚠️ 중요: 이 엔드포인트는 /settlements/excel과 /settlements/dashboard보다 나중에 정의되어야 합니다.
     * 그렇지 않으면 "excel"이나 "dashboard"가 settlementId로 파싱되어 NumberFormatException이 발생합니다.
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return 정산 상세 정보 (포함된 주문 아이템 목록 포함)
     */
    /**
     * 정산 변경 이력 조회 (관리자용)
     * GET /api/admin/settlements/{settlementId}/history
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return 정산 변경 이력 목록
     */
    @GetMapping("/settlements/{settlementId}/history")
    public ResponseEntity<ApiResponse<List<SettlementHistoryDto>>> getSettlementHistory(
            HttpSession session,
            @PathVariable Long settlementId
    ) {
        List<SettlementHistoryDto> response = settlementService.getSettlementHistory(session, settlementId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 정산 상세 조회 (관리자용)
     * GET /api/admin/settlements/{settlementId}
     * 
     * ⚠️ 중요: 이 엔드포인트는 /settlements/excel과 /settlements/dashboard보다 나중에 정의되어야 합니다.
     * 그렇지 않으면 "excel"이나 "dashboard"가 settlementId로 파싱되어 NumberFormatException이 발생합니다.
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return 정산 상세 정보 (포함된 주문 아이템 목록 포함)
     */
    @GetMapping("/settlements/{settlementId}")
    public ResponseEntity<ApiResponse<SettlementDetailResponseDto>> getSettlementDetail(
            HttpSession session,
            @PathVariable Long settlementId
    ) {
        SettlementDetailResponseDto response = settlementService.getSettlementDetail(session, settlementId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 고객 통계 조회 (관리자용)
     * GET /api/admin/customers/statistics
     * 
     * ⚠️ 중요: 이 엔드포인트는 /customers/{customerId}보다 먼저 정의되어야 합니다.
     * 그렇지 않으면 "statistics"가 customerId로 파싱되어 NumberFormatException이 발생합니다.
     * 
     * @param session HTTP 세션
     * @return 고객 통계 정보
     */
    @GetMapping("/customers/statistics")
    public ResponseEntity<ApiResponse<CustomerStatisticsDto>> getCustomerStatistics(
            HttpSession session
    ) {
        CustomerStatisticsDto response = adminService.getCustomerStatistics(session);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 매출 현황 통계 조회 (관리자용)
     * GET /api/admin/sales/statistics?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/sales/statistics")
    public ResponseEntity<ApiResponse<com.swimshop.swim_mall.admin.dto.SalesStatisticsDto>> getSalesStatistics(
            HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate endDate
    ) {
        com.swimshop.swim_mall.admin.dto.SalesStatisticsDto response = adminService.getSalesStatistics(session, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 고객 목록 조회 (관리자용)
     * GET /api/admin/customers
     * 
     * @param session HTTP 세션
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param pageSize 페이지 크기 (기본값: 20)
     * @param searchKeyword 검색 키워드 (이름 또는 이메일, 선택)
     * @param emailVerified 이메일 인증 여부 필터 (true/false/null, 선택)
     * @return 고객 목록 및 통계
     */
    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getCustomerList(
            HttpSession session,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) String emailVerifiedStr,
            @RequestParam(required = false) String grade
    ) {
        // String으로 받아서 Boolean으로 변환 (Spring의 자동 변환이 안정적이지 않을 수 있음)
        Boolean emailVerified = null;
        if (emailVerifiedStr != null && !emailVerifiedStr.trim().isEmpty()) {
            String trimmed = emailVerifiedStr.trim().toLowerCase();
            if ("true".equals(trimmed)) {
                emailVerified = true;
            } else if ("false".equals(trimmed)) {
                emailVerified = false;
            }
            // 그 외의 값은 null로 처리 (전체 조회)
        }

        // 등급 필터 (코드: BEGINNER, SWIMMER, PRO, MASTER, LEGEND)
        com.swimshop.swim_mall.common.enums.CustomerGradeEnum gradeEnum = null;
        if (grade != null && !grade.trim().isEmpty()) {
            try {
                gradeEnum = com.swimshop.swim_mall.common.enums.CustomerGradeEnum.fromCode(grade.trim());
            } catch (IllegalArgumentException e) {
                // 잘못된 값이면 필터 없이(null) 처리
            }
        }
        
        CustomerListResponseDto response = adminService.getCustomerList(
                session, page, pageSize, searchKeyword, emailVerified, gradeEnum
        );
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("customers", response.getCustomers());
        // 표준 메타 추가(0-based page로 통일: response.page는 0-based 계약)
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", response.getPage());
        meta.put("size", response.getPageSize());
        meta.put("total", response.getTotalCount());
        body.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success(body));
    }
    
    /**
     * 고객 상세 조회 (관리자용)
     * GET /api/admin/customers/{customerId}
     * 
     * ⚠️ 중요: 이 엔드포인트는 /customers/statistics보다 나중에 정의되어야 합니다.
     * 그렇지 않으면 "statistics"가 customerId로 파싱되어 NumberFormatException이 발생합니다.
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 고객 상세 정보 (주문/리뷰/반품 내역 포함)
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDetailResponseDto>> getCustomerDetail(
            HttpSession session,
            @PathVariable Long customerId
    ) {
        CustomerDetailResponseDto response = adminService.getCustomerDetail(session, customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 고객 정보 수정 (관리자용)
     * PATCH /api/admin/customers/{customerId}
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param requestDto 수정할 정보 (이름, 이메일, 활성화 상태)
     * @param active 쿼리 파라미터로 활성화 상태 설정 (선택사항, requestDto의 active보다 우선)
     * @return 수정된 고객 상세 정보
     */
    @PatchMapping("/customers/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDetailResponseDto>> updateCustomer(
            HttpSession session,
            @PathVariable Long customerId,
            @RequestBody(required = false) AdminCustomerUpdateRequestDto requestDto,
            @RequestParam(required = false) Boolean active
    ) {
        // requestDto가 null이면 빈 객체 생성
        if (requestDto == null) {
            requestDto = new AdminCustomerUpdateRequestDto();
        }
        
        // 쿼리 파라미터 active가 있으면 그것을 우선 사용
        if (active != null) {
            requestDto.setActive(active);
        }
        
        // body가 있고 이메일이 있으면 validation 수행
        if (requestDto.getCustomerEmail() != null && !requestDto.getCustomerEmail().trim().isEmpty()) {
            // 간단한 이메일 형식 검증 (@Email 어노테이션 대신)
            if (!requestDto.getCustomerEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(com.swimshop.swim_mall.common.error.ErrorCode.INVALID_REQUEST, "올바른 이메일 형식이 아닙니다."));
            }
        }
        
        CustomerDetailResponseDto response = adminService.updateCustomer(session, customerId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 고객 계정 활성화/비활성화 (관리자용)
     * PATCH /api/admin/customers/{customerId}/status
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param active true면 활성화, false면 비활성화
     * @return 수정된 고객 상세 정보
     */
    @PatchMapping("/customers/{customerId}/status")
    public ResponseEntity<ApiResponse<CustomerDetailResponseDto>> updateCustomerAccountStatus(
            HttpSession session,
            @PathVariable Long customerId,
            @RequestParam Boolean active
    ) {
        CustomerDetailResponseDto response = adminService.updateCustomerAccountStatus(session, customerId, active);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 고객 비밀번호 초기화 (관리자용)
     * POST /api/admin/customers/{customerId}/reset-password
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 임시 비밀번호 (이메일로도 발송됨)
     */
    @PostMapping("/customers/{customerId}/reset-password")
    public ResponseEntity<ApiResponse<String>> resetCustomerPassword(
            HttpSession session,
            @PathVariable Long customerId
    ) {
        String tempPassword = adminService.resetCustomerPassword(session, customerId);
        return ResponseEntity.ok(ApiResponse.success(tempPassword));
    }
    
    /**
     * 고객 이메일 인증 재발송 (관리자용)
     * POST /api/admin/customers/{customerId}/resend-email-verification
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 성공 메시지
     */
    @PostMapping("/customers/{customerId}/resend-email-verification")
    public ResponseEntity<ApiResponse<String>> resendEmailVerification(
            HttpSession session,
            @PathVariable Long customerId
    ) {
        adminService.resendEmailVerification(session, customerId);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증 링크가 재발송되었습니다."));
    }
    
    /**
     * 고객 관리 작업 이력 조회 (관리자용)
     * GET /api/admin/customers/{customerId}/history
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 작업 이력 목록
     */
    @GetMapping("/customers/{customerId}/history")
    public ResponseEntity<ApiResponse<List<com.swimshop.swim_mall.admin.dto.CustomerHistoryDto>>> getCustomerHistory(
            HttpSession session,
            @PathVariable Long customerId
    ) {
        List<com.swimshop.swim_mall.admin.dto.CustomerHistoryDto> history = adminService.getCustomerHistory(session, customerId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    /**
     * 고객 메모 목록 조회 (관리자용)
     * GET /api/admin/customers/{customerId}/notes
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 메모 목록
     */
    @GetMapping("/customers/{customerId}/notes")
    public ResponseEntity<ApiResponse<List<com.swimshop.swim_mall.admin.dto.CustomerNoteDto>>> getCustomerNotes(
            HttpSession session,
            @PathVariable Long customerId
    ) {
        List<com.swimshop.swim_mall.admin.dto.CustomerNoteDto> notes = adminService.getCustomerNotes(session, customerId);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }
    
    /**
     * 고객 메모 작성 (관리자용)
     * POST /api/admin/customers/{customerId}/notes
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param requestDto 메모 내용
     * @return 작성된 메모 정보
     */
    @PostMapping("/customers/{customerId}/notes")
    public ResponseEntity<ApiResponse<com.swimshop.swim_mall.admin.dto.CustomerNoteDto>> createCustomerNote(
            HttpSession session,
            @PathVariable Long customerId,
            @Valid @RequestBody com.swimshop.swim_mall.admin.dto.CustomerNoteRequestDto requestDto
    ) {
        com.swimshop.swim_mall.admin.dto.CustomerNoteDto note = adminService.createCustomerNote(session, customerId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(note));
    }
    
    /**
     * 고객 메모 수정 (관리자용)
     * PATCH /api/admin/customers/{customerId}/notes/{noteId}
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param noteId 메모 ID
     * @param requestDto 수정할 내용
     * @return 수정된 메모 정보
     */
    @PatchMapping("/customers/{customerId}/notes/{noteId}")
    public ResponseEntity<ApiResponse<com.swimshop.swim_mall.admin.dto.CustomerNoteDto>> updateCustomerNote(
            HttpSession session,
            @PathVariable Long customerId,
            @PathVariable Long noteId,
            @Valid @RequestBody com.swimshop.swim_mall.admin.dto.CustomerNoteRequestDto requestDto
    ) {
        com.swimshop.swim_mall.admin.dto.CustomerNoteDto note = adminService.updateCustomerNote(session, customerId, noteId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(note));
    }
    
    /**
     * 고객 메모 삭제 (관리자용)
     * DELETE /api/admin/customers/{customerId}/notes/{noteId}
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param noteId 메모 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/customers/{customerId}/notes/{noteId}")
    public ResponseEntity<ApiResponse<String>> deleteCustomerNote(
            HttpSession session,
            @PathVariable Long customerId,
            @PathVariable Long noteId
    ) {
        adminService.deleteCustomerNote(session, customerId, noteId);
        return ResponseEntity.ok(ApiResponse.success("메모가 삭제되었습니다."));
    }
    
    /**
     * 고객 태그 목록 조회 (관리자용)
     * GET /api/admin/customers/tags
     * 
     * @param session HTTP 세션
     * @return 태그 목록
     */
    @GetMapping("/customers/tags")
    public ResponseEntity<ApiResponse<List<com.swimshop.swim_mall.admin.dto.CustomerTagDto>>> getAllCustomerTags(
            HttpSession session
    ) {
        List<com.swimshop.swim_mall.admin.dto.CustomerTagDto> tags = adminService.getAllCustomerTags(session);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }
    
    /**
     * 고객 태그 생성 (관리자용)
     * POST /api/admin/customers/tags
     * 
     * @param session HTTP 세션
     * @param requestDto 태그 정보
     * @return 생성된 태그 정보
     */
    @PostMapping("/customers/tags")
    public ResponseEntity<ApiResponse<com.swimshop.swim_mall.admin.dto.CustomerTagDto>> createCustomerTag(
            HttpSession session,
            @Valid @RequestBody com.swimshop.swim_mall.admin.dto.CustomerTagRequestDto requestDto
    ) {
        com.swimshop.swim_mall.admin.dto.CustomerTagDto tag = adminService.createCustomerTag(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }
    
    /**
     * 고객 태그 수정 (관리자용)
     * PATCH /api/admin/customers/tags/{tagId}
     * 
     * @param session HTTP 세션
     * @param tagId 태그 ID
     * @param requestDto 수정할 정보
     * @return 수정된 태그 정보
     */
    @PatchMapping("/customers/tags/{tagId}")
    public ResponseEntity<ApiResponse<com.swimshop.swim_mall.admin.dto.CustomerTagDto>> updateCustomerTag(
            HttpSession session,
            @PathVariable Long tagId,
            @Valid @RequestBody com.swimshop.swim_mall.admin.dto.CustomerTagRequestDto requestDto
    ) {
        com.swimshop.swim_mall.admin.dto.CustomerTagDto tag = adminService.updateCustomerTag(session, tagId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }
    
    /**
     * 고객 태그 삭제 (관리자용)
     * DELETE /api/admin/customers/tags/{tagId}
     * 
     * @param session HTTP 세션
     * @param tagId 태그 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/customers/tags/{tagId}")
    public ResponseEntity<ApiResponse<String>> deleteCustomerTag(
            HttpSession session,
            @PathVariable Long tagId
    ) {
        adminService.deleteCustomerTag(session, tagId);
        return ResponseEntity.ok(ApiResponse.success("태그가 삭제되었습니다."));
    }
    
    /**
     * 고객의 태그 목록 조회 (관리자용)
     * GET /api/admin/customers/{customerId}/tags
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 태그 목록
     */
    @GetMapping("/customers/{customerId}/tags")
    public ResponseEntity<ApiResponse<List<com.swimshop.swim_mall.admin.dto.CustomerTagDto>>> getCustomerTags(
            HttpSession session,
            @PathVariable Long customerId
    ) {
        List<com.swimshop.swim_mall.admin.dto.CustomerTagDto> tags = adminService.getCustomerTags(session, customerId);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }
    
    /**
     * 고객에 태그 추가 (관리자용)
     * POST /api/admin/customers/{customerId}/tags/{tagId}
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param tagId 태그 ID
     * @return 성공 메시지
     */
    @PostMapping("/customers/{customerId}/tags/{tagId}")
    public ResponseEntity<ApiResponse<String>> addTagToCustomer(
            HttpSession session,
            @PathVariable Long customerId,
            @PathVariable Long tagId
    ) {
        adminService.addTagToCustomer(session, customerId, tagId);
        return ResponseEntity.ok(ApiResponse.success("태그가 추가되었습니다."));
    }
    
    /**
     * 고객에서 태그 제거 (관리자용)
     * DELETE /api/admin/customers/{customerId}/tags/{tagId}
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param tagId 태그 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/customers/{customerId}/tags/{tagId}")
    public ResponseEntity<ApiResponse<String>> removeTagFromCustomer(
            HttpSession session,
            @PathVariable Long customerId,
            @PathVariable Long tagId
    ) {
        adminService.removeTagFromCustomer(session, customerId, tagId);
        return ResponseEntity.ok(ApiResponse.success("태그가 제거되었습니다."));
    }
    
    /**
     * 고객 활동 로그 조회 (관리자용)
     * GET /api/admin/customers/{customerId}/activity-logs
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param page 페이지 번호 (선택, 기본값: 0)
     * @param pageSize 페이지 크기 (선택, 기본값: 50)
     * @return 활동 로그 목록
     */
    @GetMapping("/customers/{customerId}/activity-logs")
    public ResponseEntity<ApiResponse<List<com.swimshop.swim_mall.admin.dto.CustomerActivityLogDto>>> getCustomerActivityLogs(
            HttpSession session,
            @PathVariable Long customerId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        List<com.swimshop.swim_mall.admin.dto.CustomerActivityLogDto> logs = adminService.getCustomerActivityLogs(session, customerId, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
    
    // ========== 고객 등급 관리 ==========
    
    /**
     * 등급 목록 조회 (관리자용)
     * GET /api/admin/customer-grades
     */
    @GetMapping("/customer-grades")
    public ResponseEntity<ApiResponse<List<CustomerGradeDto>>> getCustomerGrades(HttpSession session) {
        List<CustomerGradeDto> grades = adminService.getCustomerGrades(session);
        return ResponseEntity.ok(ApiResponse.success(grades));
    }
    
    /**
     * 등급 상세 조회 (관리자용)
     * GET /api/admin/customer-grades/{gradeId}
     */
    @GetMapping("/customer-grades/{gradeId}")
    public ResponseEntity<ApiResponse<CustomerGradeDto>> getCustomerGrade(
            HttpSession session,
            @PathVariable Long gradeId
    ) {
        CustomerGradeDto grade = adminService.getCustomerGrade(session, gradeId);
        return ResponseEntity.ok(ApiResponse.success(grade));
    }
    
    /**
     * 등급 수정 (관리자용)
     * 등급명과 등급 레벨은 Enum으로 고정되어 있어 변경 불가능합니다.
     * 최소 구매액, 최소 주문 건수, 할인율, 포인트 적립률, 활성화 여부만 수정 가능합니다.
     * PATCH /api/admin/customer-grades/{gradeId}
     */
    @PatchMapping("/customer-grades/{gradeId}")
    public ResponseEntity<ApiResponse<CustomerGradeDto>> updateCustomerGrade(
            HttpSession session,
            @PathVariable Long gradeId,
            @Valid @RequestBody CustomerGradeRequestDto requestDto
    ) {
        CustomerGradeDto grade = adminService.updateCustomerGrade(session, gradeId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(grade));
    }

}
