package com.swimshop.swim_mall.point.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.common.response.PagedResponse;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.point.dto.PointBalanceResponseDto;
import com.swimshop.swim_mall.point.dto.PointHistoryResponseDto;
import com.swimshop.swim_mall.point.dto.PointManualRequestDto;
import com.swimshop.swim_mall.point.entity.PointHistoryEntity;
import com.swimshop.swim_mall.point.service.PointService;
import com.swimshop.swim_mall.admin.repository.AdminRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PointController {

    private final PointService pointService;
    private final CustomerRepository customerRepository;
    private final AdminRepository adminRepository;
    private final AuthService authService;

    /**
     * 고객 포인트 잔액 조회
     * GET /api/customer/points/balance
     */
    @GetMapping("/customer/points/balance")
    public ResponseEntity<ApiResponse<PointBalanceResponseDto>> getPointBalance(HttpSession session) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 세션에서 고객 ID 가져오기
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId");
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        PointBalanceResponseDto response = new PointBalanceResponseDto(
                customer.getCustomerId(),
                customer.getCustomerName(),
                customer.getPointBalance()
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 고객 포인트 내역 조회
     * GET /api/customer/points/history
     */
    @GetMapping("/customer/points/history")
    public ResponseEntity<ApiResponse<PagedResponse<PointHistoryResponseDto>>> getPointHistory(
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 고객만 접근 가능
        authService.requireRole(session, AccountRole.CUSTOMER);
        
        // 세션에서 고객 ID 가져오기
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId");
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        List<PointHistoryEntity> histories = pointService.getPointHistory(customer);
        int total = histories.size();
        int from = Math.max(0, Math.min((page - 1) * size, total));
        int to = Math.max(from, Math.min(from + size, total));

        List<PointHistoryResponseDto> items = histories.subList(from, to).stream()
                .map(history -> new PointHistoryResponseDto(
                        history.getHistoryId(),
                        history.getPointType(),
                        history.getPointAmount(),
                        history.getPointBalanceAfter(),
                        history.getOrderItemNo(),
                        history.getOrderNo(),
                        history.getDescription(),
                        history.getExpireDate(),
                        history.getCreatedAt(),
                        history.getAdmin() != null ? history.getAdmin().getAdminName() : null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(new PagedResponse<>(items, total, page, size)));
    }

    /**
     * 관리자: 고객 포인트 수동 지급
     * POST /api/admin/customers/{customerId}/points/add
     */
    @PostMapping("/admin/customers/{customerId}/points/add")
    public ResponseEntity<ApiResponse<String>> addPointManually(
            @PathVariable Long customerId,
            @Valid @RequestBody PointManualRequestDto requestDto,
            HttpSession session) {
        // 관리자만 접근 가능
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 세션에서 관리자 ID 가져오기
        Object adminIdObj = session.getAttribute("subjectId");
        Long adminId = toLong(adminIdObj);
        
        if (adminId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        com.swimshop.swim_mall.admin.entity.AdminEntity admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
        
        pointService.manualAddPoint(customer, requestDto.getPointAmount(), requestDto.getDescription(), admin);
        
        return ResponseEntity.ok(ApiResponse.success("포인트가 지급되었습니다."));
    }

    /**
     * 관리자: 고객 포인트 수동 차감
     * POST /api/admin/customers/{customerId}/points/deduct
     */
    @PostMapping("/admin/customers/{customerId}/points/deduct")
    public ResponseEntity<ApiResponse<String>> deductPointManually(
            @PathVariable Long customerId,
            @Valid @RequestBody PointManualRequestDto requestDto,
            HttpSession session) {
        // 관리자만 접근 가능
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 세션에서 관리자 ID 가져오기
        Object adminIdObj = session.getAttribute("subjectId");
        Long adminId = toLong(adminIdObj);
        
        if (adminId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        com.swimshop.swim_mall.admin.entity.AdminEntity admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
        
        pointService.manualDeductPoint(customer, requestDto.getPointAmount(), requestDto.getDescription(), admin);
        
        return ResponseEntity.ok(ApiResponse.success("포인트가 차감되었습니다."));
    }

    /**
     * 관리자: 고객 포인트 내역 조회
     * GET /api/admin/customers/{customerId}/points/history
     */
    @GetMapping("/admin/customers/{customerId}/points/history")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getCustomerPointHistory(
            @PathVariable Long customerId,
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 관리자만 접근 가능
        authService.requireRole(session, AccountRole.ADMIN);
        
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
        
        List<PointHistoryEntity> histories = pointService.getPointHistory(customer);
        int total = histories.size();
        // 0-based 페이지 인덱싱
        int from = Math.max(0, Math.min(page * size, total));
        int to = Math.max(from, Math.min(from + size, total));
        List<PointHistoryEntity> pageSlice = histories.subList(from, to);
        
        List<PointHistoryResponseDto> items = pageSlice.stream()
                .map(history -> new PointHistoryResponseDto(
                        history.getHistoryId(),
                        history.getPointType(),
                        history.getPointAmount(),
                        history.getPointBalanceAfter(),
                        history.getOrderItemNo(),
                        history.getOrderNo(),
                        history.getDescription(),
                        history.getExpireDate(),
                        history.getCreatedAt(),
                        history.getAdmin() != null ? history.getAdmin().getAdminName() : null
                ))
                .collect(Collectors.toList());
        
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("pointHistory", items);
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", page); // 0-based 그대로
        meta.put("size", size);
        meta.put("total", total);
        body.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return (Long) value;
    }
}
