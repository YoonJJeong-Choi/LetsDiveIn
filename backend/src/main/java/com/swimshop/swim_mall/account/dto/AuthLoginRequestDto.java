package com.swimshop.swim_mall.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AuthLoginRequestDto {

    private String email;
    private String password;
}
