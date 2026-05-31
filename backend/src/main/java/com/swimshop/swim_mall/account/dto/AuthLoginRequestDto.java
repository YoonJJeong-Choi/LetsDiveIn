package com.swimshop.swim_mall.account.dto;

import com.swimshop.swim_mall.common.enums.AuthPortal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthLoginRequestDto {

    private String email;
    private String password;
    private AuthPortal portal;
}
