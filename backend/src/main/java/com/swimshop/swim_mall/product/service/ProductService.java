package com.swimshop.swim_mall.product.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.common.enums.DiscountType;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.enums.ProductType;
import com.swimshop.swim_mall.common.enums.ProductSubType;
import com.swimshop.swim_mall.common.enums.SaleStatus;
import com.swimshop.swim_mall.color.service.ColorService;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.inventory.dto.InventoryResponseDto;
import com.swimshop.swim_mall.inventory.service.InventoryService;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.option.repository.OptionRepository;
import com.swimshop.swim_mall.product.dto.ProductListDto;
import com.swimshop.swim_mall.product.dto.ProductImageDto;
import com.swimshop.swim_mall.product.dto.ProductImagesUpdateRequestDto;
import com.swimshop.swim_mall.product.dto.ProductOptionDto;
import com.swimshop.swim_mall.product.dto.ProductSearchRequestDto;
import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.product.entity.ProductImageEntity;
import com.swimshop.swim_mall.product.entity.ProductViewLogEntity;
import com.swimshop.swim_mall.product.repository.ProductRepository;
import com.swimshop.swim_mall.product.repository.ProductImageRepository;
import com.swimshop.swim_mall.product.repository.ProductViewLogRepository;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.sale.entity.SalePolicyEntity;
import com.swimshop.swim_mall.sale.repository.SalePolicyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OptionRepository optionRepository;
    private final InventoryService inventoryService;
    private final ColorService colorService;
    private final OrderItemRepository orderItemRepository;
    private final SalePolicyRepository salePolicyRepository;
    private final ProductViewLogRepository productViewLogRepository;
    private final ProductCustomerImageUrlResolver productCustomerImageUrlResolver;

    private String normalizeForSearch(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 전체 상품 목록 조회 (모든 상태 포함, DTO 변환)
     */
    public List<ProductListDto> ProductList(){
        List<ProductEntity> products = productRepository.findAll();
        
        // DTO로 변환
        List<ProductListDto> result = new ArrayList<>();
        
        for (ProductEntity product : products) {
            // 상품의 모든 옵션 조회
            List<OptionEntity> options = optionRepository.findByProduct_ProductNo(product.getProductNo());
            
            // 기본 가격
            Long basePrice = Long.parseLong(product.getProductPrice());
            
            // 옵션 DTO 변환 및 최소/최대 가격 계산
            List<ProductOptionDto> optionDtos = new ArrayList<>();
            Long minPrice = basePrice;
            Long maxPrice = basePrice;
            
            for (OptionEntity option : options) {
                Long addPrice = option.getOptionAddPrice() != null ? option.getOptionAddPrice() : 0L;
                Long totalPrice = basePrice + addPrice;
                OptionSaleInfo saleInfo = resolveSaleInfo(product.getProductNo(), option.getOptionNo(), totalPrice);
                
                // 재고 정보 조회 (고객용 - 권한 확인 없음)
                Integer stockQuantity = null;
                Boolean stockAvailable = null;
                try {
                    InventoryResponseDto inventory = inventoryService.getInventoryByOptionNoPublic(option.getOptionNo());
                    stockQuantity = inventory.getStockQuantity();
                    stockAvailable = inventory.getInStock();
                } catch (Exception e) {
                    // 재고 정보가 없으면 null 유지
                }
                
                optionDtos.add(new ProductOptionDto(
                    option.getOptionNo(),
                    option.getColor(),
                    option.getSize(),
                    addPrice,
                    totalPrice,
                    saleInfo.salePrice,
                    saleInfo.salePercent,
                    stockQuantity,
                    stockAvailable
                ));
                
                // 최소/최대 가격 업데이트
                if (totalPrice < minPrice) minPrice = totalPrice;
                if (totalPrice > maxPrice) maxPrice = totalPrice;
            }
            
            // 이미지 조회
            List<ProductImageDto> imageDtos = new ArrayList<>();
            try {
                List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
                for (ProductImageEntity img : imgs) {
                    imageDtos.add(new ProductImageDto(
                        img.getId(), productCustomerImageUrlResolver.resolveGalleryUrl(img), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
                    ));
                }
            } catch (Exception ignored) {}
            // DTO 생성
            result.add(new ProductListDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                product.getProductDescription(),
                productCustomerImageUrlResolver.resolveDisplayUrlOrEmpty(product),
                product.getProductCreatedAt(),
                minPrice,
                maxPrice,
                optionDtos,
                imageDtos
            ));
        }
        
        return result;
    }
    
    /**
     * 고객용 상품 목록 조회 (ACTIVE 및 PENDING_UPDATE 상태, 옵션 정보 포함)
     * PENDING_UPDATE 상태도 고객에게 노출 (수정 승인 대기 중이지만 기존 내용은 유지)
     */
    public List<ProductListDto> getActiveProductList(String sortBy, String sortDir, Boolean saleOnly, Boolean inStock) {
        // 1. ACTIVE 및 PENDING_UPDATE 상태인 상품 조회 (수정 승인 대기 중이어도 노출)
        List<ProductEntity> activeProducts = productRepository.findByProductActiveStatusIn(
            List.of(ActiveStatus.ACTIVE, ActiveStatus.PENDING_UPDATE)
        );
        if (Boolean.TRUE.equals(saleOnly)) {
            Set<Long> discountedProductNos = getDiscountedProductNos();
            activeProducts = activeProducts.stream()
                    .filter(p -> discountedProductNos.contains(p.getProductNo()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 2. 각 상품의 옵션을 조회하여 DTO로 변환
        List<ProductListDto> result = new ArrayList<>();
        
        for (ProductEntity product : activeProducts) {
            // 상품의 모든 옵션 조회
            List<OptionEntity> options = optionRepository.findByProduct_ProductNo(product.getProductNo());
            
            // 기본 가격
            Long basePrice = Long.parseLong(product.getProductPrice());
            
            // 옵션 DTO 변환 및 최소/최대 가격 계산
            List<ProductOptionDto> optionDtos = new ArrayList<>();
            Long minPrice = basePrice;
            Long maxPrice = basePrice;
            
            for (OptionEntity option : options) {
                Long addPrice = option.getOptionAddPrice() != null ? option.getOptionAddPrice() : 0L;
                Long totalPrice = basePrice + addPrice;
                OptionSaleInfo saleInfo = resolveSaleInfo(product.getProductNo(), option.getOptionNo(), totalPrice);
                
                // 재고 정보 조회 (고객용 - 권한 확인 없음)
                Integer stockQuantity = null;
                Boolean stockAvailable = null;
                try {
                    InventoryResponseDto inventory = inventoryService.getInventoryByOptionNoPublic(option.getOptionNo());
                    stockQuantity = inventory.getStockQuantity();
                    stockAvailable = inventory.getInStock();
                } catch (Exception e) {
                    // 재고 정보가 없으면 null 유지
                }
                
                optionDtos.add(new ProductOptionDto(
                    option.getOptionNo(),
                    option.getColor(),
                    option.getSize(),
                    addPrice,
                    totalPrice,
                    saleInfo.salePrice,
                    saleInfo.salePercent,
                    stockQuantity,
                    stockAvailable
                ));
                
                // 최소/최대 가격 업데이트
                if (totalPrice < minPrice) minPrice = totalPrice;
                if (totalPrice > maxPrice) maxPrice = totalPrice;
            }
            
            // 옵션이 없는 상품의 경우, 가상 옵션 DTO 생성 (재고 정보 포함)
            if (options.isEmpty()) {
                // 재고 정보 조회 (옵션이 없는 상품의 경우)
                Integer stockQuantity = null;
                Boolean stockAvailable = null;
                try {
                    InventoryResponseDto inventory = inventoryService.getInventoryByProductNoPublic(product.getProductNo());
                    stockQuantity = inventory.getStockQuantity();
                    stockAvailable = inventory.getInStock();
                } catch (Exception e) {
                    // 재고 정보가 없으면 null 유지
                }
                
                OptionSaleInfo saleInfo = resolveSaleInfo(product.getProductNo(), null, basePrice);
                // 가상 옵션 DTO 생성 (optionNo는 null)
                optionDtos.add(new ProductOptionDto(
                        null, // optionNo 없음
                        null, // color 없음
                        null, // size 없음
                        0L,   // addPrice 없음
                        basePrice, // totalPrice는 기본 가격
                        saleInfo.salePrice,
                        saleInfo.salePercent,
                        stockQuantity,
                        stockAvailable
                ));
            }
            
            if (!matchesAvailability(optionDtos, inStock)) {
                continue;
            }

            // 상품 설명 (이미 String 타입)
            String description = product.getProductDescription();
            // 이미지 조회
            List<ProductImageDto> imageDtos = new ArrayList<>();
            try {
                List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
                for (ProductImageEntity img : imgs) {
                    imageDtos.add(new ProductImageDto(
                        img.getId(), productCustomerImageUrlResolver.resolveGalleryUrl(img), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
                    ));
                }
            } catch (Exception ignored) {}
            // DTO 생성
            result.add(new ProductListDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                description,
                productCustomerImageUrlResolver.resolveDisplayUrlOrEmpty(product),
                product.getProductCreatedAt(),
                minPrice,
                maxPrice,
                optionDtos,
                imageDtos
            ));
        }
        
        return applySortToProductList(result, sortBy, sortDir);
    }
    
    /**
     * 상품 검색/필터링
     */
    public List<ProductListDto> searchProducts(ProductSearchRequestDto searchRequest) {
        // 1. Enum 변환
        ProductType productTypeEnum = null;
        if (searchRequest.getProductType() != null && !searchRequest.getProductType().isEmpty()) {
            try {
                productTypeEnum = ProductType.valueOf(searchRequest.getProductType());
            } catch (IllegalArgumentException e) {
                // 잘못된 enum 값이면 null로 처리
            }
        }
        
        ProductSubType productSubTypeEnum = null;
        if (searchRequest.getProductSubType() != null && !searchRequest.getProductSubType().isEmpty()) {
            try {
                productSubTypeEnum = ProductSubType.valueOf(searchRequest.getProductSubType());
            } catch (IllegalArgumentException e) {
                // 잘못된 enum 값이면 null로 처리
            }
        }
        
        // 2. 카테고리 필터링으로 상품 조회 (ACTIVE 및 PENDING_UPDATE 상태)
        // PENDING_UPDATE 상태도 포함하여 조회
        List<ProductEntity> products = new ArrayList<>();
        List<ProductEntity> activeProducts = productRepository.findByCategory(
                ActiveStatus.ACTIVE,
                productTypeEnum,
                productSubTypeEnum
        );
        List<ProductEntity> pendingUpdateProducts = productRepository.findByCategory(
                ActiveStatus.PENDING_UPDATE,
                productTypeEnum,
                productSubTypeEnum
        );
        products.addAll(activeProducts);
        products.addAll(pendingUpdateProducts);
        if (Boolean.TRUE.equals(searchRequest.getSaleOnly())) {
            Set<Long> discountedProductNos = getDiscountedProductNos();
            products = products.stream()
                    .filter(p -> discountedProductNos.contains(p.getProductNo()))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 3. 키워드 필터링 (서비스 레벨에서 처리)
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            String keywordLower = normalizeForSearch(searchRequest.getKeyword());
            products = products.stream()
                    .filter(p -> {
                        boolean nameMatch = normalizeForSearch(p.getProductName()).contains(keywordLower);
                        boolean descMatch = normalizeForSearch(p.getProductDescription()).contains(keywordLower);
                        boolean typeMatch = p.getProductType() != null
                                && (normalizeForSearch(p.getProductType().name()).contains(keywordLower)
                                    || normalizeForSearch(p.getProductType().getLabel()).contains(keywordLower));
                        boolean subTypeMatch = p.getProductSubType() != null
                                && normalizeForSearch(p.getProductSubType().name()).contains(keywordLower);
                        boolean productBrandMatch = normalizeForSearch(p.getBrandName()).contains(keywordLower);
                        boolean brandMatch = p.getPartner() != null
                                && p.getPartner().getRepresentativeBrandCode() != null
                                && normalizeForSearch(p.getPartner().getRepresentativeBrandCode()).contains(keywordLower);
                        boolean optionMatch = optionRepository.findByProduct_ProductNo(p.getProductNo()).stream()
                                .anyMatch(opt -> normalizeForSearch(opt.getColor()).contains(keywordLower)
                                        || normalizeForSearch(opt.getSize()).contains(keywordLower));
                        return nameMatch || descMatch || typeMatch || subTypeMatch || productBrandMatch || brandMatch || optionMatch;
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        // 3-1. 파트너 대표 브랜드 코드 필터링
        if (searchRequest.getPartnerBrandCode() != null && !searchRequest.getPartnerBrandCode().trim().isEmpty()) {
            String brandCode = searchRequest.getPartnerBrandCode().trim().toUpperCase();
            products = products.stream()
                    .filter(p -> p.getPartner() != null
                            && p.getPartner().getRepresentativeBrandCode() != null
                            && p.getPartner().getRepresentativeBrandCode().equalsIgnoreCase(brandCode))
                    .collect(java.util.stream.Collectors.toList());
        }
        
        // 4. DTO 변환 및 옵션 필터링
        List<ProductListDto> result = new ArrayList<>();
        
        // 색상 정규화 준비
        Set<String> normalizedColorComparisons = java.util.Collections.emptySet();
        String rawOptionSearch = null;
        if (searchRequest.getOptionName() != null && !searchRequest.getOptionName().isEmpty()) {
            rawOptionSearch = searchRequest.getOptionName().trim().toLowerCase();
            if (colorService != null) {
                var normalized = colorService.normalizeColor(searchRequest.getOptionName());
                if (normalized.isPresent()) {
                    normalizedColorComparisons = colorService.getComparableStringsForColorCode(normalized.get().getCode());
                }
            }
        }

        for (ProductEntity product : products) {
            // 상품의 모든 옵션 조회
            List<OptionEntity> options = optionRepository.findByProduct_ProductNo(product.getProductNo());
            
            // 옵션 필터링 (색상 또는 사이즈로 검색)
            if (searchRequest.getOptionName() != null && !searchRequest.getOptionName().isEmpty()) {
                String searchTerm = rawOptionSearch;
                final Set<String> cmp = normalizedColorComparisons;
                options = options.stream().filter(opt -> {
                    String c = opt.getColor() != null ? opt.getColor().trim().toLowerCase() : null;
                    String s = opt.getSize() != null ? opt.getSize().trim().toLowerCase() : null;
                    boolean colorMatch;
                    if (cmp != null && !cmp.isEmpty() && c != null) {
                        // 표준 컬러 코드 정규화 기반 비교 (label + synonyms 완전일치)
                        colorMatch = cmp.contains(c);
                        // 보완: 일부 상표가 접두/접미로 색상을 포함하는 케이스 지원 (contains)
                        if (!colorMatch) {
                            colorMatch = cmp.stream().anyMatch(v -> c.contains(v));
                        }
                    } else {
                        // Fallback: 부분일치
                        colorMatch = (c != null && c.contains(searchTerm));
                    }
                    boolean sizeMatch = (s != null && s.contains(searchTerm));
                    return colorMatch || sizeMatch;
                }).collect(java.util.stream.Collectors.toList());
                
                // 옵션 필터링 후 매칭되는 옵션이 없으면 상품 제외
                if (options.isEmpty()) {
                    continue;
                }
            }
            
            // 기본 가격
            Long basePrice = Long.parseLong(product.getProductPrice());
            
            // 옵션 DTO 변환 및 최소/최대 가격 계산
            List<ProductOptionDto> optionDtos = new ArrayList<>();
            Long minPrice = basePrice;
            Long maxPrice = basePrice;
            
            for (OptionEntity option : options) {
                Long addPrice = option.getOptionAddPrice() != null ? option.getOptionAddPrice() : 0L;
                Long totalPrice = basePrice + addPrice;
                OptionSaleInfo saleInfo = resolveSaleInfo(product.getProductNo(), option.getOptionNo(), totalPrice);
                
                // 재고 정보 조회 (고객용 - 권한 확인 없음)
                Integer stockQuantity = null;
                Boolean inStock = null;
                try {
                    InventoryResponseDto inventory = inventoryService.getInventoryByOptionNoPublic(option.getOptionNo());
                    stockQuantity = inventory.getStockQuantity();
                    inStock = inventory.getInStock();
                } catch (Exception e) {
                    // 재고 정보가 없으면 null 유지
                }
                
                optionDtos.add(new ProductOptionDto(
                    option.getOptionNo(),
                    option.getColor(),
                    option.getSize(),
                    addPrice,
                    totalPrice,
                    saleInfo.salePrice,
                    saleInfo.salePercent,
                    stockQuantity,
                    inStock
                ));
                
                // 최소/최대 가격 업데이트
                if (totalPrice < minPrice) minPrice = totalPrice;
                if (totalPrice > maxPrice) maxPrice = totalPrice;
            }
            
            if (!matchesAvailability(optionDtos, searchRequest.getInStock())) {
                continue;
            }

            // 가격 필터링 (옵션 가격까지 고려)
            if (searchRequest.getMinPrice() != null && maxPrice < searchRequest.getMinPrice()) {
                continue; // 최대 가격이 최소 필터보다 작으면 제외
            }
            if (searchRequest.getMaxPrice() != null && minPrice > searchRequest.getMaxPrice()) {
                continue; // 최소 가격이 최대 필터보다 크면 제외
            }
            
            // 이미지 조회
            List<ProductImageDto> imageDtos = new ArrayList<>();
            try {
                List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
                for (ProductImageEntity img : imgs) {
                    imageDtos.add(new ProductImageDto(
                        img.getId(), productCustomerImageUrlResolver.resolveGalleryUrl(img), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
                    ));
                }
            } catch (Exception ignored) {}
            // DTO 생성
            result.add(new ProductListDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                product.getProductDescription(),
                productCustomerImageUrlResolver.resolveDisplayUrlOrEmpty(product),
                product.getProductCreatedAt(),
                minPrice,
                maxPrice,
                optionDtos,
                imageDtos
            ));
        }
        
        return applySortToProductList(result, searchRequest.getSortBy(), searchRequest.getSortDir());
    }

    private List<ProductListDto> applySortToProductList(List<ProductListDto> products, String sortBy, String sortDir) {
        if (products == null || products.isEmpty()) {
            return products;
        }

        String normalizedSortBy = sortBy == null ? "latest" : sortBy.trim().toLowerCase();
        String normalizedSortDir = sortDir == null ? "desc" : sortDir.trim().toLowerCase();
        boolean asc = "asc".equals(normalizedSortDir);

        Comparator<ProductListDto> comparator;
        switch (normalizedSortBy) {
            case "price":
                comparator = Comparator.comparing(dto -> dto.getMinPrice() != null ? dto.getMinPrice() : 0L);
                break;
            case "popularity":
                Map<Long, Long> soldCountMap = getSoldQuantityMap();
                comparator = Comparator.comparing(
                        (ProductListDto dto) -> soldCountMap.getOrDefault(dto.getProductNo(), 0L)
                );
                break;
            case "latest":
            default:
                comparator = Comparator.comparing(
                        dto -> dto.getProductCreatedAt() != null
                                ? dto.getProductCreatedAt()
                                : java.time.LocalDateTime.MIN
                );
                break;
        }

        if (!asc) {
            comparator = comparator.reversed();
        }

        products.sort(comparator.thenComparing(ProductListDto::getProductNo));
        return products;
    }

    private Map<Long, Long> getSoldQuantityMap() {
        List<Object[]> rows = orderItemRepository.findSoldQuantityByProduct(
                List.of(OrderStatus.PAID, OrderStatus.ACTIVE)
        );
        Map<Long, Long> soldCountMap = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            Long productNo = row[0] == null ? null : ((Number) row[0]).longValue();
            Long quantity = row[1] == null ? 0L : ((Number) row[1]).longValue();
            if (productNo != null) {
                soldCountMap.put(productNo, quantity);
            }
        }
        return soldCountMap;
    }

    private Set<Long> getDiscountedProductNos() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<SalePolicyEntity> activePolicies =
                salePolicyRepository.findByStatusAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        SaleStatus.ACTIVE,
                        now,
                        now
                );

        Set<Long> productNos = new HashSet<>();
        if (activePolicies.isEmpty()) {
            return productNos;
        }

        Map<Long, Long> optionToProductNo = new HashMap<>();
        List<Long> optionNos = activePolicies.stream()
                .filter(policy -> policy.getTargetOptionNo() != null)
                .map(SalePolicyEntity::getTargetOptionNo)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (!optionNos.isEmpty()) {
            optionRepository.findAllById(optionNos).forEach(opt -> optionToProductNo.put(
                    opt.getOptionNo(),
                    opt.getProduct() != null ? opt.getProduct().getProductNo() : null
            ));
        }

        for (SalePolicyEntity policy : activePolicies) {
            if (policy.getTargetProductNo() != null) {
                productNos.add(policy.getTargetProductNo());
                continue;
            }
            if (policy.getTargetOptionNo() != null) {
                Long productNo = optionToProductNo.get(policy.getTargetOptionNo());
                if (productNo != null) {
                    productNos.add(productNo);
                }
            }
        }
        return productNos;
    }

    private OptionSaleInfo resolveSaleInfo(Long productNo, Long optionNo, Long basePrice) {
        if (productNo == null || basePrice == null || basePrice <= 0) {
            return OptionSaleInfo.none();
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<SalePolicyEntity> eligible = salePolicyRepository.findEligibleFor(productNo, optionNo, SaleStatus.ACTIVE, now);
        if (eligible == null || eligible.isEmpty()) {
            return OptionSaleInfo.none();
        }
        SalePolicyEntity policy = eligible.get(0);
        long discountAmount = calculateDiscountAmount(policy, basePrice);
        if (discountAmount <= 0) {
            return OptionSaleInfo.none();
        }
        long salePrice = Math.max(0, basePrice - discountAmount);
        if (salePrice >= basePrice) {
            return OptionSaleInfo.none();
        }
        int salePercent = (int) Math.round((discountAmount * 100.0) / basePrice);
        return new OptionSaleInfo(salePrice, salePercent);
    }

    private long calculateDiscountAmount(SalePolicyEntity policy, long basePrice) {
        if (policy == null || policy.getDiscountType() == null || policy.getDiscountValue() == null) {
            return 0L;
        }
        long discount;
        if (policy.getDiscountType() == DiscountType.PERCENT) {
            discount = Math.round((basePrice * policy.getDiscountValue()) / 100.0);
        } else {
            discount = policy.getDiscountValue();
        }
        if (discount <= 0) {
            return 0L;
        }
        if (policy.getMaxDiscountAmount() != null && policy.getMaxDiscountAmount() > 0) {
            discount = Math.min(discount, policy.getMaxDiscountAmount());
        }
        return Math.min(basePrice, discount);
    }

    private static class OptionSaleInfo {
        final Long salePrice;
        final Integer salePercent;

        OptionSaleInfo(Long salePrice, Integer salePercent) {
            this.salePrice = salePrice;
            this.salePercent = salePercent;
        }

        static OptionSaleInfo none() {
            return new OptionSaleInfo(null, null);
        }
    }

    private boolean matchesAvailability(List<ProductOptionDto> optionDtos, Boolean requestedInStock) {
        if (requestedInStock == null) {
            return true;
        }
        if (optionDtos == null || optionDtos.isEmpty()) {
            return false;
        }
        boolean hasInStock = optionDtos.stream().anyMatch(opt -> Boolean.TRUE.equals(opt.getInStock()));
        if (Boolean.TRUE.equals(requestedInStock)) {
            return hasInStock;
        }
        boolean hasKnownStockInfo = optionDtos.stream().anyMatch(opt -> opt.getInStock() != null);
        return hasKnownStockInfo && !hasInStock;
    }

    /**
     * 상품 상세 조회 (ACTIVE 및 PENDING_UPDATE 상태)
     * PENDING_UPDATE 상태도 고객에게 노출 (수정 승인 대기 중이지만 기존 내용은 유지)
     */
    public ProductListDto getProductDetail(Long productNo) {
        // 1. 상품 조회 (ACTIVE 및 PENDING_UPDATE 상태)
        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        if (product.getProductActiveStatus() != ActiveStatus.ACTIVE 
                && product.getProductActiveStatus() != ActiveStatus.PENDING_UPDATE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 옵션 조회
        List<OptionEntity> options = optionRepository.findByProduct_ProductNo(productNo);

        // 3. 기본 가격
        Long basePrice = Long.parseLong(product.getProductPrice());

        // 4. 옵션 DTO 변환 및 최소/최대 가격 계산
        List<ProductOptionDto> optionDtos = new ArrayList<>();
        Long minPrice = basePrice;
        Long maxPrice = basePrice;

        for (OptionEntity option : options) {
            Long addPrice = option.getOptionAddPrice() != null ? option.getOptionAddPrice() : 0L;
            Long totalPrice = basePrice + addPrice;

            // 재고 정보 조회 (고객용 - 권한 확인 없음)
            Integer stockQuantity = null;
            Boolean inStock = null;
            try {
                InventoryResponseDto inventory = inventoryService.getInventoryByOptionNoPublic(option.getOptionNo());
                stockQuantity = inventory.getStockQuantity();
                inStock = inventory.getInStock();
            } catch (Exception e) {
                // 재고 정보가 없으면 null 유지
            }

            optionDtos.add(new ProductOptionDto(
                    option.getOptionNo(),
                    option.getColor(),
                    option.getSize(),
                    addPrice,
                    totalPrice,
                    stockQuantity,
                    inStock
            ));

            // 최소/최대 가격 업데이트
            if (totalPrice < minPrice) minPrice = totalPrice;
            if (totalPrice > maxPrice) maxPrice = totalPrice;
        }
        
        // 옵션이 없는 상품의 경우, 가상 옵션 DTO 생성 (재고 정보 포함)
        if (options.isEmpty()) {
            // 재고 정보 조회 (옵션이 없는 상품의 경우)
            Integer stockQuantity = null;
            Boolean inStock = null;
            try {
                InventoryResponseDto inventory = inventoryService.getInventoryByProductNoPublic(product.getProductNo());
                stockQuantity = inventory.getStockQuantity();
                inStock = inventory.getInStock();
            } catch (Exception e) {
                // 재고 정보가 없으면 null 유지
            }
            
            // 가상 옵션 DTO 생성 (optionNo는 null)
            optionDtos.add(new ProductOptionDto(
                    null, // optionNo 없음
                    null, // color 없음
                    null, // size 없음
                    0L,   // addPrice 없음
                    basePrice, // totalPrice는 기본 가격
                    stockQuantity,
                    inStock
            ));
        }

        // 5. 상품 설명 (이미 String 타입)
        String description = product.getProductDescription();
        String brandName = product.getBrandName();
        if ((brandName == null || brandName.isBlank()) && product.getPartner() != null) {
            brandName = product.getPartner().getRepresentativeBrandCode();
        }
        
        // 5-1. 이미지 조회
        List<ProductImageDto> imageDtos = new ArrayList<>();
        try {
            List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
            for (ProductImageEntity img : imgs) {
                imageDtos.add(new ProductImageDto(
                    img.getId(), productCustomerImageUrlResolver.resolveGalleryUrl(img), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
                ));
            }
        } catch (Exception ignored) {}

        // 6. DTO 생성
        return new ProductListDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                description,
                productCustomerImageUrlResolver.resolveDisplayUrlOrEmpty(product),
                product.getSku(),
                brandName,
                product.getMaterialInfo(),
                product.getCareInstructions(),
                product.getSizeGuideText(),
                product.getSizeGuideJson(),
                product.getOriginCountry(),
                product.getManufactureCountry(),
                product.getProductCreatedAt(),
                minPrice,
                maxPrice,
                optionDtos,
                imageDtos
        );
    }

    public List<ProductListDto> getRelatedProducts(Long productNo, String preferredColor, int limit) {
        ProductEntity base = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (base.getProductActiveStatus() != ActiveStatus.ACTIVE
                && base.getProductActiveStatus() != ActiveStatus.PENDING_UPDATE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        String normalizedColor = preferredColor == null ? null : preferredColor.trim().toLowerCase();
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<ProductEntity> pool = productRepository.findByProductActiveStatusIn(
                List.of(ActiveStatus.ACTIVE, ActiveStatus.PENDING_UPDATE)
        ).stream()
                .filter(p -> !p.getProductNo().equals(productNo))
                .collect(java.util.stream.Collectors.toList());

        Comparator<ProductEntity> relatedComparator = Comparator
                .comparingInt((ProductEntity p) -> relatedScore(base, p, normalizedColor)).reversed()
                .thenComparing(ProductEntity::getProductCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ProductEntity::getProductNo, Comparator.reverseOrder());

        return pool.stream()
                .sorted(relatedComparator)
                .limit(safeLimit)
                .map(this::toProductListDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private int relatedScore(ProductEntity base, ProductEntity candidate, String normalizedColor) {
        int score = 0;
        if (base.getProductSubType() != null && base.getProductSubType() == candidate.getProductSubType()) {
            score += 100;
        } else if (base.getProductType() == candidate.getProductType()) {
            score += 40;
        }
        if (normalizedColor != null && !normalizedColor.isBlank() && hasColor(candidate.getProductNo(), normalizedColor)) {
            score += 20;
        }
        return score;
    }

    private boolean hasColor(Long productNo, String normalizedColor) {
        List<OptionEntity> options = optionRepository.findByProduct_ProductNo(productNo);
        return options.stream()
                .map(OptionEntity::getColor)
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toLowerCase())
                .anyMatch(c -> c.equals(normalizedColor));
    }

    private ProductListDto toProductListDto(ProductEntity product) {
        List<OptionEntity> options = optionRepository.findByProduct_ProductNo(product.getProductNo());
        Long basePrice = Long.parseLong(product.getProductPrice());
        List<ProductOptionDto> optionDtos = new ArrayList<>();
        Long minPrice = basePrice;
        Long maxPrice = basePrice;

        for (OptionEntity option : options) {
            Long addPrice = option.getOptionAddPrice() != null ? option.getOptionAddPrice() : 0L;
            Long totalPrice = basePrice + addPrice;

            Integer stockQuantity = null;
            Boolean inStock = null;
            try {
                InventoryResponseDto inventory = inventoryService.getInventoryByOptionNoPublic(option.getOptionNo());
                stockQuantity = inventory.getStockQuantity();
                inStock = inventory.getInStock();
            } catch (Exception ignored) {}

            optionDtos.add(new ProductOptionDto(
                    option.getOptionNo(),
                    option.getColor(),
                    option.getSize(),
                    addPrice,
                    totalPrice,
                    stockQuantity,
                    inStock
            ));

            if (totalPrice < minPrice) minPrice = totalPrice;
            if (totalPrice > maxPrice) maxPrice = totalPrice;
        }

        if (options.isEmpty()) {
            optionDtos.add(new ProductOptionDto(
                    null, null, null, 0L, basePrice, null, null
            ));
        }

        List<ProductImageDto> imageDtos = new ArrayList<>();
        try {
            List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
            for (ProductImageEntity img : imgs) {
                imageDtos.add(new ProductImageDto(
                        img.getId(), productCustomerImageUrlResolver.resolveGalleryUrl(img), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
                ));
            }
        } catch (Exception ignored) {}

        return new ProductListDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                product.getProductDescription(),
                productCustomerImageUrlResolver.resolveDisplayUrlOrEmpty(product),
                product.getSku(),
                product.getBrandName(),
                product.getMaterialInfo(),
                product.getCareInstructions(),
                product.getSizeGuideText(),
                product.getSizeGuideJson(),
                product.getOriginCountry(),
                product.getManufactureCountry(),
                product.getProductCreatedAt(),
                minPrice,
                maxPrice,
                optionDtos,
                imageDtos
        );
    }

    @Transactional
    public Long incrementProductViewCount(Long productNo, Long customerId, String guestId) {
        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getProductActiveStatus() != ActiveStatus.ACTIVE
                && product.getProductActiveStatus() != ActiveStatus.PENDING_UPDATE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusHours(24);
        boolean shouldIncrement = true;
        if (customerId != null) {
            shouldIncrement = !productViewLogRepository.existsByProductNoAndCustomerIdAndViewedAtAfter(
                    productNo,
                    customerId,
                    threshold
            );
        } else if (guestId != null && !guestId.isBlank()) {
            shouldIncrement = !productViewLogRepository.existsByProductNoAndGuestIdAndViewedAtAfter(
                    productNo,
                    guestId,
                    threshold
            );
        }

        if (!shouldIncrement) {
            return getProductViewCount(productNo);
        }

        productViewLogRepository.save(ProductViewLogEntity.builder()
                .productNo(productNo)
                .customerId(customerId)
                .guestId(customerId == null ? guestId : null)
                .build());
        return getProductViewCount(productNo);
    }

    public Long getProductViewCount(Long productNo) {
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now().minusHours(24);
        return productViewLogRepository.countByProductNoAndViewedAtAfter(productNo, threshold);
    }
    
    /**
     * 상품 이미지 목록 조회 (정렬 순서 기준)
     */
    public List<ProductImageDto> getProductImages(Long productNo) {
        ProductEntity product = productRepository.findById(productNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        List<ProductImageEntity> images = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
        List<ProductImageDto> dtos = new ArrayList<>();
        for (ProductImageEntity img : images) {
            dtos.add(new ProductImageDto(
                img.getId(),
                productCustomerImageUrlResolver.resolveGalleryUrl(img),
                img.getFileId(),
                img.getSortOrder(),
                img.getIsPrimary()
            ));
        }
        return dtos;
    }

    /**
     * 상품 이미지 일괄 업데이트 (정렬/대표 동기화)
     * 기존 이미지를 모두 삭제 후 새 목록 저장
     */
    @Transactional
    public void updateProductImages(Long productNo, ProductImagesUpdateRequestDto request) {
        ProductEntity product = productRepository.findById(productNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (request.getImages() == null || request.getImages().isEmpty()) {
            // 모두 삭제하고 대표 이미지 URL도 비움
            productImageRepository.deleteByProduct(product);
            product.update(
                product.getProductName(),
                product.getProductType(),
                product.getProductSubType(),
                product.getProductPrice(),
                product.getProductDescription(),
                null,
                product.getSku(),
                product.getBrandName(),
                product.getMaterialInfo(),
                product.getOriginCountry(),
                product.getManufactureCountry(),
                product.getCareInstructions(),
                product.getSizeGuideText(),
                product.getSizeGuideJson(),
                product.getProductActiveStatus()
            );
            return;
        }

        // 대표 이미지는 하나만 허용
        long primaryCount = request.getImages().stream().filter(i -> Boolean.TRUE.equals(i.getIsPrimary())).count();
        if (primaryCount == 0) {
            // 대표가 없다면 sortOrder가 가장 작은 것을 대표로 자동 지정
            int minOrder = request.getImages().stream().mapToInt(i -> i.getSortOrder() == null ? Integer.MAX_VALUE : i.getSortOrder()).min().orElse(0);
            for (var i : request.getImages()) {
                if (i.getSortOrder() != null && i.getSortOrder() == minOrder) {
                    // reflection 없이 직접 세터가 없으므로 새 리스트 구성 시 반영
                    // 아래 저장 루프에서 첫 번째 대표로 저장됨
                    // 표시 목적이므로 그대로 진행
                }
            }
        }
        if (primaryCount > 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        // 기존 이미지 삭제 후 재저장
        productImageRepository.deleteByProduct(product);

        String primaryUrl = null;
        for (var i : request.getImages()) {
            boolean isPrimary = Boolean.TRUE.equals(i.getIsPrimary());
            if (primaryUrl == null && isPrimary) {
                primaryUrl = i.getImageUrl();
            }
            ProductImageEntity entity = ProductImageEntity.create(
                product,
                i.getImageUrl(),
                i.getFileId(),
                i.getSortOrder() != null ? i.getSortOrder() : 0,
                isPrimary
            );
            productImageRepository.save(entity);
        }

        // 대표 이미지 URL 동기화 (없으면 정렬순 첫번째)
        if (primaryUrl == null) {
            var first = request.getImages().stream()
                .sorted(java.util.Comparator.comparingInt(img -> img.getSortOrder() != null ? img.getSortOrder() : Integer.MAX_VALUE))
                .findFirst();
            primaryUrl = first.map(ProductImagesUpdateRequestDto.ImageItem::getImageUrl).orElse(null);
        }

        product.update(
            product.getProductName(),
            product.getProductType(),
            product.getProductSubType(),
            product.getProductPrice(),
            product.getProductDescription(),
            primaryUrl,
            product.getSku(),
            product.getBrandName(),
            product.getMaterialInfo(),
            product.getOriginCountry(),
            product.getManufactureCountry(),
            product.getCareInstructions(),
            product.getSizeGuideText(),
            product.getSizeGuideJson(),
            product.getProductActiveStatus()
        );
    }
    
}
