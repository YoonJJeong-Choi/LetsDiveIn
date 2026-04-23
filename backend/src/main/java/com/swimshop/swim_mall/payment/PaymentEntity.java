package com.swimshop.swim_mall.payment;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.payment.enums.PaymentStatus;

@Entity
@Table(name = "payment")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentNo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_no", unique = true, nullable = true)
    private OrderEntity order; //주문 조인 (1:1, FK 소유)

    @Column(nullable = false)
    private Long paymentAmount; //결제 금액

    @Column(nullable = false)
    private String paymentMethod; //결제수단

    // 결제사/추적용 필드(nullable, 무중단 추가)
    @Column(nullable = true, length = 50)
    private String provider; // 예: "toss"

    @Column(nullable = true, length = 200)
    private String paymentKey; // PG에서 부여한 paymentKey

    @Column(nullable = true, length = 200)
    private String pgOrderId; // PG 측 orderId

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 30)
    private PaymentStatus status; // READY/PAID/CANCELED 등

    @Column(nullable = true, length = 500)
    private String receiptUrl; // 영수증/결과 링크

    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT", nullable = true)
    private String rawApprovePayload; // 승인 응답 원문(JSON 등)

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public void setPaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }

    public void setPgOrderId(String pgOrderId) {
        this.pgOrderId = pgOrderId;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public void setRawApprovePayload(String rawApprovePayload) {
        this.rawApprovePayload = rawApprovePayload;
    }

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime paymentCreatedAt = LocalDateTime.now(); //생성일시
    
    @Column(nullable = true)
    private LocalDateTime paidAt; // 결제 승인 시점 (결제 성공 시에만 기록)

    @Column(nullable = false)
    @Builder.Default
    private Boolean paymentCancelYn = false; //취소여부
    
    @Column(nullable = true, length = 500)
    private String failReason; // 결제 실패 사유 (실패 시에만 기록)

    /**
     * 결제 성공으로 전환 (멱등 처리용)
     */
    public void markPaid(Long amount, String method, LocalDateTime paidAt) {
        if (amount != null) {
            this.paymentAmount = amount;
        }
        if (method != null && !method.isEmpty()) {
            this.paymentMethod = method;
        }
        this.paidAt = paidAt != null ? paidAt : LocalDateTime.now();
        this.failReason = null;
        this.paymentCancelYn = false;
    }
}
