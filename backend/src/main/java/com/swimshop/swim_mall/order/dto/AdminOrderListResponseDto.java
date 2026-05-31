package com.swimshop.swim_mall.order.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminOrderListResponseDto {

    private final List<OrderResponseDto> items;
    private final long total;
    private final int page;
    private final int size;
    private final Map<String, Long> statusCounts;
}
