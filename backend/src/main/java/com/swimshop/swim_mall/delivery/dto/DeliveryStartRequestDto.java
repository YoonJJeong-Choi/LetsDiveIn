package com.swimshop.swim_mall.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배송 시작 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStartRequestDto {
    
    @NotBlank(message = "송장번호는 필수입니다")
    private String trackingNumber; // 배송 송장번호
    
    @NotBlank(message = "택배사는 필수입니다")
    private String courier; // 배송 택배사
}
