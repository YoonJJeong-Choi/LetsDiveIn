package com.swimshop.swim_mall.inventory.service;

import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.inventory.dto.InventoryResponseDto;
import com.swimshop.swim_mall.inventory.dto.InventoryUpdateRequestDto;
import com.swimshop.swim_mall.inventory.entity.InventoryEntity;
import com.swimshop.swim_mall.inventory.repository.InventoryRepository;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.option.repository.OptionRepository;
import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.product.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;

    /**
     * 재고 확인 (장바구니 추가 시 사용)
     * @param optionNo 옵션 번호 (옵션이 있는 상품의 경우)
     * @param productNo 상품 번호 (옵션이 없는 상품의 경우)
     * @param quantity 확인할 수량
     * @throws BusinessException 재고 정보 없음 또는 재고 부족 시
     */
    public void validateStockAvailability(Long optionNo, Long productNo, Integer quantity) {
        // optionNo와 productNo 중 하나는 반드시 존재해야 함
        if (optionNo == null && productNo == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "옵션 번호 또는 상품 번호가 필요합니다.");
        }
        if (optionNo != null && productNo != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "옵션 번호와 상품 번호를 동시에 지정할 수 없습니다.");
        }
        
        Integer availableStock = null;
        
        if (optionNo != null) {
            // 옵션이 있는 상품의 경우
            InventoryEntity inventory = inventoryRepository.findByOption_OptionNo(optionNo)
                    .orElse(null);
            
            if (inventory == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "재고 정보가 없어 장바구니에 담을 수 없습니다.");
            }
            
            availableStock = inventory.getInventoryStock();
        } else {
            // 옵션이 없는 상품의 경우
            InventoryEntity inventory = inventoryRepository.findByProduct_ProductNo(productNo)
                    .orElse(null);
            
            if (inventory == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "재고 정보가 없어 장바구니에 담을 수 없습니다.");
            }
            
            availableStock = inventory.getInventoryStock();
        }
        
        // 재고가 0이면 품절
        if (availableStock == null || availableStock <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "품절된 상품입니다.");
        }
        
        // 요청 수량이 재고보다 많으면 에러
        if (quantity > availableStock) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    String.format("재고가 부족합니다. 현재 재고: %d개", availableStock));
        }
    }

    /**
     * 재고 확인 및 차감 (주문 생성 시 사용)
     * @param optionNo 옵션 번호 (옵션이 있는 상품의 경우)
     * @param productNo 상품 번호 (옵션이 없는 상품의 경우)
     * @param quantity 차감할 수량
     * @throws BusinessException 재고 부족 시
     */
    @Transactional
    public void decreaseStockForOrder(Long optionNo, Long productNo, Integer quantity) {
        // optionNo와 productNo 중 하나는 반드시 존재해야 함
        if (optionNo == null && productNo == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "옵션 번호 또는 상품 번호가 필요합니다.");
        }
        if (optionNo != null && productNo != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "옵션 번호와 상품 번호를 동시에 지정할 수 없습니다.");
        }

        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "주문 수량은 1 이상이어야 합니다.");
        }

        InventoryEntity inventory = null;
        String itemInfo = "";

        if (optionNo != null) {
            // 옵션이 있는 상품
            OptionEntity option = optionRepository.findById(optionNo)
                    .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));

            inventory = inventoryRepository.findByOption_OptionNo(optionNo)
                    .orElse(null);

            itemInfo = String.format("옵션: %s/%s", 
                    option.getColor() != null ? option.getColor() : "-",
                    option.getSize() != null ? option.getSize() : "-");
        } else {
            // 옵션이 없는 상품
            ProductEntity product = productRepository.findById(productNo)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

            inventory = inventoryRepository.findByProduct_ProductNo(productNo)
                    .orElse(null);

            itemInfo = String.format("상품: %s", product.getProductName());
        }

        if (inventory == null) {
            // Inventory 레코드가 없으면 재고 0으로 간주
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    String.format("재고가 부족합니다. (%s, 요청 수량: %d, 현재 재고: 0)", 
                            itemInfo, quantity));
        }

        // 재고 확인
        if (inventory.getInventoryStock() < quantity) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    String.format("재고가 부족합니다. (%s, 요청 수량: %d, 현재 재고: %d)",
                            itemInfo, quantity, inventory.getInventoryStock()));
        }

        // 재고 차감
        inventory.decreaseStock(quantity);
        inventoryRepository.save(inventory);
    }

    /**
     * 재고 복구 (주문 취소 시 사용)
     * @param optionNo 옵션 번호 (옵션이 있는 상품의 경우)
     * @param productNo 상품 번호 (옵션이 없는 상품의 경우)
     * @param quantity 복구할 수량
     */
    @Transactional
    public void restoreStockForOrder(Long optionNo, Long productNo, Integer quantity) {
        // optionNo와 productNo 중 하나는 반드시 존재해야 함
        if (optionNo == null && productNo == null) {
            return; // 둘 다 null이면 복구 불가 (이미 삭제된 경우 등)
        }

        if (quantity <= 0) {
            return; // 수량이 0 이하면 복구 불필요
        }

        InventoryEntity inventory = null;

        if (optionNo != null) {
            // 옵션이 있는 상품
            OptionEntity option = optionRepository.findById(optionNo)
                    .orElse(null);

            if (option == null) {
                // 옵션이 삭제된 경우 복구 불가
                return;
            }

            inventory = inventoryRepository.findByOption_OptionNo(optionNo)
                    .orElse(null);

            if (inventory == null) {
                // Inventory 레코드가 없으면 새로 생성 (재고 복구)
                inventory = InventoryEntity.builder()
                        .option(option)
                        .inventoryStock(quantity)
                        .build();
                inventoryRepository.save(inventory);
            } else {
                // 기존 재고에 복구
                inventory.increaseStock(quantity);
                inventoryRepository.save(inventory);
            }
        } else {
            // 옵션이 없는 상품
            ProductEntity product = productRepository.findById(productNo)
                    .orElse(null);

            if (product == null) {
                // 상품이 삭제된 경우 복구 불가
                return;
            }

            inventory = inventoryRepository.findByProduct_ProductNo(productNo)
                    .orElse(null);

            if (inventory == null) {
                // Inventory 레코드가 없으면 새로 생성 (재고 복구)
                inventory = InventoryEntity.builder()
                        .product(product)
                        .inventoryStock(quantity)
                        .build();
                inventoryRepository.save(inventory);
            } else {
                // 기존 재고에 복구
                inventory.increaseStock(quantity);
                inventoryRepository.save(inventory);
            }
        }
    }

    /**
     * 파트너의 전체 재고 목록 조회
     * @param session HTTP 세션
     * @return 재고 목록
     */
    public List<InventoryResponseDto> getMyInventories(HttpSession session) {
        // 파트너 권한 확인
        authService.requireRole(session, AccountRole.PARTNER);

        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long partnerId = currentUser.getSubjectId();

        return getInventoriesByPartnerId(partnerId);
    }

    /**
     * 특정 파트너의 전체 재고 목록 조회 (관리자용)
     * @param partnerId 파트너 ID
     * @return 재고 목록
     */
    public List<InventoryResponseDto> getInventoriesByPartnerId(Long partnerId) {
        // 파트너의 재고 목록 조회
        List<InventoryEntity> inventories = inventoryRepository.findByPartnerId(partnerId);

        return inventories.stream()
                .map(this::toInventoryResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 전체 재고 목록 조회 (관리자용 - 모든 파트너)
     * @return 재고 목록
     */
    public List<InventoryResponseDto> getAllInventories() {
        // 모든 재고 조회
        List<InventoryEntity> inventories = inventoryRepository.findAll();

        return inventories.stream()
                .map(this::toInventoryResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 옵션의 재고 조회 (파트너용 - 권한 확인 포함)
     * @param session HTTP 세션
     * @param optionNo 옵션 번호
     * @return 재고 정보
     */
    public InventoryResponseDto getInventoryByOptionNo(HttpSession session, Long optionNo) {
        // 파트너 권한 확인
        authService.requireRole(session, AccountRole.PARTNER);

        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long partnerId = currentUser.getSubjectId();

        // 옵션 조회 및 권한 확인
        OptionEntity option = optionRepository.findById(optionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));

        // 파트너 소유 확인
        if (!option.getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 옵션만 조회할 수 있습니다.");
        }

        return getInventoryByOptionNoPublic(optionNo);
    }

    /**
     * 특정 옵션의 재고 조회 (고객용/관리자용 - 권한 확인 없음)
     * @param optionNo 옵션 번호
     * @return 재고 정보
     */
    public InventoryResponseDto getInventoryByOptionNoPublic(Long optionNo) {
        // 옵션 조회
        OptionEntity option = optionRepository.findById(optionNo)
                .orElse(null);

        if (option == null) {
            // 옵션이 없으면 재고 정보 없음
            return InventoryResponseDto.builder()
                    .inventoryNo(null)
                    .productNo(null)
                    .productName(null)
                    .optionNo(null)
                    .color(null)
                    .size(null)
                    .stockQuantity(null) // 재고 정보 없음
                    .inStock(null) // 재고 정보 없음
                    .build();
        }

        // 재고 조회 (없으면 재고 0으로 간주)
        InventoryEntity inventory = inventoryRepository.findByOption_OptionNo(optionNo)
                .orElse(null);

        if (inventory == null) {
            // Inventory 레코드가 없으면 재고 정보 없음 (null 반환)
            // 프론트엔드에서 재고 정보 없음과 재고 0개를 구분할 수 있도록
            return InventoryResponseDto.builder()
                    .inventoryNo(null)
                    .productNo(option.getProduct().getProductNo())
                    .productName(option.getProduct().getProductName())
                    .optionNo(option.getOptionNo())
                    .color(option.getColor())
                    .size(option.getSize())
                    .stockQuantity(null) // 재고 정보 없음
                    .inStock(null) // 재고 정보 없음
                    .build();
        }

        return toInventoryResponseDto(inventory);
    }

    /**
     * 재고 생성
     * @param session HTTP 세션
     * @param optionNo 옵션 번호
     * @param requestDto 재고 생성 요청 DTO
     * @return 생성된 재고 정보
     */
    @Transactional
    public InventoryResponseDto createInventory(HttpSession session, Long optionNo, InventoryUpdateRequestDto requestDto) {
        // 파트너 권한 확인
        authService.requireRole(session, AccountRole.PARTNER);
        
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long partnerId = currentUser.getSubjectId();
        
        // 옵션 조회 및 권한 확인
        OptionEntity option = optionRepository.findById(optionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
        
        // 파트너 소유 확인
        if (!option.getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 옵션만 재고를 생성할 수 있습니다.");
        }
        
        // 재고 수량 검증
        if (requestDto.getStockQuantity() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "재고 수량은 0 이상이어야 합니다.");
        }
        
        // 이미 재고가 존재하는지 확인
        if (inventoryRepository.findByOption_OptionNo(optionNo).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 재고가 존재합니다. 수정 API를 사용해주세요.");
        }
        
        // 재고 생성
        InventoryEntity inventory = InventoryEntity.builder()
                .option(option)
                .inventoryStock(requestDto.getStockQuantity())
                .build();
        inventory = inventoryRepository.save(inventory);
        
        return toInventoryResponseDto(inventory);
    }

    /**
     * 재고 수정
     * @param session HTTP 세션
     * @param optionNo 옵션 번호
     * @param requestDto 재고 수정 요청 DTO
     * @return 수정된 재고 정보
     */
    @Transactional
    public InventoryResponseDto updateInventory(HttpSession session, Long optionNo, InventoryUpdateRequestDto requestDto) {
        // 파트너 권한 확인
        authService.requireRole(session, AccountRole.PARTNER);
        
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long partnerId = currentUser.getSubjectId();
        
        // 옵션 조회 및 권한 확인
        OptionEntity option = optionRepository.findById(optionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
        
        // 파트너 소유 확인
        if (!option.getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 옵션만 수정할 수 있습니다.");
        }
        
        // 재고 수량 검증
        if (requestDto.getStockQuantity() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "재고 수량은 0 이상이어야 합니다.");
        }
        
        // 재고 조회 또는 생성
        InventoryEntity inventory = inventoryRepository.findByOption_OptionNo(optionNo)
                .orElse(null);
        
        if (inventory == null) {
            // Inventory 레코드가 없으면 새로 생성 (하위 호환성 유지)
            inventory = InventoryEntity.builder()
                    .option(option)
                    .inventoryStock(requestDto.getStockQuantity())
                    .build();
            inventory = inventoryRepository.save(inventory);
        } else {
            // 기존 재고 수정
            inventory.updateStock(requestDto.getStockQuantity());
            inventory = inventoryRepository.save(inventory);
        }
        
        return toInventoryResponseDto(inventory);
    }

    /**
     * 특정 상품의 재고 조회 (옵션이 없는 상품의 경우)
     * @param session HTTP 세션
     * @param productNo 상품 번호
     * @return 재고 정보
     */
    public InventoryResponseDto getInventoryByProductNo(HttpSession session, Long productNo) {
        // 파트너 권한 확인
        authService.requireRole(session, AccountRole.PARTNER);
        
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long partnerId = currentUser.getSubjectId();
        
        // 상품 조회
        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        // 옵션이 있는 상품인지 확인
        List<OptionEntity> options = optionRepository.findByProduct_ProductNo(productNo);
        boolean hasOptions = !options.isEmpty();
        
        if (hasOptions) {
            // 옵션이 있는 상품은 product 엔드포인트 사용 불가
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "옵션이 있는 상품은 옵션 단위 재고 관리 API를 사용해주세요.");
        }
        
        // 옵션이 없는 상품: ProductEntity.partner로 소유권 확인
        if (product.getPartner() == null || !product.getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품만 조회할 수 있습니다.");
        }
        
        return getInventoryByProductNoPublic(productNo);
    }

    /**
     * 특정 상품의 재고 조회 (옵션이 없는 상품의 경우, 고객용/관리자용 - 권한 확인 없음)
     * @param productNo 상품 번호
     * @return 재고 정보
     */
    public InventoryResponseDto getInventoryByProductNoPublic(Long productNo) {
        // 상품 조회
        ProductEntity product = productRepository.findById(productNo)
                .orElse(null);

        if (product == null) {
            // 상품이 없으면 재고 정보 없음
            return InventoryResponseDto.builder()
                    .inventoryNo(null)
                    .productNo(null)
                    .productName(null)
                    .optionNo(null)
                    .color(null)
                    .size(null)
                    .stockQuantity(null) // 재고 정보 없음
                    .inStock(null) // 재고 정보 없음
                    .build();
        }

        // 옵션이 있는 상품인지 확인
        List<OptionEntity> options = optionRepository.findByProduct_ProductNo(productNo);
        boolean hasOptions = !options.isEmpty();

        if (hasOptions) {
            // 옵션이 있는 상품은 product 레벨 재고 없음
            return InventoryResponseDto.builder()
                    .inventoryNo(null)
                    .productNo(product.getProductNo())
                    .productName(product.getProductName())
                    .optionNo(null)
                    .color(null)
                    .size(null)
                    .stockQuantity(null) // 재고 정보 없음
                    .inStock(null) // 재고 정보 없음
                    .build();
        }

        // 재고 조회 (없으면 재고 0으로 간주)
        InventoryEntity inventory = inventoryRepository.findByProduct_ProductNo(productNo)
                .orElse(null);

        if (inventory == null) {
            // Inventory 레코드가 없으면 재고 정보 없음 (null 반환)
            return InventoryResponseDto.builder()
                    .inventoryNo(null)
                    .productNo(product.getProductNo())
                    .productName(product.getProductName())
                    .optionNo(null) // 옵션이 없으므로 null
                    .color(null)   // 옵션이 없으므로 null
                    .size(null)    // 옵션이 없으므로 null
                    .stockQuantity(null) // 재고 정보 없음
                    .inStock(null) // 재고 정보 없음
                    .build();
        }

        return toInventoryResponseDto(inventory);
    }

    /**
     * 재고 생성 (옵션이 없는 상품의 경우)
     * @param session HTTP 세션
     * @param productNo 상품 번호
     * @param requestDto 재고 생성 요청 DTO
     * @return 생성된 재고 정보
     */
    @Transactional
    public InventoryResponseDto createInventoryForProduct(HttpSession session, Long productNo, InventoryUpdateRequestDto requestDto) {
        // 파트너 권한 확인
        authService.requireRole(session, AccountRole.PARTNER);
        
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long partnerId = currentUser.getSubjectId();
        
        // 상품 조회
        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        // 옵션이 있는 상품인지 확인
        List<OptionEntity> options = optionRepository.findByProduct_ProductNo(productNo);
        boolean hasOptions = !options.isEmpty();
        
        if (hasOptions) {
            // 옵션이 있는 상품은 product 엔드포인트 사용 불가
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "옵션이 있는 상품은 옵션 단위 재고 관리 API를 사용해주세요.");
        }
        
        // 옵션이 없는 상품: ProductEntity.partner로 소유권 확인
        if (product.getPartner() == null || !product.getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품만 재고를 생성할 수 있습니다.");
        }
        
        // 재고 수량 검증
        if (requestDto.getStockQuantity() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "재고 수량은 0 이상이어야 합니다.");
        }
        
        // 이미 재고가 존재하는지 확인
        if (inventoryRepository.findByProduct_ProductNo(productNo).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 재고가 존재합니다. 수정 API를 사용해주세요.");
        }
        
        // 재고 생성
        InventoryEntity inventory = InventoryEntity.builder()
                .product(product)
                .inventoryStock(requestDto.getStockQuantity())
                .build();
        inventory = inventoryRepository.save(inventory);
        
        return toInventoryResponseDto(inventory);
    }

    /**
     * 재고 수정 (옵션이 없는 상품의 경우)
     * @param session HTTP 세션
     * @param productNo 상품 번호
     * @param requestDto 재고 수정 요청 DTO
     * @return 수정된 재고 정보
     */
    @Transactional
    public InventoryResponseDto updateInventoryForProduct(HttpSession session, Long productNo, InventoryUpdateRequestDto requestDto) {
        // 파트너 권한 확인
        authService.requireRole(session, AccountRole.PARTNER);
        
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        Long partnerId = currentUser.getSubjectId();
        
        // 상품 조회
        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        // 옵션이 있는 상품인지 확인
        List<OptionEntity> options = optionRepository.findByProduct_ProductNo(productNo);
        boolean hasOptions = !options.isEmpty();
        
        if (hasOptions) {
            // 옵션이 있는 상품은 product 엔드포인트 사용 불가
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "옵션이 있는 상품은 옵션 단위 재고 관리 API를 사용해주세요.");
        }
        
        // 옵션이 없는 상품: ProductEntity.partner로 소유권 확인
        if (product.getPartner() == null || !product.getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "자신의 상품만 수정할 수 있습니다.");
        }
        
        // 재고 수량 검증
        if (requestDto.getStockQuantity() < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "재고 수량은 0 이상이어야 합니다.");
        }
        
        // 재고 조회 또는 생성
        InventoryEntity inventory = inventoryRepository.findByProduct_ProductNo(productNo)
                .orElse(null);
        
        if (inventory == null) {
            // Inventory 레코드가 없으면 새로 생성 (하위 호환성 유지)
            inventory = InventoryEntity.builder()
                    .product(product)
                    .inventoryStock(requestDto.getStockQuantity())
                    .build();
            inventory = inventoryRepository.save(inventory);
        } else {
            // 기존 재고 수정
            inventory.updateStock(requestDto.getStockQuantity());
            inventory = inventoryRepository.save(inventory);
        }
        
        return toInventoryResponseDto(inventory);
    }

    /**
     * InventoryEntity를 InventoryResponseDto로 변환
     */
    private InventoryResponseDto toInventoryResponseDto(InventoryEntity inventory) {
        if (inventory.getOption() != null) {
            // 옵션이 있는 상품
            OptionEntity option = inventory.getOption();
            return InventoryResponseDto.builder()
                    .inventoryNo(inventory.getInventoryNo())
                    .productNo(option.getProduct().getProductNo())
                    .productName(option.getProduct().getProductName())
                    .optionNo(option.getOptionNo())
                    .color(option.getColor())
                    .size(option.getSize())
                    .stockQuantity(inventory.getInventoryStock())
                    .inStock(inventory.isInStock())
                    .build();
        } else {
            // 옵션이 없는 상품
            ProductEntity product = inventory.getProduct();
            return InventoryResponseDto.builder()
                    .inventoryNo(inventory.getInventoryNo())
                    .productNo(product.getProductNo())
                    .productName(product.getProductName())
                    .optionNo(null)
                    .color(null)
                    .size(null)
                    .stockQuantity(inventory.getInventoryStock())
                    .inStock(inventory.isInStock())
                    .build();
        }
    }

}
