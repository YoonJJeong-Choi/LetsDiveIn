package com.swimshop.swim_mall.account.dto;

import com.swimshop.swim_mall.common.enums.AccountRole;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthLoginResponseDto {

    private AccountRole role;
    private Long subjectId;   // customerId / partnerId / adminId
    private String email;
    private String name;

    public static AuthLoginResponseDto of(AccountRole role, Long subjectId, String email, String name) {
        return new AuthLoginResponseDto(role, subjectId, email, name);
    }
}
