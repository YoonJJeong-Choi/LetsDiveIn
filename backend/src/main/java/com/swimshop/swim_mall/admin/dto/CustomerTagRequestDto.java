package com.swimshop.swim_mall.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 태그 생성/수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTagRequestDto {
    
    @NotBlank(message = "태그 이름을 입력해주세요.")
    private String tagName; // 태그 이름
    
    private String tagColor; // 태그 색상 (선택사항, 기본값: #1890ff)
    
    private String description; // 태그 설명 (선택사항)
}
