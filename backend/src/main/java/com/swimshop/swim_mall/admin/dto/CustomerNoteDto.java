package com.swimshop.swim_mall.admin.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 메모 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNoteDto {
    
    private Long noteId; // 메모 ID
    private Long customerId; // 고객 ID
    private String noteContent; // 메모 내용
    private Boolean isImportant; // 중요 여부
    private Long adminId; // 작성한 관리자 ID
    private String adminName; // 작성한 관리자 이름
    private LocalDateTime createdAt; // 작성 일시
    private LocalDateTime updatedAt; // 수정 일시
}
