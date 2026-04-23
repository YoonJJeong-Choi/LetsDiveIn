package com.swimshop.swim_mall.customer.reopository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long>, CustomerRepositoryCustom {

    // 이메일로 회원 조회
    java.util.Optional<CustomerEntity> findByCustomerEmail(String email);

    // 이메일 중복 확인
    boolean existsByCustomerEmail(String email);

    // 인증 토큰으로 회원 조회
    java.util.Optional<CustomerEntity> findByEmailCheckToken(String token);
    
    // 특정 등급을 사용하는 고객 수 조회
    long countByCustomerGrade_GradeId(Long gradeId);
}
