package com.swimshop.swim_mall.customer.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.common.ratelimit.InMemoryRateLimiterService;
import com.swimshop.swim_mall.common.ratelimit.LoginLockoutService;
import com.swimshop.swim_mall.customer.dto.CustomerRequestDto;
import com.swimshop.swim_mall.customer.dto.CustomerResponseDto;
import com.swimshop.swim_mall.customer.dto.CustomerUpdateRequestDto;
import com.swimshop.swim_mall.customer.dto.PasswordChangeRequestDto;
import com.swimshop.swim_mall.customer.dto.AddressRequestDto;
import com.swimshop.swim_mall.customer.dto.AddressResponseDto;
import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.account.repository.AccountRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.entity.CustomerAddressEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.customer.reopository.CustomerAddressRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Transactional
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final InMemoryRateLimiterService rateLimiterService;

    private static final int TOKEN_EXPIRY_HOURS = 24; // 토큰 만료 시간 (24시간)
    private static final int SIGNUP_EMAIL_LIMIT = 3;
    private static final long SIGNUP_EMAIL_WINDOW_SECONDS = 3600;

    /**
     * 회원가입
     * 인증 토큰 생성 & 이메일 발송
     */
    public void join(CustomerRequestDto requestDto) {
        String emailKey = LoginLockoutService.normalizeEmail(requestDto.getCustomerEmail());
        if (!rateLimiterService.isAllowed("signup:email:" + emailKey, SIGNUP_EMAIL_LIMIT, SIGNUP_EMAIL_WINDOW_SECONDS)) {
            throw new BusinessException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "해당 이메일로 가입 요청이 너무 많습니다. 1시간 후 다시 시도해 주세요.");
        }

        // 이메일 중복 확인 (Customer 또는 Account) — 인증 완료 여부와 무관하게 동일 이메일 1계정
        if (customerRepository.existsByCustomerEmail(requestDto.getCustomerEmail())
                || accountRepository.existsByEmail(requestDto.getCustomerEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 가입된 이메일입니다.");
        }

        // 인증 토큰 생성, 만료 시간
        String emailCheckToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiryAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(requestDto.getCustomerPassword());

        // 1) Account 먼저 생성 (FK는 Customer 쪽에 있으므로 Account 선저장)
        AccountEntity account = AccountEntity.builder()
                .email(requestDto.getCustomerEmail())
                .password(encodedPassword)
                .role(AccountRole.CUSTOMER)
                .emailVerified(false)
                .emailVerifyToken(emailCheckToken)
                .emailVerifyExpiresAt(tokenExpiryAt)
                // createdAt은 @PrePersist에서 자동 설정됨
                .build();
        accountRepository.save(account);

        // 2) Customer 생성 후 Account와 1:1 연결
        CustomerEntity entity = CustomerEntity.fromDto(requestDto);
        entity.setCustomerPassword(encodedPassword);
        entity.setEmailAuthToken(emailCheckToken, tokenExpiryAt);
        entity.setAccount(account);
        customerRepository.save(entity);

        // 인증 이메일 발송
        emailService.sendCheckEmail(requestDto.getCustomerEmail(), emailCheckToken);
    }

    /**
     * 이메일 인증 확인
     */
    public void checkEmail(String token) {
        CustomerEntity customer = customerRepository.findByEmailCheckToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 인증 토큰입니다."));

        // 토큰 만료 확인
        if (customer.getTokenExpiryAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증 토큰이 만료되었습니다. 다시 회원가입을 진행해주세요.");
        }

        // 이미 인증된 경우
        if (customer.getEmailChecked()) {
            throw new IllegalArgumentException("이미 인증된 이메일입니다.");
        }

        // 이메일 인증 완료
        customer.checkEmailCompleted();
        customerRepository.save(customer);

        // Account 이메일 인증 상태 동기화
        accountRepository.findByCustomer_CustomerId(customer.getCustomerId())
                .ifPresent(acc -> {
                    acc.setEmailVerified(true);
                    accountRepository.save(acc);
                });
    }

    /**
     * 현재 로그인한 사용자 정보 조회 (고객 상세)
     * 로그인 상태 확인
     */
    public CustomerResponseDto getCurrentUser(HttpSession session) {
        // 세션에서 고객 ID (통합 로그인 시 customerId 또는 subjectId)
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId"); // 통합 로그인(CUSTOMER) 시 subjectId
        }
        Long customerId = toLong(subjObj);

        if (customerId == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        // DB에서 사용자 정보 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // DTO로 변환하여 반환
        return new CustomerResponseDto(
                customer.getCustomerId(),
                customer.getCustomerName(),
                customer.getCustomerEmail(),
                customer.getCustomerBirth(),
                customer.getCustomerCreateAt()
        );
    }

    /**
     * 마이페이지 정보 조회
     */
    public CustomerResponseDto getMyPage(HttpSession session) {
        // 현재는 기본 사용자 정보만
        // 향후 주문 내역, 리뷰, 쿠폰 등 추가 정보
        CustomerResponseDto customer = getCurrentUser(session);
        
        // 향후 확장 예시:
        // List<OrderDto> orders = orderService.getOrdersByCustomer(customer.getCustomerId());
        // List<ReviewDto> reviews = reviewService.getReviewsByCustomer(customer.getCustomerId());
        // return new MyPageResponseDto(customer, orders, reviews);
        
        return customer;
    }

    /**
     * 프로필 정보 수정
     * PUT /api/customer/me
     * 주의: 이메일과 생년월일은 수정 불가
     * - 이메일: 보안상 계정 식별자 역할
     * - 생년월일: 결제 시스템과 연관된 법적 요구사항 및 데이터 무결성 유지
     */
    public CustomerResponseDto updateProfile(HttpSession session, CustomerUpdateRequestDto requestDto) {
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 프로필 정보 업데이트 (이메일, 생년월일 제외)
        customer.updateProfile(requestDto.getCustomerName());
        customerRepository.save(customer);

        // 세션의 name과 customerName도 함께 업데이트 (통합 로그인 세션 동기화)
        session.setAttribute("name", requestDto.getCustomerName());
        session.setAttribute("customerName", requestDto.getCustomerName());

        return new CustomerResponseDto(
                customer.getCustomerId(),
                customer.getCustomerName(),
                customer.getCustomerEmail(),
                customer.getCustomerBirth(),
                customer.getCustomerCreateAt()
        );
    }

    /**
     * 비밀번호 변경
     * PUT /api/customer/password
     */
    public void changePassword(HttpSession session, PasswordChangeRequestDto requestDto) {
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), customer.getCustomerPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호와 확인 비밀번호 일치 확인
        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 암호화 및 저장
        String encodedNewPassword = passwordEncoder.encode(requestDto.getNewPassword());
        customer.setCustomerPassword(encodedNewPassword);
        customerRepository.save(customer);

        // Account 비밀번호도 동기화
        AccountEntity account = customer.getAccount();
        if (account != null) {
            account.setPassword(encodedNewPassword);
            accountRepository.save(account);
        }
    }

    /**
     * 주소 목록 조회
     * GET /api/customer/addresses
     */
    public List<AddressResponseDto> getAddresses(HttpSession session) {
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        List<CustomerAddressEntity> addresses = addressRepository.findByCustomerOrderByIsDefaultDescCreatedAtDesc(customer);
        return addresses.stream()
                .map(this::toAddressResponseDto)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 주소 추가
     * POST /api/customer/addresses
     */
    public AddressResponseDto addAddress(HttpSession session, AddressRequestDto requestDto) {
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 기본 주소로 설정하는 경우, 기존 기본 주소 해제
        if (requestDto.getIsDefault() != null && requestDto.getIsDefault()) {
            addressRepository.findByCustomerAndIsDefaultTrue(customer)
                    .ifPresent(addr -> {
                        addr.unsetAsDefault();
                        addressRepository.save(addr);
                    });
        }

        // 새 주소 생성
        CustomerAddressEntity address = CustomerAddressEntity.builder()
                .customer(customer)
                .recipientName(requestDto.getRecipientName())
                .recipientPhone(requestDto.getRecipientPhone())
                .deliveryAddress(requestDto.getDeliveryAddress())
                .deliveryAddressDetail(requestDto.getDeliveryAddressDetail())
                .deliveryZipCode(requestDto.getDeliveryZipCode())
                .isDefault(requestDto.getIsDefault() != null ? requestDto.getIsDefault() : false)
                .build();

        address = addressRepository.save(address);
        return toAddressResponseDto(address);
    }

    /**
     * 주소 수정
     * PUT /api/customer/addresses/{addressNo}
     */
    public AddressResponseDto updateAddress(HttpSession session, Long addressNo, AddressRequestDto requestDto) {
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        CustomerAddressEntity address = addressRepository.findById(addressNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "주소를 찾을 수 없습니다."));

        // 본인의 주소인지 확인
        if (!address.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 주소만 수정할 수 있습니다.");
        }

        // 기본 주소로 설정하는 경우, 기존 기본 주소 해제
        if (requestDto.getIsDefault() != null && requestDto.getIsDefault() && !address.getIsDefault()) {
            addressRepository.findByCustomerAndIsDefaultTrue(customer)
                    .ifPresent(addr -> {
                        if (!addr.getAddressNo().equals(addressNo)) {
                            addr.unsetAsDefault();
                            addressRepository.save(addr);
                        }
                    });
        }

        // 주소 정보 업데이트
        address.updateAddress(
                requestDto.getRecipientName(),
                requestDto.getRecipientPhone(),
                requestDto.getDeliveryAddress(),
                requestDto.getDeliveryAddressDetail(),
                requestDto.getDeliveryZipCode()
        );

        if (requestDto.getIsDefault() != null) {
            if (requestDto.getIsDefault()) {
                address.setAsDefault();
            } else {
                address.unsetAsDefault();
            }
        }

        address = addressRepository.save(address);
        return toAddressResponseDto(address);
    }

    /**
     * 주소 삭제
     * DELETE /api/customer/addresses/{addressNo}
     */
    public void deleteAddress(HttpSession session, Long addressNo) {
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        CustomerAddressEntity address = addressRepository.findById(addressNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "주소를 찾을 수 없습니다."));

        // 본인의 주소인지 확인
        if (!address.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 주소만 삭제할 수 있습니다.");
        }

        addressRepository.delete(address);
    }

    /**
     * 주소 엔티티를 DTO로 변환
     */
    private AddressResponseDto toAddressResponseDto(CustomerAddressEntity address) {
        return AddressResponseDto.builder()
                .addressNo(address.getAddressNo())
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .deliveryAddress(address.getDeliveryAddress())
                .deliveryAddressDetail(address.getDeliveryAddressDetail())
                .deliveryZipCode(address.getDeliveryZipCode())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    /**
     * 세션에서 고객 ID 가져오기
     */
    private Long getCustomerIdFromSession(HttpSession session) {
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId");
        }
        Long customerId = toLong(subjObj);
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return customerId;
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return (Long) value;
    }
}
