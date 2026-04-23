package com.swimshop.swim_mall.event.performance.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPerformanceListItemDto {
    private Long eventNo;
    private String eventTitle;
    private String eventStatus;
    private LocalDateTime customerEventStartAt;
    private LocalDateTime customerEventEndAt;

    private Long totalOrders;
    private Long totalOrderItems;
    private Long totalNetAmount;
    private Long adminRewardPoint;
    private Long partnerRewardPoint;
    private Double rewardToNetRatio;
    // 실적 기준 유형: ADMIN_ONLY | PARTNER_PARTICIPATION
    private String payoutType;
}

