package com.swimshop.swim_mall.customer.reopository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerAddressEntity;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddressEntity, Long> {

    // 고객의 모든 주소 조회
    List<CustomerAddressEntity> findByCustomerOrderByIsDefaultDescCreatedAtDesc(CustomerEntity customer);

    // 고객의 기본 주소 조회
    Optional<CustomerAddressEntity> findByCustomerAndIsDefaultTrue(CustomerEntity customer);

    // 고객의 주소 개수 조회
    long countByCustomer(CustomerEntity customer);
}
