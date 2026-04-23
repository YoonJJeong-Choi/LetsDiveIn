package com.swimshop.swim_mall.review.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.review.dto.ReviewRequestDto;
import com.swimshop.swim_mall.review.dto.ReviewReplyRequestDto;
import com.swimshop.swim_mall.review.dto.ReviewResponseDto;
import com.swimshop.swim_mall.review.entity.ReviewEntity;
import com.swimshop.swim_mall.review.repository.ReviewRepository;
import com.swimshop.swim_mall.review.entity.ReviewImageEntity;
import com.swimshop.swim_mall.review.repository.ReviewImageRepository;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.customer.entity.CustomerActivityLogEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerActivityLogRepository;
import com.swimshop.swim_mall.common.enums.CustomerActivityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swimshop.swim_mall.point.service.PointService;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final AuthService authService;
    private final PartnerRepository partnerRepository;
    private final CustomerActivityLogRepository customerActivityLogRepository;
    private final PointService pointService;
    private final UploadedFileRepository uploadedFileRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long REVIEW_BASE_POINT = 100L;

    /**
     * 리뷰 작성 (고객용)
     * POST /api/reviews
     */
    @Transactional
    public ReviewResponseDto createReview(HttpSession session, ReviewRequestDto requestDto) {
        // 고객만 가능
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.CUSTOMER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "고객만 리뷰 작성이 가능합니다.");
        }

        Long customerId = getCustomerIdFromSession(session);
        Long orderItemNo = requestDto.getOrderItemNo();

        // 주문 아이템 조회
        OrderItemEntity orderItem = orderItemRepository.findById(orderItemNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        // 본인 주문인지 확인
        if (!orderItem.getOrder().getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 주문 상품만 리뷰 작성이 가능합니다.");
        }

        // 구매 확정 여부 확인 (OrderItem.completedAt으로 관리)
        if (orderItem.getCompletedAt() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "구매 확정된 주문 상품만 리뷰 작성이 가능합니다.");
        }

        // 반품 상태 확인: 환불 완료된 상품은 리뷰 작성 불가
        if (orderItem.getReturnEntity() != null) {
            com.swimshop.swim_mall.common.enums.ReturnStatus returnStatus = orderItem.getReturnEntity().getReturnStatus();
            if (returnStatus == com.swimshop.swim_mall.common.enums.ReturnStatus.REFUNDED) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                        "환불 완료된 상품은 리뷰 작성이 불가능합니다.");
            }
            // REJECTED(반품 거절)인 경우는 리뷰 작성 가능 (상품을 받았으므로)
        }

        // 중복 체크: 같은 OrderItem에 리뷰가 이미 있는지 확인
        if (reviewRepository.findByOrderItem_OrderItemNo(orderItemNo).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, 
                    "이미 리뷰를 작성한 주문 상품입니다.");
        }

        // 고객 조회
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 리뷰 엔티티 생성
        ReviewEntity reviewEntity = ReviewEntity.builder()
                .orderItem(orderItem)
                .customer(customer)
                .product(orderItem.getProduct())
                .reviewContent(requestDto.getReviewContent())
                .reviewRating(requestDto.getReviewRating())
                .reviewCreatedAt(java.time.LocalDateTime.now())
                .build();

        ReviewEntity savedReview = reviewRepository.save(reviewEntity);

        // 리뷰 이미지 연결 (선택)
        if (requestDto.getImageFileIds() != null && !requestDto.getImageFileIds().isEmpty()) {
            for (Long fileId : requestDto.getImageFileIds()) {
                if (fileId == null) continue;
                try {
                    UploadedFileEntity file = uploadedFileRepository.findById(fileId)
                            .orElse(null);
                    if (file == null) continue;
                    // review-image 카테고리만 허용
                    if (!"review-image".equals(file.getCategory())) continue;
                    String url = Boolean.TRUE.equals(file.getIsPrivate())
                            ? String.format("%s/api/files/%d/download", "http://localhost:8080", file.getFileId())
                            : String.format("%s/uploads/%s/%s", "http://localhost:8080",
                                file.getCategory(), file.getStoredName());
                    ReviewImageEntity ri = ReviewImageEntity.builder()
                            .review(savedReview)
                            .file(file)
                            .imageUrl(file.getIsPrivate() ? url : file.getStoragePath().contains("uploads")
                                    ? url : file.getOriginalName())
                            .build();
                    reviewImageRepository.save(ri);
                } catch (Exception ignored) {}
            }
        }

        // 리뷰 작성 기본 포인트 적립 (이벤트 포인트와 분리된 상시 규칙)
        try {
            pointService.accumulateReviewPoint(customer, orderItem.getOrderItemNo(), REVIEW_BASE_POINT);
        } catch (Exception e) {
            // 포인트 적립 실패가 리뷰 작성 자체를 막지 않도록 분리
        }
        
        // 활동 로그 기록
        try {
            String activityDetails = objectMapper.writeValueAsString(Map.of(
                "reviewNo", savedReview.getReviewNo(),
                "productNo", savedReview.getProduct().getProductNo(),
                "productName", savedReview.getProduct().getProductName(),
                "rating", savedReview.getReviewRating()
            ));
            CustomerActivityLogEntity log = CustomerActivityLogEntity.create(
                customer,
                CustomerActivityType.REVIEW_CREATED,
                activityDetails,
                null, // IP 주소는 나중에 추가 가능
                null  // User-Agent는 나중에 추가 가능
            );
            customerActivityLogRepository.save(log);
        } catch (Exception e) {
            // 로그 기록 실패해도 리뷰는 계속 진행
        }
        
        return toReviewResponseDto(savedReview);
    }

    /**
     * 리뷰 목록 조회 (고객용)
     * GET /api/reviews/customer
     */
    public com.swimshop.swim_mall.common.response.PagedResponse<ReviewResponseDto> getReviewsByCustomer(HttpSession session, int page, int size) {
        Long customerId = getCustomerIdFromSession(session);
        List<ReviewEntity> reviews = reviewRepository.findByCustomerId(customerId);
        int total = reviews.size();
        int from = Math.max(0, Math.min((page - 1) * size, total));
        int to = Math.max(from, Math.min(from + size, total));
        List<ReviewResponseDto> items = reviews.subList(from, to).stream()
                .map(this::toReviewResponseDto)
                .collect(Collectors.toList());
        return new com.swimshop.swim_mall.common.response.PagedResponse<>(items, total, page, size);
    }

    /**
     * 상품별 리뷰 목록 조회
     * GET /api/reviews/product/{productNo}
     */
    public com.swimshop.swim_mall.common.response.PagedResponse<ReviewResponseDto> getReviewsByProduct(Long productNo, int page, int size, String sort) {
        List<ReviewEntity> reviews = reviewRepository.findByProductNo(productNo);
        // 정렬 옵션: latest(기본), rating_high, rating_low
        if ("rating_high".equalsIgnoreCase(sort)) {
            reviews.sort((a, b) -> Integer.compare(b.getReviewRating() != null ? b.getReviewRating() : 0, a.getReviewRating() != null ? a.getReviewRating() : 0));
        } else if ("rating_low".equalsIgnoreCase(sort)) {
            reviews.sort((a, b) -> Integer.compare(a.getReviewRating() != null ? a.getReviewRating() : 0, b.getReviewRating() != null ? b.getReviewRating() : 0));
        } else {
            // latest
            reviews.sort((a, b) -> {
                java.time.LocalDateTime da = a.getReviewCreatedAt();
                java.time.LocalDateTime db = b.getReviewCreatedAt();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            });
        }
        int total = reviews.size();
        int from = Math.max(0, Math.min((page - 1) * size, total));
        int to = Math.max(from, Math.min(from + size, total));
        List<ReviewResponseDto> items = reviews.subList(from, to).stream()
                .map(this::toReviewResponseDto)
                .collect(Collectors.toList());
        return new com.swimshop.swim_mall.common.response.PagedResponse<>(items, total, page, size);
    }

    /**
     * 리뷰 목록 조회 (관리자/파트너용)
     * GET /api/reviews
     * - 관리자: 전체 리뷰 조회
     * - 파트너: 자신의 상품 리뷰만 조회
     */
    public com.swimshop.swim_mall.common.response.PagedResponse<ReviewResponseDto> getAllReviews(HttpSession session, int page, int size) {
        try {
            AccountRole userRole = authService.getCurrentUser(session).getRole();
            
            if (userRole == AccountRole.ADMIN) {
                // 관리자: 전체 리뷰 조회
                List<ReviewEntity> reviews = reviewRepository.findAllWithRelations();
                int total = reviews.size();
                int from = Math.max(0, Math.min((page - 1) * size, total));
                int to = Math.max(from, Math.min(from + size, total));
                List<ReviewResponseDto> items = reviews.subList(from, to).stream()
                        .map(this::toReviewResponseDto)
                        .collect(Collectors.toList());
                return new com.swimshop.swim_mall.common.response.PagedResponse<>(items, total, page, size);
            } else if (userRole == AccountRole.PARTNER) {
                // 파트너: 자신의 상품 리뷰만 조회
                Long partnerId = getPartnerIdFromSession(session);
                List<ReviewEntity> reviews = reviewRepository.findByPartnerId(partnerId);
                int total = reviews.size();
                int from = Math.max(0, Math.min((page - 1) * size, total));
                int to = Math.max(from, Math.min(from + size, total));
                List<ReviewResponseDto> items = reviews.subList(from, to).stream()
                        .map(this::toReviewResponseDto)
                        .collect(Collectors.toList());
                return new com.swimshop.swim_mall.common.response.PagedResponse<>(items, total, page, size);
            } else {
                throw new BusinessException(ErrorCode.FORBIDDEN, "관리자 또는 파트너만 리뷰 조회가 가능합니다.");
            }
        } catch (BusinessException e) {
            // UNAUTHORIZED 예외를 FORBIDDEN으로 변환하지 않고 그대로 던짐
            throw e;
        } catch (Exception e) {
            // 예상치 못한 예외는 UNAUTHORIZED로 변환
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }

    /**
     * 작성 가능 리뷰 목록 (고객용)
     * - 고객의 주문상품 중 구매확정되었고 리뷰가 아직 없는 항목만
     */
    public com.swimshop.swim_mall.common.response.PagedResponse<ReviewResponseDto> getWritableReviews(HttpSession session, int page, int size) {
        Long customerId = getCustomerIdFromSession(session);
        // 모든 주문상품 중 해당 고객 것이며 completedAt != null && !isCancelled
        List<OrderItemEntity> all = orderItemRepository.findAll();
        List<OrderItemEntity> candidates = all.stream()
                .filter(oi -> oi.getOrder() != null && oi.getOrder().getCustomer() != null
                        && oi.getOrder().getCustomer().getCustomerId().equals(customerId))
                .filter(oi -> oi.getCompletedAt() != null && !Boolean.TRUE.equals(oi.getIsCancelled()))
                .collect(Collectors.toList());
        // 리뷰가 없는 항목만
        List<Long> reviewedOrderItemNos = reviewRepository.findAll().stream()
                .map(r -> r.getOrderItem().getOrderItemNo())
                .collect(Collectors.toList());
        List<OrderItemEntity> writable = candidates.stream()
                .filter(oi -> !reviewedOrderItemNos.contains(oi.getOrderItemNo()))
                .collect(Collectors.toList());

        int total = writable.size();
        int from = Math.max(0, Math.min((page - 1) * size, total));
        int to = Math.max(from, Math.min(from + size, total));
        List<OrderItemEntity> pageItems = writable.subList(from, to);

        // OrderItem → ReviewResponseDto 형태로 최소 정보 매핑 (UI 요구 필드)
        List<ReviewResponseDto> items = pageItems.stream().map(oi -> {
            OrderEntity order = oi.getOrder();
            return ReviewResponseDto.builder()
                    .reviewNo(null) // 아직 리뷰 없음
                    .orderItemNo(oi.getOrderItemNo())
                    .orderNo(order != null ? order.getOrderNo() : null)
                    .productNo(oi.getProduct() != null ? oi.getProduct().getProductNo() : null)
                    .productName(oi.getProduct() != null ? oi.getProduct().getProductName() : null)
                    .productImageUrl(oi.getProduct() != null ? oi.getProduct().getProductImageUrl() : null)
                    .optionNo(oi.getOption() != null ? oi.getOption().getOptionNo() : null)
                    .color(oi.getOption() != null ? oi.getOption().getColor() : null)
                    .size(oi.getOption() != null ? oi.getOption().getSize() : null)
                    .customerId(customerId)
                    .customerName(null)
                    .reviewContent(null)
                    .reviewRating(null)
                    .reviewCreatedAt(null)
                    .reviewReply(null)
                    .reviewReplyCreatedAt(null)
                    .build();
        }).collect(Collectors.toList());

        return new com.swimshop.swim_mall.common.response.PagedResponse<>(items, total, page, size);
    }

    /**
     * 리뷰 상세 조회
     * GET /api/reviews/{reviewNo}
     */
    public ReviewResponseDto getReview(Long reviewNo) {
        ReviewEntity review = reviewRepository.findById(reviewNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        return toReviewResponseDto(review);
    }

    /**
     * 리뷰 삭제 (관리자만)
     * DELETE /api/reviews/{reviewNo}
     */
    @Transactional
    public void deleteReview(HttpSession session, Long reviewNo) {
        // 관리자 권한 확인
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "관리자만 리뷰 삭제가 가능합니다.");
        }

        ReviewEntity review = reviewRepository.findById(reviewNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        reviewRepository.delete(review);
    }

    /**
     * 파트너 리뷰 목록 조회 (파트너의 상품 리뷰만)
     * GET /api/reviews/partner
     */
    public List<ReviewResponseDto> getReviewsByPartner(HttpSession session) {
        // 파트너 권한 확인
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너만 자신의 상품 리뷰 조회가 가능합니다.");
        }

        Long partnerId = getPartnerIdFromSession(session);
        List<ReviewEntity> reviews = reviewRepository.findByPartnerId(partnerId);
        return reviews.stream()
                .map(this::toReviewResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 리뷰 답변 작성 (파트너만)
     * POST /api/reviews/{reviewNo}/reply
     */
    @Transactional
    public ReviewResponseDto addReviewReply(HttpSession session, Long reviewNo, ReviewReplyRequestDto requestDto) {
        // 파트너 권한 확인
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너만 리뷰 답변 작성이 가능합니다.");
        }

        Long partnerId = getPartnerIdFromSession(session);
        
        // 비활성화된 파트너는 리뷰 관리 불가
        checkPartnerActive(partnerId);
        
        ReviewEntity review = reviewRepository.findById(reviewNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 상품의 리뷰인지 확인
        if (review.getProduct().getPartner() == null || 
            !review.getProduct().getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 상품 리뷰에만 답변을 작성할 수 있습니다.");
        }

        // 이미 답변이 있는지 확인
        if (review.getReviewReply() != null && !review.getReviewReply().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 답변이 작성된 리뷰입니다. 수정 기능을 사용해주세요.");
        }

        review.addReply(requestDto.getReviewReply());
        ReviewEntity savedReview = reviewRepository.save(review);
        return toReviewResponseDto(savedReview);
    }

    /**
     * 리뷰 답변 수정 (파트너만)
     * PUT /api/reviews/{reviewNo}/reply
     */
    @Transactional
    public ReviewResponseDto updateReviewReply(HttpSession session, Long reviewNo, ReviewReplyRequestDto requestDto) {
        // 파트너 권한 확인
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너만 리뷰 답변 수정이 가능합니다.");
        }

        Long partnerId = getPartnerIdFromSession(session);
        
        // 비활성화된 파트너는 리뷰 관리 불가
        checkPartnerActive(partnerId);
        
        ReviewEntity review = reviewRepository.findById(reviewNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 상품의 리뷰인지 확인
        if (review.getProduct().getPartner() == null || 
            !review.getProduct().getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 상품 리뷰에만 답변을 수정할 수 있습니다.");
        }

        // 답변이 있는지 확인
        if (review.getReviewReply() == null || review.getReviewReply().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "작성된 답변이 없습니다. 작성 기능을 사용해주세요.");
        }

        review.updateReply(requestDto.getReviewReply());
        ReviewEntity savedReview = reviewRepository.save(review);
        return toReviewResponseDto(savedReview);
    }

    /**
     * 리뷰 답변 삭제 (파트너만)
     * DELETE /api/reviews/{reviewNo}/reply
     */
    @Transactional
    public ReviewResponseDto deleteReviewReply(HttpSession session, Long reviewNo) {
        // 파트너 권한 확인
        AccountRole userRole = authService.getCurrentUser(session).getRole();
        if (userRole != AccountRole.PARTNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너만 리뷰 답변 삭제가 가능합니다.");
        }

        Long partnerId = getPartnerIdFromSession(session);
        
        // 비활성화된 파트너는 리뷰 관리 불가
        checkPartnerActive(partnerId);
        
        ReviewEntity review = reviewRepository.findById(reviewNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인 상품의 리뷰인지 확인
        if (review.getProduct().getPartner() == null || 
            !review.getProduct().getPartner().getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인의 상품 리뷰에만 답변을 삭제할 수 있습니다.");
        }

        // 답변이 있는지 확인
        if (review.getReviewReply() == null || review.getReviewReply().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "삭제할 답변이 없습니다.");
        }

        review.deleteReply();
        ReviewEntity savedReview = reviewRepository.save(review);
        return toReviewResponseDto(savedReview);
    }

    /**
     * ReviewEntity를 ReviewResponseDto로 변환
     */
    private ReviewResponseDto toReviewResponseDto(ReviewEntity review) {
        OrderItemEntity orderItem = review.getOrderItem();
        // 이미지 조회
        List<String> imageUrls = List.of();
        try {
            List<ReviewImageEntity> images = reviewImageRepository.findByReviewOrderByIdAsc(review);
            imageUrls = images.stream().map(ReviewImageEntity::getImageUrl).collect(Collectors.toList());
        } catch (Exception ignored) {}
        return ReviewResponseDto.builder()
                .reviewNo(review.getReviewNo())
                .orderItemNo(orderItem.getOrderItemNo())
                .orderNo(orderItem.getOrder().getOrderNo())
                .productNo(review.getProduct().getProductNo())
                .productName(review.getProduct().getProductName())
                .productImageUrl(review.getProduct().getProductImageUrl())
                .optionNo(orderItem.getOption() != null ? orderItem.getOption().getOptionNo() : null)
                .color(orderItem.getOption() != null ? orderItem.getOption().getColor() : null)
                .size(orderItem.getOption() != null ? orderItem.getOption().getSize() : null)
                .customerId(review.getCustomer().getCustomerId())
                .customerName(review.getCustomer().getCustomerName())
                .reviewContent(review.getReviewContent())
                .reviewRating(review.getReviewRating())
                .reviewCreatedAt(review.getReviewCreatedAt())
                .reviewReply(review.getReviewReply())
                .reviewReplyCreatedAt(review.getReviewReplyCreatedAt())
                .imageUrls(imageUrls)
                .build();
    }

    /**
     * 세션에서 고객 ID 추출
     */
    private Long getCustomerIdFromSession(HttpSession session) {
        Object subjObj = session.getAttribute("customerId");
        if (subjObj == null) {
            subjObj = session.getAttribute("subjectId"); // 통합 로그인(CUSTOMER) 시 subjectId
        }
        Long customerId = toLong(subjObj);
        
        if (customerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        return customerId;
    }

    /**
     * 세션에서 파트너 ID 추출
     */
    private Long getPartnerIdFromSession(HttpSession session) {
        Object subjObj = session.getAttribute("subjectId");
        Long partnerId = toLong(subjObj);
        
        if (partnerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        
        return partnerId;
    }

    /**
     * 파트너가 활성화 상태인지 확인 (비활성화된 파트너는 관리 기능 사용 불가)
     */
    private void checkPartnerActive(Long partnerId) {
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));
        
        if (partner.getPartnerStatus() == PartnerStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.PARTNER_INACTIVE);
        }
    }

    private static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return (Long) value;
    }
}
