package com.swimshop.swim_mall.product.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.product.entity.ProductImageEntity;
import com.swimshop.swim_mall.product.repository.ProductImageRepository;

import lombok.RequiredArgsConstructor;

/**
 * 상품 컬럼 {@code productImageUrl}이 비어 있고 {@code product_image.file_id}만 있는 경우(파일 업로드)
 * 고객 화면용 URL을 생성합니다. 브라우저는 동일 출처 {@code /api/files/{id}/download} 로 요청합니다.
 */
@Component
@RequiredArgsConstructor
public class ProductCustomerImageUrlResolver {

    private final ProductImageRepository productImageRepository;

    public String resolveGalleryUrl(ProductImageEntity img) {
        if (img == null) {
            return "";
        }
        String u = img.getImageUrl();
        if (u != null && !u.isBlank()) {
            return u.trim();
        }
        if (img.getFileId() != null) {
            return "/api/files/" + img.getFileId() + "/download";
        }
        return "";
    }

    /**
     * 목록/상세/장바구니 등에 넣을 대표 이미지 URL. 없으면 빈 문자열.
     */
    public String resolveDisplayUrlOrEmpty(ProductEntity product) {
        if (product == null) {
            return "";
        }
        String direct = product.getProductImageUrl();
        if (direct != null && !direct.isBlank()) {
            return direct.trim();
        }
        List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
        if (imgs.isEmpty()) {
            return "";
        }
        List<ProductImageEntity> ordered = new ArrayList<>(imgs);
        ordered.sort(Comparator
                .comparing((ProductImageEntity i) -> Boolean.TRUE.equals(i.getIsPrimary()) ? 0 : 1)
                .thenComparing(ProductImageEntity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductImageEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        for (ProductImageEntity img : ordered) {
            String url = resolveGalleryUrl(img);
            if (!url.isBlank()) {
                return url;
            }
        }
        return "";
    }
}
