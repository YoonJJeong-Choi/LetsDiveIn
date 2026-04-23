package com.swimshop.swim_mall.admin.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.account.repository.AccountRepository;
import com.swimshop.swim_mall.admin.dto.AdminBootstrapRequestDto;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.brand.entity.BrandEntity;
import com.swimshop.swim_mall.brand.repository.BrandRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.common.enums.AdminStatus;
import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.common.enums.PartnerHistoryActionType;
import com.swimshop.swim_mall.customer.service.EmailService;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.option.repository.OptionRepository;
import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.PartnerChangeRequestStatus;
import com.swimshop.swim_mall.partner.dto.PartnerApplicationResponseDto;
import com.swimshop.swim_mall.partner.dto.PartnerChangeRequestResponseDto;
import com.swimshop.swim_mall.partner.dto.PartnerHistoryResponseDto;
import com.swimshop.swim_mall.partner.entity.PartnerChangeRequestEntity;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.entity.PartnerHistoryEntity;
import com.swimshop.swim_mall.partner.repository.PartnerChangeRequestRepository;
import com.swimshop.swim_mall.partner.repository.PartnerHistoryRepository;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.product.dto.ProductListDto;
import com.swimshop.swim_mall.product.dto.ProductOptionDto;
import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.product.repository.ProductRepository;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.admin.dto.CustomerListResponseDto;
import com.swimshop.swim_mall.admin.dto.CustomerDetailResponseDto;
import com.swimshop.swim_mall.admin.dto.CustomerStatisticsDto;
import com.swimshop.swim_mall.admin.dto.AdminCustomerUpdateRequestDto;
import com.swimshop.swim_mall.admin.dto.CustomerHistoryDto;
import com.swimshop.swim_mall.admin.dto.SalesStatisticsDto;
import com.swimshop.swim_mall.admin.dto.PeriodComparisonDto;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderRepository;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.payment.PaymentRepository;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.ProductType;
import com.swimshop.swim_mall.review.entity.ReviewEntity;
import com.swimshop.swim_mall.review.repository.ReviewRepository;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.return_order.repository.ReturnRepository;
import com.swimshop.swim_mall.common.enums.OrderStatus;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.customer.entity.CustomerHistoryEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerHistoryRepository;
import com.swimshop.swim_mall.common.enums.CustomerHistoryActionType;
import com.swimshop.swim_mall.customer.entity.CustomerNoteEntity;
import com.swimshop.swim_mall.admin.dto.CustomerNoteDto;
import com.swimshop.swim_mall.admin.dto.CustomerNoteRequestDto;
import com.swimshop.swim_mall.customer.entity.CustomerTagEntity;
import com.swimshop.swim_mall.customer.entity.CustomerTagMappingEntity;
import com.swimshop.swim_mall.admin.dto.CustomerTagDto;
import com.swimshop.swim_mall.admin.dto.CustomerTagRequestDto;
import com.swimshop.swim_mall.customer.entity.CustomerActivityLogEntity;
import com.swimshop.swim_mall.admin.dto.CustomerActivityLogDto;
import com.swimshop.swim_mall.common.enums.CustomerActivityType;
import com.swimshop.swim_mall.customer.entity.CustomerGradeEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerGradeRepository;
import com.swimshop.swim_mall.admin.dto.CustomerGradeDto;
import com.swimshop.swim_mall.admin.dto.CustomerGradeRequestDto;

/**
 * 관리자 계정 생성.
 * 최초 1회만 부트스트랩(이메일+비번+이름)으로 Admin + Account 생성.
 */
