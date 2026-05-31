package com.swimshop.swim_mall.faq.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.faq.repository.FaqRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FAQ 카테고리를 InquiryCategory enum 코드(ORDER_PAYMENT 등)로 통일.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaqCategoryMigration implements CommandLineRunner {

    private final FaqRepository faqRepository;

    @Override
    @Transactional
    public void run(String... args) {
        int total = 0;
        for (InquiryCategory category : InquiryCategory.values()) {
            total += faqRepository.replaceFaqCategory(category.getLabel(), category.name());
        }
        total += faqRepository.replaceFaqCategory("포인트/쿠폰", InquiryCategory.POINT.name());
        if (total > 0) {
            log.info("FAQ categories migrated to InquiryCategory codes ({} rows updated)", total);
        }
    }
}
