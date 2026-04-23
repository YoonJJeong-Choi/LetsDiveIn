package com.swimshop.swim_mall.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {

    private Long customerId; // 고객 고유식별자
    private String customerEmail; // 고객 이메일
    private String customerName; // 고객 성함
    private String message; // 응답 메시지

    public static LoginResponseDto of(Long customerId, String customerEmail, String customerName) {
        return new LoginResponseDto(customerId, customerEmail, customerName, "로그인 성공");
    }
}
