package com.swimshop.swim_mall.partner.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 파트너 상품 등록 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductCreateRequestDto {
    
    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 200, message = "상품명은 200자 이하여야 합니다.")
    private String productName;
    
    @NotNull(message = "상품 대분류는 필수입니다.")
    private String productType; // ProductType enum 값 (예: "SWIM_CAP")
    
    private String productSubType; // ProductSubType enum 값 (예: "CAP_SILICONE", null 가능)
    
    @NotBlank(message = "상품 가격은 필수입니다.")
    private String productPrice; // 기본 가격 (원 단위, String)
    
    @NotBlank(message = "상품 설명은 필수입니다.")
    @Size(max = 5000, message = "상품 설명은 5000자 이하여야 합니다.")
    private String productDescription;
    
    @NotBlank(message = "상품 이미지 URL은 필수입니다.")
    private String productImageUrl;
    
    @NotNull(message = "옵션 목록은 필수입니다.")
    @Size(min = 1, message = "최소 1개 이상의 옵션이 필요합니다.")
    private List<OptionCreateRequestDto> options; // 옵션 목록
    
    /**
     * 옵션 생성 요청 DTO (내부 클래스)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OptionCreateRequestDto {
        
        private String color; // 색상 (예: "빨강", "파랑", "블랙", null 가능)
        
        private String size; // 사이즈 (예: "S", "M", "L", "XL", "일반", null 가능)
        
        private Long optionAddPrice; // 추가 가격 (null이면 기본 가격, 원 단위)
    }
}
