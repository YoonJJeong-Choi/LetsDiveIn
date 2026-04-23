package com.swimshop.swim_mall.sale.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.sale.dto.SalePolicyRequestDto;
import com.swimshop.swim_mall.sale.dto.SaleCampaignRequestDto;
import com.swimshop.swim_mall.sale.dto.SaleCancelRequestDto;
import com.swimshop.swim_mall.sale.dto.SaleRejectRequestDto;
import com.swimshop.swim_mall.sale.dto.SalePolicyResponseDto;
import com.swimshop.swim_mall.sale.service.SalePolicyService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api")
@RequiredArgsConstructor
@RestController
public class SalePolicyAdminController {

    private final SalePolicyService salePolicyService;

    // 관리자 CRUD
    @PostMapping("/admin/sales")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> createSale(
            HttpSession session,
            @Validated @RequestBody SalePolicyRequestDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.create(session, dto)));
    }

    @PutMapping("/admin/sales/{id}")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> updateSale(
            HttpSession session,
            @PathVariable Long id,
            @Validated @RequestBody SalePolicyRequestDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.update(session, id, dto)));
    }

    @DeleteMapping("/admin/sales/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSale(
            HttpSession session,
            @PathVariable Long id
    ) {
        salePolicyService.delete(session, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/admin/sales")
    public ResponseEntity<ApiResponse<List<SalePolicyResponseDto>>> listSales(
            HttpSession session
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.listAdmin(session)));
    }

    @GetMapping("/admin/sales/{id}")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> getSale(
            HttpSession session,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.getAdmin(session, id)));
    }

    @PatchMapping("/admin/sales/{id}/approve")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> approveSale(
            HttpSession session,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.approveByAdmin(session, id)));
    }

    @PatchMapping("/admin/sales/{id}/reject")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> rejectSale(
            HttpSession session,
            @PathVariable Long id,
            @Validated @RequestBody SaleRejectRequestDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.rejectByAdmin(session, id, dto.getRejectionReason())));
    }

    @PatchMapping("/admin/sales/{id}/cancel")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> cancelSaleByAdmin(
            HttpSession session,
            @PathVariable Long id,
            @Validated @RequestBody SaleCancelRequestDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.cancelByAdmin(session, id, dto.getCancelReason())));
    }

    // 적용 조회 (주문 생성 전 시뮬레이션 용)
    @GetMapping("/sales/applicable")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> getApplicableSale(
            HttpSession session,
            @RequestParam(required = false) Long productNo,
            @RequestParam(required = false) Long optionNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at
    ) {
        // 가격 보장(15분) 세션 락이 활성인 경우, at 파라미터가 없으면 락 시작 시각으로 판정
        LocalDateTime lockStart = (LocalDateTime) session.getAttribute("priceLockStartedAt");
        if (at == null && lockStart != null) {
            LocalDateTime lockUntil = lockStart.plusMinutes(15);
            if (!LocalDateTime.now().isAfter(lockUntil)) {
                at = lockStart;
            }
        }
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.findApplicable(productNo, optionNo, at)));
    }

    // 파트너(하이브리드)
    @PostMapping("/partner/sales")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> createPartnerSale(
            HttpSession session,
            @Validated @RequestBody SalePolicyRequestDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.createByPartner(session, dto)));
    }

    @GetMapping("/partner/sales")
    public ResponseEntity<ApiResponse<List<SalePolicyResponseDto>>> listPartnerSales(
            HttpSession session
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.listByPartner(session)));
    }

    @GetMapping("/partner/sales/{id}")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> getPartnerSale(
            HttpSession session,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.getByPartner(session, id)));
    }

    @PatchMapping("/partner/sales/{id}/cancel")
    public ResponseEntity<ApiResponse<SalePolicyResponseDto>> cancelPartnerSale(
            HttpSession session,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.cancelByPartner(session, id)));
    }

    // 파트너 단독 "캠페인(묶음)" 생성 (내부적으로 SalePolicy를 다건 생성 + 동일 campaignId 부여)
    @PostMapping("/partner/sales/campaign")
    public ResponseEntity<ApiResponse<List<SalePolicyResponseDto>>> createPartnerSaleCampaign(
            HttpSession session,
            @Validated @RequestBody SaleCampaignRequestDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(salePolicyService.createCampaignByPartner(session, dto)));
    }

    @PatchMapping("/partner/sales/campaigns/{campaignId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPartnerSaleCampaign(
            HttpSession session,
            @PathVariable String campaignId
    ) {
        salePolicyService.cancelCampaignByPartner(session, campaignId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

