package com.swimshop.swim_mall.file.controller;

import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.file.dto.FileUploadResponseDto;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.service.FileStorageService;
import com.swimshop.swim_mall.file.service.SignedFileUrlService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@RequestMapping("/api/files")
@RequiredArgsConstructor
@RestController
public class FileController {

    private final AuthService authService;
    private final FileStorageService fileStorageService;
    private final SignedFileUrlService signedFileUrlService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> upload(
            HttpSession session,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String category
    ) {
        String normalized = category == null ? "common" : category.trim().toLowerCase(Locale.ROOT);

        // 비로그인 허용: 파트너 입점 신청 전용 카테고리
        if ("partner-application-doc".equals(normalized)) {
            // 비로그인 업로드는 업로더를 'CUSTOMER/0'로 표준 저장
            FileUploadResponseDto uploaded = fileStorageService.upload(
                    file, normalized, AccountRole.CUSTOMER, 0L
            );
            return ResponseEntity.ok(ApiResponse.success(uploaded));
        }

        // 그 외는 로그인 필수
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        validateUploadPermission(currentUser.getRole(), normalized);
        FileUploadResponseDto uploaded = fileStorageService.upload(
                file, normalized, currentUser.getRole(), currentUser.getSubjectId()
        );
        return ResponseEntity.ok(ApiResponse.success(uploaded));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(
            HttpSession session,
            @PathVariable Long fileId,
            @RequestParam(required = false) Long exp,
            @RequestParam(required = false) String sig) {
        UploadedFileEntity file = fileStorageService.getById(fileId);
        boolean signedAllowed = signedFileUrlService.isValidSignedDownloadRequest(fileId, exp, sig);
        if (!signedAllowed) {
            AuthLoginResponseDto currentUser = authService.getCurrentUser(session); // 로그인 필수
            validateDownloadPermission(currentUser, file);
        }

        Resource resource = fileStorageService.download(file);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.getOriginalName())
                        .build().toString())
                .body(resource);
    }

    private void validateUploadPermission(AccountRole role, String category) {
        String normalized = category == null ? "common" : category.trim().toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "product" -> {
                if (role != AccountRole.PARTNER && role != AccountRole.ADMIN) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            case "profile" -> {
                // 파트너 프로필 이미지는 PARTNER/ADMIN 허용 (공개 저장)
                if (role != AccountRole.PARTNER && role != AccountRole.ADMIN) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            case "event" -> {
                if (role != AccountRole.ADMIN) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            case "event-banner", "event-thumbnail" -> {
                if (role != AccountRole.ADMIN) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            case "partner-doc" -> {
                if (role != AccountRole.PARTNER && role != AccountRole.ADMIN) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            case "partner-application-doc" -> {
                // 위 upload()에서 이미 비로그인 허용 처리. 여기까지 오면 잘못된 흐름
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "partner-application-doc은 별도 처리됩니다.");
            }
            case "review" -> {
                if (role != AccountRole.CUSTOMER) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            case "review-image" -> {
                if (role != AccountRole.CUSTOMER) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            case "return-image" -> {
                if (role != AccountRole.CUSTOMER) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            case "common" -> {
                if (role != AccountRole.ADMIN) {
                    throw new BusinessException(ErrorCode.FORBIDDEN);
                }
            }
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "허용되지 않은 category입니다.");
        }
    }

    private void validateDownloadPermission(AuthLoginResponseDto currentUser, UploadedFileEntity file) {
        if (!Boolean.TRUE.equals(file.getIsPrivate())) {
            return; // public 파일은 로그인 사용자면 조회 가능
        }

        if ("partner-doc".equals(file.getCategory())) {
            if (currentUser.getRole() == AccountRole.ADMIN) {
                return;
            }
            boolean isOwnerPartner = currentUser.getRole() == AccountRole.PARTNER
                    && file.getUploadedByRole() == AccountRole.PARTNER
                    && file.getUploadedBySubjectId().equals(currentUser.getSubjectId());
            if (isOwnerPartner) {
                return;
            }
        }
        // 입점 신청 파일은 관리자만 다운로드 가능
        if ("partner-application-doc".equals(file.getCategory())) {
            if (currentUser.getRole() == AccountRole.ADMIN) {
                return;
            }
        }

        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
