package com.swimshop.swim_mall.customer.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.customer.dto.CustomerRequestDto;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="customer")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId; // 고객 고유식별자

    @Column(nullable = false)
    private String customerName; // 고객 성함

    @Column(nullable = false)
    private String customerEmail; // 고객 이메일

    @Column(nullable = false)
    private String customerPassword; // 비밀번호

    @Column(nullable = false)
    private LocalDate customerBirth; //생년월일

    @Column(nullable = false)
    private LocalDateTime customerCreateAt; // 생성일시

    @Column(length = 100)
    private String emailCheckToken; // 이메일 인증 토큰

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailChecked = false; // 이메일 인증 여부

    private LocalDateTime tokenExpiryAt; // 토큰 만료 시간

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", unique = true, nullable = false)
    private AccountEntity account; // 1:1 통합 로그인 계정 (필수)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id", nullable = true)
    private CustomerGradeEntity customerGrade; // 고객 등급

    @Column(nullable = false)
    @Builder.Default
    private Long totalPurchaseAmount = 0L; // 누적 구매액 (등급 산정용)

    @Column(nullable = false)
    @Builder.Default
    private Integer totalOrderCount = 0; // 총 주문 건수 (등급 산정용)

    @Column(nullable = false)
    @Builder.Default
    private Long pointBalance = 0L; // 포인트 잔액

    // Entity 생성 팩토리 메서드 (생성일시 자동 설정)
    public static CustomerEntity fromDto(CustomerRequestDto requestDto) {
        return CustomerEntity.builder()
                .customerName(requestDto.getCustomerName())
                .customerEmail(requestDto.getCustomerEmail())
                .customerPassword(requestDto.getCustomerPassword())
                .customerBirth(requestDto.getCustomerBirth())
                .customerCreateAt(LocalDateTime.now())
                .emailChecked(false)
                .build();
    }

    // 이메일 인증 토큰 설정 메서드
    public void setEmailAuthToken(String emailCheckToken, LocalDateTime tokenExpiryAt) {
        this.emailCheckToken = emailCheckToken;
        this.tokenExpiryAt = tokenExpiryAt;
    }

    // 이메일 인증 완료 메서드
    public void checkEmailCompleted() {
        this.emailChecked = true;
        this.emailCheckToken = null;
        this.tokenExpiryAt = null;
    }

    // 평문 -> 암호화 비밀번호 설정 메서드
    public void setCustomerPassword(String customerPassword) {
        this.customerPassword = customerPassword;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
    }

    /**
     * 프로필 정보 업데이트 메서드 (고객용)
     * 주의: 이메일과 생년월일은 수정 불가
     * - 이메일: 보안상 계정 식별자 역할
     * - 생년월일: 결제 시스템과 연관된 법적 요구사항 및 데이터 무결성 유지
     */
    public void updateProfile(String customerName) {
        this.customerName = customerName;
    }
    
    /**
     * 관리자용 고객 정보 수정 메서드
     * - 이름과 이메일 수정 가능
     * - Account 엔티티의 이메일도 함께 업데이트 필요
     */
    public void updateByAdmin(String customerName, String customerEmail) {
        this.customerName = customerName;
        this.customerEmail = customerEmail;
    }
    
    /**
     * 이메일 업데이트 (Account와 동기화용)
     */
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    /**
     * 이름 업데이트
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    /**
     * 생년월일 업데이트 (관리자용)
     */
    public void setCustomerBirth(LocalDate customerBirth) {
        this.customerBirth = customerBirth;
    }

    /**
     * 등급 설정
     */
    public void setCustomerGrade(CustomerGradeEntity customerGrade) {
        this.customerGrade = customerGrade;
    }

    /**
     * 누적 구매액 업데이트
     */
    public void updateTotalPurchaseAmount(Long amount) {
        this.totalPurchaseAmount = amount != null ? amount : 0L;
    }

    /**
     * 총 주문 건수 업데이트
     */
    public void updateTotalOrderCount(Integer count) {
        this.totalOrderCount = count != null ? count : 0;
    }

    /**
     * 누적 구매액 증가
     */
    public void addPurchaseAmount(Long amount) {
        this.totalPurchaseAmount += amount;
    }

    /**
     * 주문 건수 증가
     */
    public void incrementOrderCount() {
        this.totalOrderCount++;
    }

    /**
     * 포인트 추가
     */
    public void addPoint(Long amount) {
        this.pointBalance += amount;
    }

    /**
     * 포인트 차감
     */
    public void deductPoint(Long amount) {
        if (this.pointBalance < amount) {
            throw new IllegalStateException("포인트 잔액이 부족합니다.");
        }
        this.pointBalance -= amount;
    }
}
