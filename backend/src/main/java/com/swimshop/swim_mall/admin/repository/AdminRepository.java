package com.swimshop.swim_mall.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.admin.entity.AdminEntity;

@Repository
public interface AdminRepository extends JpaRepository<AdminEntity, Long> {
}
