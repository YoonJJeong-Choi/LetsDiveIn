package com.swimshop.swim_mall.event.performance.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTimeseriesPointDto {
    private LocalDate date;
    private Long orders;
    private Long items;
    private Long netAmount;
    private Long adminReward;
    private Long partnerReward;
}

