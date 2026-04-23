package com.swimshop.swim_mall.file.repository;

import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFileEntity, Long> {
}
