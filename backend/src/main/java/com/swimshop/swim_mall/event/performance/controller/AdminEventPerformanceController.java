package com.swimshop.swim_mall.event.performance.controller;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.event.performance.dto.EventPerformanceResponseDto;
import com.swimshop.swim_mall.event.performance.dto.EventPerformanceListItemDto;
import com.swimshop.swim_mall.event.performance.dto.EventTimeseriesPointDto;
import com.swimshop.swim_mall.event.performance.dto.EventTopItemDto;
import com.swimshop.swim_mall.event.performance.service.EventPerformanceService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/events")
public class AdminEventPerformanceController {

    private final AuthService authService;
    private final EventPerformanceService performanceService;

    /**
     * 관리자: 이벤트 종료 실적 조회 (스냅샷/보상 기준)
     * GET /api/admin/events/{eventNo}/performance?from=...&to=...
     */
    @GetMapping("/{eventNo}/performance")
    public ResponseEntity<ApiResponse<EventPerformanceResponseDto>> getPerformance(
            HttpSession session,
            @PathVariable Long eventNo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        EventPerformanceResponseDto dto = performanceService.getAdminPerformance(eventNo, from, to);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    /**
     * 관리자: 종료된 이벤트 전체 실적 목록
     * GET /api/admin/events/performance/list?from=&to=
     */
    @GetMapping("/performance/list")
    public ResponseEntity<ApiResponse<List<EventPerformanceListItemDto>>> getEndedPerformanceList(
            HttpSession session,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        List<EventPerformanceListItemDto> list = performanceService.getEndedEventsPerformance(from, to);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 관리자: 실적 일자별 추이
     * GET /api/admin/events/performance/timeseries?eventNo=&from=&to=
     */
    @GetMapping("/performance/timeseries")
    public ResponseEntity<ApiResponse<List<EventTimeseriesPointDto>>> getTimeseries(
            HttpSession session,
            @RequestParam(required = false) Long eventNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        List<EventTimeseriesPointDto> data = performanceService.getTimeseries(eventNo, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 관리자: Top N 파트너/상품
     * GET /api/admin/events/performance/top?type=partner|product&limit=&eventNo=&from=&to=
     */
    @GetMapping("/performance/top")
    public ResponseEntity<ApiResponse<List<EventTopItemDto>>> getTop(
            HttpSession session,
            @RequestParam String type,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long eventNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        authService.requireRole(session, AccountRole.ADMIN);
        List<EventTopItemDto> data = performanceService.getTop(eventNo, type, limit, from, to);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

