package com.swimshop.swim_mall.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 기간별 매출 비교 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodComparisonDto {
    private Long currentPeriodSales; // 현재 기간 매출
    private Long previousPeriodSales; // 이전 기간 매출
    private Long salesChange; // 매출 변화액 (현재 - 이전)
    private Double salesChangeRate; // 매출 변화율 (%)
    private Long currentPeriodOrders; // 현재 기간 주문 수
    private Long previousPeriodOrders; // 이전 기간 주문 수
    private Long orderChange; // 주문 수 변화
    private Double orderChangeRate; // 주문 수 변화율 (%)
}
