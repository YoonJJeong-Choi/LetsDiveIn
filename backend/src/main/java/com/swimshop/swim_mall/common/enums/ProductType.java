package com.swimshop.swim_mall.common.enums;

public enum ProductType {

    SWIMSUIT_MEN("남성 수영복"),
    SWIMSUIT_WOMEN("여성 수영복"),
    SWIMSUIT_KIDS("아동 수영복"),
    SWIM_CAP("수모"),
    SWIM_GOGGLES("수경"),
    FINS("오리발"),
    SWIM_TOY("수영용품"),
    ETC("기타");

    private final String label;

    ProductType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}

