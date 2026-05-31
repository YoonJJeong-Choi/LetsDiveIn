package com.swimshop.swim_mall.file.service;

import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.file.dto.FileUploadResponseDto;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileUploadResponseDto upload(MultipartFile file, String category, AccountRole uploaderRole, Long uploaderSubjectId);
    UploadedFileEntity getById(Long fileId);
    Resource download(UploadedFileEntity file);
    void delete(UploadedFileEntity file);
}
