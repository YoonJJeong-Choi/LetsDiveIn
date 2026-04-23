package com.swimshop.swim_mall.settlement.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정산 변경 이력 엔티티
 * 정산의 모든 변경 사항을 기록합니다.
 */
@Entity
@Table(name = "settlement_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SettlementHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId; // 이력 고유식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "settlement_id")
    private SettlementEntity settlement; // 정산

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "admin_id")
    private AdminEntity admin; // 변경한 관리자

    @Column(nullable = false)
    private LocalDateTime changedAt; // 변경 일시

    @Column(nullable = false, length = 50)
    private String actionType; // 변경 타입 (CREATE, STATUS_CHANGE, PAID_DATE_UPDATE)

    @Column(nullable = true, length = 1000)
    private String oldValue; // 변경 전 값 (JSON 형태)

    @Column(nullable = true, length = 1000)
    private String newValue; // 변경 후 값 (JSON 형태)

    @Column(nullable = true, length = 500)
    private String reason; // 변경 사유

    /**
     * 정산 이력 생성
     */
    public static SettlementHistoryEntity create(
            SettlementEntity settlement,
            AdminEntity admin,
            String actionType,
            String oldValue,
            String newValue,
            String reason
    ) {
        SettlementHistoryEntity entity = new SettlementHistoryEntity();
        entity.settlement = settlement;
        entity.admin = admin;
        entity.changedAt = LocalDateTime.now();
        entity.actionType = actionType;
        entity.oldValue = oldValue;
        entity.newValue = newValue;
        entity.reason = reason;
        return entity;
    }
}
