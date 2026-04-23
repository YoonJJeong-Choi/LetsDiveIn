package com.swimshop.swim_mall.customer.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 태그 엔티티
 * 관리자가 고객을 분류하기 위한 태그 (예: VIP, 일반, 문제 고객 등)
 */
@Entity
@Table(name = "customer_tag")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long tagId;

    /**
     * 태그 이름 (예: "VIP", "일반", "문제 고객")
     */
    @Column(nullable = false, length = 50, unique = true)
    private String tagName;

    /**
     * 태그 색상 (예: "#ff4d4f", "#52c41a")
     */
    @Column(nullable = false, length = 20)
    private String tagColor;

    /**
     * 태그 설명
     */
    @Column(length = 200)
    private String description;

    /**
     * 생성 일시
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 태그 생성
     */
    public static CustomerTagEntity create(String tagName, String tagColor, String description) {
        return CustomerTagEntity.builder()
                .tagName(tagName)
                .tagColor(tagColor != null ? tagColor : "#1890ff") // 기본 색상
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 태그 수정
     */
    public void update(String tagName, String tagColor, String description) {
        this.tagName = tagName;
        if (tagColor != null) {
            this.tagColor = tagColor;
        }
        this.description = description;
    }
}
