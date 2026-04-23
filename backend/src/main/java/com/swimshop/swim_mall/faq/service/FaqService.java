package com.swimshop.swim_mall.faq.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.faq.dto.FaqRequestDto;
import com.swimshop.swim_mall.faq.dto.FaqResponseDto;
import com.swimshop.swim_mall.faq.dto.FaqUpdateRequestDto;
import com.swimshop.swim_mall.faq.entity.FaqEntity;
import com.swimshop.swim_mall.faq.repository.FaqRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqService {

    private final FaqRepository faqRepository;
    private final AdminRepository adminRepository;
    private final AuthService authService;

    /**
     * FAQ 생성 (관리자만)
     */
    public FaqResponseDto createFaq(HttpSession session, FaqRequestDto requestDto) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        // 현재 로그인한 관리자 조회
        Long adminId = (Long) session.getAttribute("subjectId");
        AdminEntity admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
        
        // FAQ 엔티티 생성
        FaqEntity faq = FaqEntity.builder()
                .faqQuestion(requestDto.getFaqQuestion())
                .faqAnswer(requestDto.getFaqAnswer())
                .faqCategory(requestDto.getFaqCategory())
                .admin(admin)
                .build();
        
        FaqEntity savedFaq = faqRepository.save(faq);
        
        return new FaqResponseDto(
            savedFaq.getFaqNo(),
            savedFaq.getFaqQuestion(),
            savedFaq.getFaqAnswer(),
            savedFaq.getFaqCategory(),
            savedFaq.getAdmin().getAdminName()
        );
    }

    /**
     * FAQ 목록 조회 (카테고리별 필터링 가능, 공개 API)
     */
    @Transactional(readOnly = true)
    public List<FaqResponseDto> getFaqList(String category) {
        List<FaqEntity> faqs;
        
        if (category != null && !category.isEmpty()) {
            faqs = faqRepository.findByFaqCategoryOrderByFaqNoDesc(category);
        } else {
            faqs = faqRepository.findAllByOrderByFaqNoDesc();
        }
        
        return faqs.stream()
            .map(faq -> new FaqResponseDto(
                faq.getFaqNo(),
                faq.getFaqQuestion(),
                faq.getFaqAnswer(),
                faq.getFaqCategory(),
                faq.getAdmin().getAdminName()
            ))
            .collect(Collectors.toList());
    }

    /**
     * FAQ 상세 조회 (공개 API)
     */
    @Transactional(readOnly = true)
    public FaqResponseDto getFaqDetail(Long faqNo) {
        FaqEntity faq = faqRepository.findById(faqNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
        
        return new FaqResponseDto(
            faq.getFaqNo(),
            faq.getFaqQuestion(),
            faq.getFaqAnswer(),
            faq.getFaqCategory(),
            faq.getAdmin().getAdminName()
        );
    }

    /**
     * FAQ 수정 (관리자만)
     */
    public FaqResponseDto updateFaq(HttpSession session, Long faqNo, FaqUpdateRequestDto requestDto) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        FaqEntity faq = faqRepository.findById(faqNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
        
        // FAQ 수정
        faq.update(
            requestDto.getFaqQuestion(),
            requestDto.getFaqAnswer(),
            requestDto.getFaqCategory()
        );
        
        FaqEntity savedFaq = faqRepository.save(faq);
        
        return new FaqResponseDto(
            savedFaq.getFaqNo(),
            savedFaq.getFaqQuestion(),
            savedFaq.getFaqAnswer(),
            savedFaq.getFaqCategory(),
            savedFaq.getAdmin().getAdminName()
        );
    }

    /**
     * FAQ 삭제 (관리자만)
     */
    public void deleteFaq(HttpSession session, Long faqNo) {
        // 관리자 권한 확인
        authService.requireRole(session, AccountRole.ADMIN);
        
        FaqEntity faq = faqRepository.findById(faqNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.FAQ_NOT_FOUND));
        
        faqRepository.delete(faq);
    }
}
