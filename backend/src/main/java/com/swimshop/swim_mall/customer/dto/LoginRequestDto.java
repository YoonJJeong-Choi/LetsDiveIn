package com.swimshop.swim_mall.customer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequestDto {

    private String customerEmail; // 고객 이메일
    private String customerPassword; // 비밀번호
}
