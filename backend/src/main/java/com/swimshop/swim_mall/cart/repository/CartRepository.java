package com.swimshop.swim_mall.cart.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.cart.entity.CartEntity;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, Long> {
    
    // 고객별 장바구니 조회
    Optional<CartEntity> findByCustomer(CustomerEntity customer);
    
    // 고객 ID로 장바구니 조회
    Optional<CartEntity> findByCustomer_CustomerId(Long customerId);
}
