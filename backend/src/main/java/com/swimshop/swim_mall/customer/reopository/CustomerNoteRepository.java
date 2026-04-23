package com.swimshop.swim_mall.customer.reopository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerNoteEntity;

@Repository
public interface CustomerNoteRepository extends JpaRepository<CustomerNoteEntity, Long> {
    
    /**
     * 고객별 메모 조회 (최신순)
     */
    @Query("SELECT n FROM CustomerNoteEntity n " +
           "LEFT JOIN FETCH n.admin a " +
           "WHERE n.customer.customerId = :customerId " +
           "ORDER BY n.isImportant DESC, n.createdAt DESC")
    List<CustomerNoteEntity> findByCustomer_CustomerIdOrderByCreatedAtDesc(
        @Param("customerId") Long customerId
    );
    
    /**
     * 고객별 중요 메모만 조회
     */
    @Query("SELECT n FROM CustomerNoteEntity n " +
           "LEFT JOIN FETCH n.admin a " +
           "WHERE n.customer.customerId = :customerId AND n.isImportant = true " +
           "ORDER BY n.createdAt DESC")
    List<CustomerNoteEntity> findImportantNotesByCustomer_CustomerId(
        @Param("customerId") Long customerId
    );
}
