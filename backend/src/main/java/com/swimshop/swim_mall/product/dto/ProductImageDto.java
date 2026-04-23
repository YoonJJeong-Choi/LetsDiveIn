package com.swimshop.swim_mall.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDto {
	private Long id;
	private String imageUrl;
	private Long fileId;
	private Integer sortOrder;
	private Boolean isPrimary;
}

