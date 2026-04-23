package com.swimshop.swim_mall.partner.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.PartnerHistoryActionType;
import com.swimshop.swim_mall.common.enums.PartnerStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파트너 상태 변경 이력
 * 실제 상태 변경이 발생한 경우만 기록 (신청만 하고 승인/거절되지 않은 경우는 제외)
 */
@Entity
@Table(name = "partner_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PartnerHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerEntity partner;

    /**
     * 이력 액션 타입 (실제 상태 변경이 발생한 경우만 기록)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PartnerHistoryActionType actionType;

    /**
     * 변경 전 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private PartnerStatus fromStatus;

    /**
     * 변경 후 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private PartnerStatus toStatus;

    /**
     * 사유 (신청 사유, 거절 사유 등)
     */
    @Column(nullable = true, length = 1000)
    private String reason;

    /**
     * 처리한 관리자 (신청의 경우 null)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = true)
    private AdminEntity admin;

    /**
     * 이력 생성일시
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 파트너 이력 생성 (입점 신청)
     */
    public static PartnerHistoryEntity createApplication(PartnerEntity partner) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.APPLICATION)
                .fromStatus(null)
                .toStatus(PartnerStatus.PENDING)
                .reason(null)
                .admin(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (입점 승인)
     */
    public static PartnerHistoryEntity createApproval(PartnerEntity partner, AdminEntity admin) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.APPROVAL)
                .fromStatus(PartnerStatus.PENDING)
                .toStatus(PartnerStatus.APPROVED)
                .reason(null)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (입점 거절)
     */
    public static PartnerHistoryEntity createRejection(PartnerEntity partner, AdminEntity admin, String reason) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.REJECTION)
                .fromStatus(PartnerStatus.PENDING)
                .toStatus(PartnerStatus.REJECTED)
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (휴업 승인 - APPROVED → INACTIVE)
     */
    public static PartnerHistoryEntity createDeactivationApproved(PartnerEntity partner, AdminEntity admin) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.DEACTIVATION_APPROVED)
                .fromStatus(PartnerStatus.APPROVED)
                .toStatus(PartnerStatus.INACTIVE)
                .reason(partner.getDeactivationRequestReason())
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (재활성화 승인 - INACTIVE → APPROVED)
     */
    public static PartnerHistoryEntity createReactivationApproved(PartnerEntity partner, AdminEntity admin, String reason) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.REACTIVATION_APPROVED)
                .fromStatus(PartnerStatus.INACTIVE)
                .toStatus(PartnerStatus.APPROVED)
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (관리자 직접 비활성화 - APPROVED → INACTIVE)
     */
    public static PartnerHistoryEntity createDeactivated(PartnerEntity partner, AdminEntity admin) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.DEACTIVATED)
                .fromStatus(PartnerStatus.APPROVED)
                .toStatus(PartnerStatus.INACTIVE)
                .reason("관리자 직접 비활성화")
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (관리자 직접 재활성화 - INACTIVE → APPROVED)
     */
    public static PartnerHistoryEntity createActivated(PartnerEntity partner, AdminEntity admin) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.ACTIVATED)
                .fromStatus(PartnerStatus.INACTIVE)
                .toStatus(PartnerStatus.APPROVED)
                .reason("관리자 직접 재활성화")
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (휴업 신청 - 상태 변경 없음)
     */
    public static PartnerHistoryEntity createDeactivationRequest(PartnerEntity partner, String reason) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.DEACTIVATION_REQUEST)
                .fromStatus(partner.getPartnerStatus())
                .toStatus(partner.getPartnerStatus()) // 상태 변경 없음
                .reason(reason)
                .admin(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (휴업 신청 거절 - 상태 변경 없음)
     */
    public static PartnerHistoryEntity createDeactivationRejection(PartnerEntity partner, AdminEntity admin, String reason) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.DEACTIVATION_REJECTION)
                .fromStatus(partner.getPartnerStatus())
                .toStatus(partner.getPartnerStatus()) // 상태 변경 없음
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (재활성화 신청 - 상태 변경 없음)
     */
    public static PartnerHistoryEntity createReactivationRequest(PartnerEntity partner, String reason) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.REACTIVATION_REQUEST)
                .fromStatus(partner.getPartnerStatus())
                .toStatus(partner.getPartnerStatus()) // 상태 변경 없음
                .reason(reason)
                .admin(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파트너 이력 생성 (재활성화 신청 거절 - 상태 변경 없음)
     */
    public static PartnerHistoryEntity createReactivationRejection(PartnerEntity partner, AdminEntity admin, String reason) {
        return PartnerHistoryEntity.builder()
                .partner(partner)
                .actionType(PartnerHistoryActionType.REACTIVATION_REJECTION)
                .fromStatus(partner.getPartnerStatus())
                .toStatus(partner.getPartnerStatus()) // 상태 변경 없음
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
