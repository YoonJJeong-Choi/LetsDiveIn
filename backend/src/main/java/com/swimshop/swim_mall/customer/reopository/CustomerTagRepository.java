package com.swimshop.swim_mall.customer.reopository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerTagEntity;

@Repository
public interface CustomerTagRepository extends JpaRepository<CustomerTagEntity, Long> {
    
    /**
     * 태그 이름으로 조회
     */
    Optional<CustomerTagEntity> findByTagName(String tagName);
    
    /**
     * 모든 태그 조회 (이름순)
     */
    List<CustomerTagEntity> findAllByOrderByTagNameAsc();
}
