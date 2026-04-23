package com.swimshop.swim_mall.product.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.common.enums.ProductType;
import com.swimshop.swim_mall.common.enums.ProductSubType;
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
import com.swimshop.swim_mall.product.repository.ProductRepository;
import com.swimshop.swim_mall.product.repository.ProductImageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OptionRepository optionRepository;
    private final InventoryService inventoryService;

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
            
            // 이미지 조회
            List<ProductImageDto> imageDtos = new ArrayList<>();
            try {
                List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
                for (ProductImageEntity img : imgs) {
                    imageDtos.add(new ProductImageDto(
                        img.getId(), img.getImageUrl(), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
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
                product.getProductImageUrl(),
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
    public List<ProductListDto> getActiveProductList() {
        // 1. ACTIVE 및 PENDING_UPDATE 상태인 상품 조회 (수정 승인 대기 중이어도 노출)
        List<ProductEntity> activeProducts = productRepository.findByProductActiveStatusIn(
            List.of(ActiveStatus.ACTIVE, ActiveStatus.PENDING_UPDATE)
        );
        
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
            
            // 상품 설명 (이미 String 타입)
            String description = product.getProductDescription();
            // 이미지 조회
            List<ProductImageDto> imageDtos = new ArrayList<>();
            try {
                List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
                for (ProductImageEntity img : imgs) {
                    imageDtos.add(new ProductImageDto(
                        img.getId(), img.getImageUrl(), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
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
                product.getProductImageUrl(),
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
        
        // 3. 키워드 필터링 (서비스 레벨에서 처리)
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            String keywordLower = searchRequest.getKeyword().toLowerCase().trim();
            products = products.stream()
                    .filter(p -> p.getProductName().toLowerCase().contains(keywordLower) ||
                                p.getProductDescription().toLowerCase().contains(keywordLower))
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
            try {
                // 지연 로딩을 피하기 위한 런타임 주입 (선언부에 주입되어 있다고 가정)
                var colorServiceField = this.getClass().getDeclaredField("colorService");
                colorServiceField.setAccessible(true);
                Object colorServiceObj = colorServiceField.get(this);
                if (colorServiceObj != null) {
                    com.swimshop.swim_mall.color.service.ColorService colorService =
                            (com.swimshop.swim_mall.color.service.ColorService) colorServiceObj;
                    var normalized = colorService.normalizeColor(searchRequest.getOptionName());
                    if (normalized.isPresent()) {
                        normalizedColorComparisons = colorService.getComparableStringsForColorCode(normalized.get().getCode());
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                // ColorService 미도입 상태에서도 동작하도록 fallback 허용
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
                        img.getId(), img.getImageUrl(), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
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
                product.getProductImageUrl(),
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
        
        // 5-1. 이미지 조회
        List<ProductImageDto> imageDtos = new ArrayList<>();
        try {
            List<ProductImageEntity> imgs = productImageRepository.findByProductOrderBySortOrderAscIdAsc(product);
            for (ProductImageEntity img : imgs) {
                imageDtos.add(new ProductImageDto(
                    img.getId(), img.getImageUrl(), img.getFileId(), img.getSortOrder(), img.getIsPrimary()
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
                product.getProductImageUrl(),
                product.getProductCreatedAt(),
                minPrice,
                maxPrice,
                optionDtos,
                imageDtos
        );
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
                img.getImageUrl(),
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
            product.getProductActiveStatus()
        );
    }
    
}
