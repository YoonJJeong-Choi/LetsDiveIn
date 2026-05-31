package com.swimshop.swim_mall.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.List;

import com.swimshop.swim_mall.product.dto.ProductListDto;
import com.swimshop.swim_mall.product.dto.ProductSearchRequestDto;
import com.swimshop.swim_mall.product.service.ProductService;

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
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false, defaultValue = "false") Boolean saleOnly,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false, defaultValue = "latest") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        List<ProductListDto> products = productService.getActiveProductList(sortBy, sortDir, saleOnly, inStock);
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
            @RequestParam(required = false, defaultValue = "false") Boolean saleOnly,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") int page, // 0-based
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false, defaultValue = "latest") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir
    ) {
        ProductSearchRequestDto searchRequest = new ProductSearchRequestDto(
                keyword, productType, productSubType, minPrice, maxPrice, optionName, partnerBrandCode, saleOnly, inStock, sortBy, sortDir
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

    @GetMapping("/related/{productNo}")
    public ResponseEntity<com.swimshop.swim_mall.common.response.ApiResponse<List<ProductListDto>>> getRelatedProducts(
            @PathVariable Long productNo,
            @RequestParam(required = false) String color,
            @RequestParam(defaultValue = "8") int limit
    ) {
        List<ProductListDto> related = productService.getRelatedProducts(productNo, color, limit);
        return ResponseEntity.ok(com.swimshop.swim_mall.common.response.ApiResponse.success(related));
    }

    @PostMapping("/{productNo}/view")
    public ResponseEntity<com.swimshop.swim_mall.common.response.ApiResponse<Long>> incrementProductView(
            HttpSession session,
            HttpServletRequest request,
            @PathVariable Long productNo
    ) {
        Long customerId = getCustomerIdFromSession(session);
        String guestId = request.getHeader("X-Guest-Id");
        Long viewCount = productService.incrementProductViewCount(productNo, customerId, guestId);
        return ResponseEntity.ok(com.swimshop.swim_mall.common.response.ApiResponse.success(viewCount));
    }

    @GetMapping("/{productNo}/view")
    public ResponseEntity<com.swimshop.swim_mall.common.response.ApiResponse<Long>> getProductView(
            @PathVariable Long productNo
    ) {
        Long viewCount = productService.getProductViewCount(productNo);
        return ResponseEntity.ok(com.swimshop.swim_mall.common.response.ApiResponse.success(viewCount));
    }

    private Long getCustomerIdFromSession(HttpSession session) {
        Object customerIdObj = session.getAttribute("customerId");
        if (customerIdObj == null) {
            customerIdObj = session.getAttribute("subjectId");
        }
        return toLong(customerIdObj);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

}
