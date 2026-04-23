package com.swimshop.swim_mall.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class CustomerResponseDto {

    private Long customerId; // 고객 고유식별자
    private String customerName; // 고객 성함
    private String customerEmail; // 고객 이메일
    private LocalDate customerBirth; //생년월일
    private LocalDateTime customerCreateAt; // 생성일시

    public CustomerResponseDto(Long customerId, String customerName, String customerEmail, 
                               LocalDate customerBirth, LocalDateTime customerCreateAt) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerBirth = customerBirth;
        this.customerCreateAt = customerCreateAt;
    }
}

