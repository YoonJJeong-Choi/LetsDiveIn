package com.swimshop.swim_mall.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import com.swimshop.swim_mall.return_order.entity.ReturnImageEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileAccessService implements FileAccessService {

    private static final String LOCALHOST = "http://localhost:8080";

    private final UploadedFileRepository uploadedFileRepository;
    private final SignedFileUrlService signedFileUrlService;

    @Value("${app.public-base-url:${app.base-url:http://localhost:8080}}")
    private String appPublicBaseUrl;

    @Override
    public String getAiReadableUrl(ReturnImageEntity returnImage) {
        if (returnImage == null) {
            throw new IllegalArgumentException("returnImage is required");
        }
        UploadedFileEntity file = returnImage.getFile();
        if (file != null) {
            return buildAiReadableUrl(file);
        }
        String imageUrl = returnImage.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalStateException("return image url is missing");
        }
        return replaceLegacyLocalhost(imageUrl.trim());
    }

    @Override
    public String getAiReadableUrl(Long fileId) {
        if (fileId == null) {
            throw new IllegalArgumentException("fileId is required");
        }
        UploadedFileEntity file = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalStateException("uploaded file not found: " + fileId));
        return buildAiReadableUrl(file);
    }

    private String buildAiReadableUrl(UploadedFileEntity file) {
        if (file.getFileId() == null) {
            throw new IllegalStateException("uploaded file id is missing");
        }
        if (Boolean.TRUE.equals(file.getIsPrivate())) {
            return signedFileUrlService.createSignedDownloadUrl(file.getFileId());
        }
        String base = trimTrailingSlash(appPublicBaseUrl);
        return String.format("%s/uploads/%s/%s", base, file.getCategory(), file.getStoredName());
    }

    private String replaceLegacyLocalhost(String rawUrl) {
        String base = trimTrailingSlash(appPublicBaseUrl);
        if (rawUrl.startsWith(LOCALHOST)) {
            return base + rawUrl.substring(LOCALHOST.length());
        }
        return rawUrl;
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return LOCALHOST;
        }
        return baseUrl.replaceAll("/+$", "");
    }
}
