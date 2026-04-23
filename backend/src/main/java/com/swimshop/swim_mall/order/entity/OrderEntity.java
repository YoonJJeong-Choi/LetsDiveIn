package com.swimshop.swim_mall.order.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.payment.PaymentEntity;

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderNo;

    @Column(nullable = false)
    private Long orderTotalPrice; //총 주문 금액

    @Column(nullable = false)
    @Builder.Default
    private Long usedPointAmount = 0L; // 주문 시 사용 포인트 스냅샷

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime orderCreatedAt = LocalDateTime.now(); //생성일시

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT; // 주문 생성 직후: PENDING_PAYMENT

    // 주의: 구매 확정은 OrderItem.completedAt으로 관리합니다.
    // Order 레벨에서는 구매 확정 상태를 관리하지 않습니다.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "customer_id")
    private CustomerEntity customer; //고객 조인

    @OneToOne(mappedBy = "order")
    private PaymentEntity payment; // 결제 조인 (FK는 Payment가 소유)

    // 배송지 정보
    @Column(nullable = false)
    private String recipientName; // 수령인 이름

    @Column(nullable = false)
    private String recipientPhone; // 수령인 전화번호

    @Column(nullable = false)
    private String deliveryAddress; // 배송지 주소

    @Column(nullable = true)
    private String deliveryAddressDetail; // 배송지 상세 주소

    @Column(nullable = true)
    private String deliveryZipCode; // 우편번호

    // 주문 메모
    @Column(nullable = true, length = 1000)
    private String orderMemo; // 주문 메모
    
    // 결제 정보 (결제 성공/실패 시 Payment 생성에 사용)
    @Column(nullable = false)
    private String paymentMethod; // 결제 방법 (예: "CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT")

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemEntity> orderItems = new ArrayList<>(); //주문 상품 목록
    
    /**
     * 주문 상태 업데이트 (검증 없이 변경 - 자동 상태 변경용)
     */
    public void updateStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
    }
    
    /**
     * 주문 상태 변경 (관리자 수동 변경용 - 검증 포함)
     * @param newStatus 변경할 상태
     * @throws IllegalStateException 허용되지 않은 상태 전이인 경우
     */
    public void changeStatus(OrderStatus newStatus) {
        // 같은 상태로 변경하는 것은 허용
        if (this.orderStatus == newStatus) {
            return;
        }
        
        // 취소된 주문은 변경 불가
        if (this.orderStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("취소된 주문의 상태는 변경할 수 없습니다.");
        }
        
        // 취소로 변경하는 경우: 발주 확인되지 않은 경우만 허용
        if (newStatus == OrderStatus.CANCELLED) {
            // 발주 확인된 주문 상품이 있는지 확인
            boolean hasConfirmedItem = this.orderItems.stream()
                    .anyMatch(orderItem -> orderItem.getConfirmedAt() != null);
            
            if (hasConfirmedItem) {
                throw new IllegalStateException("발주 확인된 주문은 취소할 수 없습니다.");
            }
            
            // 배송이 시작된 경우도 취소 불가 (추가 안전장치)
            boolean hasShippedDelivery = this.orderItems.stream()
                    .anyMatch(orderItem -> {
                        if (orderItem.getDelivery() != null) {
                            DeliveryStatus deliveryStatus = orderItem.getDelivery().getDeliveryStatus();
                            return deliveryStatus == DeliveryStatus.SHIPPED || 
                                   deliveryStatus == DeliveryStatus.DELIVERED;
                        }
                        return false;
                    });
            
            if (hasShippedDelivery) {
                throw new IllegalStateException("배송이 시작된 주문은 취소할 수 없습니다.");
            }
        }
        
        // 상태 전이 검증
        validateStatusTransition(this.orderStatus, newStatus);
        
        this.orderStatus = newStatus;
    }
    
    /**
     * 상태 전이 검증
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        switch (currentStatus) {
            case PENDING_PAYMENT:
                // 결제 대기 → 결제 완료, 결제 실패, 취소만 가능
                if (newStatus != OrderStatus.PAID && 
                    newStatus != OrderStatus.PAYMENT_FAILED && 
                    newStatus != OrderStatus.CANCELLED) {
                    throw new IllegalStateException(
                        String.format("결제 대기 상태에서는 결제 완료, 결제 실패, 취소만 가능합니다. 요청한 상태: %s", 
                            newStatus.getLabel()));
                }
                break;
                
            case PAID:
                // 결제 완료 → 주문 진행중, 취소만 가능
                if (newStatus != OrderStatus.ACTIVE && newStatus != OrderStatus.CANCELLED) {
                    throw new IllegalStateException(
                        String.format("결제 완료 상태에서는 주문 진행중, 취소만 가능합니다. 요청한 상태: %s", 
                            newStatus.getLabel()));
                }
                break;
                
            case ACTIVE:
                // 주문 진행중 → 취소만 가능 (취소는 배송 전만, 위에서 이미 검증됨)
                // 주의: 구매 확정은 OrderItem 단위로 관리하므로 Order 상태 변경과 무관
                if (newStatus != OrderStatus.CANCELLED) {
                    throw new IllegalStateException(
                        String.format("주문 진행중 상태에서는 취소만 가능합니다. 요청한 상태: %s", 
                            newStatus.getLabel()));
                }
                break;
                
            case PAYMENT_FAILED:
                // 결제 실패 → 결제 대기(재시도)만 가능
                if (newStatus != OrderStatus.PENDING_PAYMENT) {
                    throw new IllegalStateException(
                        String.format("결제 실패 상태에서는 결제 대기(재시도)만 가능합니다. 요청한 상태: %s", 
                            newStatus.getLabel()));
                }
                break;
                
            case CANCELLED:
                // 취소된 주문은 변경 불가 (위에서 이미 검증됨)
                throw new IllegalStateException("취소된 주문의 상태는 변경할 수 없습니다.");
                
            default:
                throw new IllegalStateException(
                    String.format("알 수 없는 주문 상태입니다. 현재 상태: %s", currentStatus.getLabel()));
        }
    }
    
    /**
     * 결제 연결 (결제 성공/실패 시 Payment 생성 후 연결)
     */
    public void setPayment(PaymentEntity payment) {
        this.payment = payment;
    }
    
    /**
     * 결제수단 동기화(웹훅/승인에서 실제 수단 도착 시 갱신)
     */
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    // 주의: 구매 확정은 OrderItem.completedAt으로 관리합니다.
    // Order 레벨에서는 구매 확정 상태를 관리하지 않습니다.
}
