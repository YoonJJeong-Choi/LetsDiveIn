package com.swimshop.swim_mall.point.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.swimshop.swim_mall.common.enums.PointType;

import lombok.Getter;

@Getter
public class PointHistoryResponseDto {
    
    private Long historyId;
    private PointType pointType;
    private Long pointAmount;
    private Long pointBalanceAfter;
    private Long orderItemNo;
    private Long orderNo;
    private String description;
    private LocalDate expireDate;
    private LocalDateTime createdAt;
    private String adminName; // 관리자 이름 (수동 지급/차감 시)
    
    public PointHistoryResponseDto(Long historyId, PointType pointType, Long pointAmount,
                                  Long pointBalanceAfter, Long orderItemNo, Long orderNo,
                                  String description, LocalDate expireDate, LocalDateTime createdAt,
                                  String adminName) {
        this.historyId = historyId;
        this.pointType = pointType;
        this.pointAmount = pointAmount;
        this.pointBalanceAfter = pointBalanceAfter;
        this.orderItemNo = orderItemNo;
        this.orderNo = orderNo;
        this.description = description;
        this.expireDate = expireDate;
        this.createdAt = createdAt;
        this.adminName = adminName;
    }
}
