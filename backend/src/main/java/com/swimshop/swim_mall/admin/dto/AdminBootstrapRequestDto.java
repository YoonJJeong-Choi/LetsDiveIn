package com.swimshop.swim_mall.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminBootstrapRequestDto {

    private String email;      // 로그인 이메일 (Account에 저장)
    private String password;   // 로그인 비밀번호
    private String adminName;  // 관리자명
}
