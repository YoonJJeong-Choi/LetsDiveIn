package com.swimshop.swim_mall.account.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.common.enums.AccountRole;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<AccountEntity> findByCustomer_CustomerId(Long customerId);

    Optional<AccountEntity> findByPartner_PartnerId(Long partnerId);

    Optional<AccountEntity> findByAdmin_AdminId(Long adminId);

    Optional<AccountEntity> findByEmailVerifyToken(String token);
}
