package com.swimshop.swim_mall.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.repository.OrderRepository;
import com.swimshop.swim_mall.payment.PaymentEntity;
import com.swimshop.swim_mall.payment.PaymentRepository;
import com.swimshop.swim_mall.payment.dto.PaymentApproveRequestDto;
import com.swimshop.swim_mall.payment.dto.PaymentResponseDto;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuthService authService;

    /**
     * 결제 승인 처리
     * PENDING_PAYMENT → PAID → ACTIVE
     */
    @Transactional
    public PaymentResponseDto approvePayment(HttpSession session, PaymentApproveRequestDto requestDto) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 주문 조회
        OrderEntity order = orderRepository.findByOrderNo(requestDto.getOrderNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문이 현재 고객의 것인지 검증
        Long customerId = getCustomerIdFromSession(session);
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        // 중복 결제 방지 강화: Payment 레코드 존재 여부로도 체크
        // 같은 orderId에 결제 성공 기록이 이미 있으면 재결제 차단
        if (order.getPayment() != null) {
            PaymentEntity existingPayment = order.getPayment();
            // 결제 성공 기록이 있으면 (paidAt이 있으면 성공, 없으면 실패)
            if (existingPayment.getPaidAt() != null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "이미 결제가 완료된 주문입니다. 결제 번호: " + existingPayment.getPaymentNo() + 
                    ", 결제 시점: " + existingPayment.getPaidAt());
            }
        }
        
        // 주문 상태 검증 (PENDING_PAYMENT 상태만 결제 승인 가능)
        // 중복 결제 방지: 이미 결제된 주문은 다시 결제할 수 없음
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus != OrderStatus.PENDING_PAYMENT) {
            String errorMessage;
            if (currentStatus == OrderStatus.PAID || currentStatus == OrderStatus.ACTIVE) {
                // 조기 리턴: 이미 결제 완료 상태면 즉시 반환
                PaymentEntity existingPayment = order.getPayment();
                if (existingPayment != null && existingPayment.getPaidAt() != null) {
                    // 기존 결제 정보 반환
                    return PaymentResponseDto.builder()
                            .paymentNo(existingPayment.getPaymentNo())
                            .orderNo(order.getOrderNo())
                            .paymentAmount(existingPayment.getPaymentAmount())
                            .paymentMethod(existingPayment.getPaymentMethod())
                            .paymentStatus(getPaymentStatusFromOrder(order.getOrderStatus()))
                            .orderStatus(order.getOrderStatus())
                            .paymentCreatedAt(existingPayment.getPaymentCreatedAt())
                            .paidAt(existingPayment.getPaidAt())
                            .paymentCancelYn(existingPayment.getPaymentCancelYn())
                            .build();
                }
                errorMessage = "이미 결제가 완료된 주문입니다. (현재 상태: " + currentStatus.getLabel() + ")";
            } else if (currentStatus == OrderStatus.PAYMENT_FAILED) {
                errorMessage = "결제 실패한 주문입니다. 재결제 기능은 아직 구현되지 않았습니다. (현재 상태: " + currentStatus.getLabel() + ")";
            } else if (currentStatus == OrderStatus.CANCELLED) {
                errorMessage = "취소된 주문입니다. 결제할 수 없습니다. (현재 상태: " + currentStatus.getLabel() + ")";
            } else {
                errorMessage = "결제 대기중인 주문만 결제 승인할 수 있습니다. (현재 상태: " + currentStatus.getLabel() + ")";
            }
            throw new BusinessException(ErrorCode.INVALID_REQUEST, errorMessage);
        }
        
        // 결제 금액 검증 (클라이언트가 금액을 보내면 서버 금액과 비교)
        Long serverAmount = order.getOrderTotalPrice();
        if (requestDto.getPaymentAmount() != null) {
            if (!requestDto.getPaymentAmount().equals(serverAmount)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "결제 금액이 주문 금액과 일치하지 않습니다. 주문 금액: " + serverAmount + ", 요청 금액: " + requestDto.getPaymentAmount());
            }
        }
        
        // 결제 성공: Payment 레코드 생성 (결제 기록)
        // 서버에서 계산한 금액을 사용 (클라이언트 금액이 아닌 서버 금액이 최종 결제 금액)
        LocalDateTime paidAt = LocalDateTime.now();
        PaymentEntity payment = PaymentEntity.builder()
                .paymentAmount(serverAmount)
                .paymentMethod(order.getPaymentMethod())
                .paidAt(paidAt) // 결제 승인 시점 기록
                .build();
        payment = paymentRepository.save(payment);
        
        // 양방향 관계 설정: Order에 Payment 연결
        order.setPayment(payment);
        
        // 주문 상태 업데이트: PENDING_PAYMENT → PAID
        // 발주 확인은 별도 API에서 처리 (PAID → ACTIVE)
        order.updateStatus(OrderStatus.PAID);
        orderRepository.save(order);
        
        // 배송 생성은 발주 확인 시점에 수행됨
        
        // 응답 DTO 생성 (결제 상태는 주문 상태에서 파생)
        return PaymentResponseDto.builder()
                .paymentNo(payment.getPaymentNo())
                .orderNo(order.getOrderNo())
                .paymentAmount(payment.getPaymentAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(getPaymentStatusFromOrder(order.getOrderStatus()))
                .orderStatus(order.getOrderStatus()) // 주문 상태 포함
                .paymentCreatedAt(payment.getPaymentCreatedAt())
                .paidAt(payment.getPaidAt()) // 결제 승인 시점 포함
                .paymentCancelYn(payment.getPaymentCancelYn())
                .build();
    }
    
    /**
     * 결제 실패 처리
     * PENDING_PAYMENT → PAYMENT_FAILED
     */
    @Transactional
    public PaymentResponseDto failPayment(HttpSession session, Long orderNo, String reason) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 주문 조회
        OrderEntity order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문이 현재 고객의 것인지 검증
        Long customerId = getCustomerIdFromSession(session);
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        // 주문 상태 검증 (PENDING_PAYMENT 상태만 결제 실패 처리 가능)
        OrderStatus currentStatus = order.getOrderStatus();
        if (currentStatus != OrderStatus.PENDING_PAYMENT) {
            String errorMessage;
            if (currentStatus == OrderStatus.PAID || currentStatus == OrderStatus.ACTIVE) {
                errorMessage = "이미 결제가 완료된 주문입니다. 결제 실패 처리할 수 없습니다. (현재 상태: " + currentStatus.getLabel() + ")";
            } else if (currentStatus == OrderStatus.PAYMENT_FAILED) {
                errorMessage = "이미 결제 실패 처리된 주문입니다. (현재 상태: " + currentStatus.getLabel() + ")";
            } else if (currentStatus == OrderStatus.CANCELLED) {
                errorMessage = "취소된 주문입니다. 결제 실패 처리할 수 없습니다. (현재 상태: " + currentStatus.getLabel() + ")";
            } else {
                errorMessage = "결제 대기중인 주문만 결제 실패 처리할 수 있습니다. (현재 상태: " + currentStatus.getLabel() + ")";
            }
            throw new BusinessException(ErrorCode.INVALID_REQUEST, errorMessage);
        }
        
        // 결제 실패: Payment 레코드 생성 (실패 기록, failReason 포함)
        PaymentEntity payment = PaymentEntity.builder()
                .paymentAmount(order.getOrderTotalPrice())
                .paymentMethod(order.getPaymentMethod())
                .failReason(reason != null ? reason.trim() : "결제 실패")
                .build();
        payment = paymentRepository.save(payment);
        
        // 양방향 관계 설정: Order에 Payment 연결
        order.setPayment(payment);
        
        // 주문 상태 업데이트: PENDING_PAYMENT → PAYMENT_FAILED
        order.updateStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
        
        // 응답 DTO 생성 (결제 상태는 주문 상태에서 파생)
        return PaymentResponseDto.builder()
                .paymentNo(payment.getPaymentNo())
                .orderNo(order.getOrderNo())
                .paymentAmount(payment.getPaymentAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(getPaymentStatusFromOrder(order.getOrderStatus()))
                .orderStatus(order.getOrderStatus()) // 주문 상태 포함
                .paymentCreatedAt(payment.getPaymentCreatedAt())
                .paidAt(payment.getPaidAt()) // 결제 실패 시 null
                .paymentCancelYn(payment.getPaymentCancelYn())
                .build();
    }
    
    /**
     * 결제 번호로 결제 조회
     * GET /api/payments/{paymentNo}
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPayment(HttpSession session, Long paymentNo) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 결제 조회
        PaymentEntity payment = paymentRepository.findById(paymentNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        
        // 결제가 현재 고객의 주문인지 검증
        Long customerId = getCustomerIdFromSession(session);
        if (!payment.getOrder().getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        OrderEntity order = payment.getOrder();
        
        // 응답 DTO 생성
        return PaymentResponseDto.builder()
                .paymentNo(payment.getPaymentNo())
                .orderNo(order.getOrderNo())
                .paymentAmount(payment.getPaymentAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(getPaymentStatusFromOrder(order.getOrderStatus()))
                .orderStatus(order.getOrderStatus())
                .paymentCreatedAt(payment.getPaymentCreatedAt())
                .paidAt(payment.getPaidAt())
                .paymentCancelYn(payment.getPaymentCancelYn())
                .build();
    }
    
    /**
     * 주문 번호로 결제 조회
     * GET /api/orders/{orderNo}/payment
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByOrderNo(HttpSession session, Long orderNo) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 주문 조회
        OrderEntity order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        
        // 주문이 현재 고객의 것인지 검증
        Long customerId = getCustomerIdFromSession(session);
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        // 결제 조회
        PaymentEntity payment = order.getPayment();
        if (payment == null) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "이 주문에 대한 결제 정보가 없습니다.");
        }
        
        // 응답 DTO 생성
        return PaymentResponseDto.builder()
                .paymentNo(payment.getPaymentNo())
                .orderNo(order.getOrderNo())
                .paymentAmount(payment.getPaymentAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(getPaymentStatusFromOrder(order.getOrderStatus()))
                .orderStatus(order.getOrderStatus())
                .paymentCreatedAt(payment.getPaymentCreatedAt())
                .paidAt(payment.getPaidAt())
                .paymentCancelYn(payment.getPaymentCancelYn())
                .build();
    }
    
    /**
     * 주문 상태에서 결제 상태 문자열 반환 (응답용)
     */
    private String getPaymentStatusFromOrder(OrderStatus orderStatus) {
        switch (orderStatus) {
            case PAID:
            case ACTIVE:
                return "APPROVED";
            case PAYMENT_FAILED:
                return "FAILED";
            case PENDING_PAYMENT:
                return "PENDING";
            case CANCELLED:
                return "CANCELED";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * 세션에서 고객 ID 가져오기
     */
    private Long getCustomerIdFromSession(HttpSession session) {
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId"); // 통합 로그인(CUSTOMER) 시 subjectId
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        return customerId;
    }
    
    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return (Long) value;
    }
}
