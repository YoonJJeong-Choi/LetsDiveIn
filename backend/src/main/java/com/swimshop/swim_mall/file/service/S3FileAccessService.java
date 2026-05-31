package com.swimshop.swim_mall.file.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import com.swimshop.swim_mall.return_order.entity.ReturnImageEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class S3FileAccessService implements FileAccessService {

    private final UploadedFileRepository uploadedFileRepository;
    private final S3StorageService s3StorageService;

    @Override
    public String getAiReadableUrl(ReturnImageEntity returnImage) {
        if (returnImage == null) {
            throw new IllegalArgumentException("returnImage is required");
        }
        UploadedFileEntity file = returnImage.getFile();
        if (file == null) {
            throw new IllegalStateException("return image file is missing");
        }
        return getAiReadableUrl(file.getFileId());
    }

    @Override
    public String getAiReadableUrl(Long fileId) {
        if (fileId == null) {
            throw new IllegalArgumentException("fileId is required");
        }
        UploadedFileEntity file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalStateException("uploaded file not found: " + fileId));
        return s3StorageService.createPresignedGetUrl(file.getStoragePath());
    }
}
