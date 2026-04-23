package com.swimshop.swim_mall.partner.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerProfileUpdateRequestDto {
    private String contactPersonName;
    private String customerServicePhone;
    private String profileImageUrl;
    private String introduction;
}
