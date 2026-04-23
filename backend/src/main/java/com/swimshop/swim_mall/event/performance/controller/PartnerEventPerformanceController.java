package com.swimshop.swim_mall.event.performance.controller;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.event.performance.dto.EventPerformanceListItemDto;
import com.swimshop.swim_mall.event.performance.dto.EventPerformanceResponseDto;
import com.swimshop.swim_mall.event.performance.service.EventPerformanceService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/partner/events")
public class PartnerEventPerformanceController {

	private final AuthService authService;
	private final EventPerformanceService performanceService;

	private Long requirePartnerId(HttpSession session) {
		authService.requireRole(session, AccountRole.PARTNER);
		var user = authService.getCurrentUser(session);
		return user.getSubjectId();
	}

	@GetMapping("/{eventNo}/performance")
	public ResponseEntity<ApiResponse<EventPerformanceResponseDto>> getPerformance(
			HttpSession session,
			@PathVariable Long eventNo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		Long partnerId = requirePartnerId(session);
		EventPerformanceResponseDto dto = performanceService.getPartnerPerformance(partnerId, eventNo, from, to);
		return ResponseEntity.ok(ApiResponse.success(dto));
	}

	@GetMapping("/performance/list")
	public ResponseEntity<ApiResponse<List<EventPerformanceListItemDto>>> getEndedPerformanceList(
			HttpSession session,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		Long partnerId = requirePartnerId(session);
		List<EventPerformanceListItemDto> list = performanceService.getPartnerEndedEventsPerformance(partnerId, from, to);
		return ResponseEntity.ok(ApiResponse.success(list));
	}

	@GetMapping("/performance/timeseries")
	public ResponseEntity<ApiResponse<java.util.List<com.swimshop.swim_mall.event.performance.dto.EventTimeseriesPointDto>>> getTimeseries(
			HttpSession session,
			@RequestParam(required = false) Long eventNo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		Long partnerId = requirePartnerId(session);
		var data = performanceService.getPartnerTimeseries(partnerId, eventNo, from, to);
		return ResponseEntity.ok(ApiResponse.success(data));
	}

	@GetMapping("/performance/top")
	public ResponseEntity<ApiResponse<java.util.List<com.swimshop.swim_mall.event.performance.dto.EventTopItemDto>>> getTop(
			HttpSession session,
			@RequestParam String type,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) Long eventNo,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
	) {
		Long partnerId = requirePartnerId(session);
		var data = performanceService.getPartnerTop(partnerId, eventNo, type, limit, from, to);
		return ResponseEntity.ok(ApiResponse.success(data));
	}
}

