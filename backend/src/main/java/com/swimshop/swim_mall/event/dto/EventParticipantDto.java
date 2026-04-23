package com.swimshop.swim_mall.event.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipantDto {
    private Long partnerId;
    private String partnerName;
    private String partnerContact;
    private Boolean active;
    private LocalDateTime participatedAt;
    private Integer linkedSalePolicyCount;
    private String linkedSaleTargets;
}

