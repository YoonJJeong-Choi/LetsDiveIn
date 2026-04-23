package com.swimshop.swim_mall.product.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.product.dto.ProductImageDto;
import com.swimshop.swim_mall.product.dto.ProductImagesUpdateRequestDto;
import com.swimshop.swim_mall.product.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class ProductAdminController {

	private final ProductService productService;

	@GetMapping("/{productNo}/images")
	public ResponseEntity<List<ProductImageDto>> getImages(@PathVariable Long productNo) {
		return ResponseEntity.ok(productService.getProductImages(productNo));
	}

	@PatchMapping("/{productNo}/images")
	public ResponseEntity<Void> updateImages(
		@PathVariable Long productNo,
		@Validated @RequestBody ProductImagesUpdateRequestDto request
	) {
		productService.updateProductImages(productNo, request);
		return ResponseEntity.ok().build();
	}
}

