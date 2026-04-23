package com.swimshop.swim_mall.return_order.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 반품 변경 이력 엔티티
 * 반품의 모든 변경 사항을 기록합니다.
 */
@Entity
@Table(name = "return_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReturnHistoryEntity {
    
    // 필드에 직접 접근 가능하도록 설정 (이력 기록용)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId; // 이력 고유식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "return_no")
    private ReturnEntity returnEntity; // 반품

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, name = "admin_id")
    private AdminEntity admin; // 변경한 관리자 (null 가능)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true, name = "partner_id")
    private PartnerEntity partner; // 변경한 파트너 (null 가능)

    @Column(nullable = false)
    private LocalDateTime changedAt; // 변경 일시

    @Column(nullable = false, length = 50)
    private String actionType; // 변경 타입 (CREATE, APPROVE, REJECT, STATUS_CHANGE, TRACKING_UPDATE)

    @Column(nullable = true, length = 1000)
    private String oldValue; // 변경 전 값 (JSON 형태)

    @Column(nullable = true, length = 1000)
    private String newValue; // 변경 후 값 (JSON 형태)

    @Column(nullable = true, length = 500)
    private String reason; // 변경 사유

    /**
     * 반품 이력 생성 (관리자용)
     */
    public static ReturnHistoryEntity createByAdmin(
            ReturnEntity returnEntity,
            AdminEntity admin,
            String actionType,
            String oldValue,
            String newValue,
            String reason
    ) {
        ReturnHistoryEntity entity = new ReturnHistoryEntity();
        entity.returnEntity = returnEntity;
        entity.admin = admin;
        entity.partner = null;
        entity.changedAt = LocalDateTime.now();
        entity.actionType = actionType;
        entity.oldValue = oldValue;
        entity.newValue = newValue;
        entity.reason = reason;
        return entity;
    }

    /**
     * 반품 이력 생성 (파트너용)
     */
    public static ReturnHistoryEntity createByPartner(
            ReturnEntity returnEntity,
            PartnerEntity partner,
            String actionType,
            String oldValue,
            String newValue,
            String reason
    ) {
        ReturnHistoryEntity entity = new ReturnHistoryEntity();
        entity.returnEntity = returnEntity;
        entity.admin = null;
        entity.partner = partner;
        entity.changedAt = LocalDateTime.now();
        entity.actionType = actionType;
        entity.oldValue = oldValue;
        entity.newValue = newValue;
        entity.reason = reason;
        return entity;
    }
    
    /**
     * 반품 이력 생성 (고객 신청용 - 관리자/파트너 없음)
     */
    public static ReturnHistoryEntity createByCustomer(
            ReturnEntity returnEntity,
            String actionType,
            String oldValue,
            String newValue,
            String reason
    ) {
        ReturnHistoryEntity entity = new ReturnHistoryEntity();
        entity.returnEntity = returnEntity;
        entity.admin = null;
        entity.partner = null;
        entity.changedAt = LocalDateTime.now();
        entity.actionType = actionType;
        entity.oldValue = oldValue;
        entity.newValue = newValue;
        entity.reason = reason;
        return entity;
    }
}
