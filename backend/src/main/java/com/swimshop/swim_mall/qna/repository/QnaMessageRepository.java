package com.swimshop.swim_mall.qna.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swimshop.swim_mall.qna.entity.QnaEntity;
import com.swimshop.swim_mall.qna.entity.QnaMessageEntity;

@Repository
public interface QnaMessageRepository extends JpaRepository<QnaMessageEntity, Long> {

    List<QnaMessageEntity> findByQnaOrderByCreatedAtAsc(QnaEntity qna);

    long countByQna(QnaEntity qna);
}