@Transactional
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PartnerRepository partnerRepository;
    private final PartnerChangeRequestRepository partnerChangeRequestRepository;
    private final PartnerHistoryRepository partnerHistoryRepository;
    private final EmailService emailService;
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;
    private final AuthService authService;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final ReturnRepository returnRepository;
    private final BrandRepository brandRepository;
    private final com.swimshop.swim_mall.payment.PaymentRepository paymentRepository;
    private final CustomerHistoryRepository customerHistoryRepository;
    private final ObjectMapper objectMapper;
    private final com.swimshop.swim_mall.customer.reopository.CustomerNoteRepository customerNoteRepository;
    private final com.swimshop.swim_mall.customer.reopository.CustomerTagRepository customerTagRepository;
    private final com.swimshop.swim_mall.customer.reopository.CustomerTagMappingRepository customerTagMappingRepository;
    private final com.swimshop.swim_mall.customer.reopository.CustomerActivityLogRepository customerActivityLogRepository;
    private final CustomerGradeRepository customerGradeRepository;

    /**
     * 최초 관리자 1명만 생성. 이미 Admin이 있으면 예외.
     */
    public void bootstrapFirstAdmin(AdminBootstrapRequestDto dto) {
        if (adminRepository.count() > 0) {
            throw new IllegalStateException("이미 관리자가 존재합니다. 부트스트랩은 최초 1회만 가능합니다.");
        }
        if (accountRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("해당 이메일은 이미 사용 중입니다.");
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        LocalDateTime now = LocalDateTime.now();

        // 1) Account 먼저 생성 (로그인용)
        AccountEntity account = AccountEntity.builder()
                .email(dto.getEmail())
                .password(encodedPassword)
                .role(AccountRole.ADMIN)
                .emailVerified(true)
                .status("ACTIVE")
                // createdAt은 @PrePersist에서 자동 설정됨
                .build();
        accountRepository.save(account);

        // 2) Admin 생성 시 Account를 바로 전달
        AdminEntity admin = AdminEntity.create(
                dto.getAdminName(),
                AdminStatus.ACTIVE,
                now,
                account // Account를 생성 시 바로 전달
        );
        adminRepository.save(admin);
    }

    /**
     * 파트너 신청 목록 조회 (PENDING 상태만)
     * 
     * @return 파트너 신청 목록
     */
    @Transactional(readOnly = true)
    public List<PartnerApplicationResponseDto> getPendingPartners() {
        List<PartnerEntity> pendingPartners = partnerRepository.findAll().stream()
                .filter(partner -> partner.getPartnerStatus() == PartnerStatus.PENDING)
                .collect(Collectors.toList());

        return pendingPartners.stream()
                .map(partner -> PartnerApplicationResponseDto.builder()
                        .partnerId(partner.getPartnerId())
                        .partnerName(partner.getPartnerName())
                        .partnerContact(partner.getPartnerContact())
                        .partnerBankAccount(partner.getPartnerBankAccount())
                        .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                        .businessRegistrationFileId(partner.getBusinessRegistrationFileId())
                        .bankAccountFileId(partner.getBankAccountFileId())
                        .rejectionReason(partner.getRejectionReason())
                        .partnerStatus(partner.getPartnerStatus())
                        .partnerApprovedAt(partner.getPartnerApprovedAt())
                        .email(partner.getEmail())
                        .deactivationRequestReason(partner.getDeactivationRequestReason())
                        .deactivationRequestedAt(partner.getDeactivationRequestedAt())
                        .reactivationRequestReason(partner.getReactivationRequestReason())
                        .reactivationRequestedAt(partner.getReactivationRequestedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 파트너 목록 조회 (상태별 필터링 및 휴업/재활성화 신청 필터 가능)
     * 
     * 필터는 AND 조건으로 작동합니다:
     * - 상태 필터: 특정 상태의 파트너만 조회
     * - 휴업 신청 필터: 휴업 신청이 있는 파트너만 조회
     * - 재활성화 신청 필터: 재활성화 신청이 있는 파트너만 조회
     * 
     * 예시:
     * - status=APPROVED, hasDeactivationRequest=true → "승인 · 운영 중" 상태이면서 휴업 신청이 있는 파트너
     * - status=INACTIVE, hasReactivationRequest=true → "승인 · 비활성" 상태이면서 재활성화 신청이 있는 파트너
     * 
     * @param status 필터링할 상태 (null이면 전체 조회)
     * @param hasDeactivationRequest 휴업 신청이 있는 파트너만 조회 (true일 때만 필터링)
     * @param hasReactivationRequest 재활성화 신청이 있는 파트너만 조회 (true일 때만 필터링)
     * @return 파트너 목록
     */
    @Transactional(readOnly = true)
    public List<PartnerApplicationResponseDto> getAllPartners(
            PartnerStatus status, 
            Boolean hasDeactivationRequest,
            Boolean hasReactivationRequest
    ) {
        List<PartnerEntity> partners = partnerRepository.findAll().stream()
                .filter(partner -> status == null || partner.getPartnerStatus() == status)
                .filter(partner -> {
                    if (hasDeactivationRequest == null || !hasDeactivationRequest) {
                        return true; // 필터링하지 않음
                    }
                    // hasDeactivationRequest가 true면 휴업 신청이 있는 파트너만
                    return partner.getDeactivationRequestedAt() != null;
                })
                .filter(partner -> {
                    if (hasReactivationRequest == null || !hasReactivationRequest) {
                        return true; // 필터링하지 않음
                    }
                    // hasReactivationRequest가 true면 재활성화 신청이 있는 파트너만
                    return partner.getReactivationRequestedAt() != null;
                })
                .collect(Collectors.toList());

        return partners.stream()
                .map(partner -> PartnerApplicationResponseDto.builder()
                        .partnerId(partner.getPartnerId())
                        .partnerName(partner.getPartnerName())
                        .partnerContact(partner.getPartnerContact())
                        .partnerBankAccount(partner.getPartnerBankAccount())
                        .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                        .businessRegistrationFileId(partner.getBusinessRegistrationFileId())
                        .bankAccountFileId(partner.getBankAccountFileId())
                        .rejectionReason(partner.getRejectionReason())
                        .partnerStatus(partner.getPartnerStatus())
                        .partnerApprovedAt(partner.getPartnerApprovedAt())
                        .email(partner.getEmail())
                        .deactivationRequestReason(partner.getDeactivationRequestReason())
                        .deactivationRequestedAt(partner.getDeactivationRequestedAt())
                        .reactivationRequestReason(partner.getReactivationRequestReason())
                        .reactivationRequestedAt(partner.getReactivationRequestedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 파트너 상세 조회
     * 
     * @param partnerId 파트너 ID
     * @return 파트너 상세 정보
     */
    @Transactional(readOnly = true)
    public PartnerApplicationResponseDto getPartnerDetail(Long partnerId) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        return PartnerApplicationResponseDto.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerContact(partner.getPartnerContact())
                .partnerBankAccount(partner.getPartnerBankAccount())
                .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                .businessRegistrationFileId(partner.getBusinessRegistrationFileId())
                .bankAccountFileId(partner.getBankAccountFileId())
                .rejectionReason(partner.getRejectionReason())
                .partnerStatus(partner.getPartnerStatus())
                .partnerApprovedAt(partner.getPartnerApprovedAt())
                .email(partner.getEmail())
                .deactivationRequestReason(partner.getDeactivationRequestReason())
                .deactivationRequestedAt(partner.getDeactivationRequestedAt())
                .reactivationRequestReason(partner.getReactivationRequestReason())
                .reactivationRequestedAt(partner.getReactivationRequestedAt())
                .build();
    }

    /**
     * 파트너 비활성화 (APPROVED → INACTIVE)
     * 파트너 비활성화 시 해당 파트너의 모든 상품과 옵션도 함께 비활성화됨
     * 
     * @param partnerId 파트너 ID
     * @param session 현재 세션 (관리자 정보 가져오기용, null 가능)
     * @return 비활성화된 파트너 정보
     */
    @Transactional
    public PartnerApplicationResponseDto deactivatePartner(Long partnerId, HttpSession session) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        if (partner.getPartnerStatus() != PartnerStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "승인된 파트너만 비활성화할 수 있습니다. 현재 상태: " + partner.getPartnerStatus().getLabel());
        }

        // Account가 있는지 확인
        if (partner.getAccount() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "Account가 없는 파트너는 비활성화할 수 없습니다.");
        }

        // 현재 로그인한 관리자 정보 가져오기 (세션이 있는 경우)
        AdminEntity admin = null;
        if (session != null) {
            try {
                var currentUser = authService.getCurrentUser(session);
                admin = adminRepository.findById(currentUser.getSubjectId())
                        .orElse(null);
            } catch (Exception e) {
                // 세션 정보를 가져올 수 없는 경우 무시
            }
        }

        // 1. 파트너 상태를 INACTIVE로 변경
        partner.updateStatus(PartnerStatus.INACTIVE, partner.getPartnerApprovedAt(), null);
        partner = partnerRepository.save(partner);

        // 2. Account 상태도 비활성화
        AccountEntity account = partner.getAccount();
        if (account != null) {
            account.setStatus("INACTIVE");
            accountRepository.save(account);
        }

        // 3. 해당 파트너의 모든 상품 비활성화 (INACTIVE 상태로 변경)
        List<ProductEntity> products = productRepository.findByPartner_PartnerId(partnerId);
        for (ProductEntity product : products) {
            if (product.getProductActiveStatus() == ActiveStatus.ACTIVE) {
                product.deactivate();
                productRepository.save(product);
            }
        }

        // 4. 해당 파트너의 모든 옵션 비활성화 (INACTIVE 상태로 변경)
        List<OptionEntity> options = optionRepository.findByPartner_PartnerIdAndOptionStatus(
                partnerId, ActiveStatus.ACTIVE);
        for (OptionEntity option : options) {
            option.deactivate();
            optionRepository.save(option);
        }

        // 5. 휴업 신청 승인인 경우 이전 휴업 신청 정보 초기화 (재신청 가능하도록)
        if (partner.getDeactivationRequestedAt() != null) {
            partner.cancelDeactivationRequest();
        }

        // 6. 이력 기록 (관리자가 있고, 휴업 신청 승인이 아닌 경우 - 직접 비활성화)
        // approveDeactivationRequest에서 이미 이력을 기록하므로 중복 방지
        if (admin != null && partner.getDeactivationRequestedAt() == null) {
            PartnerHistoryEntity history = PartnerHistoryEntity.createDeactivated(partner, admin);
            partnerHistoryRepository.save(history);
        }

        return PartnerApplicationResponseDto.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerContact(partner.getPartnerContact())
                .partnerBankAccount(partner.getPartnerBankAccount())
                .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                .partnerStatus(partner.getPartnerStatus())
                .partnerApprovedAt(partner.getPartnerApprovedAt())
                .email(partner.getEmail())
                .deactivationRequestReason(partner.getDeactivationRequestReason())
                .deactivationRequestedAt(partner.getDeactivationRequestedAt())
                .reactivationRequestReason(partner.getReactivationRequestReason())
                .reactivationRequestedAt(partner.getReactivationRequestedAt())
                .build();
    }

    /**
     * 파트너 재활성화 (INACTIVE → APPROVED)
     * 파트너 재활성화 시 해당 파트너의 모든 상품과 옵션도 함께 활성화됨
     * 
     * @param partnerId 파트너 ID
     * @param session 현재 세션 (관리자 정보 가져오기용, null 가능)
     * @return 재활성화된 파트너 정보
     */
    @Transactional
    public PartnerApplicationResponseDto activatePartner(Long partnerId, HttpSession session) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        if (partner.getPartnerStatus() != PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "비활성화된 파트너만 재활성화할 수 있습니다. 현재 상태: " + partner.getPartnerStatus().getLabel());
        }

        // Account가 있는지 확인
        if (partner.getAccount() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "Account가 없는 파트너는 재활성화할 수 없습니다.");
        }

        // 현재 로그인한 관리자 정보 가져오기 (세션이 있는 경우)
        AdminEntity admin = null;
        if (session != null) {
            try {
                var currentUser = authService.getCurrentUser(session);
                admin = adminRepository.findById(currentUser.getSubjectId())
                        .orElse(null);
            } catch (Exception e) {
                // 세션 정보를 가져올 수 없는 경우 무시
            }
        }

        // 1. 파트너 상태를 APPROVED로 변경
        LocalDateTime approvedAt = partner.getPartnerApprovedAt() != null 
                ? partner.getPartnerApprovedAt() 
                : LocalDateTime.now();
        
        // 재활성화 신청이 있었는지 확인 (초기화 전에 확인)
        boolean hadReactivationRequest = partner.getReactivationRequestedAt() != null;
        String reactivationReason = partner.getReactivationRequestReason();
        
        partner.updateStatus(PartnerStatus.APPROVED, approvedAt, null);
        
        // 재활성화 신청 정보 초기화 (있는 경우)
        if (hadReactivationRequest) {
            partner.cancelReactivationRequest();
        }
        
        // 이전 휴업 신청 정보 초기화 (재활성화 후 다시 휴업 신청 가능하도록)
        if (partner.getDeactivationRequestedAt() != null) {
            partner.cancelDeactivationRequest();
        }
        
        partner = partnerRepository.save(partner);

        // 2. Account 상태도 활성화
        AccountEntity account = partner.getAccount();
        if (account != null) {
            account.setStatus("ACTIVE");
            accountRepository.save(account);
        }

        // 3. 해당 파트너의 모든 상품 활성화 (ACTIVE 상태로 변경)
        List<ProductEntity> products = productRepository.findByPartner_PartnerId(partnerId);
        for (ProductEntity product : products) {
            if (product.getProductActiveStatus() == ActiveStatus.INACTIVE) {
                product.activate();
                productRepository.save(product);
            }
        }

        // 4. 해당 파트너의 모든 옵션 활성화 (ACTIVE 상태로 변경)
        List<OptionEntity> options = optionRepository.findByPartner_PartnerIdAndOptionStatus(
                partnerId, ActiveStatus.INACTIVE);
        for (OptionEntity option : options) {
            option.activate();
            optionRepository.save(option);
        }

        // 5. 이력 기록 (관리자가 있는 경우)
        if (admin != null) {
            PartnerHistoryEntity history;
            if (hadReactivationRequest) {
                // 재활성화 신청 승인으로 기록
                history = PartnerHistoryEntity.createReactivationApproved(partner, admin, reactivationReason);
            } else {
                // 관리자 직접 재활성화
                history = PartnerHistoryEntity.createActivated(partner, admin);
            }
            partnerHistoryRepository.save(history);
        }

        return PartnerApplicationResponseDto.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerContact(partner.getPartnerContact())
                .partnerBankAccount(partner.getPartnerBankAccount())
                .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                .partnerStatus(partner.getPartnerStatus())
                .partnerApprovedAt(partner.getPartnerApprovedAt())
                .email(partner.getEmail())
                .reactivationRequestReason(partner.getReactivationRequestReason())
                .reactivationRequestedAt(partner.getReactivationRequestedAt())
                .build();
    }

    /**
     * 파트너 승인
     * 
     * @param partnerId 파트너 ID
     * @param session 현재 세션 (관리자 정보 가져오기용)
     * @return 승인된 파트너 정보
     */
    @Transactional
    public PartnerApplicationResponseDto approvePartner(Long partnerId, HttpSession session) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        if (partner.getPartnerStatus() != PartnerStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "대기 중인 파트너만 승인할 수 있습니다. 현재 상태: " + partner.getPartnerStatus().getLabel());
        }

        // 현재 로그인한 관리자 정보 가져오기
        var currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "관리자 정보를 찾을 수 없습니다."));

        String brandCode = partner.getRepresentativeBrandCode() != null
                ? partner.getRepresentativeBrandCode().trim().toUpperCase()
                : null;
        if (brandCode == null || brandCode.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "대표 브랜드 코드는 필수입니다.");
        }
        if (!brandRepository.existsById(brandCode)) {
            // 승인 시점에 brand 마스터를 자동 생성하여 파트너 대표 브랜드와 동기화
            brandRepository.save(BrandEntity.builder()
                    .code(brandCode)
                    .canonicalName(brandCode)
                    .displayName(brandCode)
                    .slug(brandCode.toLowerCase())
                    .isActive(true)
                    .sortOrder(999)
                    .build());
        }

        // 1. 임시 비밀번호 생성 (8자리 랜덤 문자열)
        String tempPassword = generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
        
        // 2. Account 생성 (승인 시에만 생성)
        String email = partner.getEmail();
        AccountEntity account = AccountEntity.builder()
                .email(email)
                .password(encodedPassword)
                .role(AccountRole.PARTNER)
                .emailVerified(true) // 승인 시 이메일 인증 완료로 간주
                .status("ACTIVE")
                .build();
        account = accountRepository.save(account);

        // 3. Partner에 Account 연결 및 상태 업데이트
        partner.setAccount(account);
        partner.updateStatus(PartnerStatus.APPROVED, LocalDateTime.now(), null);
        partner = partnerRepository.save(partner);
        
        // 4. 이력 기록
        PartnerHistoryEntity history = PartnerHistoryEntity.createApproval(partner, admin);
        partnerHistoryRepository.save(history);
        
        // 5. 승인 이메일 발송 (임시 비밀번호 포함)
        emailService.sendPartnerApprovalEmail(email, partner.getPartnerName(), email, tempPassword);

        return PartnerApplicationResponseDto.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerContact(partner.getPartnerContact())
                .partnerBankAccount(partner.getPartnerBankAccount())
                .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                .partnerStatus(partner.getPartnerStatus())
                .partnerApprovedAt(partner.getPartnerApprovedAt())
                .email(email)
                .deactivationRequestReason(partner.getDeactivationRequestReason())
                .deactivationRequestedAt(partner.getDeactivationRequestedAt())
                .reactivationRequestReason(partner.getReactivationRequestReason())
                .reactivationRequestedAt(partner.getReactivationRequestedAt())
                .build();
    }

    /**
     * 파트너 거절 (REJECTED로 변경)
     * 
     * @param partnerId 파트너 ID
     * @param rejectionReason 거절 사유
     * @param session 현재 세션 (관리자 정보 가져오기용)
     */
    @Transactional
    public void rejectPartner(Long partnerId, String rejectionReason, HttpSession session) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        if (partner.getPartnerStatus() != PartnerStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "대기 중인 파트너만 거절할 수 있습니다. 현재 상태: " + partner.getPartnerStatus().getLabel());
        }

        // 현재 로그인한 관리자 정보 가져오기
        var currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "관리자 정보를 찾을 수 없습니다."));

        // 파트너 상태를 REJECTED로 변경 및 거절 사유 저장
        partner.updateStatus(PartnerStatus.REJECTED, null, rejectionReason);
        partner = partnerRepository.save(partner);

        // 이력 기록
        PartnerHistoryEntity history = PartnerHistoryEntity.createRejection(partner, admin, rejectionReason);
        partnerHistoryRepository.save(history);

        // 거절 이메일 발송
        emailService.sendPartnerRejectionEmail(
                partner.getEmail(), 
                partner.getPartnerName(), 
                rejectionReason
        );
    }

    /**
     * 휴업 신청 목록 조회
     * APPROVED 상태이면서 deactivationRequestedAt이 null이 아닌 파트너 목록
     * 
     * @return 휴업 신청한 파트너 목록
     */
    public List<PartnerApplicationResponseDto> getDeactivationRequests() {
        List<PartnerEntity> partners = partnerRepository.findAll().stream()
                .filter(p -> p.getPartnerStatus() == PartnerStatus.APPROVED 
                        && p.getDeactivationRequestedAt() != null)
                .collect(Collectors.toList());

        return partners.stream()
                .map(partner -> PartnerApplicationResponseDto.builder()
                        .partnerId(partner.getPartnerId())
                        .partnerName(partner.getPartnerName())
                        .partnerContact(partner.getPartnerContact())
                        .partnerBankAccount(partner.getPartnerBankAccount())
                        .businessRegistrationNumber(partner.getBusinessRegistrationNumber())
                        .partnerStatus(partner.getPartnerStatus())
                        .partnerApprovedAt(partner.getPartnerApprovedAt())
                        .email(partner.getEmail())
                        .deactivationRequestReason(partner.getDeactivationRequestReason())
                        .deactivationRequestedAt(partner.getDeactivationRequestedAt())
                        .reactivationRequestReason(partner.getReactivationRequestReason())
                        .reactivationRequestedAt(partner.getReactivationRequestedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 휴업 신청 승인
     * 파트너를 INACTIVE로 변경하고 상품/옵션도 비활성화
     * 휴업 신청 정보는 이력으로 유지됨 (초기화하지 않음)
     * 
     * @param partnerId 파트너 ID
     * @param session 현재 세션 (관리자 정보 가져오기용)
     * @return 비활성화된 파트너 정보
     */
    @Transactional
    public PartnerApplicationResponseDto approveDeactivationRequest(Long partnerId, HttpSession session) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        // APPROVED 상태이고 휴업 신청이 있는지 확인
        if (partner.getPartnerStatus() != PartnerStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "운영 중인 파트너만 휴업 신청 승인이 가능합니다. 현재 상태: " + partner.getPartnerStatus().getLabel());
        }

        if (partner.getDeactivationRequestedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "휴업 신청이 존재하지 않습니다.");
        }

        // 현재 로그인한 관리자 정보 가져오기
        var currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "관리자 정보를 찾을 수 없습니다."));

        // 휴업 신청 승인 이력 기록 (상태 변경 전에 기록)
        PartnerHistoryEntity history = PartnerHistoryEntity.createDeactivationApproved(partner, admin);
        partnerHistoryRepository.save(history);

        // 기존 deactivatePartner 로직 재사용
        // deactivatePartner에서는 이력 기록을 하지 않음 (이미 기록됨)
        return deactivatePartner(partnerId, session);
    }

    /**
     * 휴업 신청 거절
     * 휴업 신청 정보를 초기화하고 APPROVED 상태 유지
     * 
     * @param partnerId 파트너 ID
     * @param rejectionReason 거절 사유
     * @param session 현재 세션 (관리자 정보 가져오기용)
     */
    @Transactional
    public void rejectDeactivationRequest(Long partnerId, String rejectionReason, HttpSession session) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        // APPROVED 상태이고 휴업 신청이 있는지 확인
        if (partner.getPartnerStatus() != PartnerStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "운영 중인 파트너만 휴업 신청 거절이 가능합니다. 현재 상태: " + partner.getPartnerStatus().getLabel());
        }

        if (partner.getDeactivationRequestedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "휴업 신청이 존재하지 않습니다.");
        }

        // 현재 로그인한 관리자 정보 가져오기
        var currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "관리자 정보를 찾을 수 없습니다."));

        // 휴업 신청 거절 이력 기록 (초기화 전에 기록)
        PartnerHistoryEntity history = PartnerHistoryEntity.createDeactivationRejection(partner, admin, rejectionReason);
        partnerHistoryRepository.save(history);

        // 거절 사유 저장
        partner.setDeactivationRejectionReason(rejectionReason);

        // 휴업 신청 정보 초기화
        partner.cancelDeactivationRequest();
        partner = partnerRepository.save(partner);

        // 거절 이메일 발송 (선택사항 - 필요시 EmailService에 메서드 추가)
        // emailService.sendDeactivationRejectionEmail(...);
    }

    /**
     * 재활성화 신청 거절
     * 재활성화 신청 정보를 초기화하고 INACTIVE 상태 유지
     * 기존 파트너 관리 페이지에서 사용
     * 
     * @param partnerId 파트너 ID
     * @param rejectionReason 거절 사유
     * @param session 현재 세션 (관리자 정보 가져오기용)
     */
    @Transactional
    public void rejectReactivationRequest(Long partnerId, String rejectionReason, HttpSession session) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        // INACTIVE 상태이고 재활성화 신청이 있는지 확인
        if (partner.getPartnerStatus() != PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "비활성화된 파트너만 재활성화 신청 거절이 가능합니다. 현재 상태: " + partner.getPartnerStatus().getLabel());
        }

        if (partner.getReactivationRequestedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "재활성화 신청이 존재하지 않습니다.");
        }

        // 현재 로그인한 관리자 정보 가져오기
        var currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "관리자 정보를 찾을 수 없습니다."));

        // 재활성화 신청 거절 이력 기록 (초기화 전에 기록)
        PartnerHistoryEntity history = PartnerHistoryEntity.createReactivationRejection(partner, admin, rejectionReason);
        partnerHistoryRepository.save(history);

        // 거절 사유 저장
        partner.setReactivationRejectionReason(rejectionReason);

        // 재활성화 신청 정보 초기화
        partner.cancelReactivationRequest();
        partner = partnerRepository.save(partner);

        // 거절 이메일 발송 (선택사항 - 필요시 EmailService에 메서드 추가)
        // emailService.sendReactivationRejectionEmail(...);
    }

    /**
     * 파트너 이력 조회
     * 
     * @param partnerId 파트너 ID
     * @param actionType 액션 타입 필터 (선택, null이면 전체)
     * @return 파트너 이력 목록
     */
    @Transactional(readOnly = true)
    public List<PartnerHistoryResponseDto> getPartnerHistory(Long partnerId, String actionTypeStr) {
        // 파트너 존재 확인
        partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        // 이력 조회
        List<PartnerHistoryEntity> histories;
        if (actionTypeStr != null && !actionTypeStr.isEmpty()) {
            try {
                PartnerHistoryActionType actionType = PartnerHistoryActionType.valueOf(actionTypeStr);
                histories = partnerHistoryRepository.findByPartner_PartnerIdAndActionTypeOrderByCreatedAtDesc(
                        partnerId, actionType);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 액션 타입입니다: " + actionTypeStr);
            }
        } else {
            histories = partnerHistoryRepository.findByPartner_PartnerIdOrderByCreatedAtDesc(partnerId);
        }

        // DTO 변환
        return histories.stream()
                .map(history -> PartnerHistoryResponseDto.builder()
                        .historyId(history.getHistoryId())
                        .partnerId(history.getPartner().getPartnerId())
                        .partnerName(history.getPartner().getPartnerName())
                        .actionType(history.getActionType())
                        .fromStatus(history.getFromStatus())
                        .toStatus(history.getToStatus())
                        .reason(history.getReason())
                        .adminId(history.getAdmin() != null ? history.getAdmin().getAdminId() : null)
                        .adminName(history.getAdmin() != null ? history.getAdmin().getAdminName() : null)
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 상품 승인 (PENDING → ACTIVE 또는 PENDING_UPDATE → ACTIVE)
     * 관리자가 파트너가 등록한 상품을 승인하여 판매 가능 상태로 전환
     * 또는 활성 상품의 수정 사항을 승인
     * 
     * @param productNo 상품 번호
     * @param session 현재 세션
     * @return 승인된 상품 정보 (DTO)
     */
    @Transactional
    public ProductListDto approveProduct(Long productNo, HttpSession session) {
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "관리자 정보를 찾을 수 없습니다."));

        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "상품을 찾을 수 없습니다."));

        ActiveStatus currentStatus = product.getProductActiveStatus();
        
        // PENDING 또는 PENDING_UPDATE 상태만 승인 가능
        if (currentStatus != ActiveStatus.PENDING && currentStatus != ActiveStatus.PENDING_UPDATE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "승인 대기 중인 상품만 승인할 수 있습니다. 현재 상태: " + currentStatus.getLabel());
        }

        // 상품 승인 (PENDING → ACTIVE 또는 PENDING_UPDATE → ACTIVE)
        // 옵션에서 파트너 정보를 가져와서 상품에 설정 (상품 등록 시 partner가 설정되지 않은 경우 대비)
        // 모든 상태의 옵션 조회 (PENDING, PENDING_UPDATE 옵션도 승인해야 함)
        List<OptionEntity> options = optionRepository.findByProduct_ProductNoAllStatus(productNo);
        if (!options.isEmpty() && product.getPartner() == null) {
            // 첫 번째 옵션의 파트너 정보를 상품에 설정
            PartnerEntity partner = options.get(0).getPartner();
            product.setPartner(partner);
        }
        
        product.activate();
        product = productRepository.save(product);

        // 해당 상품의 모든 옵션도 활성화 (PENDING 또는 PENDING_UPDATE 상태인 옵션만)
        for (OptionEntity option : options) {
            if (option.getOptionStatus() == ActiveStatus.PENDING || option.getOptionStatus() == ActiveStatus.PENDING_UPDATE) {
                option.activate();
                optionRepository.save(option);
            }
        }

        // DTO로 변환하여 반환
        return convertToProductListDto(List.of(product)).get(0);
    }

    /**
     * 상품 거절 (PENDING → REJECTED)
     * 관리자가 파트너가 등록한 상품을 거절
     * 
     * @param productNo 상품 번호
     * @param rejectionReason 거절 사유
     * @param session 현재 세션
     * @return 거절된 상품 정보 (DTO)
     */
    @Transactional
    public ProductListDto rejectProduct(Long productNo, String rejectionReason, HttpSession session) {
        AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "관리자 정보를 찾을 수 없습니다."));

        ProductEntity product = productRepository.findById(productNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "상품을 찾을 수 없습니다."));

        ActiveStatus currentStatus = product.getProductActiveStatus();
        
        // PENDING 또는 PENDING_UPDATE 상태만 거절 가능
        if (currentStatus != ActiveStatus.PENDING && currentStatus != ActiveStatus.PENDING_UPDATE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                "승인 대기 중인 상품만 거절할 수 있습니다. 현재 상태: " + currentStatus.getLabel());
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "거절 사유를 입력해주세요.");
        }

        // 상품 거절 처리
        if (currentStatus == ActiveStatus.PENDING) {
            // PENDING 상태: REJECTED로 변경 (신규 등록 거절)
            product.reject(rejectionReason);
        } else if (currentStatus == ActiveStatus.PENDING_UPDATE) {
            // PENDING_UPDATE 상태: 원래 ACTIVE 상태로 복구 (수정 거절)
            // 수정 내용은 거절되지만 원래 상품은 계속 판매 가능
            product.rejectUpdate(rejectionReason);
        }
        product = productRepository.save(product);

        // 해당 상품의 모든 옵션도 처리
        List<OptionEntity> options = optionRepository.findByProduct_ProductNoAllStatus(productNo);
        for (OptionEntity option : options) {
            if (currentStatus == ActiveStatus.PENDING) {
                // PENDING 상태: 옵션도 REJECTED로 변경
                if (option.getOptionStatus() == ActiveStatus.PENDING || option.getOptionStatus() == ActiveStatus.PENDING_UPDATE) {
                    option.reject();
                    optionRepository.save(option);
                }
            } else if (currentStatus == ActiveStatus.PENDING_UPDATE) {
                // PENDING_UPDATE 상태: 변경된 옵션만 원래 상태로 복구
                if (option.getOptionStatus() == ActiveStatus.PENDING_UPDATE) {
                    // PENDING_UPDATE 옵션은 원본 데이터로 복구
                    option.rejectUpdate();
                    optionRepository.save(option);
                } else if (option.getOptionStatus() == ActiveStatus.PENDING) {
                    // 새로 추가된 옵션은 REJECTED로 변경
                    option.reject();
                    optionRepository.save(option);
                }
                // ACTIVE 상태 옵션은 그대로 유지
            }
        }

        // DTO로 변환하여 반환
        return convertToProductListDto(List.of(product)).get(0);
    }

    /**
     * 상품 목록 조회 (상태별 필터링 가능)
     * 
     * @param status 필터링할 상태 (null이면 전체)
     * @return 상품 목록 (DTO)
     */
    @Transactional(readOnly = true)
    public List<ProductListDto> getAllProducts(ActiveStatus status) {
        List<ProductEntity> products;
        if (status == null) {
            products = productRepository.findAll();
        } else {
            products = productRepository.findByProductActiveStatus(status);
        }
        return convertToProductListDto(products);
    }
    
    /**
     * ProductEntity 리스트를 ProductListDto 리스트로 변환
     */
    private List<ProductListDto> convertToProductListDto(List<ProductEntity> products) {
        List<ProductListDto> result = new ArrayList<>();
        
        for (ProductEntity product : products) {
            // 상품의 모든 옵션 조회 (관리자용: 모든 상태 포함)
            List<OptionEntity> options = optionRepository.findByProduct_ProductNoAllStatus(product.getProductNo());
            
            // 기본 가격
            Long basePrice = Long.parseLong(product.getProductPrice());
            
            // 옵션 DTO 변환 및 최소/최대 가격 계산
            List<ProductOptionDto> optionDtos = new ArrayList<>();
            Long minPrice = basePrice;
            Long maxPrice = basePrice;
            
            for (OptionEntity option : options) {
                Long addPrice = option.getOptionAddPrice() != null ? option.getOptionAddPrice() : 0L;
                Long totalPrice = basePrice + addPrice;
                
                // 관리자용: 옵션 상태 포함
                optionDtos.add(new ProductOptionDto(
                    option.getOptionNo(),
                    option.getColor(),
                    option.getSize(),
                    addPrice,
                    totalPrice,
                    option.getOptionStatus()
                ));
                
                // 최소/최대 가격 업데이트
                if (totalPrice < minPrice) minPrice = totalPrice;
                if (totalPrice > maxPrice) maxPrice = totalPrice;
            }
            
            // 파트너 정보 추출 (ProductEntity의 partner 또는 옵션의 partner에서 가져오기)
            Long partnerId = null;
            String partnerName = null;
            String partnerContact = null;
            String partnerEmail = null;
            
            if (product.getPartner() != null) {
                // ProductEntity에 파트너 정보가 있는 경우
                partnerId = product.getPartner().getPartnerId();
                partnerName = product.getPartner().getPartnerName();
                partnerContact = product.getPartner().getPartnerContact();
                partnerEmail = product.getPartner().getEmail();
            } else if (!options.isEmpty()) {
                // 옵션에서 파트너 정보 가져오기
                PartnerEntity partner = options.get(0).getPartner();
                partnerId = partner.getPartnerId();
                partnerName = partner.getPartnerName();
                partnerContact = partner.getPartnerContact();
                partnerEmail = partner.getEmail();
            }
            
            // DTO 생성 (관리자용: 상태, 거절 사유, 파트너 정보 포함)
            result.add(new ProductListDto(
                product.getProductNo(),
                product.getProductName(),
                product.getProductType().name(),
                product.getProductSubType() != null ? product.getProductSubType().name() : null,
                product.getProductPrice(),
                product.getProductDescription(),
                product.getProductImageUrl(),
                product.getProductCreatedAt(),
                minPrice,
                maxPrice,
                optionDtos,
                java.util.Collections.emptyList(),
                product.getProductActiveStatus().name(), // 관리자용: 상태 추가
                product.getRejectionReason(), // 거절 사유 추가
                partnerId, // 파트너 ID
                partnerName, // 파트너 이름
                partnerContact, // 파트너 연락처
                partnerEmail // 파트너 이메일
            ));
        }
        
        return result;
    }

    /**
     * 임시 비밀번호 생성 (8자리 랜덤 문자열)
     */
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 관리자용 고객 목록 조회
     * 
     * @param session HTTP 세션
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지 크기
     * @param searchKeyword 검색 키워드 (이름 또는 이메일, 선택)
     * @param emailVerified 이메일 인증 여부 필터 (null이면 전체, 선택)
     * @return 고객 목록 및 통계
     */
    @Transactional(readOnly = true)
    public CustomerListResponseDto getCustomerList(
            HttpSession session,
            Integer page,
            Integer pageSize,
            String searchKeyword,
            Boolean emailVerified,
            com.swimshop.swim_mall.common.enums.CustomerGradeEnum grade
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 페이징 정보 설정
        if (page == null || page < 0) {
            page = 0;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 20;
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "customerCreateAt"));
        
        // 고객 목록 조회
        Page<CustomerEntity> customerPage = customerRepository.findCustomersForAdmin(
                pageable,
                searchKeyword,
                emailVerified,
                grade
        );
        
        // 통계 정보 포함하여 조회
        List<CustomerEntity> customers = customerRepository.findCustomersWithStatistics(
                pageable,
                searchKeyword,
                emailVerified,
                grade
        );
        
        // DTO 변환
        List<CustomerListResponseDto.CustomerListItemDto> customerDtos = customers.stream()
                .map(customer -> {
                    // 주문 통계 계산
                    List<OrderEntity> orders = orderRepository.findByCustomerOrderByOrderCreatedAtDesc(
                            customer
                    );
                    long orderCount = orders.size();
                    long totalOrderAmount = orders.stream()
                            .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED)
                            .mapToLong(OrderEntity::getOrderTotalPrice)
                            .sum();
                    
                    // 리뷰 통계 계산
                    long reviewCount = reviewRepository.findByCustomerId(customer.getCustomerId()).size();
                    
                    // 반품 통계 계산
                    long returnCount = returnRepository.findByCustomerId(customer.getCustomerId()).size();
                    
                    // Account 상태 가져오기
                    String accountStatus = null;
                    if (customer.getAccount() != null) {
                        accountStatus = customer.getAccount().getStatus();
                    }
                    
                    // 고객 태그 조회
                    List<CustomerTagMappingEntity> tagMappings = customerTagMappingRepository
                            .findByCustomer_CustomerId(customer.getCustomerId());
                    List<CustomerTagDto> tags = tagMappings.stream()
                            .map(mapping -> {
                                CustomerTagEntity tag = mapping.getTag();
                                return CustomerTagDto.builder()
                                        .tagId(tag.getTagId())
                                        .tagName(tag.getTagName())
                                        .tagColor(tag.getTagColor())
                                        .description(tag.getDescription())
                                        .build();
                            })
                            .collect(Collectors.toList());
                    
                    // 고객 등급 코드 (없으면 null)
                    com.swimshop.swim_mall.common.enums.CustomerGradeEnum customerGradeCode = null;
                    if (customer.getCustomerGrade() != null && customer.getCustomerGrade().getGradeName() != null) {
                        customerGradeCode = customer.getCustomerGrade().getGradeName();
                    }
                    
                    return CustomerListResponseDto.CustomerListItemDto.builder()
                            .customerId(customer.getCustomerId())
                            .customerName(customer.getCustomerName())
                            .customerEmail(customer.getCustomerEmail())
                            .customerBirth(customer.getCustomerBirth())
                            .customerCreateAt(customer.getCustomerCreateAt())
                            .emailChecked(customer.getEmailChecked())
                            .accountStatus(accountStatus)
                            .orderCount(orderCount)
                            .totalOrderAmount(totalOrderAmount)
                            .reviewCount(reviewCount)
                            .returnCount(returnCount)
                            .tags(tags)
                            .customerGradeCode(customerGradeCode)
                            .build();
                })
                .collect(Collectors.toList());
        
        return CustomerListResponseDto.builder()
                .customers(customerDtos)
                .totalCount(customerPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }
    
    /**
     * 관리자용 고객 상세 조회
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 고객 상세 정보 (주문/리뷰/반품 내역 포함)
     */
    @Transactional(readOnly = true)
    public CustomerDetailResponseDto getCustomerDetail(
            HttpSession session,
            Long customerId
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 주문 통계 계산
        List<OrderEntity> allOrders = orderRepository.findByCustomerOrderByOrderCreatedAtDesc(customer);
        long totalOrderCount = allOrders.size();
        long totalOrderAmount = allOrders.stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED)
                .mapToLong(OrderEntity::getOrderTotalPrice)
                .sum();
        
        // 리뷰 통계 계산
        List<ReviewEntity> allReviews = reviewRepository.findByCustomerId(customerId);
        long totalReviewCount = allReviews.size();
        
        // 반품 통계 계산
        List<ReturnEntity> allReturns = returnRepository.findByCustomerId(customerId);
        long totalReturnCount = allReturns.size();
        
        // 최근 주문 목록 (최대 10개)
        List<CustomerDetailResponseDto.OrderSummaryDto> recentOrders = allOrders.stream()
                .limit(10)
                .map(order -> CustomerDetailResponseDto.OrderSummaryDto.builder()
                        .orderNo(order.getOrderNo())
                        .orderCreatedAt(order.getOrderCreatedAt())
                        .orderStatus(order.getOrderStatus().name())
                        .orderTotalPrice(order.getOrderTotalPrice())
                        .itemCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                        .build())
                .collect(Collectors.toList());
        
        // 최근 리뷰 목록 (최대 10개)
        List<CustomerDetailResponseDto.ReviewSummaryDto> recentReviews = allReviews.stream()
                .limit(10)
                .map(review -> {
                    String productName = review.getProduct() != null
                            ? review.getProduct().getProductName()
                            : "상품 정보 없음";
                    Long productNo = review.getProduct() != null
                            ? review.getProduct().getProductNo()
                            : null;
                    
                    return CustomerDetailResponseDto.ReviewSummaryDto.builder()
                            .reviewNo(review.getReviewNo())
                            .productNo(productNo)
                            .productName(productName)
                            .rating(review.getReviewRating())
                            .reviewContent(review.getReviewContent())
                            .reviewCreatedAt(review.getReviewCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        // 최근 반품 목록 (최대 10개)
        List<CustomerDetailResponseDto.ReturnSummaryDto> recentReturns = allReturns.stream()
                .limit(10)
                .map(returnEntity -> {
                    String productName = returnEntity.getOrderItem().getProduct() != null
                            ? returnEntity.getOrderItem().getProduct().getProductName()
                            : "상품 정보 없음";
                    Long orderNo = returnEntity.getOrderItem().getOrder() != null
                            ? returnEntity.getOrderItem().getOrder().getOrderNo()
                            : null;
                    
                    return CustomerDetailResponseDto.ReturnSummaryDto.builder()
                            .returnNo(returnEntity.getReturnNo())
                            .orderNo(orderNo)
                            .productName(productName)
                            .returnStatus(returnEntity.getReturnStatus().name())
                            .returnAmount(returnEntity.getReturnAmount())
                            .returnRequestedAt(returnEntity.getReturnRequestedAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        // Account 상태 가져오기
        String accountStatus = null;
        if (customer.getAccount() != null) {
            accountStatus = customer.getAccount().getStatus();
        }
        
        // 고객 태그 조회
        List<CustomerTagMappingEntity> tagMappings = customerTagMappingRepository
                .findByCustomer_CustomerId(customerId);
        List<CustomerTagDto> tags = tagMappings.stream()
                .map(mapping -> {
                    CustomerTagEntity tag = mapping.getTag();
                    return CustomerTagDto.builder()
                            .tagId(tag.getTagId())
                            .tagName(tag.getTagName())
                            .tagColor(tag.getTagColor())
                            .description(tag.getDescription())
                            .build();
                })
                .collect(Collectors.toList());
        
        // 등급 정보 변환
        CustomerGradeDto customerGradeDto = null;
        if (customer.getCustomerGrade() != null) {
            customerGradeDto = toCustomerGradeDto(customer.getCustomerGrade());
        }
        
        return CustomerDetailResponseDto.builder()
                .customerId(customer.getCustomerId())
                .customerName(customer.getCustomerName())
                .customerEmail(customer.getCustomerEmail())
                .customerBirth(customer.getCustomerBirth())
                .customerCreateAt(customer.getCustomerCreateAt())
                .emailChecked(customer.getEmailChecked())
                .accountStatus(accountStatus)
                .customerGrade(customerGradeDto)
                .totalPurchaseAmount(customer.getTotalPurchaseAmount())
                .totalOrderCount(totalOrderCount)
                .totalOrderAmount(totalOrderAmount)
                .totalReviewCount(totalReviewCount)
                .totalReturnCount(totalReturnCount)
                .recentOrders(recentOrders)
                .recentReviews(recentReviews)
                .recentReturns(recentReturns)
                .tags(tags)
                .build();
    }
    
    /**
     * 관리자용 고객 통계 조회
     * 
     * @param session HTTP 세션
     * @return 고객 통계 정보
     */
    @Transactional(readOnly = true)
    public CustomerStatisticsDto getCustomerStatistics(HttpSession session) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 전체 고객 수
        long totalCustomerCount = customerRepository.count();
        
        // 이메일 인증 완료 고객 수
        long emailVerifiedCount = customerRepository.findAll().stream()
                .filter(CustomerEntity::getEmailChecked)
                .count();
        
        // 이메일 미인증 고객 수
        long emailUnverifiedCount = totalCustomerCount - emailVerifiedCount;
        
        // 활성 고객 수 (Account 상태가 ACTIVE인 고객)
        List<CustomerEntity> allCustomers = customerRepository.findAll();
        long activeCustomerCount = allCustomers.stream()
                .filter(customer -> {
                    AccountEntity account = customer.getAccount();
                    return account != null && "ACTIVE".equals(account.getStatus());
                })
                .count();
        
        // 오늘 가입한 고객 수
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
        long newCustomerCountToday = allCustomers.stream()
                .filter(customer -> {
                    LocalDateTime createdAt = customer.getCustomerCreateAt();
                    return createdAt != null && 
                           createdAt.isAfter(startOfToday) && 
                           createdAt.isBefore(endOfToday);
                })
                .count();
        
        // 이번 달 가입한 고객 수
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDateTime startOfMonth = firstDayOfMonth.atStartOfDay();
        long newCustomerCountThisMonth = allCustomers.stream()
                .filter(customer -> {
                    LocalDateTime createdAt = customer.getCustomerCreateAt();
                    return createdAt != null && createdAt.isAfter(startOfMonth);
                })
                .count();
        
        // 등급별 고객 수 계산
        Map<String, Long> customerCountByGrade = allCustomers.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        customer -> {
                            if (customer.getCustomerGrade() != null && customer.getCustomerGrade().getGradeName() != null) {
                                return customer.getCustomerGrade().getGradeName().getCode();
                            }
                            return "NONE"; // 등급이 없는 고객
                        },
                        java.util.stream.Collectors.counting()
                ));
        
        // 모든 등급을 포함하도록 초기화 (없으면 0)
        Map<String, Long> gradeCountMap = new java.util.HashMap<>();
        gradeCountMap.put("BEGINNER", customerCountByGrade.getOrDefault("BEGINNER", 0L));
        gradeCountMap.put("SWIMMER", customerCountByGrade.getOrDefault("SWIMMER", 0L));
        gradeCountMap.put("PRO", customerCountByGrade.getOrDefault("PRO", 0L));
        gradeCountMap.put("MASTER", customerCountByGrade.getOrDefault("MASTER", 0L));
        gradeCountMap.put("LEGEND", customerCountByGrade.getOrDefault("LEGEND", 0L));
        gradeCountMap.put("NONE", customerCountByGrade.getOrDefault("NONE", 0L));
        
        return CustomerStatisticsDto.builder()
                .totalCustomerCount(totalCustomerCount)
                .emailVerifiedCount(emailVerifiedCount)
                .emailUnverifiedCount(emailUnverifiedCount)
                .activeCustomerCount(activeCustomerCount)
                .newCustomerCountToday(newCustomerCountToday)
                .newCustomerCountThisMonth(newCustomerCountThisMonth)
                .customerCountByGrade(gradeCountMap)
                .build();
    }
    
    /**
     * 관리자용 고객 정보 수정
     * 비활성화된 계정도 수정 가능합니다 (관리자는 비활성화된 계정도 관리할 수 있음).
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param requestDto 수정할 정보 (이름, 이메일, 활성화 상태)
     * @return 수정된 고객 상세 정보
     */
    @Transactional
    public CustomerDetailResponseDto updateCustomer(
            HttpSession session,
            Long customerId,
            AdminCustomerUpdateRequestDto requestDto
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 최소한 하나의 필드는 업데이트되어야 함
        if (requestDto.getCustomerName() == null && requestDto.getCustomerEmail() == null 
                && requestDto.getCustomerBirth() == null && requestDto.getActive() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "최소한 하나의 필드(이름, 이메일, 생년월일, 또는 활성화 상태)는 업데이트해야 합니다.");
        }
        
        // 현재 관리자 정보 가져오기
        AdminEntity admin = getCurrentAdmin(session);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // Account 엔티티 조회
        AccountEntity account = customer.getAccount();
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        
        // 변경 전 값 저장 (이력 기록용)
        String oldCustomerName = customer.getCustomerName();
        String oldCustomerEmail = customer.getCustomerEmail();
        LocalDate oldCustomerBirth = customer.getCustomerBirth();
        String oldAccountStatus = account.getStatus();
        
        // 변경 사항 추적
        boolean nameChanged = false;
        boolean emailChanged = false;
        boolean birthChanged = false;
        boolean statusChanged = false;
        
        // 이메일 업데이트 (제공된 경우에만)
        if (requestDto.getCustomerEmail() != null && !requestDto.getCustomerEmail().trim().isEmpty()) {
            // 이메일 중복 확인 (다른 고객이 사용 중인지)
            if (!customer.getCustomerEmail().equals(requestDto.getCustomerEmail())) {
                if (customerRepository.existsByCustomerEmail(requestDto.getCustomerEmail()) ||
                    accountRepository.existsByEmail(requestDto.getCustomerEmail())) {
                    throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
                }
                emailChanged = true;
            }
            
            // 이메일 변경 시 Account도 함께 업데이트
            if (!customer.getCustomerEmail().equals(requestDto.getCustomerEmail())) {
                account.setEmail(requestDto.getCustomerEmail());
                accountRepository.save(account);
            }
            
            customer.setCustomerEmail(requestDto.getCustomerEmail());
        }
        
        // 이름 업데이트 (제공된 경우에만)
        if (requestDto.getCustomerName() != null && !requestDto.getCustomerName().trim().isEmpty()) {
            if (!oldCustomerName.equals(requestDto.getCustomerName())) {
                nameChanged = true;
            }
            customer.setCustomerName(requestDto.getCustomerName());
        }
        
        // 생년월일 업데이트 (제공된 경우에만)
        if (requestDto.getCustomerBirth() != null) {
            if (oldCustomerBirth == null || !oldCustomerBirth.equals(requestDto.getCustomerBirth())) {
                birthChanged = true;
            }
            customer.setCustomerBirth(requestDto.getCustomerBirth());
        }
        
        // 계정 상태 업데이트 (제공된 경우에만)
        if (requestDto.getActive() != null) {
            String newStatus = requestDto.getActive() ? "ACTIVE" : "INACTIVE";
            if (!newStatus.equals(oldAccountStatus)) {
                statusChanged = true;
            }
            account.setStatus(newStatus);
            accountRepository.save(account);
        }
        
        customerRepository.save(customer);
        
        // 이력 기록 (변경 사항이 있는 경우)
        if (nameChanged || emailChanged || birthChanged || statusChanged) {
            try {
                java.util.Map<String, Object> oldValueMap = new java.util.HashMap<>();
                oldValueMap.put("customerName", oldCustomerName != null ? oldCustomerName : "");
                oldValueMap.put("customerEmail", oldCustomerEmail != null ? oldCustomerEmail : "");
                oldValueMap.put("customerBirth", oldCustomerBirth != null ? oldCustomerBirth.toString() : "");
                oldValueMap.put("accountStatus", oldAccountStatus != null ? oldAccountStatus : "");
                String oldValueJson = objectMapper.writeValueAsString(oldValueMap);
                
                java.util.Map<String, Object> newValueMap = new java.util.HashMap<>();
                newValueMap.put("customerName", customer.getCustomerName());
                newValueMap.put("customerEmail", customer.getCustomerEmail());
                newValueMap.put("customerBirth", customer.getCustomerBirth() != null ? customer.getCustomerBirth().toString() : "");
                newValueMap.put("accountStatus", account.getStatus() != null ? account.getStatus() : "");
                String newValueJson = objectMapper.writeValueAsString(newValueMap);
                
                String reason = "관리자 정보 수정";
                if (statusChanged) {
                    reason += " (계정 상태: " + oldAccountStatus + " → " + account.getStatus() + ")";
                }
                
                CustomerHistoryEntity history = CustomerHistoryEntity.createInfoUpdate(
                    customer, admin, oldValueJson, newValueJson, reason
                );
                customerHistoryRepository.save(history);
            } catch (Exception e) {
                // 이력 기록 실패해도 작업은 계속 진행
                // 로그만 남기고 예외는 던지지 않음
            }
        }
        
        // 수정된 고객 상세 정보 반환
        return getCustomerDetail(session, customerId);
    }
    
    /**
     * 고객 계정 활성화/비활성화
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param active true면 활성화, false면 비활성화
     * @return 수정된 고객 상세 정보
     */
    @Transactional
    public CustomerDetailResponseDto updateCustomerAccountStatus(
            HttpSession session,
            Long customerId,
            Boolean active
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 현재 관리자 정보 가져오기
        AdminEntity admin = getCurrentAdmin(session);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // Account 엔티티 조회
        AccountEntity account = customer.getAccount();
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        
        // 변경 전 상태 저장
        String oldStatus = account.getStatus();
        
        // 계정 상태 업데이트
        String newStatus = active ? "ACTIVE" : "INACTIVE";
        account.setStatus(newStatus);
        accountRepository.save(account);
        
        // 이력 기록
        try {
            String reason = active ? "관리자 계정 활성화" : "관리자 계정 비활성화";
            CustomerHistoryEntity history;
            if (active) {
                history = CustomerHistoryEntity.createStatusActivate(customer, admin, reason);
            } else {
                history = CustomerHistoryEntity.createStatusDeactivate(customer, admin, reason);
            }
            customerHistoryRepository.save(history);
        } catch (Exception e) {
            // 이력 기록 실패해도 작업은 계속 진행
        }
        
        // 수정된 고객 상세 정보 반환
        return getCustomerDetail(session, customerId);
    }
    
    /**
     * 고객 비밀번호 초기화
     * 비활성화된 계정도 비밀번호 초기화 가능합니다 (관리자는 비활성화된 계정도 관리할 수 있음).
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 임시 비밀번호 (이메일로도 발송됨)
     */
    @Transactional
    public String resetCustomerPassword(
            HttpSession session,
            Long customerId
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 현재 관리자 정보 가져오기
        AdminEntity admin = getCurrentAdmin(session);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // Account 엔티티 조회
        AccountEntity account = customer.getAccount();
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        
        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);
        
        // 비밀번호 업데이트 (Customer와 Account 모두)
        customer.setCustomerPassword(encodedPassword);
        account.setPassword(encodedPassword);
        
        customerRepository.save(customer);
        accountRepository.save(account);
        
        // 이력 기록
        try {
            String reason = "관리자 비밀번호 초기화";
            CustomerHistoryEntity history = CustomerHistoryEntity.createPasswordReset(
                customer, admin, reason
            );
            customerHistoryRepository.save(history);
        } catch (Exception e) {
            // 이력 기록 실패해도 작업은 계속 진행
        }
        
        // 이메일 발송
        try {
            emailService.sendPasswordResetEmail(
                    customer.getCustomerEmail(),
                    customer.getCustomerName(),
                    tempPassword
            );
        } catch (Exception e) {
            // 이메일 발송 실패해도 비밀번호 초기화는 성공 처리
            // 로그만 남기고 계속 진행
        }
        
        return tempPassword;
    }
    
    /**
     * 고객 이메일 인증 재발송
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     */
    @Transactional
    public void resendEmailVerification(
            HttpSession session,
            Long customerId
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 현재 관리자 정보 가져오기
        AdminEntity admin = getCurrentAdmin(session);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 이미 인증된 경우 재발송 불필요
        if (customer.getEmailChecked()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }
        
        // 인증 토큰 생성
        String emailCheckToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiryAt = LocalDateTime.now().plusHours(24);
        
        // 토큰 설정
        customer.setEmailAuthToken(emailCheckToken, tokenExpiryAt);
        customerRepository.save(customer);
        
        // Account 엔티티도 업데이트
        AccountEntity account = customer.getAccount();
        if (account != null) {
            account.setEmailVerifyToken(emailCheckToken, tokenExpiryAt);
            accountRepository.save(account);
        }
        
        // 이력 기록
        try {
            String reason = "관리자 이메일 인증 재발송";
            CustomerHistoryEntity history = CustomerHistoryEntity.createEmailVerificationResend(
                customer, admin, reason
            );
            customerHistoryRepository.save(history);
        } catch (Exception e) {
            // 이력 기록 실패해도 작업은 계속 진행
        }
        
        // 이메일 발송
        try {
            emailService.resendEmailVerification(customer.getCustomerEmail(), emailCheckToken);
        } catch (Exception e) {
            // 이메일 발송 실패 시 예외 발생
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
    
    /**
     * 고객 관리 작업 이력 조회
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 작업 이력 목록
     */
    @Transactional(readOnly = true)
    public List<CustomerHistoryDto> getCustomerHistory(
            HttpSession session,
            Long customerId
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 존재 확인
        customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 이력 조회
        List<CustomerHistoryEntity> histories = customerHistoryRepository
                .findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId);
        
        return histories.stream()
                .map(history -> CustomerHistoryDto.builder()
                        .historyId(history.getHistoryId())
                        .customerId(history.getCustomer().getCustomerId())
                        .actionType(history.getActionType())
                        .oldValue(history.getOldValue())
                        .newValue(history.getNewValue())
                        .reason(history.getReason())
                        .adminId(history.getAdmin().getAdminId())
                        .adminName(history.getAdmin().getAdminName())
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 고객 메모 작성
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param requestDto 메모 내용
     * @return 작성된 메모 정보
     */
    @Transactional
    public CustomerNoteDto createCustomerNote(
            HttpSession session,
            Long customerId,
            CustomerNoteRequestDto requestDto
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 현재 관리자 정보 가져오기
        AdminEntity admin = getCurrentAdmin(session);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 메모 생성
        CustomerNoteEntity note = CustomerNoteEntity.create(
            customer,
            admin,
            requestDto.getNoteContent(),
            requestDto.getIsImportant()
        );
        
        note = customerNoteRepository.save(note);
        
        return CustomerNoteDto.builder()
                .noteId(note.getNoteId())
                .customerId(customer.getCustomerId())
                .noteContent(note.getNoteContent())
                .isImportant(note.getIsImportant())
                .adminId(admin.getAdminId())
                .adminName(admin.getAdminName())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
    
    /**
     * 고객 메모 수정
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param noteId 메모 ID
     * @param requestDto 수정할 내용
     * @return 수정된 메모 정보
     */
    @Transactional
    public CustomerNoteDto updateCustomerNote(
            HttpSession session,
            Long customerId,
            Long noteId,
            CustomerNoteRequestDto requestDto
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 메모 조회
        CustomerNoteEntity note = customerNoteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "메모를 찾을 수 없습니다."));
        
        // 메모가 해당 고객의 것인지 확인
        if (!note.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 고객의 메모가 아닙니다.");
        }
        
        // 메모 수정
        note.update(requestDto.getNoteContent(), requestDto.getIsImportant());
        note = customerNoteRepository.save(note);
        
        return CustomerNoteDto.builder()
                .noteId(note.getNoteId())
                .customerId(customer.getCustomerId())
                .noteContent(note.getNoteContent())
                .isImportant(note.getIsImportant())
                .adminId(note.getAdmin().getAdminId())
                .adminName(note.getAdmin().getAdminName())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
    
    /**
     * 고객 메모 삭제
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param noteId 메모 ID
     */
    @Transactional
    public void deleteCustomerNote(
            HttpSession session,
            Long customerId,
            Long noteId
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 메모 조회
        CustomerNoteEntity note = customerNoteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "메모를 찾을 수 없습니다."));
        
        // 메모가 해당 고객의 것인지 확인
        if (!note.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 고객의 메모가 아닙니다.");
        }
        
        // 메모 삭제
        customerNoteRepository.delete(note);
    }
    
    /**
     * 고객 메모 목록 조회
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 메모 목록
     */
    @Transactional(readOnly = true)
    public List<CustomerNoteDto> getCustomerNotes(
            HttpSession session,
            Long customerId
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 존재 확인
        customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 메모 목록 조회
        List<CustomerNoteEntity> notes = customerNoteRepository
                .findByCustomer_CustomerIdOrderByCreatedAtDesc(customerId);
        
        return notes.stream()
                .map(note -> CustomerNoteDto.builder()
                        .noteId(note.getNoteId())
                        .customerId(note.getCustomer().getCustomerId())
                        .noteContent(note.getNoteContent())
                        .isImportant(note.getIsImportant())
                        .adminId(note.getAdmin().getAdminId())
                        .adminName(note.getAdmin().getAdminName())
                        .createdAt(note.getCreatedAt())
                        .updatedAt(note.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 고객 태그 목록 조회
     * 
     * @param session HTTP 세션
     * @return 태그 목록
     */
    @Transactional(readOnly = true)
    public List<CustomerTagDto> getAllCustomerTags(HttpSession session) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        List<CustomerTagEntity> tags = customerTagRepository.findAllByOrderByTagNameAsc();
        
        return tags.stream()
                .map(tag -> {
                    long customerCount = customerTagMappingRepository.countByTag_TagId(tag.getTagId());
                    return CustomerTagDto.builder()
                            .tagId(tag.getTagId())
                            .tagName(tag.getTagName())
                            .tagColor(tag.getTagColor())
                            .description(tag.getDescription())
                            .customerCount(customerCount)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 고객 태그 생성
     * 
     * @param session HTTP 세션
     * @param requestDto 태그 정보
     * @return 생성된 태그 정보
     */
    @Transactional
    public CustomerTagDto createCustomerTag(
            HttpSession session,
            CustomerTagRequestDto requestDto
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 태그 이름 중복 확인
        if (customerTagRepository.findByTagName(requestDto.getTagName()).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 존재하는 태그 이름입니다.");
        }
        
        // 태그 생성
        CustomerTagEntity tag = CustomerTagEntity.create(
            requestDto.getTagName(),
            requestDto.getTagColor(),
            requestDto.getDescription()
        );
        
        tag = customerTagRepository.save(tag);
        
        return CustomerTagDto.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .tagColor(tag.getTagColor())
                .description(tag.getDescription())
                .customerCount(0L)
                .build();
    }
    
    /**
     * 고객 태그 수정
     * 
     * @param session HTTP 세션
     * @param tagId 태그 ID
     * @param requestDto 수정할 정보
     * @return 수정된 태그 정보
     */
    @Transactional
    public CustomerTagDto updateCustomerTag(
            HttpSession session,
            Long tagId,
            CustomerTagRequestDto requestDto
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 태그 조회
        CustomerTagEntity tag = customerTagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "태그를 찾을 수 없습니다."));
        
        // 태그 이름 변경 시 중복 확인
        if (!tag.getTagName().equals(requestDto.getTagName())) {
            if (customerTagRepository.findByTagName(requestDto.getTagName()).isPresent()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 존재하는 태그 이름입니다.");
            }
        }
        
        // 태그 수정
        tag.update(requestDto.getTagName(), requestDto.getTagColor(), requestDto.getDescription());
        tag = customerTagRepository.save(tag);
        
        long customerCount = customerTagMappingRepository.countByTag_TagId(tag.getTagId());
        
        return CustomerTagDto.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .tagColor(tag.getTagColor())
                .description(tag.getDescription())
                .customerCount(customerCount)
                .build();
    }
    
    /**
     * 고객 태그 삭제
     * 
     * @param session HTTP 세션
     * @param tagId 태그 ID
     */
    @Transactional
    public void deleteCustomerTag(HttpSession session, Long tagId) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 태그 조회
        CustomerTagEntity tag = customerTagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "태그를 찾을 수 없습니다."));
        
        // 태그를 사용하는 고객이 있는지 확인
        long customerCount = customerTagMappingRepository.countByTag_TagId(tagId);
        if (customerCount > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                "이 태그를 사용하는 고객이 " + customerCount + "명 있습니다. 먼저 고객에서 태그를 제거해주세요.");
        }
        
        // 태그 삭제
        customerTagRepository.delete(tag);
    }
    
    /**
     * 고객에 태그 추가
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param tagId 태그 ID
     */
    @Transactional
    public void addTagToCustomer(HttpSession session, Long customerId, Long tagId) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 태그 조회
        CustomerTagEntity tag = customerTagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "태그를 찾을 수 없습니다."));
        
        // 이미 태그가 있는지 확인
        List<CustomerTagMappingEntity> existingMappings = customerTagMappingRepository
                .findByCustomer_CustomerId(customerId);
        boolean alreadyHasTag = existingMappings.stream()
                .anyMatch(m -> m.getTag().getTagId().equals(tagId));
        
        if (alreadyHasTag) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 해당 태그가 추가되어 있습니다.");
        }
        
        // 태그 추가
        CustomerTagMappingEntity mapping = CustomerTagMappingEntity.create(customer, tag);
        customerTagMappingRepository.save(mapping);
    }
    
    /**
     * 고객에서 태그 제거
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param tagId 태그 ID
     */
    @Transactional
    public void removeTagFromCustomer(HttpSession session, Long customerId, Long tagId) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 존재 확인
        customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 태그 존재 확인
        customerTagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "태그를 찾을 수 없습니다."));
        
        // 태그 제거
        customerTagMappingRepository.deleteByCustomer_CustomerIdAndTag_TagId(customerId, tagId);
    }
    
    /**
     * 고객의 태그 목록 조회
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @return 태그 목록
     */
    @Transactional(readOnly = true)
    public List<CustomerTagDto> getCustomerTags(HttpSession session, Long customerId) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 존재 확인
        customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 고객의 태그 조회
        List<CustomerTagMappingEntity> mappings = customerTagMappingRepository
                .findByCustomer_CustomerId(customerId);
        
        return mappings.stream()
                .map(mapping -> {
                    CustomerTagEntity tag = mapping.getTag();
                    return CustomerTagDto.builder()
                            .tagId(tag.getTagId())
                            .tagName(tag.getTagName())
                            .tagColor(tag.getTagColor())
                            .description(tag.getDescription())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 고객 활동 로그 조회
     * 
     * @param session HTTP 세션
     * @param customerId 고객 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param pageSize 페이지 크기
     * @return 활동 로그 목록
     */
    @Transactional(readOnly = true)
    public List<CustomerActivityLogDto> getCustomerActivityLogs(
            HttpSession session,
            Long customerId,
            Integer page,
            Integer pageSize
    ) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 고객 존재 확인
        customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        // 페이징 설정
        int pageNum = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 50;
        Pageable pageable = PageRequest.of(pageNum, size, Sort.by(Sort.Direction.DESC, "activityAt"));
        
        // 활동 로그 조회
        Page<CustomerActivityLogEntity> logPage = customerActivityLogRepository
                .findByCustomer_CustomerIdOrderByActivityAtDesc(customerId, pageable);
        
        return logPage.getContent().stream()
                .map(log -> CustomerActivityLogDto.builder()
                        .logId(log.getLogId())
                        .customerId(log.getCustomer().getCustomerId())
                        .activityType(log.getActivityType())
                        .activityTypeLabel(log.getActivityType().getLabel())
                        .activityAt(log.getActivityAt())
                        .activityDetails(log.getActivityDetails())
                        .ipAddress(log.getIpAddress())
                        .userAgent(log.getUserAgent())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 현재 세션의 관리자 정보 가져오기
     */
    private AdminEntity getCurrentAdmin(HttpSession session) {
        var currentUser = authService.getCurrentUser(session);
        return adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }
    
    /**
     * 매출 현황 통계 조회
     * 
     * @param session HTTP 세션
     * @param startDate 시작 날짜 (선택, 기본값: 30일 전)
     * @param endDate 종료 날짜 (선택, 기본값: 오늘)
     * @return 매출 통계 정보
     */
    @Transactional(readOnly = true)
    public SalesStatisticsDto getSalesStatistics(HttpSession session, LocalDate startDate, LocalDate endDate) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 날짜 기본값 설정
        final LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();
        final LocalDate finalStartDate = startDate != null ? startDate : finalEndDate.minusDays(30);
        
        // 배송 완료되고 구매확정된 주문상품(OrderItem)만 조회
        List<OrderItemEntity> completedOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> {
                    // 취소되지 않은 주문상품만
                    if (item.getIsCancelled()) return false;
                    
                    // 배송이 있고 배송 완료 상태인 것만
                    if (item.getDelivery() == null) return false;
                    if (item.getDelivery().getDeliveryStatus() != DeliveryStatus.DELIVERED) return false;
                    
                    // 구매확정된 주문상품만 (completedAt이 null이 아니어야 함)
                    if (item.getCompletedAt() == null) return false;
                    
                    // 기간 필터링 (구매확정일 기준)
                    LocalDate completedDate = item.getCompletedAt().toLocalDate();
                    return !completedDate.isBefore(finalStartDate) && !completedDate.isAfter(finalEndDate);
                })
                .collect(Collectors.toList());
        
        // 전체 통계
        Long totalSales = completedOrderItems.stream()
                .mapToLong(OrderItemEntity::getItemTotalPrice)
                .sum();
        Long totalOrders = completedOrderItems.stream()
                .map(OrderItemEntity::getOrder)
                .map(OrderEntity::getOrderNo)
                .distinct()
                .count();
        Long totalQuantity = completedOrderItems.stream()
                .mapToLong(OrderItemEntity::getItemQuantity)
                .sum();
        Long averageOrderAmount = totalOrders > 0 ? totalSales / totalOrders : 0L;
        
        // 일별 매출
        List<SalesStatisticsDto.PeriodSalesDto> dailySales = calculateDailySales(completedOrderItems, finalStartDate, finalEndDate);
        
        // 주별 매출
        List<SalesStatisticsDto.PeriodSalesDto> weeklySales = calculateWeeklySales(completedOrderItems, finalStartDate, finalEndDate);
        
        // 월별 매출
        List<SalesStatisticsDto.PeriodSalesDto> monthlySales = calculateMonthlySales(completedOrderItems, finalStartDate, finalEndDate);
        
        // 상품별 매출 TOP 10
        List<SalesStatisticsDto.ProductSalesDto> topProducts = calculateTopProducts(completedOrderItems, 10);
        
        // 고객별 매출 TOP 10
        List<SalesStatisticsDto.CustomerSalesDto> topCustomers = calculateTopCustomers(completedOrderItems, 10);
        
        // 파트너별 매출
        List<SalesStatisticsDto.PartnerSalesDto> partnerSales = calculatePartnerSales(completedOrderItems);
        
        // 카테고리별 매출
        List<SalesStatisticsDto.CategorySalesDto> categorySales = calculateCategorySales(completedOrderItems);
        
        // 기간 비교 (이전 기간과 비교)
        PeriodComparisonDto periodComparison = calculatePeriodComparison(completedOrderItems, finalStartDate, finalEndDate);
        
        return SalesStatisticsDto.builder()
                .totalSales(totalSales)
                .totalOrders(totalOrders)
                .totalQuantity(totalQuantity)
                .averageOrderAmount(averageOrderAmount)
                .dailySales(dailySales)
                .weeklySales(weeklySales)
                .monthlySales(monthlySales)
                .topProducts(topProducts)
                .topCustomers(topCustomers)
                .partnerSales(partnerSales)
                .categorySales(categorySales)
                .periodComparison(periodComparison)
                .build();
    }
    
    /**
     * 일별 매출 계산
     */
    private List<SalesStatisticsDto.PeriodSalesDto> calculateDailySales(
            List<OrderItemEntity> orderItems, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<OrderItemEntity>> dailyMap = orderItems.stream()
                .collect(Collectors.groupingBy(item -> 
                    item.getCompletedAt().toLocalDate()));
        
        List<SalesStatisticsDto.PeriodSalesDto> result = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<OrderItemEntity> dayItems = dailyMap.getOrDefault(current, new ArrayList<>());
            Long daySales = dayItems.stream()
                    .mapToLong(OrderItemEntity::getItemTotalPrice)
                    .sum();
            Long dayOrderCount = dayItems.stream()
                    .map(OrderItemEntity::getOrder)
                    .map(OrderEntity::getOrderNo)
                    .distinct()
                    .count();
            Long dayQuantity = dayItems.stream()
                    .mapToLong(OrderItemEntity::getItemQuantity)
                    .sum();
            Long dayAverage = dayOrderCount > 0 ? daySales / dayOrderCount : 0L;
            
            result.add(SalesStatisticsDto.PeriodSalesDto.builder()
                    .date(current)
                    .totalSales(daySales)
                    .totalOrders(dayOrderCount)
                    .totalQuantity(dayQuantity)
                    .averageOrderAmount(dayAverage)
                    .build());
            current = current.plusDays(1);
        }
        return result;
    }
    
    /**
     * 주별 매출 계산
     */
    private List<SalesStatisticsDto.PeriodSalesDto> calculateWeeklySales(
            List<OrderItemEntity> orderItems, LocalDate startDate, LocalDate endDate) {
        // 주의 첫 날(월요일)을 키로 사용
        Map<LocalDate, List<OrderItemEntity>> weeklyMap = orderItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    LocalDate completedDate = item.getCompletedAt().toLocalDate();
                    // 해당 주의 월요일 찾기
                    DayOfWeek dayOfWeek = completedDate.getDayOfWeek();
                    int daysToSubtract = dayOfWeek.getValue() - 1; // 월요일이 1이므로
                    return completedDate.minusDays(daysToSubtract);
                }));
        
        return weeklyMap.entrySet().stream()
                .map(entry -> {
                    LocalDate weekStart = entry.getKey();
                    List<OrderItemEntity> weekItems = entry.getValue();
                    Long weekSales = weekItems.stream()
                            .mapToLong(OrderItemEntity::getItemTotalPrice)
                            .sum();
                    Long weekOrderCount = weekItems.stream()
                            .map(OrderItemEntity::getOrder)
                            .map(OrderEntity::getOrderNo)
                            .distinct()
                            .count();
                    Long weekQuantity = weekItems.stream()
                            .mapToLong(OrderItemEntity::getItemQuantity)
                            .sum();
                    Long weekAverage = weekOrderCount > 0 ? weekSales / weekOrderCount : 0L;
                    
                    return SalesStatisticsDto.PeriodSalesDto.builder()
                            .date(weekStart) // 주의 첫 날 (월요일)
                            .totalSales(weekSales)
                            .totalOrders(weekOrderCount)
                            .totalQuantity(weekQuantity)
                            .averageOrderAmount(weekAverage)
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto.PeriodSalesDto::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 월별 매출 계산
     */
    private List<SalesStatisticsDto.PeriodSalesDto> calculateMonthlySales(
            List<OrderItemEntity> orderItems, LocalDate startDate, LocalDate endDate) {
        Map<YearMonth, List<OrderItemEntity>> monthlyMap = orderItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    LocalDate completedDate = item.getCompletedAt().toLocalDate();
                    return YearMonth.from(completedDate);
                }));
        
        return monthlyMap.entrySet().stream()
                .map(entry -> {
                    YearMonth month = entry.getKey();
                    List<OrderItemEntity> monthItems = entry.getValue();
                    Long monthSales = monthItems.stream()
                            .mapToLong(OrderItemEntity::getItemTotalPrice)
                            .sum();
                    Long monthOrderCount = monthItems.stream()
                            .map(OrderItemEntity::getOrder)
                            .map(OrderEntity::getOrderNo)
                            .distinct()
                            .count();
                    Long monthQuantity = monthItems.stream()
                            .mapToLong(OrderItemEntity::getItemQuantity)
                            .sum();
                    Long monthAverage = monthOrderCount > 0 ? monthSales / monthOrderCount : 0L;
                    
                    return SalesStatisticsDto.PeriodSalesDto.builder()
                            .date(month.atDay(1)) // 월의 첫 날
                            .totalSales(monthSales)
                            .totalOrders(monthOrderCount)
                            .totalQuantity(monthQuantity)
                            .averageOrderAmount(monthAverage)
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto.PeriodSalesDto::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * 상품별 매출 TOP N 계산
     */
    private List<SalesStatisticsDto.ProductSalesDto> calculateTopProducts(
            List<OrderItemEntity> orderItems, int topN) {
        Map<Long, ProductSalesInfo> productMap = new HashMap<>();
        
        for (OrderItemEntity item : orderItems) {
            Long productNo = item.getProduct().getProductNo();
            ProductSalesInfo info = productMap.getOrDefault(productNo, 
                new ProductSalesInfo(item.getProduct().getProductName()));
            
            info.totalSales += item.getItemTotalPrice();
            info.totalQuantity += item.getItemQuantity();
            info.orderCount++;
            
            productMap.put(productNo, info);
        }
        
        return productMap.entrySet().stream()
                .map(entry -> SalesStatisticsDto.ProductSalesDto.builder()
                        .productNo(entry.getKey())
                        .productName(entry.getValue().productName)
                        .totalSales(entry.getValue().totalSales)
                        .totalQuantity(entry.getValue().totalQuantity)
                        .orderCount(entry.getValue().orderCount)
                        .build())
                .sorted(Comparator.comparing(SalesStatisticsDto.ProductSalesDto::getTotalSales).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }
    
    /**
     * 고객별 매출 TOP N 계산
     */
    private List<SalesStatisticsDto.CustomerSalesDto> calculateTopCustomers(
            List<OrderItemEntity> orderItems, int topN) {
        Map<Long, CustomerSalesInfo> customerMap = new HashMap<>();
        
        for (OrderItemEntity item : orderItems) {
            Long customerId = item.getOrder().getCustomer().getCustomerId();
            CustomerEntity customer = item.getOrder().getCustomer();
            CustomerSalesInfo info = customerMap.getOrDefault(customerId,
                new CustomerSalesInfo(
                    customer.getCustomerName(),
                    customer.getCustomerEmail()));
            
            info.totalSales += item.getItemTotalPrice();
            info.orderCount++;
            
            customerMap.put(customerId, info);
        }
        
        return customerMap.entrySet().stream()
                .map(entry -> {
                    CustomerSalesInfo info = entry.getValue();
                    Long avgAmount = info.orderCount > 0 ? info.totalSales / info.orderCount : 0L;
                    return SalesStatisticsDto.CustomerSalesDto.builder()
                            .customerId(entry.getKey())
                            .customerName(info.customerName)
                            .customerEmail(info.customerEmail)
                            .totalSales(info.totalSales)
                            .orderCount(info.orderCount)
                            .averageOrderAmount(avgAmount)
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto.CustomerSalesDto::getTotalSales).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }
    
    /**
     * 기간 비교 계산
     */
    private PeriodComparisonDto calculatePeriodComparison(
            List<OrderItemEntity> currentOrderItems, LocalDate startDate, LocalDate endDate) {
        // 현재 기간 통계
        Long currentSales = currentOrderItems.stream()
                .mapToLong(OrderItemEntity::getItemTotalPrice)
                .sum();
        Long currentOrdersCount = currentOrderItems.stream()
                .map(OrderItemEntity::getOrder)
                .map(OrderEntity::getOrderNo)
                .distinct()
                .count();
        
        // 이전 기간 계산 (같은 길이의 기간)
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate previousStartDate = startDate.minusDays(daysBetween);
        LocalDate previousEndDate = startDate.minusDays(1);
        
        // 이전 기간의 배송 완료되고 구매확정된 주문상품 조회
        List<OrderItemEntity> previousOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> {
                    // 취소되지 않은 주문상품만
                    if (item.getIsCancelled()) return false;
                    
                    // 배송이 있고 배송 완료 상태인 것만
                    if (item.getDelivery() == null) return false;
                    if (item.getDelivery().getDeliveryStatus() != DeliveryStatus.DELIVERED) return false;
                    
                    // 구매확정된 주문상품만
                    if (item.getCompletedAt() == null) return false;
                    
                    // 기간 필터링 (구매확정일 기준)
                    LocalDate completedDate = item.getCompletedAt().toLocalDate();
                    return !completedDate.isBefore(previousStartDate) && !completedDate.isAfter(previousEndDate);
                })
                .collect(Collectors.toList());
        
        Long previousSales = previousOrderItems.stream()
                .mapToLong(OrderItemEntity::getItemTotalPrice)
                .sum();
        Long previousOrdersCount = previousOrderItems.stream()
                .map(OrderItemEntity::getOrder)
                .map(OrderEntity::getOrderNo)
                .distinct()
                .count();
        
        // 변화율 계산
        Long salesChange = currentSales - previousSales;
        Double salesChangeRate = previousSales > 0 
                ? ((double) salesChange / previousSales) * 100.0 
                : (currentSales > 0 ? 100.0 : 0.0);
        
        Long orderChange = currentOrdersCount - previousOrdersCount;
        Double orderChangeRate = previousOrdersCount > 0 
                ? ((double) orderChange / previousOrdersCount) * 100.0 
                : (currentOrdersCount > 0 ? 100.0 : 0.0);
        
        return PeriodComparisonDto.builder()
                .currentPeriodSales(currentSales)
                .previousPeriodSales(previousSales)
                .salesChange(salesChange)
                .salesChangeRate(salesChangeRate)
                .currentPeriodOrders(currentOrdersCount)
                .previousPeriodOrders(previousOrdersCount)
                .orderChange(orderChange)
                .orderChangeRate(orderChangeRate)
                .build();
    }
    
    /**
     * 파트너별 매출 계산
     */
    private List<SalesStatisticsDto.PartnerSalesDto> calculatePartnerSales(List<OrderItemEntity> orderItems) {
        Map<Long, PartnerSalesInfo> partnerMap = new HashMap<>();
        
        for (OrderItemEntity item : orderItems) {
            com.swimshop.swim_mall.partner.entity.PartnerEntity partner = null;
            
            // Option이 있으면 Option의 파트너, 없으면 Product의 파트너
            if (item.getOption() != null) {
                partner = item.getOption().getPartner();
            } else if (item.getProduct() != null) {
                partner = item.getProduct().getPartner();
            }
            
            // 파트너가 없는 경우는 스킵 (일반 상품 등)
            if (partner == null) continue;
            
            Long partnerId = partner.getPartnerId();
            PartnerSalesInfo info = partnerMap.getOrDefault(partnerId,
                new PartnerSalesInfo(
                    partner.getPartnerName(),
                    partner.getEmail() != null ? partner.getEmail() : ""));
            
            info.totalSales += item.getItemTotalPrice();
            info.totalQuantity += item.getItemQuantity();
            info.orderCount++;
            
            partnerMap.put(partnerId, info);
        }
        
        return partnerMap.entrySet().stream()
                .map(entry -> {
                    PartnerSalesInfo info = entry.getValue();
                    Long avgAmount = info.orderCount > 0 ? info.totalSales / info.orderCount : 0L;
                    return SalesStatisticsDto.PartnerSalesDto.builder()
                            .partnerId(entry.getKey())
                            .partnerName(info.partnerName)
                            .partnerEmail(info.partnerEmail)
                            .totalSales(info.totalSales)
                            .totalQuantity(info.totalQuantity)
                            .orderCount(info.orderCount)
                            .averageOrderAmount(avgAmount)
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto.PartnerSalesDto::getTotalSales).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 매출 계산
     */
    private List<SalesStatisticsDto.CategorySalesDto> calculateCategorySales(List<OrderItemEntity> orderItems) {
        Map<String, CategorySalesInfo> categoryMap = new HashMap<>();
        
        for (OrderItemEntity item : orderItems) {
            ProductType productType = item.getProduct().getProductType();
            String categoryName = productType.getLabel();
            
            CategorySalesInfo info = categoryMap.getOrDefault(categoryName,
                new CategorySalesInfo(categoryName));
            
            info.totalSales += item.getItemTotalPrice();
            info.totalQuantity += item.getItemQuantity();
            info.orderCount++;
            
            categoryMap.put(categoryName, info);
        }
        
        return categoryMap.entrySet().stream()
                .map(entry -> {
                    CategorySalesInfo info = entry.getValue();
                    return SalesStatisticsDto.CategorySalesDto.builder()
                            .categoryName(info.categoryName)
                            .totalSales(info.totalSales)
                            .totalQuantity(info.totalQuantity)
                            .orderCount(info.orderCount)
                            .build();
                })
                .sorted(Comparator.comparing(SalesStatisticsDto.CategorySalesDto::getTotalSales).reversed())
                .collect(Collectors.toList());
    }
    
    // Helper classes
    private static class ProductSalesInfo {
        String productName;
        Long totalSales = 0L;
        Long totalQuantity = 0L;
        Long orderCount = 0L;
        
        ProductSalesInfo(String productName) {
            this.productName = productName;
        }
    }
    
    private static class CustomerSalesInfo {
        String customerName;
        String customerEmail;
        Long totalSales = 0L;
        Long orderCount = 0L;
        
        CustomerSalesInfo(String customerName, String customerEmail) {
            this.customerName = customerName;
            this.customerEmail = customerEmail;
        }
    }
    
    private static class PartnerSalesInfo {
        String partnerName;
        String partnerEmail;
        Long totalSales = 0L;
        Long totalQuantity = 0L;
        Long orderCount = 0L;
        
        PartnerSalesInfo(String partnerName, String partnerEmail) {
            this.partnerName = partnerName;
            this.partnerEmail = partnerEmail;
        }
    }
    
    private static class CategorySalesInfo {
        String categoryName;
        Long totalSales = 0L;
        Long totalQuantity = 0L;
        Long orderCount = 0L;
        
        CategorySalesInfo(String categoryName) {
            this.categoryName = categoryName;
        }
    }

    // ========== 파트너 고위험 정보 수정 신청 ==========

    @Transactional(readOnly = true)
    public List<PartnerChangeRequestResponseDto> getPartnerChangeRequests(HttpSession session, PartnerChangeRequestStatus status) {
        authService.requireRole(session, AccountRole.ADMIN);
        List<PartnerChangeRequestEntity> list = (status == null)
                ? partnerChangeRequestRepository.findAll()
                : partnerChangeRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        return list.stream().map(this::toPartnerChangeRequestDto).collect(Collectors.toList());
    }

    @Transactional
    public PartnerChangeRequestResponseDto approvePartnerChangeRequest(HttpSession session, Long requestId) {
        authService.requireRole(session, AccountRole.ADMIN);
        PartnerChangeRequestEntity request = partnerChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "수정 신청을 찾을 수 없습니다."));
        if (request.getStatus() != PartnerChangeRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "대기중인 신청만 승인할 수 있습니다.");
        }

        var currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        PartnerEntity partner = request.getPartner();
        partner.applyApprovedInfoChange(
                request.getRequestedPartnerName(),
                request.getRequestedPartnerBankAccount(),
                request.getRequestedBusinessRegistrationNumber(),
                request.getRequestedRepresentativeBrandCode()
        );
        partnerRepository.save(partner);

        PartnerChangeRequestEntity updated = PartnerChangeRequestEntity.builder()
                .requestId(request.getRequestId())
                .partner(request.getPartner())
                .requestedPartnerName(request.getRequestedPartnerName())
                .requestedPartnerBankAccount(request.getRequestedPartnerBankAccount())
                .requestedBusinessRegistrationNumber(request.getRequestedBusinessRegistrationNumber())
                .requestedRepresentativeBrandCode(request.getRequestedRepresentativeBrandCode())
                .status(PartnerChangeRequestStatus.APPROVED)
                .requestReason(request.getRequestReason())
                .rejectReason(null)
                .processedByAdmin(admin)
                .createdAt(request.getCreatedAt())
                .processedAt(LocalDateTime.now())
                .build();
        updated = partnerChangeRequestRepository.save(updated);
        return toPartnerChangeRequestDto(updated);
    }

    @Transactional
    public PartnerChangeRequestResponseDto rejectPartnerChangeRequest(HttpSession session, Long requestId, String rejectReason) {
        authService.requireRole(session, AccountRole.ADMIN);
        PartnerChangeRequestEntity request = partnerChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "수정 신청을 찾을 수 없습니다."));
        if (request.getStatus() != PartnerChangeRequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "대기중인 신청만 거절할 수 있습니다.");
        }

        var currentUser = authService.getCurrentUser(session);
        AdminEntity admin = adminRepository.findById(currentUser.getSubjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        PartnerChangeRequestEntity updated = PartnerChangeRequestEntity.builder()
                .requestId(request.getRequestId())
                .partner(request.getPartner())
                .requestedPartnerName(request.getRequestedPartnerName())
                .requestedPartnerBankAccount(request.getRequestedPartnerBankAccount())
                .requestedBusinessRegistrationNumber(request.getRequestedBusinessRegistrationNumber())
                .requestedRepresentativeBrandCode(request.getRequestedRepresentativeBrandCode())
                .status(PartnerChangeRequestStatus.REJECTED)
                .requestReason(request.getRequestReason())
                .rejectReason(rejectReason)
                .processedByAdmin(admin)
                .createdAt(request.getCreatedAt())
                .processedAt(LocalDateTime.now())
                .build();
        updated = partnerChangeRequestRepository.save(updated);
        return toPartnerChangeRequestDto(updated);
    }

    private PartnerChangeRequestResponseDto toPartnerChangeRequestDto(PartnerChangeRequestEntity e) {
        return PartnerChangeRequestResponseDto.builder()
                .requestId(e.getRequestId())
                .partnerId(e.getPartner().getPartnerId())
                .partnerName(e.getPartner().getPartnerName())
                .requestedPartnerName(e.getRequestedPartnerName())
                .requestedPartnerBankAccount(e.getRequestedPartnerBankAccount())
                .requestedBusinessRegistrationNumber(e.getRequestedBusinessRegistrationNumber())
                .requestedRepresentativeBrandCode(e.getRequestedRepresentativeBrandCode())
                .status(e.getStatus())
                .requestReason(e.getRequestReason())
                .rejectReason(e.getRejectReason())
                .processedAdminId(e.getProcessedByAdmin() != null ? e.getProcessedByAdmin().getAdminId() : null)
                .createdAt(e.getCreatedAt())
                .processedAt(e.getProcessedAt())
                .build();
    }
    
    // ========== 고객 등급 관리 ==========
    
    /**
     * 등급 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public List<CustomerGradeDto> getCustomerGrades(HttpSession session) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        return customerGradeRepository.findByIsActiveTrueOrderByGradeLevelAsc().stream()
                .map(this::toCustomerGradeDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 등급 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public CustomerGradeDto getCustomerGrade(HttpSession session, Long gradeId) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        CustomerGradeEntity grade = customerGradeRepository.findById(gradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GRADE_NOT_FOUND));
        
        return toCustomerGradeDto(grade);
    }
    
    /**
     * 등급 수정 (관리자용)
     * 등급명과 등급 레벨은 Enum으로 고정되어 있어 변경 불가능합니다.
     * 최소 구매액, 최소 주문 건수, 할인율, 포인트 적립률, 활성화 여부만 수정 가능합니다.
     */
    @Transactional
    public CustomerGradeDto updateCustomerGrade(HttpSession session, Long gradeId, CustomerGradeRequestDto requestDto) {
        authService.requireRole(session, AccountRole.ADMIN);
        
        CustomerGradeEntity grade = customerGradeRepository.findById(gradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GRADE_NOT_FOUND));
        
        // 등급명과 등급 레벨은 변경 불가 (Enum으로 고정)
        // 최소 구매액, 최소 주문 건수, 할인율, 포인트 적립률, 활성화 여부만 수정 가능
        grade.update(
                requestDto.getMinPurchaseAmount(),
                requestDto.getMinOrderCount() != null ? requestDto.getMinOrderCount() : 0,
                requestDto.getDiscountRate() != null ? requestDto.getDiscountRate() : 0.0,
                requestDto.getPointAccumulationRate() != null ? requestDto.getPointAccumulationRate() : 0.0,
                requestDto.getIsActive() != null ? requestDto.getIsActive() : true
        );
        
        grade = customerGradeRepository.save(grade);
        
        return toCustomerGradeDto(grade);
    }
    
    /**
     * CustomerGradeEntity를 CustomerGradeDto로 변환
     */
    private CustomerGradeDto toCustomerGradeDto(CustomerGradeEntity grade) {
        return CustomerGradeDto.builder()
                .gradeId(grade.getGradeId())
                .gradeName(grade.getGradeName().getCode()) // Enum을 String으로 변환
                .gradeLevel(grade.getGradeLevel())
                .minPurchaseAmount(grade.getMinPurchaseAmount())
                .minOrderCount(grade.getMinOrderCount())
                .discountRate(grade.getDiscountRate())
                .pointAccumulationRate(grade.getPointAccumulationRate())
                .isActive(grade.getIsActive())
                .createdAt(grade.getCreatedAt())
                .updatedAt(grade.getUpdatedAt())
                .build();
    }
}
