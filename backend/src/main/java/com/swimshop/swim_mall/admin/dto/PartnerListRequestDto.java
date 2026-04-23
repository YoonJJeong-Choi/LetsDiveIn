package com.swimshop.swim_mall.admin.dto;

import com.swimshop.swim_mall.common.enums.PartnerStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파트너 목록 조회 요청 DTO (쿼리 파라미터)
 */
@Getter
@NoArgsConstructor
public class PartnerListRequestDto {
    private PartnerStatus status; // 상태 필터 (PENDING, APPROVED, REJECTED, INACTIVE 또는 null = 전체)
}
