package com.swimshop.swim_mall.settlement.entity;

import java.time.LocalDate;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settlement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SettlementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId; // 정산 고유식별자

    @Column(nullable = false)
    private Long totalSalesAmount; // 총 판매 금액 (원 단위)

    @Column(nullable = false)
    private Long commissionAmount; // 수수료 (원 단위)

    @Column(nullable = false)
    private Long settlementAmount; // 정산금액 (원 단위) = 총 판매금액 - 수수료

    @Column(nullable = false)
    private LocalDate settlementCreatedAt; // 생성일자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus settlementStatus; //상태관리

    @Column(nullable = true)
    private LocalDate settlementPeriodStart; // 정산 기간 시작일

    @Column(nullable = true)
    private LocalDate settlementPeriodEnd; // 정산 기간 종료일

    @Column(nullable = true)
    private LocalDate settlementPaidDate; // 정산 지급일 (수동 설정)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "admin_id")
    private AdminEntity admin; // 관리자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "partner_id")
    private PartnerEntity partner; // 입점업체

    /**
     * 정산 엔티티 생성 (빌더 패턴 사용)
     */
    public static SettlementEntity create(
            Long totalSalesAmount,
            Long commissionAmount,
            Long settlementAmount,
            LocalDate settlementCreatedAt,
            SettlementStatus settlementStatus,
            LocalDate settlementPeriodStart,
            LocalDate settlementPeriodEnd,
            AdminEntity admin,
            PartnerEntity partner
    ) {
        SettlementEntity entity = new SettlementEntity();
        entity.totalSalesAmount = totalSalesAmount;
        entity.commissionAmount = commissionAmount;
        entity.settlementAmount = settlementAmount;
        entity.settlementCreatedAt = settlementCreatedAt;
        entity.settlementStatus = settlementStatus;
        entity.settlementPeriodStart = settlementPeriodStart;
        entity.settlementPeriodEnd = settlementPeriodEnd;
        entity.admin = admin;
        entity.partner = partner;
        return entity;
    }

    /**
     * 정산 상태 변경
     */
    public void updateStatus(SettlementStatus status) {
        this.settlementStatus = status;
    }

    /**
     * 정산 상태 변경 및 지급일 설정
     */
    public void updateStatus(SettlementStatus status, LocalDate paidDate) {
        this.settlementStatus = status;
        this.settlementPaidDate = paidDate;
    }

    /**
     * 정산 지급일만 수정 (상태는 유지)
     */
    public void updatePaidDate(LocalDate paidDate) {
        this.settlementPaidDate = paidDate;
    }

}
