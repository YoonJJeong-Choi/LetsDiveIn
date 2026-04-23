package com.swimshop.swim_mall.sale.dto;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.SaleScope;
import com.swimshop.swim_mall.common.enums.SaleStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SalePolicyResponseDto {
    private Long id;
    private String campaignId;
    private Long eventNo;
    private EventType eventType;
    private String eventTitle;
    private SaleScope scope;
    private Long targetProductNo;
    private Long targetOptionNo;
    private DiscountType discountType;
    private Long discountValue;
    private Long maxDiscountAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private SaleStatus status;
    private String rejectionReason;
    private Long createdByPartnerId;
    private String createdByPartnerName;
    private Long adminNo;
    private String adminName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

