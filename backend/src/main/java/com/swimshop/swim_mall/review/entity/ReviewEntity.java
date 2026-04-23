package com.swimshop.swim_mall.review.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;

@Entity
@Table(name = "review", uniqueConstraints = {
    @UniqueConstraint(name = "uk_review_order_item", columnNames = "order_item_no")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewNo; //리뷰 고유식별자

    @Column(nullable = false, length = 1000)
    private String reviewContent; //리뷰 내용

    @Column(nullable = false)
    @Min(1)
    @Max(5)
    private Integer reviewRating; //평점 1~5

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_no", nullable = false, unique = true)
    private OrderItemEntity orderItem; // 주문 상품 조인 (구매 인증, 중복 방지)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer; // 고객 조인

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_no", nullable = false)
    private ProductEntity product; // 상품 조인

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime reviewCreatedAt = LocalDateTime.now(); //생성일시

    @Column(nullable = true, length = 1000)
    private String reviewReply; //리뷰 답변 내용 (파트너/관리자용)

    @Column(nullable = true)
    private LocalDateTime reviewReplyCreatedAt; //답변 작성일시

    /**
     * 리뷰 답변 작성
     */
    public void addReply(String replyContent) {
        this.reviewReply = replyContent;
        this.reviewReplyCreatedAt = LocalDateTime.now();
    }

    /**
     * 리뷰 답변 수정
     */
    public void updateReply(String replyContent) {
        this.reviewReply = replyContent;
        // 작성일시는 유지
    }

    /**
     * 리뷰 답변 삭제
     */
    public void deleteReply() {
        this.reviewReply = null;
        this.reviewReplyCreatedAt = null;
    }
}
