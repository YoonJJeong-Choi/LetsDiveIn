package com.swimshop.swim_mall.account.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 통합 로그인용 계정.
 * 이메일+비밀번호+역할을 갖고, Customer / Partner / Admin 중 정확히 하나만 연결된다.
 * - Customer: 회원가입·이메일 인증 후 Account 생성
 * - Partner / Admin: 생성 시점에 Account 생성 (이메일/비밀번호는 Account에만 저장)
 */
@Entity
@Table(
    name = "account",
    uniqueConstraints = @UniqueConstraint(columnNames = "email")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole role;

    /** 이메일 인증 완료 여부 (CUSTOMER 회원가입 플로우용) */
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(length = 100)
    private String emailVerifyToken;

    private LocalDateTime emailVerifyExpiresAt;

    /** 
     * PARTNER/ADMIN 상태 (승인·정지 등). CUSTOMER는 사용 안 함
     * 주의: "운영 상태(승인/정지)"는 Partner/Admin 엔티티의 status 필드를 우선 사용
     * Account.status는 "로그인 가능 여부(잠금)" 같은 계정 레벨 상태용
     */
    @Column(length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 1:1 역방향 — FK는 Customer/Partner/Admin 쪽(account_id)에 있음 */
    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
    @JsonIgnore // 순환 참조 방지
    private CustomerEntity customer;

    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
    @JsonIgnore // 순환 참조 방지
    private PartnerEntity partner;

    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY)
    @JsonIgnore // 순환 참조 방지
    private AdminEntity admin;

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEmailVerified(boolean verified) {
        this.emailVerified = verified;
        if (verified) {
            this.emailVerifyToken = null;
            this.emailVerifyExpiresAt = null;
        }
    }

    public void setEmailVerifyToken(String token, LocalDateTime expiresAt) {
        this.emailVerifyToken = token;
        this.emailVerifyExpiresAt = expiresAt;
    }
    
    public void setEmailVerifyExpiresAt(LocalDateTime expiresAt) {
        this.emailVerifyExpiresAt = expiresAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 엔티티 저장 전 createdAt 자동 설정
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
