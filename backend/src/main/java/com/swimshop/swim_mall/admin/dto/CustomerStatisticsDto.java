package com.swimshop.swim_mall.admin.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관리자용 고객 통계 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerStatisticsDto {
    
    private Long totalCustomerCount; // 전체 고객 수
    private Long emailVerifiedCount; // 이메일 인증 완료 고객 수
    private Long emailUnverifiedCount; // 이메일 미인증 고객 수
    private Long activeCustomerCount; // 활성 고객 수 (주문 이력 있는 고객)
    private Long newCustomerCountToday; // 오늘 가입한 고객 수
    private Long newCustomerCountThisMonth; // 이번 달 가입한 고객 수
    private Map<String, Long> customerCountByGrade; // 등급별 고객 수 (등급 코드 -> 고객 수)
}
