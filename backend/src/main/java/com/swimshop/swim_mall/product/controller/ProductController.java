package com.swimshop.swim_mall.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.swimshop.swim_mall.product.dto.ProductListDto;
import com.swimshop.swim_mall.product.dto.ProductSearchRequestDto;
import com.swimshop.swim_mall.product.service.ProductService;
import com.swimshop.swim_mall.common.response.PagedResponse;

@RequestMapping("/api/product")
@RequiredArgsConstructor
@RestController
public class ProductController {

    private final ProductService productService;

    /**
     * 전체 상품 목록 조회 (모든 상태 포함)
     */
    @GetMapping("/list")
    public ResponseEntity<List<ProductListDto>> ProductList(){
        return ResponseEntity.ok(productService.ProductList());
    }

    /**
     * 고객용 상품 목록 조회 (ACTIVE 상태만, 옵션 정보 포함)
     */
    @GetMapping("/list/active")
    public ResponseEntity<com.swimshop.swim_mall.common.response.ApiResponse<java.util.Map<String, Object>>> getActiveProductList(
            @RequestParam(defaultValue = "0") int page, // 0-based
            @RequestParam(defaultValue = "12") int size
    ) {
        List<ProductListDto> products = productService.getActiveProductList();
        int total = products.size();
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int from = Math.max(0, Math.min(p * s, total));
        int to = Math.max(from, Math.min(from + s, total));
        List<ProductListDto> items = products.subList(from, to);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("items", items);
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", p);
        meta.put("size", s);
        meta.put("total", total);
        body.put("meta", meta);
        return ResponseEntity.ok(com.swimshop.swim_mall.common.response.ApiResponse.success(body));
    }

    /**
     * 상품 검색/필터링
     * 
     * @param keyword 검색 키워드 (상품명, 설명)
     * @param productType 대분류 (ProductType enum name)
     * @param productSubType 소분류 (ProductSubType enum name)
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @param optionName 옵션명 (색상, 사이즈 등)
     * @param partnerBrandCode 파트너 대표 브랜드 코드
     */
    @GetMapping("/search")
    public ResponseEntity<com.swimshop.swim_mall.common.response.ApiResponse<java.util.Map<String, Object>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String productSubType,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String optionName,
            @RequestParam(required = false) String partnerBrandCode,
            @RequestParam(defaultValue = "0") int page, // 0-based
            @RequestParam(defaultValue = "12") int size
    ) {
        ProductSearchRequestDto searchRequest = new ProductSearchRequestDto(
                keyword, productType, productSubType, minPrice, maxPrice, optionName, partnerBrandCode
        );
        List<ProductListDto> products = productService.searchProducts(searchRequest);
        int total = products.size();
        int p = Math.max(0, page);
        int s = Math.max(1, size);
        int from = Math.max(0, Math.min(p * s, total));
        int to = Math.max(from, Math.min(from + s, total));
        List<ProductListDto> items = products.subList(from, to);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("items", items);
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", p);
        meta.put("size", s);
        meta.put("total", total);
        body.put("meta", meta);
        return ResponseEntity.ok(com.swimshop.swim_mall.common.response.ApiResponse.success(body));
    }

    /**
     * 상품 상세 조회 (ACTIVE 상태만)
     * 주의: 이 매핑은 다른 모든 구체적인 경로(/list, /list/active, /search) 다음에 와야 함
     */
    @GetMapping("/detail/{productNo}")
    public ResponseEntity<ProductListDto> getProductDetail(@PathVariable Long productNo) {
        ProductListDto product = productService.getProductDetail(productNo);
        return ResponseEntity.ok(product);
    }

}
