package com.swimshop.swim_mall.customer.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 메모/노트 엔티티
 * 관리자가 고객에 대한 메모를 작성하고 관리하는 용도
 */
@Entity
@Table(name = "customer_note")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CustomerNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    /**
     * 메모 내용
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String noteContent;

    /**
     * 작성한 관리자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private AdminEntity admin;

    /**
     * 작성 일시
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @Column(nullable = true)
    private LocalDateTime updatedAt;

    /**
     * 중요 여부 (true면 중요 메모로 표시)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isImportant = false;

    /**
     * 메모 생성
     */
    public static CustomerNoteEntity create(
            CustomerEntity customer,
            AdminEntity admin,
            String noteContent,
            Boolean isImportant
    ) {
        return CustomerNoteEntity.builder()
                .customer(customer)
                .admin(admin)
                .noteContent(noteContent)
                .isImportant(isImportant != null ? isImportant : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(null)
                .build();
    }

    /**
     * 메모 수정
     */
    public void update(String noteContent, Boolean isImportant) {
        this.noteContent = noteContent;
        if (isImportant != null) {
            this.isImportant = isImportant;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
