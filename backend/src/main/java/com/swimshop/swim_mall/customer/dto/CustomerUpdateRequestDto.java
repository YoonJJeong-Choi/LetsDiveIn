package com.swimshop.swim_mall.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomerUpdateRequestDto {

    @NotBlank(message = "이름은 필수입니다.")
    private String customerName; // 고객 성함

    // 이메일은 수정 불가 (보안상 계정 식별자 역할)
    // 이메일 변경이 필요한 경우 계정 재생성 또는 관리자 개입 필요

    // 생년월일은 수정 불가 (결제 시스템과 연관된 법적 요구사항 및 데이터 무결성 유지)
    // 생년월일 변경이 필요한 경우 관리자 개입 필요
}
