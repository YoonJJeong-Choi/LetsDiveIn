package com.swimshop.swim_mall.return_order.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.common.enums.ReturnReasonType;
import com.swimshop.swim_mall.common.enums.ReturnRiskTier;
import com.swimshop.swim_mall.common.enums.ReturnStatus;

@Entity
@Table(name = "return")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long returnNo; //반품 고유식별자

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_no", nullable = false, unique = true)
    private OrderItemEntity orderItem; //주문 상품 조인 (1:1 관계)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus returnStatus; //반품 상태

    @Column(nullable = false)
    private LocalDateTime returnRequestedAt; //반품 신청일시

    @Column(nullable = true, length = 1000)
    private String returnReason; //반품 사유

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReturnReasonType returnReasonType;

    @Column(nullable = true)
    private Integer returnRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private ReturnRiskTier returnRiskTier;

    @Column(nullable = false)
    private Long returnAmount; //반품 금액

    @Column(nullable = true)
    private String returnTrackingNumber; //송장번호

    @Column(nullable = true)
    private String returnCourier; //택배사

    @Column(nullable = true, length = 1000)
    private String rejectionReason; //반품 거절 사유 (REJECTED 상태일 때)

    /**
     * 반품 상태 변경
     */
    public void updateStatus(ReturnStatus newStatus, String trackingNumber, String courier) {
        this.returnStatus = newStatus;
        if (trackingNumber != null) {
            this.returnTrackingNumber = trackingNumber;
        }
        if (courier != null) {
            this.returnCourier = courier;
        }
    }

    /**
     * 반품 거절 처리
     */
    public void reject(String rejectionReason) {
        this.returnStatus = ReturnStatus.REJECTED;
        this.rejectionReason = rejectionReason;
    }

    /**
     * 반품 승인 처리
     */
    public void approve() {
        this.returnStatus = ReturnStatus.APPROVED;
    }
}

