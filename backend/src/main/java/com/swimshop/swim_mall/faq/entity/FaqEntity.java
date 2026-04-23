package com.swimshop.swim_mall.faq.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.swimshop.swim_mall.admin.entity.AdminEntity;

@Entity
@Table(name = "faq")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FaqEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long faqNo; //faq 고유식별자

    @Column(nullable = false)
    private String faqQuestion; //faq 질문

    @Column(nullable = false)
    private String faqAnswer; //faq 답변

    @Column(nullable = false)
    private String faqCategory; // 카테고리 - enum

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "admin_id")
    private AdminEntity admin;
    
    /**
     * FAQ 수정 메서드
     */
    public void update(String faqQuestion, String faqAnswer, String faqCategory) {
        this.faqQuestion = faqQuestion;
        this.faqAnswer = faqAnswer;
        this.faqCategory = faqCategory;
    }
}
