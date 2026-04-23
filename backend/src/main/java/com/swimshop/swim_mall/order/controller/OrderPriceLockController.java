package com.swimshop.swim_mall.order.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;

import jakarta.servlet.http.HttpSession;

@RequestMapping("/api/orders/price-lock")
@RestController
public class OrderPriceLockController {

    /**
     * 주문서 진입 시점에 15분 가격 보장을 시작합니다.
     * 클라이언트는 주문서 진입/새로고침 때 호출하면 됩니다.
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startLock(HttpSession session) {
        LocalDateTime now = LocalDateTime.now();
        session.setAttribute("priceLockStartedAt", now);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "startedAt", now,
                "expiresAt", now.plusMinutes(15)
        )));
    }

    /**
     * 현재 가격 보장 상태를 조회합니다.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(HttpSession session) {
        LocalDateTime startedAt = (LocalDateTime) session.getAttribute("priceLockStartedAt");
        if (startedAt == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "active", false
            )));
        }
        LocalDateTime expiresAt = startedAt.plusMinutes(15);
        boolean active = !LocalDateTime.now().isAfter(expiresAt);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "active", active,
                "startedAt", startedAt,
                "expiresAt", expiresAt
        )));
    }
}

