package com.swimshop.swim_mall.event.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTopItemDto {
    private String id;
    private String name;
    private Long orders;
    private Long items;
    private Long netAmount;
    private Long reward;
}

