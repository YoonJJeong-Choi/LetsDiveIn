package com.swimshop.swim_mall.return_order.dto;

import com.swimshop.swim_mall.common.enums.ReturnStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 반품 상태 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnUpdateRequestDto {
    
    private ReturnStatus returnStatus; // 변경할 반품 상태
    private String returnTrackingNumber; // 반품 송장번호 (선택)
    private String returnCourier; // 반품 택배사 (선택)
    private String rejectionReason; // 반품 거절 사유 (REJECTED 상태일 때 선택)
}
