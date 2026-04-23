package com.swimshop.swim_mall.product.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductImagesUpdateRequestDto {
	@NotEmpty
	private List<ImageItem> images;

	@Getter
	@NoArgsConstructor
	public static class ImageItem {
		@NotNull
		private String imageUrl;
		private Long fileId;
		@NotNull
		private Integer sortOrder;
		@NotNull
		private Boolean isPrimary;
	}
}

