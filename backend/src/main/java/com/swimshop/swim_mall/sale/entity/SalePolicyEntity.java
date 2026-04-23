package com.swimshop.swim_mall.sale.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.SaleScope;
import com.swimshop.swim_mall.common.enums.SaleStatus;
import com.swimshop.swim_mall.event.entity.EventEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sale_policy")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SalePolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_no", nullable = true)
    private EventEntity event; // 연계 이벤트(선택)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleScope scope; // PRODUCT / OPTION

    @Column(nullable = true)
    private Long targetProductNo; // scope=PRODUCT일 때 대상

    @Column(nullable = true)
    private Long targetOptionNo;  // scope=OPTION일 때 대상

    @Column(nullable = true)
    private Long createdByPartnerId; // 파트너 단독 세일 생성자 (관리자 생성 시 null)

    /**
     * 캠페인(묶음)으로 생성된 정책들을 묶기 위한 식별자.
     * (캠페인 테이블을 새로 만들지 않고 SalePolicy에 같은 값을 저장하는 방식)
     */
    @Column(nullable = true)
    private String campaignId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType; // PERCENT / FIXED

    @Column(nullable = false)
    private Long discountValue; // PERCENT이면 1~100, FIXED이면 금액(원)

    @Column(nullable = true)
    private Long maxDiscountAmount; // 상한(선택, PERCENT일 때 주로 사용)

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status; // ACTIVE / INACTIVE

    @Column(nullable = true, length = 500)
    private String rejectionReason; // 관리자 승인 거절 사유

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_no", nullable = false)
    private AdminEntity admin; // 생성/관리자

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(SaleStatus status) {
        this.status = status;
    }

    public void changeRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void update(
            EventEntity event,
            SaleScope scope,
            Long targetProductNo,
            Long targetOptionNo,
            Long createdByPartnerId,
            String campaignId,
            DiscountType discountType,
            Long discountValue,
            Long maxDiscountAmount,
            LocalDateTime startAt,
            LocalDateTime endAt,
            SaleStatus status
    ) {
        this.event = event;
        this.scope = scope;
        this.targetProductNo = targetProductNo;
        this.targetOptionNo = targetOptionNo;
        this.createdByPartnerId = createdByPartnerId;
        this.campaignId = campaignId;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
    }

}

