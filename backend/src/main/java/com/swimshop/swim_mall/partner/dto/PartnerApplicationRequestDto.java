package com.swimshop.swim_mall.partner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파트너 입점 신청 요청 DTO
 */
@Getter
@NoArgsConstructor
public class PartnerApplicationRequestDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "파트너명은 필수입니다.")
    private String partnerName;

    @NotBlank(message = "연락처는 필수입니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "연락처 형식은 010-XXXX-XXXX입니다.")
    private String partnerContact;

    @NotBlank(message = "정산계좌는 필수입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{3}-\\d{6,}$", message = "정산계좌 형식은 XXX-XXX-XXXXXX입니다.")
    private String partnerBankAccount;

    @NotBlank(message = "사업자등록번호는 필수입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호 형식은 XXX-XX-XXXXX입니다.")
    private String businessRegistrationNumber; // 사업자등록번호 (예: 123-45-67890)

    @NotBlank(message = "대표 브랜드 코드는 필수입니다.")
    private String representativeBrandCode; // 대표 취급 브랜드 코드 (예: SPEEDO)

    // 업로드된 서류 fileId (필수 2종)
    @jakarta.validation.constraints.NotNull(message = "사업자등록증 사본 파일이 필요합니다.")
    private Long businessRegistrationFileId;

    @jakarta.validation.constraints.NotNull(message = "통장 사본 파일이 필요합니다.")
    private Long bankAccountFileId;
}
