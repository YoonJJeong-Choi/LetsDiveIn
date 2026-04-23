package com.swimshop.swim_mall.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 메모 작성/수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNoteRequestDto {
    
    @NotBlank(message = "메모 내용을 입력해주세요.")
    private String noteContent; // 메모 내용
    
    private Boolean isImportant; // 중요 여부 (선택사항, 기본값: false)
}
