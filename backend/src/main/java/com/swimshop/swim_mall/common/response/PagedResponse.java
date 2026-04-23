package com.swimshop.swim_mall.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PagedResponse<T> {
    private final List<T> items;
    private final long total;
    private final int page;
    private final int size;
}
