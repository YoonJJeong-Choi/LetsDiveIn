package com.swimshop.swim_mall.delivery.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.delivery.dto.DeliveryResponseDto;
import com.swimshop.swim_mall.delivery.dto.DeliveryStartRequestDto;
import com.swimshop.swim_mall.delivery.dto.DeliveryUpdateRequestDto;
import com.swimshop.swim_mall.delivery.service.DeliveryService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@RestController
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * 전체 배송 목록 조회 (관리자/파트너용)
     * GET /api/deliveries
     * - 관리자: 모든 배송 조회
     * - 파트너: 자신의 상품 배송만 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryResponseDto>>> getAllDeliveries(
            HttpSession session
    ) {
        List<DeliveryResponseDto> deliveries = deliveryService.getAllDeliveries(session);
        return ResponseEntity.ok(ApiResponse.success(deliveries));
    }

    /**
     * 배송 번호로 배송 조회
     * GET /api/deliveries/{deliveryNo}
     */
    @GetMapping("/{deliveryNo}")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> getDelivery(
            HttpSession session,
            @PathVariable Long deliveryNo
    ) {
        DeliveryResponseDto delivery = deliveryService.getDelivery(session, deliveryNo);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    /**
     * 배송 시작 (READY → SHIPPED)
     * PATCH /api/deliveries/{deliveryNo}/start
     */
    @PatchMapping("/{deliveryNo}/start")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> startDelivery(
            HttpSession session,
            @PathVariable Long deliveryNo,
            @Valid @RequestBody DeliveryStartRequestDto requestDto
    ) {
        DeliveryResponseDto delivery = deliveryService.startDelivery(session, deliveryNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    /**
     * 배송 완료 (SHIPPED → DELIVERED)
     * PATCH /api/deliveries/{deliveryNo}/complete
     */
    @PatchMapping("/{deliveryNo}/complete")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> completeDelivery(
            HttpSession session,
            @PathVariable Long deliveryNo
    ) {
        DeliveryResponseDto delivery = deliveryService.completeDelivery(session, deliveryNo);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }

    /**
     * 배송 정보 수정 (송장번호, 택배사)
     * PATCH /api/deliveries/{deliveryNo}
     */
    @PatchMapping("/{deliveryNo}")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> updateDelivery(
            HttpSession session,
            @PathVariable Long deliveryNo,
            @Valid @RequestBody DeliveryUpdateRequestDto requestDto
    ) {
        DeliveryResponseDto delivery = deliveryService.updateDelivery(session, deliveryNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }
}
