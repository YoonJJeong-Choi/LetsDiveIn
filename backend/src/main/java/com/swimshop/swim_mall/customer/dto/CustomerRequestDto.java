package com.swimshop.swim_mall.customer.dto;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomerRequestDto {

    private String customerName; // 고객 성함
    private String customerEmail; // 고객 이메일
    private String customerPassword; // 비밀번호
    private LocalDate customerBirth; //생년월일
}
