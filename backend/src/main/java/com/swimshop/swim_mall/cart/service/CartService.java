package com.swimshop.swim_mall.cart.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.cart.dto.CartItemRequestDto;
import com.swimshop.swim_mall.cart.dto.CartItemResponseDto;
import com.swimshop.swim_mall.cart.dto.CartItemUpdateDto;
import com.swimshop.swim_mall.cart.dto.CartResponseDto;
import com.swimshop.swim_mall.cart.entity.CartEntity;
import com.swimshop.swim_mall.cart.entity.CartItemEntity;
import com.swimshop.swim_mall.cart.repository.CartItemRepository;
import com.swimshop.swim_mall.cart.repository.CartRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.inventory.service.InventoryService;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.option.repository.OptionRepository;
import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.product.repository.ProductRepository;
import com.swimshop.swim_mall.product.service.ProductCustomerImageUrlResolver;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;
    private final AuthService authService;
    private final InventoryService inventoryService;
    private final ProductCustomerImageUrlResolver productCustomerImageUrlResolver;

    /**
     * 고객의 장바구니 조회 (없으면 생성)
     */
    @Transactional
    public CartEntity getOrCreateCart(Long customerId) {
        Optional<CartEntity> cartOpt = cartRepository.findByCustomer_CustomerId(customerId);
        
        if (cartOpt.isPresent()) {
            return cartOpt.get();
        }
        
        // 장바구니가 없으면 생성
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        CartEntity cart = CartEntity.builder()
                .customer(customer)
                .build();
        
        return cartRepository.save(cart);
    }

    /**
     * 장바구니 전체 조회
     */
    @Transactional
    public CartResponseDto getCart(HttpSession session) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 세션에서 고객 ID (통합 로그인 시 customerId 또는 subjectId)
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId"); // 통합 로그인(CUSTOMER) 시 subjectId
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        CartEntity cart = getOrCreateCart(customerId);
        List<CartItemEntity> items = cartItemRepository.findByCart(cart);
        
        List<CartItemResponseDto> itemDtos = new ArrayList<>();
        Long totalPrice = 0L;
        
        for (CartItemEntity item : items) {
            Long itemTotalPrice = item.getItemPrice() * item.getQuantity();
            totalPrice += itemTotalPrice;
            
            itemDtos.add(new CartItemResponseDto(
                    item.getCartItemNo(),
                    item.getProduct().getProductNo(),
                    item.getProduct().getProductName(),
                    productCustomerImageUrlResolver.resolveDisplayUrlOrEmpty(item.getProduct()),
                    item.getOption() != null ? item.getOption().getOptionNo() : null,
                    item.getOption() != null ? item.getOption().getColor() : null,
                    item.getOption() != null ? item.getOption().getSize() : null,
                    item.getQuantity(),
                    item.getItemPrice(),
                    itemTotalPrice
            ));
        }
        
        return new CartResponseDto(cart.getCartNo(), itemDtos, totalPrice);
    }

    /**
     * 장바구니에 아이템 추가
     */
    @Transactional
    public void addItem(HttpSession session, CartItemRequestDto requestDto) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId");
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        CartEntity cart = getOrCreateCart(customerId);
        
        ProductEntity product = productRepository.findById(requestDto.getProductNo())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        // 상품에 옵션이 있는지 확인
        List<OptionEntity> productOptions = optionRepository.findByProduct_ProductNo(product.getProductNo());
        boolean hasOptions = productOptions != null && !productOptions.isEmpty();
        
        // 옵션이 있는 상품인데 optionNo가 null이면 에러
        if (hasOptions && requestDto.getOptionNo() == null) {
            throw new BusinessException(ErrorCode.OPTION_REQUIRED);
        }
        
        OptionEntity option = null;
        if (requestDto.getOptionNo() != null) {
            option = optionRepository.findById(requestDto.getOptionNo())
                    .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
            
            // 선택한 옵션이 해당 상품의 옵션인지 확인
            if (!productOptions.contains(option)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }
        
        // 재고 검증 (장바구니 추가 전)
        Long optionNoForInventory = option != null ? option.getOptionNo() : null;
        Long productNoForInventory = option == null ? product.getProductNo() : null;
        inventoryService.validateStockAvailability(optionNoForInventory, productNoForInventory, requestDto.getQuantity());
        
        // 가격 계산: 기본 가격 + 옵션 추가 가격
        Long basePrice = Long.parseLong(product.getProductPrice());
        Long optionAddPrice = (option != null && option.getOptionAddPrice() != null) 
                ? option.getOptionAddPrice() : 0L;
        Long calculatedPrice = basePrice + optionAddPrice;
        
        // 같은 상품+옵션 조합이 이미 있는지 확인
        Optional<CartItemEntity> existingItem;
        if (option != null) {
            existingItem = cartItemRepository.findByCartAndProductAndOption(cart, product, option);
        } else {
            existingItem = cartItemRepository.findByCartAndProductAndOptionIsNull(cart, product);
        }
        
        if (existingItem.isPresent()) {
            // 이미 있으면 수량만 증가 (재고 검증 포함)
            CartItemEntity item = existingItem.get();
            Integer newQuantity = item.getQuantity() + requestDto.getQuantity();
            
            // 증가된 수량에 대해 재고 검증
            inventoryService.validateStockAvailability(optionNoForInventory, productNoForInventory, newQuantity);
            
            item.updateQuantity(newQuantity);
        } else {
            // 없으면 새로 추가 (계산된 가격 사용)
            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .product(product)
                    .option(option)
                    .quantity(requestDto.getQuantity())
                    .itemPrice(calculatedPrice) // 백엔드에서 계산한 가격 사용
                    .build();
            
            cartItemRepository.save(newItem);
        }
    }

    /**
     * 장바구니 아이템 수정 (수량, 옵션 동시 변경 가능)
     */
    @Transactional
    public void updateItem(HttpSession session, Long cartItemNo, CartItemUpdateDto updateDto) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId");
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 기존 아이템 조회 및 권한 확인
        CartItemEntity item = cartItemRepository.findById(cartItemNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        
        if (!item.getCart().getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 수량 수정 (재고 검증 포함)
        if (updateDto.getQuantity() != null) {
            if (updateDto.getQuantity() <= 0) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY);
            }
            
            // 재고 검증 (현재 옵션 기준)
            Long optionNoForInventory = item.getOption() != null ? item.getOption().getOptionNo() : null;
            Long productNoForInventory = item.getOption() == null ? item.getProduct().getProductNo() : null;
            inventoryService.validateStockAvailability(optionNoForInventory, productNoForInventory, updateDto.getQuantity());
            
            item.updateQuantity(updateDto.getQuantity());
        }

        // 옵션 변경 (optionNo가 명시적으로 전달된 경우만)
        if (updateDto.getOptionNo() != null) {
            Long newOptionNo = updateDto.getOptionNo();

            // 새 옵션 조회
            OptionEntity newOption = null;
            if (newOptionNo != null) {
                newOption = optionRepository.findById(newOptionNo)
                        .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
                
                // 같은 상품의 옵션인지 확인
                if (!newOption.getProduct().getProductNo().equals(item.getProduct().getProductNo())) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST);
                }
            }

            // 같은 상품+새 옵션 조합이 이미 장바구니에 있는지 확인
            Optional<CartItemEntity> existingItem;
            if (newOption != null) {
                existingItem = cartItemRepository.findByCartAndProductAndOption(
                        item.getCart(), item.getProduct(), newOption);
            } else {
                existingItem = cartItemRepository.findByCartAndProductAndOptionIsNull(
                        item.getCart(), item.getProduct());
            }

            if (existingItem.isPresent() && !existingItem.get().getCartItemNo().equals(cartItemNo)) {
                // 같은 상품+새 옵션 조합이 이미 있으면 수량 합치기
                CartItemEntity existing = existingItem.get();
                Integer mergedQuantity = existing.getQuantity() + item.getQuantity();
                // 수량도 함께 업데이트하는 경우 반영
                if (updateDto.getQuantity() != null) {
                    mergedQuantity = existing.getQuantity() + updateDto.getQuantity();
                }
                
                // 재고 검증 (새 옵션 기준)
                Long newOptionNoForInventory = newOption != null ? newOption.getOptionNo() : null;
                Long newProductNoForInventory = newOption == null ? item.getProduct().getProductNo() : null;
                inventoryService.validateStockAvailability(newOptionNoForInventory, newProductNoForInventory, mergedQuantity);
                
                existing.updateQuantity(mergedQuantity);
                // 현재 아이템 삭제
                cartItemRepository.delete(item);
                return; // 아이템이 삭제되었으므로 더 이상 처리하지 않음
            } else {
                // 없으면 현재 아이템의 옵션과 가격 업데이트
                // 재고 검증 (새 옵션 기준, 수량은 기존 수량 또는 업데이트된 수량)
                Integer quantityToCheck = updateDto.getQuantity() != null ? updateDto.getQuantity() : item.getQuantity();
                Long newOptionNoForInventory = newOption != null ? newOption.getOptionNo() : null;
                Long newProductNoForInventory = newOption == null ? item.getProduct().getProductNo() : null;
                inventoryService.validateStockAvailability(newOptionNoForInventory, newProductNoForInventory, quantityToCheck);
                
                Long basePrice = Long.parseLong(item.getProduct().getProductPrice());
                Long optionAddPrice = (newOption != null && newOption.getOptionAddPrice() != null) 
                        ? newOption.getOptionAddPrice() : 0L;
                Long calculatedPrice = basePrice + optionAddPrice;
                
                item.updateOptionAndPrice(newOption, calculatedPrice);
            }
        }
    }

    /**
     * 장바구니 아이템 삭제
     */
    @Transactional
    public void removeItem(HttpSession session, Long cartItemNo) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId");
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        CartItemEntity item = cartItemRepository.findById(cartItemNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        
        // 본인의 장바구니인지 확인
        if (!item.getCart().getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
        cartItemRepository.delete(item);
    }

    /**
     * 주문에 담긴 상품·옵션과 일치하는 장바구니 라인만 제거 (결제 승인 후).
     */
    @Transactional
    public void removeOrderedLinesFromCart(OrderEntity order) {
        var cartOpt = cartRepository.findByCustomer(order.getCustomer());
        if (cartOpt.isEmpty()) {
            return;
        }
        var cart = cartOpt.get();
        var cartItems = cartItemRepository.findByCart(cart);
        var orderItems = order.getOrderItems();
        var toDelete = new ArrayList<CartItemEntity>();
        for (var ci : cartItems) {
            boolean matches = orderItems.stream().anyMatch(oi -> {
                Long oiProd = oi.getProduct() != null ? oi.getProduct().getProductNo() : null;
                Long oiOpt = oi.getOption() != null ? oi.getOption().getOptionNo() : null;
                Long ciProd = ci.getProduct() != null ? ci.getProduct().getProductNo() : null;
                Long ciOpt = ci.getOption() != null ? ci.getOption().getOptionNo() : null;
                return java.util.Objects.equals(oiProd, ciProd) && java.util.Objects.equals(oiOpt, ciOpt);
            });
            if (matches) {
                toDelete.add(ci);
            }
        }
        if (!toDelete.isEmpty()) {
            cartItemRepository.deleteAll(toDelete);
        }
    }
    
    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return (Long) value;
    }
}
