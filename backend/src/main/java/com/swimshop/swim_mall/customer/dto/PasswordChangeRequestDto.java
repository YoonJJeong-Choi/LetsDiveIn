package com.swimshop.swim_mall.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordChangeRequestDto {

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword; // 현재 비밀번호

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String newPassword; // 새 비밀번호

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String confirmPassword; // 비밀번호 확인
}
