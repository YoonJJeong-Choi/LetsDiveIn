package com.swimshop.swim_mall.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddressRequestDto {

    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String recipientName; // 수령인 이름

    @NotBlank(message = "수령인 전화번호는 필수입니다.")
    private String recipientPhone; // 수령인 전화번호

    @NotBlank(message = "배송지 주소는 필수입니다.")
    private String deliveryAddress; // 배송지 주소

    private String deliveryAddressDetail; // 배송지 상세 주소 (선택)

    private String deliveryZipCode; // 우편번호 (선택)

    private Boolean isDefault = false; // 기본 주소 여부
}
