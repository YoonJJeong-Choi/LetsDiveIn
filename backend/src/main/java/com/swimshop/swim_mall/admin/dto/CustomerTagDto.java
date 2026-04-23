package com.swimshop.swim_mall.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 태그 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTagDto {
    
    private Long tagId; // 태그 ID
    private String tagName; // 태그 이름
    private String tagColor; // 태그 색상
    private String description; // 태그 설명
    private Long customerCount; // 해당 태그를 가진 고객 수 (선택사항)
}
