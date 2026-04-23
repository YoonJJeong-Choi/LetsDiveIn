package com.swimshop.swim_mall.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SaleScope {
    PRODUCT("상품"),
    OPTION("옵션");

    private final String label;
}

