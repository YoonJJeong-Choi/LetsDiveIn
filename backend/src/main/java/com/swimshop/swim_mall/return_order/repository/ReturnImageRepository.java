package com.swimshop.swim_mall.return_order.repository;

import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnImageRepository extends JpaRepository<ReturnImageEntity, Long> {
    List<ReturnImageEntity> findByReturnEntityOrderByIdAsc(ReturnEntity returnEntity);
}
