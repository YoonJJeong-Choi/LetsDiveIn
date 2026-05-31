package com.swimshop.swim_mall.qna.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.common.enums.QnaAuthorType;
import com.swimshop.swim_mall.common.enums.QnaScope;
import com.swimshop.swim_mall.common.enums.QnaStatus;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.order.repository.OrderRepository;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.qna.dto.QnaCreateRequestDto;
import com.swimshop.swim_mall.qna.dto.QnaMessageResponseDto;
import com.swimshop.swim_mall.qna.dto.QnaReplyRequestDto;
import com.swimshop.swim_mall.qna.dto.QnaResponseDto;
import com.swimshop.swim_mall.qna.entity.QnaEntity;
import com.swimshop.swim_mall.qna.entity.QnaMessageEntity;
import com.swimshop.swim_mall.qna.repository.QnaMessageRepository;
import com.swimshop.swim_mall.qna.repository.QnaRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class QnaService {

    private final QnaRepository qnaRepository;
    private final QnaMessageRepository qnaMessageRepository;
    private final QnaRoutingService qnaRoutingService;
    private final AuthService authService;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PartnerRepository partnerRepository;
    private final AdminRepository adminRepository;

    public QnaResponseDto createQna(HttpSession session, QnaCreateRequestDto request) {
        authService.requireRole(session, AccountRole.CUSTOMER);
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        InquiryCategory category = InquiryCategory.fromCode(request.getCategory());
        QnaScope scope = resolveScope(category, request.getScope());

        validateCreateRequest(category, scope, request.getOrderItemNo());

        OrderEntity order = resolveOrder(customer, request.getOrderNo());
        OrderItemEntity orderItem = resolveOrderItem(customer, order, request.getOrderItemNo());

        if (category.requiresOrderItem() || (category == InquiryCategory.RETURN_EXCHANGE && scope == QnaScope.ORDER_ITEM)) {
            if (orderItem == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "주문 상품 선택이 필요합니다");
            }
            if (order == null && orderItem.getOrder() != null) {
                order = orderItem.getOrder();
            }
        }

        Long partnerId = qnaRoutingService.resolvePartnerNoForCreate(category, scope, orderItem);
        PartnerEntity partner = partnerId != null
                ? partnerRepository.findById(partnerId).orElse(null)
                : null;

        LocalDateTime now = LocalDateTime.now();
        QnaEntity qna = QnaEntity.builder()
                .customer(customer)
                .category(category)
                .scope(scope)
                .status(QnaStatus.PENDING)
                .title(request.getTitle().trim())
                .order(order)
                .orderItem(orderItem)
                .partner(partner)
                .createdAt(now)
                .updatedAt(now)
                .build();
        qna = qnaRepository.save(qna);

        QnaMessageEntity customerMessage = QnaMessageEntity.builder()
                .qna(qna)
                .authorType(QnaAuthorType.CUSTOMER)
                .body(request.getBody().trim())
                .createdAt(now)
                .build();
        qnaMessageRepository.save(customerMessage);

        return toDetailDto(qna, List.of(customerMessage));
    }

    @Transactional(readOnly = true)
    public List<QnaResponseDto> getCustomerQnaList(HttpSession session, String status, String category) {
        authService.requireRole(session, AccountRole.CUSTOMER);
        Long customerId = getCustomerIdFromSession(session);
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        List<QnaEntity> qnas = qnaRepository.findByCustomerOrderByCreatedAtDesc(customer);
        return filterAndMapList(qnas, status, category);
    }

    @Transactional(readOnly = true)
    public QnaResponseDto getCustomerQnaDetail(HttpSession session, Long qnaNo) {
        authService.requireRole(session, AccountRole.CUSTOMER);
        Long customerId = getCustomerIdFromSession(session);
        QnaEntity qna = qnaRepository.findByIdWithRelations(qnaNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.QNA_NOT_FOUND));
        assertCustomerOwner(qna, customerId);
        return toDetailDto(qna, qnaMessageRepository.findByQnaOrderByCreatedAtAsc(qna));
    }

    @Transactional(readOnly = true)
    public List<QnaResponseDto> getAdminQnaList(HttpSession session, boolean all) {
        authService.requireRole(session, AccountRole.ADMIN);
        List<QnaEntity> qnas = all
                ? qnaRepository.findAllWithRelationsOrderByCreatedAtDesc()
                : qnaRepository.findPlatformQnaOrderByCreatedAtDesc();
        return qnas.stream().map(this::toListDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QnaResponseDto getAdminQnaDetail(HttpSession session, Long qnaNo) {
        authService.requireRole(session, AccountRole.ADMIN);
        QnaEntity qna = qnaRepository.findByIdWithRelations(qnaNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.QNA_NOT_FOUND));
        return toDetailDto(qna, qnaMessageRepository.findByQnaOrderByCreatedAtAsc(qna));
    }

    public QnaResponseDto replyAsAdmin(HttpSession session, Long qnaNo, QnaReplyRequestDto request) {
        authService.requireRole(session, AccountRole.ADMIN);
        Long adminId = (Long) session.getAttribute("subjectId");
        AdminEntity admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        QnaEntity qna = qnaRepository.findByIdWithRelations(qnaNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.QNA_NOT_FOUND));

        if (qna.getPartnerId() != null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "파트너 담당 QnA는 관리자가 답변할 수 없습니다");
        }

        return saveReply(qna, QnaAuthorType.ADMIN, admin, null, request.getBody());
    }

    @Transactional(readOnly = true)
    public List<QnaResponseDto> getPartnerQnaList(HttpSession session) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);
        return qnaRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QnaResponseDto getPartnerQnaDetail(HttpSession session, Long qnaNo) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);
        QnaEntity qna = qnaRepository.findByIdWithRelations(qnaNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.QNA_NOT_FOUND));
        assertPartnerOwner(qna, partnerId);
        return toDetailDto(qna, qnaMessageRepository.findByQnaOrderByCreatedAtAsc(qna));
    }

    public QnaResponseDto replyAsPartner(HttpSession session, Long qnaNo, QnaReplyRequestDto request) {
        authService.requireRole(session, AccountRole.PARTNER);
        Long partnerId = getPartnerIdFromSession(session);
        PartnerEntity partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNER_NOT_FOUND));

        QnaEntity qna = qnaRepository.findByIdWithRelations(qnaNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.QNA_NOT_FOUND));
        assertPartnerOwner(qna, partnerId);

        return saveReply(qna, QnaAuthorType.PARTNER, null, partner, request.getBody());
    }

    private QnaResponseDto saveReply(
            QnaEntity qna,
            QnaAuthorType authorType,
            AdminEntity admin,
            PartnerEntity partner,
            String body) {

        if (qna.getStatus() == QnaStatus.ANSWERED) {
            throw new BusinessException(ErrorCode.QNA_ALREADY_ANSWERED);
        }
        if (qnaMessageRepository.countByQna(qna) >= 2) {
            throw new BusinessException(ErrorCode.QNA_ALREADY_ANSWERED);
        }

        LocalDateTime now = LocalDateTime.now();
        QnaMessageEntity reply = QnaMessageEntity.builder()
                .qna(qna)
                .authorType(authorType)
                .authorAdmin(admin)
                .authorPartner(partner)
                .body(body.trim())
                .createdAt(now)
                .build();
        qnaMessageRepository.save(reply);

        qna.markAnswered(now);
        qnaRepository.save(qna);

        List<QnaMessageEntity> messages = qnaMessageRepository.findByQnaOrderByCreatedAtAsc(qna);
        return toDetailDto(qna, messages);
    }

    private void validateCreateRequest(InquiryCategory category, QnaScope scope, Long orderItemNo) {
        if (category.requiresOrderItem() && orderItemNo == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "주문 상품 선택이 필요합니다");
        }
        if (category == InquiryCategory.RETURN_EXCHANGE) {
            if (scope == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "취소/반품/교환 문의 유형(scope)을 선택해 주세요");
            }
            if (scope == QnaScope.ORDER_ITEM && orderItemNo == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "주문 상품 선택이 필요합니다");
            }
        }
    }

    private QnaScope resolveScope(InquiryCategory category, String scopeCode) {
        if (category != InquiryCategory.RETURN_EXCHANGE) {
            return null;
        }
        return QnaScope.fromCode(scopeCode);
    }

    private OrderEntity resolveOrder(CustomerEntity customer, Long orderNo) {
        if (orderNo == null) {
            return null;
        }
        OrderEntity order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    private OrderItemEntity resolveOrderItem(CustomerEntity customer, OrderEntity order, Long orderItemNo) {
        if (orderItemNo == null) {
            return null;
        }
        OrderItemEntity orderItem = orderItemRepository.findByIdWithPartners(orderItemNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
        if (!orderItem.getOrder().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (order != null && !orderItem.getOrder().getOrderNo().equals(order.getOrderNo())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "선택한 주문과 주문 상품이 일치하지 않습니다");
        }
        return orderItem;
    }

    private List<QnaResponseDto> filterAndMapList(List<QnaEntity> qnas, String status, String category) {
        return qnas.stream()
                .filter(q -> status == null || status.isBlank() || q.getStatus().name().equalsIgnoreCase(status))
                .filter(q -> {
                    if (category == null || category.isBlank()) {
                        return true;
                    }
                    try {
                        return q.getCategory() == InquiryCategory.fromCode(category);
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
                })
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    private QnaResponseDto toListDto(QnaEntity qna) {
        List<QnaMessageEntity> messages = qnaMessageRepository.findByQnaOrderByCreatedAtAsc(qna);
        String preview = messages.stream()
                .filter(m -> m.getAuthorType() == QnaAuthorType.CUSTOMER)
                .map(QnaMessageEntity::getBody)
                .findFirst()
                .map(b -> b.length() > 80 ? b.substring(0, 80) + "…" : b)
                .orElse(null);

        return buildDto(qna, messages, preview, false);
    }

    private QnaResponseDto toDetailDto(QnaEntity qna, List<QnaMessageEntity> messages) {
        return buildDto(qna, messages, null, true);
    }

    private QnaResponseDto buildDto(
            QnaEntity qna,
            List<QnaMessageEntity> messages,
            String lastMessagePreview,
            boolean includeMessages) {

        String productName = null;
        if (qna.getOrderItem() != null && qna.getOrderItem().getProduct() != null) {
            productName = qna.getOrderItem().getProduct().getProductName();
        }

        QnaResponseDto.QnaResponseDtoBuilder builder = QnaResponseDto.builder()
                .qnaNo(qna.getQnaNo())
                .category(qna.getCategory())
                .categoryLabel(qna.getCategory().getLabel())
                .scope(qna.getScope())
                .scopeLabel(qna.getScope() != null ? qna.getScope().getLabel() : null)
                .status(qna.getStatus())
                .statusLabel(qna.getStatus().getLabel())
                .title(qna.getTitle())
                .orderNo(qna.getOrder() != null ? qna.getOrder().getOrderNo() : null)
                .orderItemNo(qna.getOrderItem() != null ? qna.getOrderItem().getOrderItemNo() : null)
                .productName(productName)
                .partnerNo(qna.getPartnerId())
                .partnerName(qna.getPartner() != null ? qna.getPartner().getPartnerName() : null)
                .customerName(qna.getCustomer().getCustomerName())
                .createdAt(qna.getCreatedAt())
                .answeredAt(qna.getAnsweredAt())
                .lastMessagePreview(lastMessagePreview);

        if (includeMessages) {
            builder.messages(messages.stream()
                    .sorted(Comparator.comparing(QnaMessageEntity::getCreatedAt))
                    .map(this::toMessageDto)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    private QnaMessageResponseDto toMessageDto(QnaMessageEntity message) {
        String authorName = switch (message.getAuthorType()) {
            case CUSTOMER -> message.getQna().getCustomer().getCustomerName();
            case ADMIN -> message.getAuthorAdmin() != null ? message.getAuthorAdmin().getAdminName() : "관리자";
            case PARTNER -> message.getAuthorPartner() != null ? message.getAuthorPartner().getPartnerName() : "파트너";
        };

        return QnaMessageResponseDto.builder()
                .messageNo(message.getMessageNo())
                .authorType(message.getAuthorType())
                .body(message.getBody())
                .authorName(authorName)
                .createdAt(message.getCreatedAt())
                .build();
    }

    private void assertCustomerOwner(QnaEntity qna, Long customerId) {
        if (!qna.getCustomer().getCustomerId().equals(customerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void assertPartnerOwner(QnaEntity qna, Long partnerId) {
        if (qna.getPartnerId() == null || !qna.getPartnerId().equals(partnerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Long getCustomerIdFromSession(HttpSession session) {
        Object customerId = session.getAttribute("customerId");
        if (customerId instanceof Long id) {
            return id;
        }
        Object subjectId = session.getAttribute("subjectId");
        if (subjectId instanceof Long id) {
            return id;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private Long getPartnerIdFromSession(HttpSession session) {
        Object subjectId = session.getAttribute("subjectId");
        if (subjectId instanceof Long id) {
            return id;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
