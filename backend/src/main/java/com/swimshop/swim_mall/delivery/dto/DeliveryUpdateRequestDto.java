package com.swimshop.swim_mall.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배송 정보 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryUpdateRequestDto {
    
    private String trackingNumber; // 배송 송장번호
    private String courier; // 배송 택배사
}
