package com.swimshop.swim_mall.qna.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.common.enums.QnaStatus;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.qna.entity.QnaEntity;

@Repository
public interface QnaRepository extends JpaRepository<QnaEntity, Long> {

    @Query("SELECT q FROM QnaEntity q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.partner " +
           "LEFT JOIN FETCH q.order " +
           "LEFT JOIN FETCH q.orderItem oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE q.customer = :customer " +
           "ORDER BY q.createdAt DESC")
    List<QnaEntity> findByCustomerOrderByCreatedAtDesc(@Param("customer") CustomerEntity customer);

    @Query("SELECT q FROM QnaEntity q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.partner " +
           "LEFT JOIN FETCH q.order " +
           "LEFT JOIN FETCH q.orderItem oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE q.partner IS NULL " +
           "ORDER BY q.createdAt DESC")
    List<QnaEntity> findPlatformQnaOrderByCreatedAtDesc();

    @Query("SELECT q FROM QnaEntity q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.partner " +
           "LEFT JOIN FETCH q.order " +
           "LEFT JOIN FETCH q.orderItem oi " +
           "LEFT JOIN FETCH oi.product " +
           "ORDER BY q.createdAt DESC")
    List<QnaEntity> findAllWithRelationsOrderByCreatedAtDesc();

    @Query("SELECT q FROM QnaEntity q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.partner " +
           "LEFT JOIN FETCH q.order " +
           "LEFT JOIN FETCH q.orderItem oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE q.partner.partnerId = :partnerId " +
           "ORDER BY q.createdAt DESC")
    List<QnaEntity> findByPartnerIdOrderByCreatedAtDesc(@Param("partnerId") Long partnerId);

    @Query("SELECT q FROM QnaEntity q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.partner " +
           "LEFT JOIN FETCH q.order " +
           "LEFT JOIN FETCH q.orderItem oi " +
           "LEFT JOIN FETCH oi.product " +
           "WHERE q.qnaNo = :qnaNo")
    Optional<QnaEntity> findByIdWithRelations(@Param("qnaNo") Long qnaNo);

    /** AI 답변 초안용 — 주문상품·옵션·배송·반품까지 한 번에 로드 */
    @Query("SELECT q FROM QnaEntity q " +
           "LEFT JOIN FETCH q.customer " +
           "LEFT JOIN FETCH q.partner " +
           "LEFT JOIN FETCH q.order " +
           "LEFT JOIN FETCH q.orderItem oi " +
           "LEFT JOIN FETCH oi.product " +
           "LEFT JOIN FETCH oi.option " +
           "LEFT JOIN FETCH oi.delivery " +
           "LEFT JOIN FETCH oi.returnEntity " +
           "WHERE q.qnaNo = :qnaNo")
    Optional<QnaEntity> findByIdForDraftAssist(@Param("qnaNo") Long qnaNo);

    List<QnaEntity> findByCustomerAndStatusOrderByCreatedAtDesc(CustomerEntity customer, QnaStatus status);

    List<QnaEntity> findByCustomerAndCategoryOrderByCreatedAtDesc(CustomerEntity customer, InquiryCategory category);
}
