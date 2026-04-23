package com.swimshop.swim_mall.cart.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.cart.entity.CartEntity;
import com.swimshop.swim_mall.cart.entity.CartItemEntity;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.product.entity.ProductEntity;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    
    // 장바구니별 아이템 목록 조회
    List<CartItemEntity> findByCart(CartEntity cart);
    
    // 장바구니, 상품, 옵션으로 아이템 조회 (중복 체크용)
    Optional<CartItemEntity> findByCartAndProductAndOption(
        CartEntity cart, 
        ProductEntity product, 
        OptionEntity option
    );
    
    // 장바구니, 상품으로 아이템 조회 (옵션 없는 경우)
    Optional<CartItemEntity> findByCartAndProductAndOptionIsNull(
        CartEntity cart, 
        ProductEntity product
    );
}
