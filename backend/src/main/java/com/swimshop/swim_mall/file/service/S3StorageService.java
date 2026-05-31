package com.swimshop.swim_mall.file.service;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.file.dto.FileUploadResponseDto;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class S3StorageService implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf"
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    @Value("${app.public-base-url:${app.base-url:http://localhost:8080}}")
    private String publicBaseUrl;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${s3.presigned-ttl-sec:600}")
    private long presignedTtlSec;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final UploadedFileRepository uploadedFileRepository;

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
        boolean isPrivate = "partner-doc".equals(safeCategory)
                || "partner-application-doc".equals(safeCategory)
                || "return-image".equals(safeCategory);
        String visibilityRoot = isPrivate ? "private" : "public";
        String storedName = UUID.randomUUID() + "." + extension;
        String objectKey = visibilityRoot + "/" + safeCategory + "/" + storedName;

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 읽기에 실패했습니다.");
        }

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) fileBytes.length)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 파일 저장에 실패했습니다.");
        }

        UploadedFileEntity saved = uploadedFileRepository.save(UploadedFileEntity.builder()
                .category(safeCategory)
                .originalName(originalName)
                .storedName(storedName)
                .contentType(contentType)
                .size((long) fileBytes.length)
                .storagePath(objectKey)
                .isPrivate(isPrivate)
                .uploadedByRole(uploaderRole)
                .uploadedBySubjectId(uploaderSubjectId)
                .build());

        String base = publicBaseUrl.replaceAll("/+$", "");
        String fileUrl = String.format("%s/api/files/%d/download", base, saved.getFileId());

        return FileUploadResponseDto.builder()
                .fileId(saved.getFileId())
                .fileUrl(fileUrl)
                .originalName(originalName)
                .storedName(storedName)
                .contentType(contentType)
                .size((long) fileBytes.length)
                .category(safeCategory)
                .build();
    }

    @Override
    public UploadedFileEntity getById(Long fileId) {
        return uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "파일을 찾을 수 없습니다."));
    }

    @Override
    public Resource download(UploadedFileEntity file) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(file.getStoragePath())
                    .build();
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
            return new ByteArrayResource(bytes.asByteArray());
        } catch (NoSuchKeyException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파일이 존재하지 않습니다.");
        } catch (S3Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 파일 조회에 실패했습니다.");
        }
    }

    @Override
    public void delete(UploadedFileEntity file) {
        if (file == null || file.getStoragePath() == null || file.getStoragePath().isBlank()) {
            return;
        }
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(file.getStoragePath())
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception ignored) {
        }
    }

    public String createPresignedGetUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(Math.max(60, presignedTtlSec)))
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
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
