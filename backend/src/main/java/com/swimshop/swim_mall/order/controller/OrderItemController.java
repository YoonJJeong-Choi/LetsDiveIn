package com.swimshop.swim_mall.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.delivery.dto.DeliveryResponseDto;
import com.swimshop.swim_mall.delivery.service.DeliveryService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * 주문 아이템 관련 API
 */
@RequestMapping("/api/order-items")
@RequiredArgsConstructor
@RestController
public class OrderItemController {

    private final DeliveryService deliveryService;

    /**
     * 주문 아이템별 배송 조회
     * GET /api/order-items/{orderItemNo}/delivery
     */
    @GetMapping("/{orderItemNo}/delivery")
    public ResponseEntity<ApiResponse<DeliveryResponseDto>> getDeliveryByOrderItem(
            HttpSession session,
            @PathVariable Long orderItemNo
    ) {
        DeliveryResponseDto delivery = deliveryService.getDeliveryByOrderItemNo(session, orderItemNo);
        return ResponseEntity.ok(ApiResponse.success(delivery));
    }
}
