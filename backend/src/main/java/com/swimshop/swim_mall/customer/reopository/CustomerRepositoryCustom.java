package com.swimshop.swim_mall.customer.reopository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.common.enums.CustomerGradeEnum;

public interface CustomerRepositoryCustom {
    
    /**
     * 관리자용 고객 목록 조회 (페이징, 검색, 필터링)
     * 
     * @param pageable 페이징 정보
     * @param searchKeyword 검색 키워드 (이름 또는 이메일)
     * @param emailVerified 이메일 인증 여부 필터 (null이면 전체)
     * @return 고객 목록 (페이징)
     */
    Page<CustomerEntity> findCustomersForAdmin(
            Pageable pageable,
            String searchKeyword,
            Boolean emailVerified,
            CustomerGradeEnum grade
    );
    
    /**
     * 관리자용 고객 목록 조회 (통계 포함)
     * 
     * @param pageable 페이징 정보
     * @param searchKeyword 검색 키워드 (이름 또는 이메일)
     * @param emailVerified 이메일 인증 여부 필터 (null이면 전체)
     * @return 고객 목록 (주문/리뷰/반품 통계 포함)
     */
    List<CustomerEntity> findCustomersWithStatistics(
            Pageable pageable,
            String searchKeyword,
            Boolean emailVerified,
            CustomerGradeEnum grade
    );
}
