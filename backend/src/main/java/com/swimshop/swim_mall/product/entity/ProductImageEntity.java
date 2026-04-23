package com.swimshop.swim_mall.product.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_image")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductImageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_no", nullable = false, referencedColumnName = "productNo")
	@JsonIgnore
	private ProductEntity product;

	@Column(nullable = false, length = 1000)
	private String imageUrl;

	@Column(nullable = true)
	private Long fileId; // 업로드 파일 ID (선택)

	@Column(nullable = false)
	private Integer sortOrder;

	@Column(nullable = false)
	private Boolean isPrimary;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	public static ProductImageEntity create(ProductEntity product, String imageUrl, Long fileId, int sortOrder, boolean isPrimary) {
		return ProductImageEntity.builder()
			.product(product)
			.imageUrl(imageUrl)
			.fileId(fileId)
			.sortOrder(sortOrder)
			.isPrimary(isPrimary)
			.createdAt(LocalDateTime.now())
			.build();
	}

	public void updateOrderAndPrimary(int newOrder, boolean primary) {
		this.sortOrder = newOrder;
		this.isPrimary = primary;
	}
}

