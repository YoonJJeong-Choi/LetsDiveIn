package com.swimshop.swim_mall.customer.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.customer.dto.CustomerRequestDto;
import com.swimshop.swim_mall.customer.dto.CustomerResponseDto;
import com.swimshop.swim_mall.customer.dto.CustomerUpdateRequestDto;
import com.swimshop.swim_mall.customer.dto.PasswordChangeRequestDto;
import com.swimshop.swim_mall.customer.dto.AddressRequestDto;
import com.swimshop.swim_mall.customer.dto.AddressResponseDto;
import com.swimshop.swim_mall.customer.service.CustomerService;

import jakarta.validation.Valid;

import jakarta.servlet.http.HttpSession;

@RequestMapping("/api/customer")
@RequiredArgsConstructor
@RestController
public class CustomerController {

    private final CustomerService customerService;
    private final AuthService authService; // AuthService 주입

    @Value("${app.customer-frontend-url:http://localhost:3000}")
    private String customerFrontendUrl;

    //회원가입
    @PostMapping("/join")
    public ResponseEntity<String> join(@RequestBody CustomerRequestDto requestDto){
        customerService.join(requestDto);
        return ResponseEntity.ok("입력하신 이메일로 인증 요청하였습니다.");
    }

    //이메일 인증 확인 (이메일 링크 클릭 시 → 고객 프론트 안내 페이지로 리다이렉트)
    @GetMapping("/check")
    public ResponseEntity<Void> checkEmail(@RequestParam String token) {
        String base = customerFrontendUrl.replaceAll("/$", "");
        try {
            customerService.checkEmail(token);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(base + "/email-verified?status=success"))
                    .build();
        } catch (IllegalArgumentException ex) {
            String code = "invalid";
            String msg = ex.getMessage();
            if (msg != null) {
                if (msg.contains("만료")) {
                    code = "expired";
                } else if (msg.contains("이미 인증")) {
                    code = "already";
                }
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(base + "/email-verified?status=" + code))
                    .build();
        }
    }

    //로그인 상태 확인 (현재 로그인한 사용자 — 고객 상세 정보)
    @GetMapping("/me")
    public ResponseEntity<CustomerResponseDto> me(HttpSession session){
        // TODO: 프로젝트 완성 후 role 체크 추가
        // authService.requireRole(session, AccountRole.CUSTOMER);
        
        CustomerResponseDto customer = customerService.getCurrentUser(session);
        return ResponseEntity.ok(customer);
    }

    //마이페이지
    @GetMapping("/mypage")
    public ResponseEntity<CustomerResponseDto> mypage(HttpSession session){
        // TODO: 프로젝트 완성 후 role 체크 추가
        // authService.requireRole(session, AccountRole.CUSTOMER);
        
        CustomerResponseDto customer = customerService.getMyPage(session);
        return ResponseEntity.ok(customer);
    }

    /**
     * 프로필 정보 수정
     * PUT /api/customer/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> updateProfile(
            HttpSession session,
            @Valid @RequestBody CustomerUpdateRequestDto requestDto) {
        CustomerResponseDto customer = customerService.updateProfile(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(customer));
    }

    /**
     * 비밀번호 변경
     * PUT /api/customer/password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            HttpSession session,
            @Valid @RequestBody PasswordChangeRequestDto requestDto) {
        customerService.changePassword(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다."));
    }

    /**
     * 주소 목록 조회
     * GET /api/customer/addresses
     */
    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponseDto>>> getAddresses(HttpSession session) {
        List<AddressResponseDto> addresses = customerService.getAddresses(session);
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    /**
     * 주소 추가
     * POST /api/customer/addresses
     */
    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponseDto>> addAddress(
            HttpSession session,
            @Valid @RequestBody AddressRequestDto requestDto) {
        AddressResponseDto address = customerService.addAddress(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    /**
     * 주소 수정
     * PUT /api/customer/addresses/{addressNo}
     */
    @PutMapping("/addresses/{addressNo}")
    public ResponseEntity<ApiResponse<AddressResponseDto>> updateAddress(
            HttpSession session,
            @PathVariable Long addressNo,
            @Valid @RequestBody AddressRequestDto requestDto) {
        AddressResponseDto address = customerService.updateAddress(session, addressNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    /**
     * 주소 삭제
     * DELETE /api/customer/addresses/{addressNo}
     */
    @DeleteMapping("/addresses/{addressNo}")
    public ResponseEntity<ApiResponse<String>> deleteAddress(
            HttpSession session,
            @PathVariable Long addressNo) {
        customerService.deleteAddress(session, addressNo);
        return ResponseEntity.ok(ApiResponse.success("주소가 삭제되었습니다."));
    }
    
}
