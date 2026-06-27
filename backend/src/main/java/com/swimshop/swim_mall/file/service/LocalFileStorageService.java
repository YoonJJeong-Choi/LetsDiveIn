package com.swimshop.swim_mall.file.service;

import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.file.dto.FileUploadResponseDto;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;

@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    private final String publicBaseUrl;
    private final Path uploadRoot;
    private final UploadedFileRepository uploadedFileRepository;
    private final int maxImageDimension;

    public LocalFileStorageService(
            @Value("${app.public-base-url:${app.base-url:http://localhost:8080}}") String publicBaseUrl,
            @Value("${app.file.upload-dir:uploads}") String uploadDir,
            @Value("${app.file.max-image-dimension:768}") int maxImageDimension,
            UploadedFileRepository uploadedFileRepository
    ) {
        this.publicBaseUrl = publicBaseUrl;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxImageDimension = maxImageDimension;
        this.uploadedFileRepository = uploadedFileRepository;
        log.info("File upload resize config initialized. maxImageDimension={}", this.maxImageDimension);
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

            byte[] fileBytes = toUploadBytes(file, contentType, extension);
            Files.write(targetPath, fileBytes);

            UploadedFileEntity saved = uploadedFileRepository.save(UploadedFileEntity.builder()
                    .category(safeCategory)
                    .originalName(originalName)
                    .storedName(storedName)
                    .contentType(contentType)
                    .size((long) fileBytes.length)
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

    @Override
    public Resource download(UploadedFileEntity file) {
        Resource resource = new FileSystemResource(file.getStoragePath());
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파일이 존재하지 않습니다.");
        }
        return resource;
    }

    @Override
    public void delete(UploadedFileEntity file) {
        if (file == null || file.getStoragePath() == null || file.getStoragePath().isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(file.getStoragePath()));
        } catch (IOException e) {
            log.warn("Failed to delete local file. fileId={} reason={}", file.getFileId(), e.getMessage());
        }
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

    private byte[] toUploadBytes(MultipartFile file, String contentType, String extension) throws IOException {
        byte[] original = file.getBytes();
        if (maxImageDimension <= 0) {
            log.info("Image resize skipped: maxImageDimension<=0. contentType={} bytes={}", contentType, original.length);
            return original;
        }
        if (!contentType.startsWith("image/")) {
            log.info("Image resize skipped: non-image contentType={} bytes={}", contentType, original.length);
            return original;
        }
        if (!("jpg".equals(extension) || "jpeg".equals(extension) || "png".equals(extension))) {
            log.info("Image resize skipped: unsupported extension={} contentType={} bytes={}", extension, contentType, original.length);
            return original;
        }
        return resizeImageBytes(original, extension);
    }

    private byte[] resizeImageBytes(byte[] original, String extension) throws IOException {
        try (InputStream in = new ByteArrayInputStream(original)) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                log.info("Image resize skipped: unreadable image bytes={}", original.length);
                return original;
            }

            int width = src.getWidth();
            int height = src.getHeight();
            int longEdge = Math.max(width, height);
            if (longEdge <= maxImageDimension) {
                log.info(
                        "Image resize skipped: already within limit. width={} height={} limit={} bytes={}",
                        width, height, maxImageDimension, original.length);
                return original;
            }

            double scale = (double) maxImageDimension / longEdge;
            int targetW = Math.max(1, (int) Math.round(width * scale));
            int targetH = Math.max(1, (int) Math.round(height * scale));

            int imageType = "png".equals(extension) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
            BufferedImage resized = new BufferedImage(targetW, targetH, imageType);
            Graphics2D g2d = resized.createGraphics();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(src, 0, 0, targetW, targetH, null);
            } finally {
                g2d.dispose();
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                String format = "png".equals(extension) ? "png" : "jpeg";
                boolean written = ImageIO.write(resized, format, out);
                if (!written) {
                    log.info("Image resize skipped: ImageIO write failed. format={} bytes={}", format, original.length);
                    return original;
                }
                log.info(
                        "Image resized. from={}x{} to={}x{} limit={} bytes={} -> {}",
                        width, height, targetW, targetH, maxImageDimension, original.length, out.size());
                return out.toByteArray();
            }
        }
    }
}
