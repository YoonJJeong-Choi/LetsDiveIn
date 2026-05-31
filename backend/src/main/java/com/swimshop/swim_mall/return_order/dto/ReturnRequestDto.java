package com.swimshop.swim_mall.return_order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.swimshop.swim_mall.common.enums.ReturnReasonType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 반품 신청 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDto {
    
    @NotNull(message = "주문 아이템 번호는 필수입니다.")
    private Long orderItemNo; // 주문 아이템 번호

    @NotNull(message = "반품 사유 유형은 필수입니다.")
    private ReturnReasonType returnReasonType;

    /** 상세 사유 (유형에 따라 필수 여부는 서비스에서 검증). 한·영 공백 포함 200자 이하. */
    @Size(max = 200, message = "상세 사유는 200자 이하로 입력해주세요.")
    private String returnReason;

    /** 불량·오배송·기타는 최소 1장 필수(서비스 검증). 변심·주문 실수는 생략 가능. */
    @Size(max = 3, message = "반품 증빙 이미지는 최대 3장까지 업로드할 수 있습니다.")
    private List<@NotNull(message = "이미지 파일 ID는 null일 수 없습니다.") Long> imageFileIds;
}
