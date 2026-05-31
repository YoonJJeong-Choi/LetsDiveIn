package com.swimshop.swim_mall.account.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.dto.AuthLoginRequestDto;
import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 통합 로그인/로그아웃.
 * Customer, Partner, Admin 모두 이메일+비밀번호로 로그인 가능.
 */
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDto> login(
            @RequestBody AuthLoginRequestDto request,
            HttpSession session,
            HttpServletRequest httpRequest
    ) {
        AuthLoginResponseDto res = authService.login(request, httpRequest);

        session.setAttribute("accountRole", res.getRole().name());
        session.setAttribute("subjectId", res.getSubjectId());
        session.setAttribute("email", res.getEmail());
        session.setAttribute("name", res.getName());

        if (res.getRole() == AccountRole.CUSTOMER) {
            session.setAttribute("customerId", res.getSubjectId());
            session.setAttribute("customerEmail", res.getEmail());
            session.setAttribute("customerName", res.getName());
        }

        return ResponseEntity.ok(res);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthLoginResponseDto> me(HttpSession session, HttpServletRequest httpRequest) {
        AuthLoginResponseDto res = authService.getCurrentUser(session, httpRequest);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("로그아웃되었습니다.");
    }
}
