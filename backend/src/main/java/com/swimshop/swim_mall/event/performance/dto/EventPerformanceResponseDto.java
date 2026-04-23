package com.swimshop.swim_mall.event.performance.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPerformanceResponseDto {
    private Long eventNo;
    private LocalDateTime fromAt;
    private LocalDateTime toAt;

    private Long totalOrders;         // distinct order count
    private Long totalOrderItems;     // item count
    private Long totalNetAmount;      // sum of itemTotalPrice

    private Long adminRewardPoint;    // sum of admin_event_reward.pointAmount
    private Long partnerRewardPoint;  // sum of partner_event_reward.pointAmount

    private Double rewardToNetRatio;  // (admin+partner)/net 또는 파트너 단독 시 partner/net

    // 실적 기준 유형: ADMIN_ONLY | PARTNER_PARTICIPATION
    private String payoutType;

    private List<Long> sampleOrderItemNos; // optional: debug/sample
}

