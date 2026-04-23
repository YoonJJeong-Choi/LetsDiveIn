package com.swimshop.swim_mall.settlement.controller;

import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.settlement.dto.SettlementDetailResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementListResponseDto;
import com.swimshop.swim_mall.settlement.dto.SettlementResponseDto;
import com.swimshop.swim_mall.settlement.service.SettlementService;
import com.swimshop.swim_mall.settlement.service.SettlementExportService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 정산 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/partner/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementExportService settlementExportService;

    /**
     * 파트너 정산 대상 목록 조회
     * GET /api/partner/settlement
     * 
     * @param session HTTP 세션
     * @param startDate 정산 기간 시작일 (선택, 형식: yyyy-MM-dd)
     * @param endDate 정산 기간 종료일 (선택, 형식: yyyy-MM-dd)
     * @param status 필터 상태 (선택, "SETTLEMENT_READY": 정산 가능만, "ALL": 전체, 기본값: "ALL")
     * @return 정산 대상 목록 및 합계 정보
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SettlementResponseDto>> getSettlementItems(
            HttpSession session,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate startDate,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) 
            LocalDate endDate,
            @RequestParam(required = false, defaultValue = "ALL") 
            String status
    ) {
        SettlementResponseDto response = settlementService.getSettlementItems(
                session, 
                startDate, 
                endDate, 
                status
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 파트너 생성된 정산 목록 조회
     * GET /api/partner/settlements
     * 
     * @param session HTTP 세션
     * @param status 정산 상태 필터 (선택, "PENDING", "COMPLETED", "CANCELLED", null이면 전체)
     * @return 생성된 정산 목록
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<SettlementListResponseDto>> getSettlementList(
            HttpSession session,
            @RequestParam(required = false) String status
    ) {
        SettlementStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = SettlementStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 유효하지 않은 상태값은 무시
            }
        }
        
        SettlementListResponseDto response = settlementService.getPartnerSettlementList(session, statusEnum);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 파트너 정산 상세 조회
     * GET /api/partner/settlements/{settlementId}
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return 정산 상세 정보
     */
    @GetMapping("/list/{settlementId}")
    public ResponseEntity<ApiResponse<SettlementDetailResponseDto>> getSettlementDetail(
            HttpSession session,
            @PathVariable Long settlementId
    ) {
        SettlementDetailResponseDto response = settlementService.getPartnerSettlementDetail(session, settlementId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 파트너 정산서 PDF 다운로드
     * GET /api/partner/settlements/{settlementId}/pdf
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return PDF 파일
     */
    @GetMapping("/list/{settlementId}/pdf")
    public ResponseEntity<byte[]> downloadSettlementPdf(
            HttpSession session,
            @PathVariable Long settlementId
    ) throws java.io.IOException {
        byte[] pdfBytes = settlementExportService.generatePartnerSettlementPdf(session, settlementId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "settlement_" + settlementId + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    
    /**
     * 파트너 정산 목록 Excel 다운로드
     * GET /api/partner/settlements/excel?status={status}
     * 
     * @param session HTTP 세션
     * @param status 정산 상태 필터 (선택)
     * @return Excel 파일
     */
    @GetMapping("/list/excel")
    public ResponseEntity<byte[]> downloadSettlementExcel(
            HttpSession session,
            @RequestParam(required = false) String status
    ) throws java.io.IOException {
        byte[] excelBytes = settlementExportService.generatePartnerSettlementExcel(session, status);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "settlements.xlsx");
        headers.setContentLength(excelBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
