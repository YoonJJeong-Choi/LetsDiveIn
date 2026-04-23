package com.swimshop.swim_mall.customer.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.CustomerHistoryActionType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 관리 작업 이력
 * 관리자가 고객 정보를 수정하거나 계정 상태를 변경할 때 기록
 */
@Entity
@Table(name = "customer_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    /**
     * 작업 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CustomerHistoryActionType actionType;

    /**
     * 변경 전 값 (JSON 형태로 저장)
     * 예: {"customerName": "홍길동", "customerEmail": "old@email.com", "accountStatus": "ACTIVE"}
     */
    @Column(nullable = true, length = 2000)
    private String oldValue;

    /**
     * 변경 후 값 (JSON 형태로 저장)
     * 예: {"customerName": "홍길동", "customerEmail": "new@email.com", "accountStatus": "ACTIVE"}
     */
    @Column(nullable = true, length = 2000)
    private String newValue;

    /**
     * 작업 사유 (선택)
     */
    @Column(nullable = true, length = 1000)
    private String reason;

    /**
     * 작업한 관리자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminEntity admin;

    /**
     * 작업 일시
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 고객 정보 수정 이력 생성
     */
    public static CustomerHistoryEntity createInfoUpdate(
            CustomerEntity customer,
            AdminEntity admin,
            String oldValue,
            String newValue,
            String reason
    ) {
        return CustomerHistoryEntity.builder()
                .customer(customer)
                .actionType(CustomerHistoryActionType.INFO_UPDATE)
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 계정 활성화 이력 생성
     */
    public static CustomerHistoryEntity createStatusActivate(
            CustomerEntity customer,
            AdminEntity admin,
            String reason
    ) {
        return CustomerHistoryEntity.builder()
                .customer(customer)
                .actionType(CustomerHistoryActionType.STATUS_ACTIVATE)
                .oldValue("{\"accountStatus\": \"INACTIVE\"}")
                .newValue("{\"accountStatus\": \"ACTIVE\"}")
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 계정 비활성화 이력 생성
     */
    public static CustomerHistoryEntity createStatusDeactivate(
            CustomerEntity customer,
            AdminEntity admin,
            String reason
    ) {
        return CustomerHistoryEntity.builder()
                .customer(customer)
                .actionType(CustomerHistoryActionType.STATUS_DEACTIVATE)
                .oldValue("{\"accountStatus\": \"ACTIVE\"}")
                .newValue("{\"accountStatus\": \"INACTIVE\"}")
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 비밀번호 초기화 이력 생성
     */
    public static CustomerHistoryEntity createPasswordReset(
            CustomerEntity customer,
            AdminEntity admin,
            String reason
    ) {
        return CustomerHistoryEntity.builder()
                .customer(customer)
                .actionType(CustomerHistoryActionType.PASSWORD_RESET)
                .oldValue(null)
                .newValue("{\"password\": \"초기화됨\"}")
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 이메일 인증 재발송 이력 생성
     */
    public static CustomerHistoryEntity createEmailVerificationResend(
            CustomerEntity customer,
            AdminEntity admin,
            String reason
    ) {
        return CustomerHistoryEntity.builder()
                .customer(customer)
                .actionType(CustomerHistoryActionType.EMAIL_VERIFICATION_RESEND)
                .oldValue(null)
                .newValue("{\"emailVerification\": \"재발송됨\"}")
                .reason(reason)
                .admin(admin)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
