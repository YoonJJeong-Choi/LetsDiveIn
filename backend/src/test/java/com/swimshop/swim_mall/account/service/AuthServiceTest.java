package com.swimshop.swim_mall.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.swimshop.swim_mall.account.dto.AuthLoginRequestDto;
import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.account.repository.AccountRepository;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.AuthPortal;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.common.ratelimit.LoginLockoutService;
import com.swimshop.swim_mall.customer.reopository.CustomerActivityLogRepository;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CustomerActivityLogRepository customerActivityLogRepository;
    @Mock
    private LoginLockoutService loginLockoutService;
    @Mock
    private AccountEntity account;
    @Mock
    private PartnerEntity partner;
    @Mock
    private AdminEntity admin;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                accountRepository,
                customerRepository,
                passwordEncoder,
                customerActivityLogRepository,
                loginLockoutService);
        ReflectionTestUtils.setField(authService, "customerFrontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(authService, "adminFrontendUrl", "http://localhost:3001");
    }

    @Test
    void login_shouldRejectAdminAccountOnCustomerPortal() {
        AuthLoginRequestDto request = loginRequest("admin@swim-mall.com", "admin123", AuthPortal.CUSTOMER);
        stubAccountLookup("admin@swim-mall.com", "admin123", AccountRole.ADMIN);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(request, new MockHttpServletRequest()));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("고객몰"));
        assertTrue(ex.getMessage().contains("고객 계정"));
    }

    @Test
    void login_shouldInferAdminPortalFromOriginAndAllowPartner() {
        AuthLoginRequestDto request = loginRequest("partner1@swim-mall.com", "partner123", null);
        stubAccountLookup("partner1@swim-mall.com", "partner123", AccountRole.PARTNER);
        when(account.getEmail()).thenReturn("partner1@swim-mall.com");
        when(account.getPartner()).thenReturn(partner);
        when(partner.getPartnerId()).thenReturn(7L);
        when(partner.getPartnerName()).thenReturn("파트너");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("Origin", "http://127.0.0.1:3001");

        AuthLoginResponseDto response = authService.login(request, httpRequest);

        assertEquals(AccountRole.PARTNER, response.getRole());
        assertEquals(7L, response.getSubjectId());
        assertEquals("partner1@swim-mall.com", response.getEmail());
    }

    @Test
    void getCurrentUser_shouldRejectAdminSessionOnCustomerPortal() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("accountRole", AccountRole.ADMIN.name());
        session.setAttribute("subjectId", 3L);
        session.setAttribute("email", "admin@swim-mall.com");
        session.setAttribute("name", "관리자");

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("Referer", "http://localhost:3000/mypage");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.getCurrentUser(session, httpRequest));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("고객몰"));
    }

    @Test
    void login_shouldAllowLegacyRequestWithoutPortalOrOrigin() {
        AuthLoginRequestDto request = loginRequest("admin@swim-mall.com", "admin123", null);
        stubAccountLookup("admin@swim-mall.com", "admin123", AccountRole.ADMIN);
        when(account.getEmail()).thenReturn("admin@swim-mall.com");
        when(account.getAdmin()).thenReturn(admin);
        when(admin.getAdminId()).thenReturn(1L);
        when(admin.getAdminName()).thenReturn("관리자");

        AuthLoginResponseDto response = authService.login(request, new MockHttpServletRequest());

        assertEquals(AccountRole.ADMIN, response.getRole());
        assertEquals(1L, response.getSubjectId());
    }

    private AuthLoginRequestDto loginRequest(String email, String password, AuthPortal portal) {
        AuthLoginRequestDto request = new AuthLoginRequestDto();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "portal", portal);
        return request;
    }

    private void stubAccountLookup(String email, String rawPassword, AccountRole role) {
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(rawPassword, "encoded-password")).thenReturn(true);
        when(account.getPassword()).thenReturn("encoded-password");
        when(account.getStatus()).thenReturn("ACTIVE");
        when(account.getRole()).thenReturn(role);
    }
}
