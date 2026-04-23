package com.swimshop.swim_mall.customer.reopository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import com.swimshop.swim_mall.customer.entity.CustomerGradeEntity;

public class CustomerRepositoryCustomImpl implements CustomerRepositoryCustom {
    
    private final EntityManager entityManager;
    
    public CustomerRepositoryCustomImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    @Override
    public Page<CustomerEntity> findCustomersForAdmin(
            Pageable pageable,
            String searchKeyword,
            Boolean emailVerified,
            com.swimshop.swim_mall.common.enums.CustomerGradeEnum grade
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CustomerEntity> query = cb.createQuery(CustomerEntity.class);
        Root<CustomerEntity> root = query.from(CustomerEntity.class);
        
        // 조건 생성
        Predicate predicate = cb.conjunction();
        
        // 검색 키워드 필터
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            String keyword = "%" + searchKeyword.trim() + "%";
            Predicate namePredicate = cb.like(root.get("customerName"), keyword);
            Predicate emailPredicate = cb.like(root.get("customerEmail"), keyword);
            predicate = cb.and(predicate, cb.or(namePredicate, emailPredicate));
        }
        
        // 이메일 인증 여부 필터
        if (emailVerified != null) {
            predicate = cb.and(predicate, cb.equal(root.get("emailChecked"), emailVerified));
        }

        // 등급 필터 (명시적 조인 필요)
        if (grade != null) {
            Join<CustomerEntity, CustomerGradeEntity> gradeJoin = root.join("customerGrade", JoinType.LEFT);
            predicate = cb.and(predicate, cb.equal(gradeJoin.get("gradeName"), grade));
        }
        
        query.where(predicate);
        query.orderBy(cb.desc(root.get("customerCreateAt")));
        
        // 전체 개수 조회 (count 쿼리에서도 같은 조인 필요)
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<CustomerEntity> countRoot = countQuery.from(CustomerEntity.class);
        Predicate countPredicate = cb.conjunction();
        
        // 검색 키워드 필터
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            String keyword = "%" + searchKeyword.trim() + "%";
            Predicate namePredicate = cb.like(countRoot.get("customerName"), keyword);
            Predicate emailPredicate = cb.like(countRoot.get("customerEmail"), keyword);
            countPredicate = cb.and(countPredicate, cb.or(namePredicate, emailPredicate));
        }
        
        // 이메일 인증 여부 필터
        if (emailVerified != null) {
            countPredicate = cb.and(countPredicate, cb.equal(countRoot.get("emailChecked"), emailVerified));
        }
        
        // 등급 필터
        if (grade != null) {
            Join<CustomerEntity, CustomerGradeEntity> countGradeJoin = countRoot.join("customerGrade", JoinType.LEFT);
            countPredicate = cb.and(countPredicate, cb.equal(countGradeJoin.get("gradeName"), grade));
        }
        
        countQuery.select(cb.count(countRoot));
        countQuery.where(countPredicate);
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        // 페이징 조회
        TypedQuery<CustomerEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<CustomerEntity> results = typedQuery.getResultList();
        
        return new PageImpl<>(results, pageable, total);
    }
    
    @Override
    public List<CustomerEntity> findCustomersWithStatistics(
            Pageable pageable,
            String searchKeyword,
            Boolean emailVerified,
            com.swimshop.swim_mall.common.enums.CustomerGradeEnum grade
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<CustomerEntity> query = cb.createQuery(CustomerEntity.class);
        Root<CustomerEntity> root = query.from(CustomerEntity.class);
        
        // 조건 생성
        Predicate predicate = cb.conjunction();
        
        // 검색 키워드 필터
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            String keyword = "%" + searchKeyword.trim() + "%";
            Predicate namePredicate = cb.like(root.get("customerName"), keyword);
            Predicate emailPredicate = cb.like(root.get("customerEmail"), keyword);
            predicate = cb.and(predicate, cb.or(namePredicate, emailPredicate));
        }
        
        // 이메일 인증 여부 필터
        if (emailVerified != null) {
            predicate = cb.and(predicate, cb.equal(root.get("emailChecked"), emailVerified));
        }

        // 등급 필터 (명시적 조인 필요)
        if (grade != null) {
            Join<CustomerEntity, CustomerGradeEntity> gradeJoin = root.join("customerGrade", JoinType.LEFT);
            predicate = cb.and(predicate, cb.equal(gradeJoin.get("gradeName"), grade));
        }
        
        query.where(predicate);
        query.orderBy(cb.desc(root.get("customerCreateAt")));
        
        // 페이징 조회
        TypedQuery<CustomerEntity> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        return typedQuery.getResultList();
    }
}
