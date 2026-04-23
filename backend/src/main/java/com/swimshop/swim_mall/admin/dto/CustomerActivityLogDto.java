package com.swimshop.swim_mall.admin.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.swimshop.swim_mall.common.enums.CustomerActivityType;
/**
 * 고객 활동 로그 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerActivityLogDto {
    
    private Long logId; // 로그 ID
    private Long customerId; // 고객 ID
    private CustomerActivityType activityType; // 활동 유형
    private String activityTypeLabel; // 활동 유형 라벨
    private LocalDateTime activityAt; // 활동 일시
    private String activityDetails; // 활동 상세 정보 (JSON)
    private String ipAddress; // IP 주소
    private String userAgent; // User-Agent
}
