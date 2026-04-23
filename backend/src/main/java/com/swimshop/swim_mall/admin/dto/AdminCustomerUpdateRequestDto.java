package com.swimshop.swim_mall.admin.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자용 고객 정보 수정 요청 DTO
 * 부분 업데이트를 지원합니다. null이 아닌 필드만 업데이트됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCustomerUpdateRequestDto {
    
    private String customerName; // 고객 이름 (null이면 업데이트하지 않음)
    
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String customerEmail; // 이메일 (null이면 업데이트하지 않음, Account 엔티티도 함께 업데이트)
    
    private LocalDate customerBirth; // 생년월일 (null이면 업데이트하지 않음)
    
    private Boolean active; // 계정 활성화 상태 (null이면 업데이트하지 않음, true면 활성화, false면 비활성화)
}
