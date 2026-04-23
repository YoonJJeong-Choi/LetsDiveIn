package com.swimshop.swim_mall.settlement.service;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.settlement.entity.SettlementEntity;
import com.swimshop.swim_mall.settlement.entity.SettlementOrderItemEntity;
import com.swimshop.swim_mall.settlement.repository.SettlementOrderItemRepository;
import com.swimshop.swim_mall.settlement.repository.SettlementRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementExportService {

    private final SettlementRepository settlementRepository;
    private final SettlementOrderItemRepository settlementOrderItemRepository;
    private final AuthService authService;

    /**
     * 정산서 PDF 생성 (관리자용)
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return PDF 바이트 배열
     */
    @Transactional(readOnly = true)
    public byte[] generateSettlementPdf(HttpSession session, Long settlementId) throws IOException {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 내부 메서드 호출
        return generateSettlementPdfInternal(settlementId);
    }
    
    /**
     * 정산서 PDF 생성 (내부 메서드, 권한 체크 없음)
     */
    private byte[] generateSettlementPdfInternal(Long settlementId) throws IOException {
        // 정산 엔티티 조회
        SettlementEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
        
        // 정산에 포함된 주문 아이템 목록 조회
        List<SettlementOrderItemEntity> settlementOrderItems = 
                settlementOrderItemRepository.findBySettlement_SettlementId(settlementId);
        
        // PDF 생성
        return generateSettlementPdfInternal(settlement, settlementOrderItems);
    }
    
    /**
     * 정산서 PDF 생성 (내부 메서드, 엔티티 직접 전달)
     */
    private byte[] generateSettlementPdfInternal(
            SettlementEntity settlement,
            List<SettlementOrderItemEntity> settlementOrderItems
    ) throws IOException {
        // PDF 생성
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        
        // 한글 폰트 설정 (Windows 시스템 폰트 사용)
        PdfFont koreanFont;
        try {
            // 맑은 고딕 시도
            try {
                koreanFont = PdfFontFactory.createFont("C:/Windows/Fonts/malgun.ttf", PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                // 나눔고딕 시도
                try {
                    koreanFont = PdfFontFactory.createFont("C:/Windows/Fonts/NanumGothic.ttf", PdfEncodings.IDENTITY_H);
                } catch (Exception e2) {
                    // 기본 폰트 사용 (한글 미지원, 대체)
                    koreanFont = PdfFontFactory.createFont();
                }
            }
        } catch (Exception e) {
            // 폰트 로드 실패 시 기본 폰트 사용
            koreanFont = PdfFontFactory.createFont();
        }
        
        Document document = new Document(pdf);
        document.setFont(koreanFont);
        
        // 제목
        document.add(new Paragraph("정산서").setFont(koreanFont).setFontSize(20).setBold());
        document.add(new Paragraph(" "));
        
        // 정산 정보 섹션
        document.add(new Paragraph("■ 정산 기본 정보").setFont(koreanFont).setBold().setFontSize(14));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("정산 ID: " + settlement.getSettlementId()).setFont(koreanFont));
        document.add(new Paragraph("파트너명: " + settlement.getPartner().getPartnerName()).setFont(koreanFont));
        document.add(new Paragraph("정산 생성일: " + settlement.getSettlementCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).setFont(koreanFont));
        if (settlement.getSettlementPeriodStart() != null && settlement.getSettlementPeriodEnd() != null) {
            document.add(new Paragraph("정산 기간: " + 
                settlement.getSettlementPeriodStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " ~ " +
                settlement.getSettlementPeriodEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).setFont(koreanFont));
        }
        if (settlement.getSettlementPaidDate() != null) {
            document.add(new Paragraph("지급일: " + settlement.getSettlementPaidDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).setFont(koreanFont));
        }
        document.add(new Paragraph("정산 상태: " + settlement.getSettlementStatus().getLabel()).setFont(koreanFont));
        document.add(new Paragraph("포함된 주문 아이템 수: " + settlementOrderItems.size() + "개").setFont(koreanFont));
        document.add(new Paragraph(" "));
        
        // 금액 정보 섹션
        document.add(new Paragraph("■ 정산 금액 정보").setFont(koreanFont).setBold().setFontSize(14));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("총 판매금액: ₩" + String.format("%,d", settlement.getTotalSalesAmount()) + "원").setFont(koreanFont));
        document.add(new Paragraph("총 수수료 (10%): ₩" + String.format("%,d", settlement.getCommissionAmount()) + "원").setFont(koreanFont));
        document.add(new Paragraph("총 정산금액: ₩" + String.format("%,d", settlement.getSettlementAmount()) + "원").setFont(koreanFont).setBold());
        document.add(new Paragraph(" "));
        document.add(new Paragraph("※ 수수료는 판매금액의 10%로 계산됩니다.").setFont(koreanFont));
        document.add(new Paragraph("※ 정산금액 = 판매금액 - 수수료").setFont(koreanFont));
        document.add(new Paragraph(" "));
        
        // 주문 아이템 목록 섹션
        if (!settlementOrderItems.isEmpty()) {
            document.add(new Paragraph("■ 포함된 주문 아이템 상세 내역").setFont(koreanFont).setBold().setFontSize(14));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("아래는 이 정산에 포함된 모든 주문 아이템의 상세 정보입니다.").setFont(koreanFont));
            document.add(new Paragraph("각 항목의 판매금액, 수수료, 정산금액을 확인할 수 있습니다.").setFont(koreanFont));
            document.add(new Paragraph(" "));
            
            // 테이블 생성 (항목명 컬럼 추가)
            Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3, 2.5f, 1, 2, 2, 2}));
            table.setFont(koreanFont);
            
            // 헤더 셀 생성 (폰트 적용)
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("주문 ID").setFont(koreanFont)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("상품명").setFont(koreanFont)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("항목명").setFont(koreanFont)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("수량").setFont(koreanFont)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("판매금액").setFont(koreanFont)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("수수료").setFont(koreanFont)));
            table.addHeaderCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("정산금액").setFont(koreanFont)));
            
            for (SettlementOrderItemEntity soi : settlementOrderItems) {
                String productName = soi.getOrderItem().getProduct() != null 
                    ? soi.getOrderItem().getProduct().getProductName() 
                    : "-";
                
                // 옵션 정보 (항목명) 생성
                String optionInfo = "-";
                if (soi.getOrderItem().getOption() != null) {
                    com.swimshop.swim_mall.option.entity.OptionEntity option = soi.getOrderItem().getOption();
                    java.util.List<String> optionParts = new java.util.ArrayList<>();
                    if (option.getColor() != null && !option.getColor().trim().isEmpty()) {
                        optionParts.add("색상: " + option.getColor());
                    }
                    if (option.getSize() != null && !option.getSize().trim().isEmpty()) {
                        optionParts.add("사이즈: " + option.getSize());
                    }
                    if (!optionParts.isEmpty()) {
                        optionInfo = String.join(", ", optionParts);
                    }
                }
                
                Long salesAmount = soi.getOrderItem().getItemTotalPrice();
                Long commissionAmount = Math.round(salesAmount * 0.10);
                Long settlementAmount = salesAmount - commissionAmount;
                
                // 데이터 셀 생성 (폰트 적용)
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(soi.getOrderItem().getOrder().getOrderNo())).setFont(koreanFont)));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(productName).setFont(koreanFont)));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(optionInfo).setFont(koreanFont)));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(String.valueOf(soi.getOrderItem().getItemQuantity())).setFont(koreanFont)));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("₩" + String.format("%,d", salesAmount)).setFont(koreanFont)));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("₩" + String.format("%,d", commissionAmount)).setFont(koreanFont)));
                table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("₩" + String.format("%,d", settlementAmount)).setFont(koreanFont)));
            }
            
            document.add(table);
        }
        
        document.close();
        return baos.toByteArray();
    }
    
    /**
     * 정산서 PDF 생성 (파트너용)
     * 
     * @param session HTTP 세션
     * @param settlementId 정산 ID
     * @return PDF 바이트 배열
     */
    @Transactional(readOnly = true)
    public byte[] generatePartnerSettlementPdf(HttpSession session, Long settlementId) throws IOException {
        // 파트너 권한 확인 및 파트너 ID 가져오기
        com.swimshop.swim_mall.account.dto.AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        if (currentUser.getRole() != AccountRole.PARTNER) {
            throw new com.swimshop.swim_mall.common.error.BusinessException(
                com.swimshop.swim_mall.common.error.ErrorCode.FORBIDDEN, "파트너 권한이 필요합니다.");
        }
        Long partnerId = currentUser.getSubjectId();
        
        // 정산 엔티티 조회
        SettlementEntity settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산을 찾을 수 없습니다."));
        
        // 파트너 소유 확인
        if (!settlement.getPartner().getPartnerId().equals(partnerId)) {
            throw new com.swimshop.swim_mall.common.error.BusinessException(
                com.swimshop.swim_mall.common.error.ErrorCode.FORBIDDEN, "본인의 정산만 다운로드할 수 있습니다.");
        }
        
        // 내부 메서드 호출
        return generateSettlementPdfInternal(settlementId);
    }
    
    /**
     * 정산 목록 Excel 다운로드 (관리자용)
     * 
     * @param session HTTP 세션
     * @param partnerId 파트너 ID (선택)
     * @param status 정산 상태 (선택)
     * @return Excel 바이트 배열
     */
    @Transactional(readOnly = true)
    public byte[] generateSettlementExcel(HttpSession session, Long partnerId, String status) throws IOException {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 정산 목록 조회
        List<SettlementEntity> settlements;
        if (partnerId != null) {
            settlements = settlementRepository.findByPartner_PartnerId(partnerId);
        } else {
            settlements = settlementRepository.findAll();
        }
        
        // 상태 필터 적용
        if (status != null && !status.isEmpty()) {
            try {
                com.swimshop.swim_mall.common.enums.SettlementStatus statusEnum = 
                    com.swimshop.swim_mall.common.enums.SettlementStatus.valueOf(status.toUpperCase());
                settlements = settlements.stream()
                        .filter(s -> s.getSettlementStatus() == statusEnum)
                        .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException e) {
                // 유효하지 않은 상태값은 무시
            }
        }
        
        // 내부 메서드 호출 (관리자용은 파트너명 컬럼 포함)
        return generateSettlementExcelInternalForAdmin(settlements);
    }
    
    /**
     * 정산 목록 Excel 생성 (관리자용, 파트너명 포함)
     */
    private byte[] generateSettlementExcelInternalForAdmin(List<SettlementEntity> settlements) throws IOException {
        // Excel 생성
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("정산 목록");
        
        // 스타일 생성
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        CellStyle currencyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat("#,##0"));
        
        // 헤더 행 생성 (파트너명 포함)
        Row headerRow = sheet.createRow(0);
        String[] headers = {"정산 ID", "파트너명", "총 판매금액", "총 수수료", "총 정산금액", 
                           "정산 기간 시작", "정산 기간 종료", "생성일", "지급일", "상태"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        int rowNum = 1;
        for (SettlementEntity settlement : settlements) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(settlement.getSettlementId());
            row.createCell(1).setCellValue(settlement.getPartner().getPartnerName());
            
            Cell salesCell = row.createCell(2);
            salesCell.setCellValue(settlement.getTotalSalesAmount());
            salesCell.setCellStyle(currencyStyle);
            
            Cell commissionCell = row.createCell(3);
            commissionCell.setCellValue(settlement.getCommissionAmount());
            commissionCell.setCellStyle(currencyStyle);
            
            Cell settlementCell = row.createCell(4);
            settlementCell.setCellValue(settlement.getSettlementAmount());
            settlementCell.setCellStyle(currencyStyle);
            
            row.createCell(5).setCellValue(
                settlement.getSettlementPeriodStart() != null 
                    ? settlement.getSettlementPeriodStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "");
            row.createCell(6).setCellValue(
                settlement.getSettlementPeriodEnd() != null 
                    ? settlement.getSettlementPeriodEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "");
            row.createCell(7).setCellValue(
                settlement.getSettlementCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            row.createCell(8).setCellValue(
                settlement.getSettlementPaidDate() != null
                    ? settlement.getSettlementPaidDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "");
            row.createCell(9).setCellValue(settlement.getSettlementStatus().getLabel());
        }
        
        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // 여유 공간 추가
        }
        
        // Excel 파일을 바이트 배열로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        
        return baos.toByteArray();
    }
    
    /**
     * 정산 목록 Excel 다운로드 (파트너용)
     * 
     * @param session HTTP 세션
     * @param status 정산 상태 (선택)
     * @return Excel 바이트 배열
     */
    @Transactional(readOnly = true)
    public byte[] generatePartnerSettlementExcel(HttpSession session, String status) throws IOException {
        // 파트너 권한 확인 및 파트너 ID 가져오기
        com.swimshop.swim_mall.account.dto.AuthLoginResponseDto currentUser = authService.getCurrentUser(session);
        if (currentUser.getRole() != AccountRole.PARTNER) {
            throw new com.swimshop.swim_mall.common.error.BusinessException(
                com.swimshop.swim_mall.common.error.ErrorCode.FORBIDDEN, "파트너 권한이 필요합니다.");
        }
        Long partnerId = currentUser.getSubjectId();
        
        // 정산 목록 조회 (본인 것만)
        List<SettlementEntity> settlements = settlementRepository.findByPartner_PartnerId(partnerId);
        
        // 상태 필터 적용
        if (status != null && !status.isEmpty()) {
            try {
                com.swimshop.swim_mall.common.enums.SettlementStatus statusEnum = 
                    com.swimshop.swim_mall.common.enums.SettlementStatus.valueOf(status.toUpperCase());
                settlements = settlements.stream()
                        .filter(s -> s.getSettlementStatus() == statusEnum)
                        .collect(java.util.stream.Collectors.toList());
            } catch (IllegalArgumentException e) {
                // 유효하지 않은 상태값은 무시
            }
        }
        
        // Excel 생성 (관리자용 메서드와 동일한 로직, 파트너 ID만 다름)
        return generateSettlementExcelInternal(settlements);
    }
    
    /**
     * 정산 목록 Excel 생성 (내부 메서드)
     */
    private byte[] generateSettlementExcelInternal(List<SettlementEntity> settlements) throws IOException {
        // Excel 생성
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("정산 목록");
        
        // 스타일 생성
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        CellStyle currencyStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        currencyStyle.setDataFormat(format.getFormat("#,##0"));
        
        // 헤더 행 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {"정산 ID", "총 판매금액", "총 수수료", "총 정산금액", 
                           "정산 기간 시작", "정산 기간 종료", "생성일", "지급일", "상태"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 데이터 행 생성
        int rowNum = 1;
        for (SettlementEntity settlement : settlements) {
            Row row = sheet.createRow(rowNum++);
            
            row.createCell(0).setCellValue(settlement.getSettlementId());
            
            Cell salesCell = row.createCell(1);
            salesCell.setCellValue(settlement.getTotalSalesAmount());
            salesCell.setCellStyle(currencyStyle);
            
            Cell commissionCell = row.createCell(2);
            commissionCell.setCellValue(settlement.getCommissionAmount());
            commissionCell.setCellStyle(currencyStyle);
            
            Cell settlementCell = row.createCell(3);
            settlementCell.setCellValue(settlement.getSettlementAmount());
            settlementCell.setCellStyle(currencyStyle);
            
            row.createCell(4).setCellValue(
                settlement.getSettlementPeriodStart() != null 
                    ? settlement.getSettlementPeriodStart().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "");
            row.createCell(5).setCellValue(
                settlement.getSettlementPeriodEnd() != null 
                    ? settlement.getSettlementPeriodEnd().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "");
            row.createCell(6).setCellValue(
                settlement.getSettlementCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            row.createCell(7).setCellValue(
                settlement.getSettlementPaidDate() != null
                    ? settlement.getSettlementPaidDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "");
            row.createCell(8).setCellValue(settlement.getSettlementStatus().getLabel());
        }
        
        // 컬럼 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000); // 여유 공간 추가
        }
        
        // Excel 파일을 바이트 배열로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        
        return baos.toByteArray();
    }
}
