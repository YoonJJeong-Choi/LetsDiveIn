package com.swimshop.swim_mall.return_order.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.swimshop.swim_mall.common.enums.ReturnReasonType;
import com.swimshop.swim_mall.common.enums.ReturnRiskTier;
import com.swimshop.swim_mall.common.enums.ReturnStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 반품 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponseDto {
    
    private Long returnNo; // 반품 번호
    private Long orderItemNo; // 주문 아이템 번호
    private Long orderNo; // 주문 번호
    private Long productNo; // 상품 번호
    private String productName; // 상품명
    private String productImageUrl; // 상품 이미지 URL
    private Long optionNo; // 옵션 번호 (nullable)
    private String color; // 색상 (nullable)
    private String size; // 사이즈 (nullable)
    private Integer quantity; // 수량
    private Long itemPrice; // 단위 가격
    private Long itemTotalPrice; // 총 가격
    private ReturnStatus returnStatus; // 반품 상태
    private LocalDateTime returnRequestedAt; // 반품 신청일시
    private ReturnReasonType returnReasonType;
    private Integer returnRiskScore;
    private ReturnRiskTier returnRiskTier;
    private String returnReason; // 반품 사유
    private Long returnAmount; // 반품 금액
    private String returnTrackingNumber; // 반품 송장번호
    private String returnCourier; // 반품 택배사
    private String rejectionReason; // 반품 거절 사유 (REJECTED 상태일 때)
    private List<String> imageUrls; // 반품 증빙 이미지 URL 목록
}
