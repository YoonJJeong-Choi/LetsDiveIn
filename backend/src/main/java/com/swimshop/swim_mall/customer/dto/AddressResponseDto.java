package com.swimshop.swim_mall.customer.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDto {

    private Long addressNo; // 주소 고유식별자
    private String recipientName; // 수령인 이름
    private String recipientPhone; // 수령인 전화번호
    private String deliveryAddress; // 배송지 주소
    private String deliveryAddressDetail; // 배송지 상세 주소
    private String deliveryZipCode; // 우편번호
    private Boolean isDefault; // 기본 주소 여부
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시
}
