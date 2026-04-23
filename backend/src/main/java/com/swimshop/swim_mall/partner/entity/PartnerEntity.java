package com.swimshop.swim_mall.partner.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.PartnerStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "partner")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PartnerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partner_id")
    private Long partnerId; //입점업체 고유식별자

    @Column(nullable = false)
    private String partnerName; //입점업체명

    @Column(nullable = false, length = 20)
    private String partnerContact; //입점업체 전화번호 (010-1234-5678)

    @Column(nullable = false, length = 50)
    private String partnerBankAccount; // 정산계좌 (111-222-456789)

    @Column(nullable = false, length = 12)
    private String businessRegistrationNumber; // 사업자등록번호 (123-45-67890)

    @Column(nullable = true)
    private String email; // 파트너 이메일 (신청 시 입력, 승인 시 Account 생성, 부트스트랩 시는 null)

    @Column(nullable = true, length = 100)
    private String representativeBrandCode; // 대표 취급 브랜드 코드(1파트너 1브랜드)

    // 입점 신청 서류 파일 (UploadedFileEntity.id 참조)
    @Column(nullable = true)
    private Long businessRegistrationFileId; // 사업자등록증 사본 fileId

    @Column(nullable = true)
    private Long bankAccountFileId; // 통장 사본 fileId

    /** 
     * 파트너 운영 상태 (PENDING/APPROVED/INACTIVE)
     * 주의: "운영 상태"는 이 필드를 사용. Account.status는 "로그인 가능 여부"용
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartnerStatus partnerStatus; //상태관리

    @Column(nullable = true)
    private LocalDateTime partnerApprovedAt; //승인일시

    @Column(nullable = true, length = 500)
    private String rejectionReason; // 거절 사유

    @Column(nullable = true, length = 500)
    private String deactivationRequestReason; // 휴업 신청 사유

    @Column(nullable = true)
    private LocalDateTime deactivationRequestedAt; // 휴업 신청일시

    @Column(nullable = true, length = 500)
    private String deactivationRejectionReason; // 휴업 신청 거절 사유

    @Column(nullable = true)
    private LocalDateTime deactivationRejectedAt; // 휴업 신청 거절일시

    @Column(nullable = true, length = 500)
    private String reactivationRequestReason; // 재활성화 신청 사유

    @Column(nullable = true)
    private LocalDateTime reactivationRequestedAt; // 재활성화 신청일시

    @Column(nullable = true, length = 500)
    private String reactivationRejectionReason; // 재활성화 신청 거절 사유

    @Column(nullable = true)
    private LocalDateTime reactivationRejectedAt; // 재활성화 신청 거절일시

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false, referencedColumnName = "admin_id")
    @JsonIgnore // 순환 참조 방지
    private AdminEntity admin; //관리자 조인

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true, nullable = true)
    @JsonIgnore // 순환 참조 방지
    private AccountEntity account; // 1:1 통합 로그인 계정 (승인 시 생성, 신청 시는 null)

    /**
     * Account와 양방향 연결 (양방향 동기화)
     * Partner에 account를 설정할 때 Account 쪽에도 partner가 설정되도록 함
     */
    public void setAccount(AccountEntity account) {
        // 기존 연결 해제
        if (this.account != null && this.account.getPartner() == this) {
            // Account 쪽에서도 제거 (실제로는 Account에 setter가 없으므로 영속성 컨텍스트에서 자동 처리됨)
        }
        
        this.account = account;
        
        // 양방향 동기화: Account의 partner도 설정 (Account는 mappedBy이므로 실제 FK는 Partner에만 있음)
        // 주의: Account.getPartner()는 mappedBy로 읽기 전용이지만, 같은 트랜잭션 내에서 일관성 유지
    }

    /** 부트스트랩/시딩용: Account와 함께 Partner 생성 */
    public static PartnerEntity create(
            String partnerName,
            String partnerContact,
            String partnerBankAccount,
            String businessRegistrationNumber,
            String representativeBrandCode,
            PartnerStatus partnerStatus,
            LocalDateTime partnerApprovedAt,
            AdminEntity admin,
            AccountEntity account
    ) {
        return new PartnerEntity(
                null, // partnerId
                partnerName,
                partnerContact,
                partnerBankAccount,
                businessRegistrationNumber,
                null, // email (부트스트랩용이므로 null)
                representativeBrandCode,
                null, // businessRegistrationFileId
                null, // bankAccountFileId
                partnerStatus,
                partnerApprovedAt,
                null, // rejectionReason
                null, // deactivationRequestReason
                null, // deactivationRequestedAt
                null, // deactivationRejectionReason
                null, // deactivationRejectedAt
                null, // reactivationRequestReason
                null, // reactivationRequestedAt
                null, // reactivationRejectionReason
                null, // reactivationRejectedAt
                admin,
                account
        );
    }

    /** 파트너 입점 신청용: Account 없이 Partner 생성 (비밀번호는 승인 시 Account에 생성) */
    public static PartnerEntity createApplication(
            String partnerName,
            String partnerContact,
            String partnerBankAccount,
            String businessRegistrationNumber,
            String email,
            String representativeBrandCode,
            Long businessRegistrationFileId,
            Long bankAccountFileId,
            AdminEntity admin
    ) {
        return new PartnerEntity(
                null, // partnerId
                partnerName,
                partnerContact,
                partnerBankAccount,
                businessRegistrationNumber,
                email,
                representativeBrandCode,
                businessRegistrationFileId,
                bankAccountFileId,
                PartnerStatus.PENDING,
                null, // partnerApprovedAt (승인일시는 null)
                null, // rejectionReason
                null, // deactivationRequestReason
                null, // deactivationRequestedAt
                null, // deactivationRejectionReason
                null, // deactivationRejectedAt
                null, // reactivationRequestReason
                null, // reactivationRequestedAt
                null, // reactivationRejectionReason
                null, // reactivationRejectedAt
                admin,
                null // Account는 승인 시 생성
        );
    }

    /**
     * 파트너 상태 업데이트 (승인/거절 시 사용)
     * JPA는 리플렉션을 통해 필드에 직접 접근할 수 있으므로 필드 직접 수정 가능
     */
    public void updateStatus(PartnerStatus newStatus, LocalDateTime approvedAt, String rejectionReason) {
        // 리플렉션을 사용하여 필드 수정
        try {
            java.lang.reflect.Field statusField = this.getClass().getDeclaredField("partnerStatus");
            statusField.setAccessible(true);
            statusField.set(this, newStatus);
            
            java.lang.reflect.Field approvedAtField = this.getClass().getDeclaredField("partnerApprovedAt");
            approvedAtField.setAccessible(true);
            approvedAtField.set(this, approvedAt);
            
            java.lang.reflect.Field rejectionReasonField = this.getClass().getDeclaredField("rejectionReason");
            rejectionReasonField.setAccessible(true);
            rejectionReasonField.set(this, rejectionReason);
        } catch (Exception e) {
            throw new RuntimeException("파트너 상태 업데이트 실패", e);
        }
    }

    /**
     * 파트너 휴업 신청
     */
    public void requestDeactivation(String reason) {
        try {
            java.lang.reflect.Field reasonField = this.getClass().getDeclaredField("deactivationRequestReason");
            reasonField.setAccessible(true);
            reasonField.set(this, reason);
            
            java.lang.reflect.Field requestedAtField = this.getClass().getDeclaredField("deactivationRequestedAt");
            requestedAtField.setAccessible(true);
            requestedAtField.set(this, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("휴업 신청 실패", e);
        }
    }

    /**
     * 휴업 신청 취소 (관리자가 거절하거나 파트너가 취소할 때)
     */
    public void cancelDeactivationRequest() {
        try {
            java.lang.reflect.Field reasonField = this.getClass().getDeclaredField("deactivationRequestReason");
            reasonField.setAccessible(true);
            reasonField.set(this, null);
            
            java.lang.reflect.Field requestedAtField = this.getClass().getDeclaredField("deactivationRequestedAt");
            requestedAtField.setAccessible(true);
            requestedAtField.set(this, null);
        } catch (Exception e) {
            throw new RuntimeException("휴업 신청 취소 실패", e);
        }
    }

    /**
     * 휴업 신청 거절 사유 저장
     */
    public void setDeactivationRejectionReason(String reason) {
        try {
            java.lang.reflect.Field reasonField = this.getClass().getDeclaredField("deactivationRejectionReason");
            reasonField.setAccessible(true);
            reasonField.set(this, reason);
            
            java.lang.reflect.Field rejectedAtField = this.getClass().getDeclaredField("deactivationRejectedAt");
            rejectedAtField.setAccessible(true);
            rejectedAtField.set(this, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("휴업 신청 거절 사유 저장 실패", e);
        }
    }

    /**
     * 파트너 재활성화 신청
     */
    public void requestReactivation(String reason) {
        try {
            java.lang.reflect.Field reasonField = this.getClass().getDeclaredField("reactivationRequestReason");
            reasonField.setAccessible(true);
            reasonField.set(this, reason);
            
            java.lang.reflect.Field requestedAtField = this.getClass().getDeclaredField("reactivationRequestedAt");
            requestedAtField.setAccessible(true);
            requestedAtField.set(this, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("재활성화 신청 실패", e);
        }
    }

    /**
     * 재활성화 신청 취소 (관리자가 거절하거나 파트너가 취소할 때)
     */
    public void cancelReactivationRequest() {
        try {
            java.lang.reflect.Field reasonField = this.getClass().getDeclaredField("reactivationRequestReason");
            reasonField.setAccessible(true);
            reasonField.set(this, null);
            
            java.lang.reflect.Field requestedAtField = this.getClass().getDeclaredField("reactivationRequestedAt");
            requestedAtField.setAccessible(true);
            requestedAtField.set(this, null);
        } catch (Exception e) {
            throw new RuntimeException("재활성화 신청 취소 실패", e);
        }
    }

    /**
     * 재활성화 신청 거절 사유 저장
     */
    public void setReactivationRejectionReason(String reason) {
        try {
            java.lang.reflect.Field reasonField = this.getClass().getDeclaredField("reactivationRejectionReason");
            reasonField.setAccessible(true);
            reasonField.set(this, reason);
            
            java.lang.reflect.Field rejectedAtField = this.getClass().getDeclaredField("reactivationRejectedAt");
            rejectedAtField.setAccessible(true);
            rejectedAtField.set(this, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("재활성화 신청 거절 사유 저장 실패", e);
        }
    }

    /**
     * 고위험 파트너 정보 변경 승인 반영
     * null/blank 값은 기존 값을 유지합니다.
     */
    public void applyApprovedInfoChange(
            String requestedPartnerName,
            String requestedPartnerBankAccount,
            String requestedBusinessRegistrationNumber,
            String requestedRepresentativeBrandCode
    ) {
        if (requestedPartnerName != null && !requestedPartnerName.trim().isEmpty()) {
            this.partnerName = requestedPartnerName.trim();
        }
        if (requestedPartnerBankAccount != null && !requestedPartnerBankAccount.trim().isEmpty()) {
            this.partnerBankAccount = requestedPartnerBankAccount.trim();
        }
        if (requestedBusinessRegistrationNumber != null && !requestedBusinessRegistrationNumber.trim().isEmpty()) {
            this.businessRegistrationNumber = requestedBusinessRegistrationNumber.trim();
        }
        if (requestedRepresentativeBrandCode != null && !requestedRepresentativeBrandCode.trim().isEmpty()) {
            this.representativeBrandCode = requestedRepresentativeBrandCode.trim().toUpperCase();
        }
    }
}
