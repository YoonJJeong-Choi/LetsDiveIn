package com.swimshop.swim_mall.account.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.dto.AuthLoginRequestDto;
import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.account.repository.AccountRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.AuthPortal;
import com.swimshop.swim_mall.common.enums.CustomerActivityType;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.common.ratelimit.LoginLockoutService;
import com.swimshop.swim_mall.customer.entity.CustomerActivityLogEntity;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerActivityLogRepository;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * 통합 로그인: Account 우선, 없으면 Customer(기존) 로그인으로 폴백.
 */
@Transactional
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerActivityLogRepository customerActivityLogRepository;
    private final LoginLockoutService loginLockoutService;
    @Value("${app.customer-frontend-url:http://localhost:3000}")
    private String customerFrontendUrl;
    @Value("${app.admin-frontend-url:http://localhost:3001}")
    private String adminFrontendUrl;

    public AuthLoginResponseDto login(AuthLoginRequestDto request, HttpServletRequest httpRequest) {
        String email = request.getEmail();
        String emailKey = LoginLockoutService.normalizeEmail(email);
        String rawPassword = request.getPassword();
        AuthPortal portal = resolvePortal(request, httpRequest);

        loginLockoutService.assertNotLocked(emailKey);

        try {
            return doLogin(email, rawPassword, portal, httpRequest, emailKey);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.INVALID_CREDENTIALS) {
                loginLockoutService.recordFailure(emailKey);
            }
            throw e;
        }
    }

    private AuthLoginResponseDto doLogin(
            String email,
            String rawPassword,
            AuthPortal portal,
            HttpServletRequest httpRequest,
            String emailKey
    ) {
        // 1) Account로 로그인 시도
        var accountOpt = accountRepository.findByEmail(email);
        if (accountOpt.isPresent()) {
            AccountEntity account = accountOpt.get();
            if (!passwordEncoder.matches(rawPassword, account.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
            // 계정 상태 확인 (INACTIVE면 로그인 불가)
            if ("INACTIVE".equals(account.getStatus())) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            if (account.getRole() == AccountRole.CUSTOMER && !Boolean.TRUE.equals(account.getEmailVerified())) {
                throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
            }
            validatePortalAccess(account.getRole(), portal);
            
            // 고객 로그인인 경우 활동 로그 기록
            if (account.getRole() == AccountRole.CUSTOMER && account.getCustomer() != null) {
                recordLoginActivity(account.getCustomer(), httpRequest);
            }

            loginLockoutService.clearFailures(emailKey);
            return buildResponseFromAccount(account);
        }

        // 2) 레거시: Customer 이메일로 조회
        CustomerEntity customer = customerRepository.findByCustomerEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(rawPassword, customer.getCustomerPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        // Account가 있으면 상태 확인
        if (customer.getAccount() != null && "INACTIVE".equals(customer.getAccount().getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
        if (!customer.getEmailChecked()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        validatePortalAccess(AccountRole.CUSTOMER, portal);
        
        // 활동 로그 기록
        recordLoginActivity(customer, httpRequest);

        loginLockoutService.clearFailures(emailKey);
        return AuthLoginResponseDto.of(
                AccountRole.CUSTOMER,
                customer.getCustomerId(),
                customer.getCustomerEmail(),
                customer.getCustomerName()
        );
    }

    private AuthLoginResponseDto buildResponseFromAccount(AccountEntity account) {
        AccountRole role = account.getRole();
        Long subjectId;
        String name;

        switch (role) {
            case CUSTOMER -> {
                CustomerEntity c = account.getCustomer();
                if (c == null) throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                subjectId = c.getCustomerId();
                name = c.getCustomerName();
            }
            case PARTNER -> {
                var p = account.getPartner();
                if (p == null) throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                subjectId = p.getPartnerId();
                name = p.getPartnerName();
            }
            case ADMIN -> {
                var a = account.getAdmin();
                if (a == null) throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
                subjectId = a.getAdminId();
                name = a.getAdminName();
            }
            default -> throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return AuthLoginResponseDto.of(role, subjectId, account.getEmail(), name);
    }

    /**
     * 통합 로그인 확인 — 현재 세션의 유저 정보 반환.
     * /api/auth/login 또는 /api/customer/login(레거시) 둘 다 지원.
     */
    public AuthLoginResponseDto getCurrentUser(HttpSession session) {
        // 1) 통합 로그인 세션 (accountRole, subjectId, email, name)
        String roleStr = (String) session.getAttribute("accountRole");
        Object subjObj = session.getAttribute("subjectId");
        Long subjectId = toLong(subjObj);
        String email = (String) session.getAttribute("email");
        String name = (String) session.getAttribute("name");

        if (roleStr != null && subjectId != null && email != null && name != null) {
            return AuthLoginResponseDto.of(
                    AccountRole.valueOf(roleStr),
                    subjectId,
                    email,
                    name
            );
        }

        // 2) 레거시: /api/customer/login 세션 (customerId만 있음)
        Long customerId = toLong(session.getAttribute("customerId"));
        if (customerId != null) {
            CustomerEntity customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
            return AuthLoginResponseDto.of(
                    AccountRole.CUSTOMER,
                    customer.getCustomerId(),
                    customer.getCustomerEmail(),
                    customer.getCustomerName()
            );
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    public AuthLoginResponseDto getCurrentUser(HttpSession session, HttpServletRequest httpRequest) {
        AuthLoginResponseDto currentUser = getCurrentUser(session);
        validatePortalAccess(currentUser.getRole(), resolvePortal(null, httpRequest));
        return currentUser;
    }
    
    /**
     * 로그인 활동 로그 기록
     */
    private void recordLoginActivity(CustomerEntity customer, HttpServletRequest httpRequest) {
        try {
            String ipAddress = httpRequest != null ? getClientIpAddress(httpRequest) : null;
            String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
            
            CustomerActivityLogEntity log = CustomerActivityLogEntity.create(
                customer,
                CustomerActivityType.LOGIN,
                null, // 로그인은 상세 정보 없음
                ipAddress,
                userAgent
            );
            customerActivityLogRepository.save(log);
        } catch (Exception e) {
            // 로그 기록 실패해도 로그인은 계속 진행
            // 로그만 남기고 예외는 던지지 않음
        }
    }
    
    /**
     * 클라이언트 IP 주소 가져오기
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return null;
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * 현재 세션의 사용자가 지정된 role을 가지고 있는지 확인.
     * 권한이 없으면 FORBIDDEN 예외를 던짐.
     * 
     * 사용 예시:
     *   authService.requireRole(session, AccountRole.CUSTOMER);
     *   authService.requireRole(session, AccountRole.ADMIN);
     * 
     * @param session HTTP 세션
     * @param requiredRole 필요한 role (CUSTOMER, PARTNER, ADMIN)
     * @throws BusinessException role이 맞지 않으면 FORBIDDEN 에러
     */
    public void requireRole(HttpSession session, AccountRole requiredRole) {
        AuthLoginResponseDto currentUser = getCurrentUser(session);
        if (currentUser.getRole() != requiredRole) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 현재 세션의 사용자가 지정된 role 중 하나라도 가지고 있는지 확인.
     * 권한이 없으면 FORBIDDEN 예외를 던짐.
     * 
     * 사용 예시:
     *   authService.requireRole(session, AccountRole.ADMIN, AccountRole.PARTNER);
     * 
     * @param session HTTP 세션
     * @param requiredRoles 허용된 role들 (하나라도 일치하면 통과)
     * @throws BusinessException role이 맞지 않으면 FORBIDDEN 에러
     */
    public void requireRole(HttpSession session, AccountRole... requiredRoles) {
        AuthLoginResponseDto currentUser = getCurrentUser(session);
        AccountRole userRole = currentUser.getRole();
        
        for (AccountRole requiredRole : requiredRoles) {
            if (userRole == requiredRole) {
                return; // 하나라도 일치하면 통과
            }
        }
        
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return (Long) value;
    }

    private void validatePortalAccess(AccountRole role, AuthPortal portal) {
        if (portal == null || portal.supports(role)) {
            return;
        }

        throw new BusinessException(
                ErrorCode.FORBIDDEN,
                "%s에서는 %s만 로그인할 수 있습니다.".formatted(portal.getLabel(), portal.getAllowedRoleLabel()));
    }

    private AuthPortal resolvePortal(AuthLoginRequestDto request, HttpServletRequest httpRequest) {
        if (request != null && request.getPortal() != null) {
            return request.getPortal();
        }
        if (httpRequest == null) {
            return null;
        }

        String source = firstNonBlank(httpRequest.getHeader("Origin"), httpRequest.getHeader("Referer"));
        if (matchesOrigin(source, customerFrontendUrl)) {
            return AuthPortal.CUSTOMER;
        }
        if (matchesOrigin(source, adminFrontendUrl)) {
            return AuthPortal.ADMIN;
        }
        return null;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }

    private boolean matchesOrigin(String actualUrl, String configuredUrl) {
        URI actual = parseUri(actualUrl);
        URI configured = parseUri(configuredUrl);
        if (actual == null || configured == null) {
            return false;
        }
        if (!sameScheme(actual, configured)) {
            return false;
        }
        if (resolvePort(actual) != resolvePort(configured)) {
            return false;
        }
        return sameHost(actual.getHost(), configured.getHost());
    }

    private URI parseUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return URI.create(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean sameScheme(URI left, URI right) {
        String leftScheme = left.getScheme() == null ? "" : left.getScheme();
        String rightScheme = right.getScheme() == null ? "" : right.getScheme();
        return leftScheme.equalsIgnoreCase(rightScheme);
    }

    private int resolvePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private boolean sameHost(String leftHost, String rightHost) {
        if (leftHost == null || rightHost == null) {
            return false;
        }
        if (leftHost.equalsIgnoreCase(rightHost)) {
            return true;
        }
        return isLocalAlias(leftHost, rightHost);
    }

    private boolean isLocalAlias(String leftHost, String rightHost) {
        String left = leftHost.toLowerCase();
        String right = rightHost.toLowerCase();
        return ("localhost".equals(left) && "127.0.0.1".equals(right))
                || ("127.0.0.1".equals(left) && "localhost".equals(right));
    }
}
