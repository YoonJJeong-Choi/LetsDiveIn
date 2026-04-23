package com.swimshop.swim_mall.customer.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.CustomerActivityType;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 활동 로그 엔티티
 * 고객의 주요 활동을 기록하는 용도
 */
@Entity
@Table(name = "customer_activity_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    /**
     * 활동 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CustomerActivityType activityType;

    /**
     * 활동 일시
     */
    @Column(nullable = false)
    private LocalDateTime activityAt;

    /**
     * 활동 상세 정보 (JSON 형태로 저장)
     * 예: {"orderNo": 123, "orderAmount": 50000}
     */
    @Column(columnDefinition = "TEXT")
    private String activityDetails;

    /**
     * IP 주소
     */
    @Column(length = 50)
    private String ipAddress;

    /**
     * User-Agent
     */
    @Column(length = 500)
    private String userAgent;

    /**
     * 활동 로그 생성
     */
    public static CustomerActivityLogEntity create(
            CustomerEntity customer,
            CustomerActivityType activityType,
            String activityDetails,
            String ipAddress,
            String userAgent
    ) {
        return CustomerActivityLogEntity.builder()
                .customer(customer)
                .activityType(activityType)
                .activityAt(LocalDateTime.now())
                .activityDetails(activityDetails)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }
}
