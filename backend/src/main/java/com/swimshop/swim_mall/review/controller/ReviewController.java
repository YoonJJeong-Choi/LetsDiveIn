package com.swimshop.swim_mall.review.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swimshop.swim_mall.common.response.ApiResponse;
import com.swimshop.swim_mall.common.response.PagedResponse;
import com.swimshop.swim_mall.review.dto.ReviewReplyRequestDto;
import com.swimshop.swim_mall.review.dto.ReviewRequestDto;
import com.swimshop.swim_mall.review.dto.ReviewResponseDto;
import com.swimshop.swim_mall.review.service.ReviewService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성 (고객용)
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponseDto>> createReview(
            HttpSession session,
            @Valid @RequestBody ReviewRequestDto requestDto) {
        ReviewResponseDto reviewDto = reviewService.createReview(session, requestDto);
        return ResponseEntity.ok(ApiResponse.success(reviewDto));
    }

    /**
     * 리뷰 목록 조회 (고객용)
     * GET /api/reviews/customer
     */
    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getReviewsByCustomer(
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<ReviewResponseDto> pr = reviewService.getReviewsByCustomer(session, page, size);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("reviews", pr.getItems());
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", pr.getPage());
        meta.put("size", pr.getSize());
        meta.put("total", pr.getTotal());
        body.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /**
     * 상품별 리뷰 목록 조회
     * GET /api/reviews/product/{productNo}
     */
    @GetMapping("/product/{productNo}")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getReviewsByProduct(
            @PathVariable Long productNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        PagedResponse<ReviewResponseDto> pr = reviewService.getReviewsByProduct(productNo, page, size, sort);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("reviews", pr.getItems());
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", pr.getPage());
        meta.put("size", pr.getSize());
        meta.put("total", pr.getTotal());
        body.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /**
     * 리뷰 목록 조회 (관리자용)
     * GET /api/reviews
     */
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getAllReviews(
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<ReviewResponseDto> pr = reviewService.getAllReviews(session, page, size);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("reviews", pr.getItems());
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("page", pr.getPage());
        meta.put("size", pr.getSize());
        meta.put("total", pr.getTotal());
        body.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /**
     * 작성 가능 리뷰 목록 조회 (고객용)
     * - 구매확정(completedAt != null)이며 리뷰가 아직 없는 주문상품 목록
     * GET /api/reviews/writable
     */
    @GetMapping("/writable")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getWritableReviews(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page, // 0-based
            @RequestParam(defaultValue = "10") int size) {
        // 서비스가 1-based를 기대하는 경우를 대비해 page+1 전달
        var pr = reviewService.getWritableReviews(session, page + 1, size);
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("writable", pr.getItems());
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        // 서비스가 1-based로 반환한다면 0-based로 보정
        int returnedPage = pr.getPage();
        meta.put("page", Math.max(0, returnedPage - 1));
        meta.put("size", pr.getSize());
        meta.put("total", pr.getTotal());
        body.put("meta", meta);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    /**
     * 리뷰 상세 조회
     * GET /api/reviews/{reviewNo}
     */
    @GetMapping("/{reviewNo}")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> getReview(
            @PathVariable Long reviewNo) {
        ReviewResponseDto reviewDto = reviewService.getReview(reviewNo);
        return ResponseEntity.ok(ApiResponse.success(reviewDto));
    }

    /**
     * 리뷰 삭제 (관리자만)
     * DELETE /api/reviews/{reviewNo}
     */
    @DeleteMapping("/{reviewNo}")
    public ResponseEntity<ApiResponse<String>> deleteReview(
            HttpSession session,
            @PathVariable Long reviewNo) {
        reviewService.deleteReview(session, reviewNo);
        return ResponseEntity.ok(ApiResponse.success("리뷰가 삭제되었습니다."));
    }

    /**
     * 파트너 리뷰 목록 조회 (파트너의 상품 리뷰만)
     * GET /api/reviews/partner
     */
    @GetMapping("/partner")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponseDto>>> getReviewsByPartner(
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<ReviewResponseDto> reviews = reviewService.getAllReviews(session, page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * 리뷰 답변 작성 (파트너만)
     * POST /api/reviews/{reviewNo}/reply
     */
    @PostMapping("/{reviewNo}/reply")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> addReviewReply(
            HttpSession session,
            @PathVariable Long reviewNo,
            @Valid @RequestBody ReviewReplyRequestDto requestDto) {
        ReviewResponseDto reviewDto = reviewService.addReviewReply(session, reviewNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(reviewDto));
    }

    /**
     * 리뷰 답변 수정 (파트너만)
     * PUT /api/reviews/{reviewNo}/reply
     */
    @PutMapping("/{reviewNo}/reply")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> updateReviewReply(
            HttpSession session,
            @PathVariable Long reviewNo,
            @Valid @RequestBody ReviewReplyRequestDto requestDto) {
        ReviewResponseDto reviewDto = reviewService.updateReviewReply(session, reviewNo, requestDto);
        return ResponseEntity.ok(ApiResponse.success(reviewDto));
    }

    /**
     * 리뷰 답변 삭제 (파트너만)
     * DELETE /api/reviews/{reviewNo}/reply
     */
    @DeleteMapping("/{reviewNo}/reply")
    public ResponseEntity<ApiResponse<ReviewResponseDto>> deleteReviewReply(
            HttpSession session,
            @PathVariable Long reviewNo) {
        ReviewResponseDto reviewDto = reviewService.deleteReviewReply(session, reviewNo);
        return ResponseEntity.ok(ApiResponse.success(reviewDto));
    }
}
