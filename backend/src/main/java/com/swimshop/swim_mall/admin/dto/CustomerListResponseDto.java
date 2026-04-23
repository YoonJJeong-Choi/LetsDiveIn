package com.swimshop.swim_mall.admin.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.swimshop.swim_mall.common.enums.CustomerGradeEnum;

/**
 * 관리자용 고객 목록 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerListResponseDto {
    
    private List<CustomerListItemDto> customers;
    private Long totalCount;
    private Integer page;
    private Integer pageSize;
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerListItemDto {
        private Long customerId;
        private String customerName;
        private String customerEmail;
        private LocalDate customerBirth;
        private LocalDateTime customerCreateAt;
        private Boolean emailChecked; // 이메일 인증 여부
        private String accountStatus; // 계정 상태 (ACTIVE, INACTIVE) - 엔티티 일치 이후 enum 전환 예정
        private Long orderCount; // 주문 건수
        private Long totalOrderAmount; // 총 주문 금액
        private Long reviewCount; // 리뷰 건수
        private Long returnCount; // 반품 건수
        private CustomerGradeEnum customerGradeCode; // 고객 등급 코드 (BEGINNER, SWIMMER, PRO, MASTER, LEGEND)
        private List<CustomerTagDto> tags; // 고객 태그 목록
    }
}
