package com.swimshop.swim_mall.partner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PartnerProfileResponseDto {
    private Long partnerId;
    private String contactPersonName;
    private String customerServicePhone;
    private String profileImageUrl;
    private String introduction;
    private LocalDateTime updatedAt;
}
