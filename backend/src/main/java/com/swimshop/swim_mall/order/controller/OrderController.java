package com.swimshop.swim_mall.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.common.response.PagedResponse;
import com.swimshop.swim_mall.delivery.dto.DeliveryResponseDto;
import com.swimshop.swim_mall.delivery.service.DeliveryService;
import com.swimshop.swim_mall.order.dto.AdminOrderListResponseDto;
import com.swimshop.swim_mall.order.dto.OrderCreateRequestDto;
import com.swimshop.swim_mall.order.dto.OrderItemResponseDto;
import com.swimshop.swim_mall.order.dto.OrderResponseDto;
import com.swimshop.swim_mall.order.dto.OrderStatusUpdateRequestDto;
import com.swimshop.swim_mall.order.service.OrderService;
import com.swimshop.swim_mall.payment.dto.PaymentResponseDto;
import com.swimshop.swim_mall.payment.service.PaymentService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/api/orders")
@RequiredArgsConstructor
@RestController
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DeliveryService deliveryService;

    /**
     * 주문 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            HttpSession session,
            @Valid @RequestBody OrderCreateRequestDto requestDto
    ) {
        OrderResponseDto order = orderService.createOrder(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 주문 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponseDto>>> getOrders(
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<OrderResponseDto> orders = orderService.getOrders(session, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * 주문 상세 조회
     */
    @GetMapping("/{orderNo}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrder(
            HttpSession session,
            @PathVariable Long orderNo
    ) {
        OrderResponseDto order = orderService.getOrder(session, orderNo);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 주문별 결제 조회
     * GET /api/orders/{orderNo}/payment
     */
    @GetMapping("/{orderNo}/payment")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentByOrder(
            HttpSession session,
            @PathVariable Long orderNo
    ) {
        PaymentResponseDto payment = paymentService.getPaymentByOrderNo(session, orderNo);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    /**
     * 주문별 배송 조회
     * GET /api/orders/{orderNo}/deliveries
     */
    @GetMapping("/{orderNo}/deliveries")
    public ResponseEntity<ApiResponse<List<DeliveryResponseDto>>> getDeliveriesByOrder(
            HttpSession session,
            @PathVariable Long orderNo
    ) {
        List<DeliveryResponseDto> deliveries = deliveryService.getDeliveriesByOrderNo(session, orderNo);
        return ResponseEntity.ok(ApiResponse.success(deliveries));
    }

    /**
     * 전체 주문 목록 조회 (관리자/파트너용)
     * GET /api/orders/admin
     * - 관리자: 모든 주문 조회
     * - 파트너: 자신의 상품 주문만 조회
     */
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<AdminOrderListResponseDto>> getAllOrders(
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        AdminOrderListResponseDto orders = orderService.getAllOrders(session, page, size, status);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * 주문 상세 조회 (관리자/파트너용)
     * GET /api/orders/{orderNo}/admin
     */
    @GetMapping("/{orderNo}/admin")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderForAdmin(
            HttpSession session,
            @PathVariable Long orderNo
    ) {
        OrderResponseDto order = orderService.getOrderForAdmin(session, orderNo);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 주문 상태 수동 변경 — 비활성화됨 (403).
     * 상태는 결제·발주 확인·배송·반품 등 전용 API로만 변경합니다.
     * PATCH /api/orders/{orderNo}/status
     */
    @PatchMapping("/{orderNo}/status")
    public ResponseEntity<ApiResponse<OrderResponseDto>> updateOrderStatus(
            HttpSession session,
            @PathVariable Long orderNo,
            @RequestBody OrderStatusUpdateRequestDto requestDto
    ) {
        OrderResponseDto order = orderService.updateOrderStatus(session, orderNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 발주 확인 (파트너/관리자용) - 주문 전체 발주 확인 (레거시 호환)
     * POST /api/orders/{orderNo}/confirm
     * @deprecated 주문 상품별 발주 확인을 사용하세요. (POST /api/orders/order-items/{orderItemNo}/confirm)
     */
    @Deprecated
    @PostMapping("/{orderNo}/confirm")
    public ResponseEntity<ApiResponse<OrderResponseDto>> confirmOrder(
            HttpSession session,
            @PathVariable Long orderNo
    ) {
        OrderResponseDto order = orderService.confirmOrder(session, orderNo);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    /**
     * 주문 상품별 발주 확인 (파트너/관리자용)
     * POST /api/orders/order-items/{orderItemNo}/confirm
     */
    @PostMapping("/order-items/{orderItemNo}/confirm")
    public ResponseEntity<ApiResponse<OrderItemResponseDto>> confirmOrderItem(
            HttpSession session,
            @PathVariable Long orderItemNo
    ) {
        OrderItemResponseDto orderItem = orderService.confirmOrderItem(session, orderItemNo);
        return ResponseEntity.ok(ApiResponse.success(orderItem));
    }

    /**
     * 주문 취소 (고객용)
     * PATCH /api/orders/{orderNo}/cancel
     */
    @PatchMapping("/{orderNo}/cancel")
    public ResponseEntity<ApiResponse<OrderResponseDto>> cancelOrder(
            HttpSession session,
            @PathVariable Long orderNo
    ) {
        OrderResponseDto order = orderService.cancelOrder(session, orderNo);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * 주문 상품별 구매 확정 (고객이 수령 확인)
     * POST /api/order-items/{orderItemNo}/complete
     */
    @PostMapping("/order-items/{orderItemNo}/complete")
    public ResponseEntity<ApiResponse<OrderItemResponseDto>> completeOrderItem(
            HttpSession session,
            @PathVariable Long orderItemNo
    ) {
        OrderItemResponseDto orderItem = orderService.completeOrderItem(session, orderItemNo);
        return ResponseEntity.ok(ApiResponse.success(orderItem));
    }
}
