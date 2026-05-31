package com.swimshop.swim_mall.qna.entity;

import java.time.LocalDateTime;

import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.common.enums.QnaAuthorType;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "qna_message")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QnaMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_no")
    private Long messageNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_no", nullable = false)
    private QnaEntity qna;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 32)
    private QnaAuthorType authorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_admin_id")
    private AdminEntity authorAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_partner_id")
    private PartnerEntity authorPartner;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
