package com.swimshop.swim_mall.common.enums;

public enum ProductSubType {

    // 여성 수영복
    ONE_PIECE("원피스", ProductType.SWIMSUIT_WOMEN),
    BIKINI("비키니", ProductType.SWIMSUIT_WOMEN),
    MONOKINI("모노키니", ProductType.SWIMSUIT_WOMEN),
    RASH_GUARD("래쉬가드", ProductType.SWIMSUIT_WOMEN),
    
    // 남성 수영복
    TRUNKS("트렁크", ProductType.SWIMSUIT_MEN),
    JAMMER("잠머", ProductType.SWIMSUIT_MEN),
    BRIEF("브리프", ProductType.SWIMSUIT_MEN),
    
    // 수영모자
    CAP_SILICONE("실리콘 수모", ProductType.SWIM_CAP),
    CAP_FABRIC("천 수모", ProductType.SWIM_CAP),
    
    // 오리발
    FINS_SHORT("숏핀", ProductType.FINS),
    FINS_LONG("롱핀", ProductType.FINS),
    
    // 기타 (소분류 없음)
    NONE("없음", null);

    private final String label;
    private final ProductType productType;

    ProductSubType(String label, ProductType productType) {
        this.label = label;
        this.productType = productType;
    }

    public String getLabel() {
        return this.label;
    }

    //소분류가 어떤 대분류에 속하는지 확인
    public ProductType getProductType() {
        return this.productType;
    }
}

