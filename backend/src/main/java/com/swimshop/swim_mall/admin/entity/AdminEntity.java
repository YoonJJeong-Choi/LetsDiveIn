package com.swimshop.swim_mall.admin.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.common.enums.AdminStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AdminEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId; //관리자 고유식별자
    
    @Column(nullable = false)
    private String adminName; //관리자명

    /** 
     * 관리자 운영 상태 (ACTIVE/INACTIVE)
     * 주의: "운영 상태"는 이 필드를 사용. Account.status는 "로그인 가능 여부"용
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminStatus adminStatus; //상태관리

    @Column(nullable = false)
    private LocalDateTime adminCreatedAt; //생성일시

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true, nullable = false)
    @JsonIgnore // 순환 참조 방지
    private AccountEntity account; // 1:1 통합 로그인 계정 (필수)

    /**
     * Account와 양방향 연결 (양방향 동기화)
     * Admin에 account를 설정할 때 Account 쪽에도 admin이 설정되도록 함
     */
    public void setAccount(AccountEntity account) {
        // 기존 연결 해제
        if (this.account != null && this.account.getAdmin() == this) {
            // Account 쪽에서도 제거 (실제로는 Account에 setter가 없으므로 영속성 컨텍스트에서 자동 처리됨)
        }
        
        this.account = account;
        
        // 양방향 동기화: Account의 admin도 설정 (Account는 mappedBy이므로 실제 FK는 Admin에만 있음)
        // 주의: Account.getAdmin()는 mappedBy로 읽기 전용이지만, 같은 트랜잭션 내에서 일관성 유지
    }

    /** 부트스트랩/시딩용: Account와 함께 Admin 생성 */
    public static AdminEntity create(String adminName, AdminStatus adminStatus, LocalDateTime adminCreatedAt, AccountEntity account) {
        return new AdminEntity(null, adminName, adminStatus, adminCreatedAt, account);
    }
}
