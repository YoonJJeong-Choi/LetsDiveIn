package com.swimshop.swim_mall.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 고객 등급 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerGradeDto {
    private Long gradeId; // 등급 고유식별자
    private String gradeName; // 등급명
    private Integer gradeLevel; // 등급 순서
    private Long minPurchaseAmount; // 최소 누적 구매액
    private Integer minOrderCount; // 최소 주문 건수
    private Double discountRate; // 할인율 (%)
    private Double pointAccumulationRate; // 포인트 적립률 (%)
    private Boolean isActive; // 활성화 여부
    private LocalDateTime createdAt; // 생성일시
    private LocalDateTime updatedAt; // 수정일시
}
