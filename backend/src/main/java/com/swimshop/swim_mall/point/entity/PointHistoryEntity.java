package com.swimshop.swim_mall.point.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.PointType;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PointHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType pointType; // ACCUMULATE(적립), USE(사용), MANUAL_ADD(수동 지급), MANUAL_DEDUCT(수동 차감), EXPIRE(만료)

    @Column(nullable = false)
    private Long pointAmount; // 포인트 금액 (적립: +, 사용: -)

    @Column(nullable = false)
    private Long pointBalanceAfter; // 거래 후 포인트 잔액

    @Column(nullable = true)
    private Long orderItemNo; // 관련 주문 상품 (적립 시)

    @Column(nullable = true)
    private Long orderNo; // 관련 주문 (사용 시)

    @Column(nullable = true, length = 500)
    private String description; // 설명 (예: "구매 확정 적립", "관리자 수동 지급")

    @Column(nullable = true)
    private LocalDate expireDate; // 만료일 (적립 시)

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성일시

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = true)
    private AdminEntity admin; // 관리자 (수동 지급/차감 시)
}
