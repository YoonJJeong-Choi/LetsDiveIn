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

/**
 * 파트너 정산 배치(건) 엔티티.
 * <p>
 * 용어 주의 — UI·기획에서 "정산 기간"을 "송금/지급이 이뤄진 날(또는 그 주기)"로 읽기 쉬운데,
 * 이 프로젝트의 {@code settlementPeriodStart}/{@code settlementPeriodEnd}는 그 뜻이 아니다.
 * 생성 시 포함된 주문 상품들의 주문일({@code OrderEntity} 기준) 최소·최대로만 채워지는
 * 매출(주문) 발생일 범위 라벨이다. 구매 확정일 기준도 아니다({@code SettlementService#createSettlement}).
 * <p>
 * 관리자가 파트너에게 실제로 지급 완료 처리한 날짜에 가까운 값은 {@code settlementPaidDate}이며,
 * 배치 레코드를 만든 날은 {@code settlementCreatedAt}이다.
 */
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
    private LocalDate settlementCreatedAt; // 정산서(건) 확정정

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus settlementStatus; //상태관리

    @Column(nullable = true)
    private LocalDate settlementPeriodStart; // 여러 주문들의 주문일 시작일

    @Column(nullable = true)
    private LocalDate settlementPeriodEnd; // 여러 주문들의 주문일 종료일

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
