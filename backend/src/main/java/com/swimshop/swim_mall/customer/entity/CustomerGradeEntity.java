package com.swimshop.swim_mall.customer.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.CustomerGradeEnum;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 등급 엔티티
 * 등급별 혜택(할인율, 포인트 적립률)을 관리
 * 등급명은 CustomerGradeEnum으로 고정되어 있으며, 생성/삭제가 불가능하고 수정만 가능합니다.
 */
@Entity
@Table(name = "customer_grade")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerGradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gradeId; // 등급 고유식별자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private CustomerGradeEnum gradeName; // 등급명 (Enum으로 고정)

    @Column(nullable = false)
    private Integer gradeLevel; // 등급 순서 (1, 2, 3, 4... 숫자가 클수록 높은 등급)

    @Column(nullable = false)
    private Long minPurchaseAmount; // 최소 누적 구매액 (이 금액 이상이어야 해당 등급)

    @Column(nullable = false)
    @Builder.Default
    private Integer minOrderCount = 0; // 최소 주문 건수 (기본값: 0)

    @Column(nullable = false)
    @Builder.Default
    private Double discountRate = 0.0; // 등급별 할인율 (%, 기본값: 0)

    @Column(nullable = false)
    @Builder.Default
    private Double pointAccumulationRate = 0.0; // 포인트 적립률 (%, 기본값: 0)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // 활성화 여부

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성일시

    @Column(nullable = true)
    private LocalDateTime updatedAt; // 수정일시

    /**
     * 등급 정보 업데이트
     * 등급명(gradeName)은 Enum으로 고정되어 있으므로 변경 불가
     */
    public void update(
            Long minPurchaseAmount,
            Integer minOrderCount,
            Double discountRate,
            Double pointAccumulationRate,
            Boolean isActive
    ) {
        this.minPurchaseAmount = minPurchaseAmount;
        this.minOrderCount = minOrderCount;
        this.discountRate = discountRate;
        this.pointAccumulationRate = pointAccumulationRate;
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 등급 비활성화
     */
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 등급 활성화
     */
    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }
}
