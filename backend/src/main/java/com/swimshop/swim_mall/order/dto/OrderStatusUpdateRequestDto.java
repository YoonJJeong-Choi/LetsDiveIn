package com.swimshop.swim_mall.order.dto;

import com.swimshop.swim_mall.common.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상태 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequestDto {
    private OrderStatus orderStatus; // 변경할 주문 상태
}
