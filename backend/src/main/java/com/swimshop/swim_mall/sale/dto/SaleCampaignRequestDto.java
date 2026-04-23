package com.swimshop.swim_mall.sale.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.swimshop.swim_mall.common.enums.SaleScope;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleCampaignRequestDto {

    private Long eventNo; // 선택

    @NotNull
    private SaleScope scope;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    @NotNull
    @Valid
    private List<SaleCampaignTargetRequestDto> targets;

    public void validatePeriod() {
        if (endAt.isBefore(startAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "세일 캠페인 종료일시는 시작일시보다 빠를 수 없습니다.");
        }
        if (targets == null || targets.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "세일 캠페인 타겟은 최소 1개 이상 필요합니다.");
        }
    }
}

