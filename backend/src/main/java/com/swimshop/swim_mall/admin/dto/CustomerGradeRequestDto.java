package com.swimshop.swim_mall.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 고객 등급 수정 요청 DTO
 * 등급명과 등급 레벨은 Enum으로 고정되어 있어 변경 불가능합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerGradeRequestDto {
    // 등급명과 등급 레벨은 Enum으로 고정되어 있어 요청에서 제외

    @NotNull(message = "최소 누적 구매액은 필수입니다.")
    @Min(value = 0, message = "최소 누적 구매액은 0 이상이어야 합니다.")
    private Long minPurchaseAmount; // 최소 누적 구매액

    @NotNull(message = "최소 주문 건수는 필수입니다.")
    @Min(value = 0, message = "최소 주문 건수는 0 이상이어야 합니다.")
    private Integer minOrderCount; // 최소 주문 건수

    @NotNull(message = "할인율은 필수입니다.")
    @Min(value = 0, message = "할인율은 0 이상이어야 합니다.")
    private Double discountRate; // 할인율 (%)

    @NotNull(message = "포인트 적립률은 필수입니다.")
    @Min(value = 0, message = "포인트 적립률은 0 이상이어야 합니다.")
    private Double pointAccumulationRate; // 포인트 적립률 (%)

    private Boolean isActive; // 활성화 여부 (기본값: true)
}
