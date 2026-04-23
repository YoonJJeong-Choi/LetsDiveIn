package com.swimshop.swim_mall.file.service;

import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.file.dto.FileUploadResponseDto;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    private final String publicBaseUrl;
    private final Path uploadRoot;
    private final UploadedFileRepository uploadedFileRepository;

    public LocalFileStorageService(
            @Value("${app.base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${app.file.upload-dir:uploads}") String uploadDir,
            UploadedFileRepository uploadedFileRepository
    ) {
        this.publicBaseUrl = publicBaseUrl;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.uploadedFileRepository = uploadedFileRepository;
    }

    @Override
    public FileUploadResponseDto upload(MultipartFile file, String category, AccountRole uploaderRole, Long uploaderSubjectId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "업로드할 파일이 없습니다.");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().trim() : "";
        String extension = extractExtension(originalName);
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "허용되지 않는 파일 확장자입니다.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "허용되지 않는 파일 타입입니다.");
        }

        String safeCategory = normalizeCategory(category);
        boolean isPrivate = "partner-doc".equals(safeCategory) || "partner-application-doc".equals(safeCategory);
        String visibilityRoot = isPrivate ? "private" : "public";
        String storedName = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = uploadRoot.resolve(visibilityRoot).resolve(safeCategory).normalize();
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(storedName).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "잘못된 파일 경로입니다.");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            UploadedFileEntity saved = uploadedFileRepository.save(UploadedFileEntity.builder()
                    .category(safeCategory)
                    .originalName(originalName)
                    .storedName(storedName)
                    .contentType(contentType)
                    .size(file.getSize())
                    .storagePath(targetPath.toString())
                    .isPrivate(isPrivate)
                    .uploadedByRole(uploaderRole)
                    .uploadedBySubjectId(uploaderSubjectId)
                    .build());

            String base = publicBaseUrl.replaceAll("/$", "");
            String fileUrl = isPrivate
                    ? String.format("%s/api/files/%d/download", base, saved.getFileId())
                    : String.format("%s/uploads/%s/%s", base, safeCategory, storedName);

            return FileUploadResponseDto.builder()
                    .fileId(saved.getFileId())
                    .fileUrl(fileUrl)
                    .originalName(originalName)
                    .storedName(storedName)
                    .contentType(contentType)
                    .size(file.getSize())
                    .category(safeCategory)
                    .build();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }
    }

    @Override
    public UploadedFileEntity getById(Long fileId) {
        return uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "파일을 찾을 수 없습니다."));
    }

    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파일 확장자가 필요합니다.");
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "common";
        }
        return category.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    }
}
