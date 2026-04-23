package com.swimshop.swim_mall.customer.reopository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerTagMappingEntity;

@Repository
public interface CustomerTagMappingRepository extends JpaRepository<CustomerTagMappingEntity, Long> {
    
    /**
     * 고객별 태그 조회
     */
    @Query("SELECT m FROM CustomerTagMappingEntity m " +
           "LEFT JOIN FETCH m.tag t " +
           "WHERE m.customer.customerId = :customerId")
    List<CustomerTagMappingEntity> findByCustomer_CustomerId(@Param("customerId") Long customerId);
    
    /**
     * 태그별 고객 수 조회
     */
    @Query("SELECT COUNT(m) FROM CustomerTagMappingEntity m WHERE m.tag.tagId = :tagId")
    long countByTag_TagId(@Param("tagId") Long tagId);
    
    /**
     * 고객-태그 매핑 삭제
     */
    void deleteByCustomer_CustomerIdAndTag_TagId(Long customerId, Long tagId);
    
    /**
     * 고객의 모든 태그 삭제
     */
    void deleteByCustomer_CustomerId(Long customerId);
}
