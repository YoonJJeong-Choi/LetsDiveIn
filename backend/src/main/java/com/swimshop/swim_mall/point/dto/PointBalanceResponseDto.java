package com.swimshop.swim_mall.point.dto;

import lombok.Getter;

@Getter
public class PointBalanceResponseDto {
    
    private Long customerId;
    private String customerName;
    private Long pointBalance;
    
    public PointBalanceResponseDto(Long customerId, String customerName, Long pointBalance) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.pointBalance = pointBalance;
    }
}
