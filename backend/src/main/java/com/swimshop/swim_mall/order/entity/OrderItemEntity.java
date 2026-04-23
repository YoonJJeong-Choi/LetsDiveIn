package com.swimshop.swim_mall.order.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.delivery.entity.DeliveryEntity;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.common.enums.OrderItemStatus;

@Entity
@Table(name = "order_item")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemNo; //주문 상품 고유식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_no", nullable = false)
    private OrderEntity order; //주문 조인

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_no", nullable = false)
    private ProductEntity product; //상품 조인

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_no", nullable = true)
    private OptionEntity option; //옵션 조인 (옵션 없으면 null)

    @Column(nullable = false)
    private Integer itemQuantity; //주문 수량

    @Column(nullable = false)
    private Long itemPrice; //주문 당시 상품 가격

    @Column(nullable = false)
    @Builder.Default
    private Long itemDiscountAmount = 0L; //할인 금액 (기본 0)

    @Column(nullable = false)
    private Long itemTotalPrice; //주문 상품 총 가격 (수량 * 단위가격 - 할인금액)

    @Column(nullable = false)
    @Builder.Default
    private Boolean isCancelled = false; //주문 상품 취소 여부 
    // (false: 정상 주문, true: 취소)

    @OneToOne(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeliveryEntity delivery; //배송 정보 (배송 시작 시 생성)

    @OneToOne(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReturnEntity returnEntity; //반품 정보 (반품 신청 시 생성)
    
    @Column(nullable = true)
    private LocalDateTime completedAt; // 구매 확정일시 (구매 확정 시 설정)
    
    @Column(nullable = true)
    private LocalDateTime confirmedAt; // 발주 확인일시 (파트너/관리자가 발주 확인 시 설정)

    @Column(nullable = true, length = 1000)
    private String partnerEventSnapshot; // 주문 시점 파트너 이벤트 대상(eventNo CSV)

    @Column(nullable = true, length = 1000)
    private String adminEventSnapshot; // 주문 시점 관리자 이벤트 대상(eventNo CSV)

    @Column(nullable = true)
    private LocalDateTime eventSnapshotLockedAt; // 이벤트 대상 판정(락인) 시점

    @Column(nullable = true)
    private Long appliedSalePolicyNo; // 주문 시 적용된 세일 정책 번호

    @Column(nullable = true, length = 100)
    private String appliedSaleCampaignId; // 주문 시 적용된 세일 캠페인 ID

    @Column(nullable = true)
    private Long appliedSaleEventNo; // 주문 시 적용된 세일 연계 이벤트 번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 30)
    private EventType appliedSaleEventType; // 주문 시 적용된 세일 연계 이벤트 타입

    @Column(nullable = true)
    private LocalDateTime saleEvaluatedAt; // 주문 시 세일 판정 시각
    
    // 이벤트 정책 스냅샷(JSON 문자열). 주문 시점 정책 고정용
    @Column(name = "admin_event_policy_snapshot", columnDefinition = "TEXT", nullable = true)
    private String adminEventPolicySnapshot;
    
    @Column(name = "partner_event_policy_snapshot", columnDefinition = "TEXT", nullable = true)
    private String partnerEventPolicySnapshot;
    
    /**
     * 주문 아이템 취소 처리
     */
    public void cancel() {
        this.isCancelled = true;
    }
    
    /**
     * 주문 상품 발주 확인 (파트너/관리자용)
     * 결제 완료된 주문 상품만 발주 확인 가능
     * 발주 확인 시 배송 정보 생성
     */
    public void confirmOrderItem() {
        // 주문 상태 확인: PAID 상태여야 함
        if (this.order.getOrderStatus() != com.swimshop.swim_mall.common.enums.OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료된 주문 상품만 발주 확인할 수 있습니다. 현재 주문 상태: " + this.order.getOrderStatus().getLabel());
        }
        
        // 이미 발주 확인된 경우
        if (this.confirmedAt != null) {
            throw new IllegalStateException("이미 발주 확인된 주문 상품입니다.");
        }
        
        this.confirmedAt = LocalDateTime.now();
    }
    
    /**
     * 주문 아이템 구매 확정 (고객이 수령 확인)
     * 배송 완료된 주문 아이템만 구매 확정 가능
     * 반품 신청 중이거나 환불 완료된 주문 아이템은 구매 확정 불가
     */
    public void completeOrderItem() {
        // 배송이 완료되었는지 확인
        if (this.delivery == null) {
            throw new IllegalStateException("배송 정보가 없는 주문 상품은 구매 확정할 수 없습니다.");
        }
        
        if (this.delivery.getDeliveryStatus() != DeliveryStatus.DELIVERED) {
            throw new IllegalStateException("배송 완료된 주문 상품만 구매 확정할 수 있습니다. 현재 배송 상태: " + this.delivery.getDeliveryStatus());
        }
        
        // 반품 상태 확인
        // 반품 진행 중이거나 환불 완료된 경우 구매 확정 불가
        if (this.returnEntity != null) {
            ReturnStatus returnStatus = this.returnEntity.getReturnStatus();
            if (returnStatus == ReturnStatus.REQUESTED || 
                returnStatus == ReturnStatus.APPROVED || 
                returnStatus == ReturnStatus.PICKUP_COMPLETED ||
                returnStatus == ReturnStatus.REFUNDED) {
                throw new IllegalStateException("반품 진행 중이거나 환불 완료된 주문 상품은 구매 확정할 수 없습니다. 현재 반품 상태: " + returnStatus);
            }
        }
        
        // 이미 구매 확정된 경우
        if (this.completedAt != null) {
            throw new IllegalStateException("이미 구매 확정된 주문 상품입니다.");
        }
        
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * 자동 구매 확정 (배송 완료 후 7일 경과 시)
     * 스케줄러에서 호출
     * 반품 진행 중이거나 환불 완료된 주문 상품은 자동 구매 확정 불가
     */
    public void autoCompleteOrderItem() {
        // 이미 구매 확정된 경우 무시
        if (this.completedAt != null) {
            return;
        }
        
        // 배송이 완료되었는지 확인
        if (this.delivery == null) {
            return; // 배송 정보가 없으면 무시
        }
        
        if (this.delivery.getDeliveryStatus() != DeliveryStatus.DELIVERED) {
            return; // 배송 완료되지 않았으면 무시
        }
        
        // 배송 완료일 확인
        LocalDateTime deliveryEndDate = this.delivery.getDeliveryEndDate();
        if (deliveryEndDate == null) {
            return; // 배송 완료일이 없으면 무시
        }
        
        // 반품 상태 확인
        // 반품 진행 중이거나 환불 완료된 경우 자동 구매 확정 불가
        if (this.returnEntity != null) {
            ReturnStatus returnStatus = this.returnEntity.getReturnStatus();
            if (returnStatus == ReturnStatus.REQUESTED || 
                returnStatus == ReturnStatus.APPROVED || 
                returnStatus == ReturnStatus.PICKUP_COMPLETED ||
                returnStatus == ReturnStatus.REFUNDED) {
                return; // 반품 진행 중이거나 환불 완료된 경우 무시
            }
        }
        
        // 배송 완료 후 7일 경과 확인
        LocalDateTime sevenDaysAfter = deliveryEndDate.plusDays(7);
        if (LocalDateTime.now().isAfter(sevenDaysAfter) || LocalDateTime.now().isEqual(sevenDaysAfter)) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void lockEventSnapshot(String partnerEventSnapshot, String adminEventSnapshot, LocalDateTime lockedAt) {
        this.partnerEventSnapshot = partnerEventSnapshot;
        this.adminEventSnapshot = adminEventSnapshot;
        this.eventSnapshotLockedAt = lockedAt;
    }

    public void lockSaleSnapshot(
            Long salePolicyNo,
            String saleCampaignId,
            Long saleEventNo,
            EventType saleEventType,
            LocalDateTime evaluatedAt
    ) {
        this.appliedSalePolicyNo = salePolicyNo;
        this.appliedSaleCampaignId = saleCampaignId;
        this.appliedSaleEventNo = saleEventNo;
        this.appliedSaleEventType = saleEventType;
        this.saleEvaluatedAt = evaluatedAt;
    }
    
    public void lockEventPolicySnapshot(String adminPolicyJson, String partnerPolicyJson) {
        this.adminEventPolicySnapshot = adminPolicyJson;
        this.partnerEventPolicySnapshot = partnerPolicyJson;
    }
    
    /**
     * 주문 상품의 현재 상태를 계산하여 반환 (읽기 전용, computed 필드)
     * 여러 필드를 기반으로 상태를 계산합니다.
     * 
     * 주의: 이 메서드는 항상 최신 상태를 계산하므로, 다른 필드들이 변경되면
     * 자동으로 올바른 상태를 반환합니다. DB에 저장되지 않는 computed 필드입니다.
     * 
     * 우선순위: 취소 > 반품 상태 > 구매 확정 > 배송 상태 > 발주 확인 > 기본 상태
     * 
     * @return 주문 상품의 현재 상태 (OrderItemStatus enum)
     */
    public OrderItemStatus getStatus() {
        // 취소된 경우
        if (this.isCancelled) {
            return OrderItemStatus.CANCELLED;
        }
        
        // 반품 상태 확인 (최우선)
        if (this.returnEntity != null) {
            ReturnStatus returnStatus = this.returnEntity.getReturnStatus();
            switch (returnStatus) {
                case REFUNDED:
                    return OrderItemStatus.REFUNDED;
                case REJECTED:
                    return OrderItemStatus.RETURN_REJECTED;
                case REQUESTED:
                case APPROVED:
                case PICKUP_COMPLETED:
                    return OrderItemStatus.RETURN_IN_PROGRESS;
                default:
                    break;
            }
        }
        
        // 구매 확정 확인
        if (this.completedAt != null) {
            return OrderItemStatus.COMPLETED;
        }
        
        // 배송 상태 확인
        if (this.delivery != null) {
            DeliveryStatus deliveryStatus = this.delivery.getDeliveryStatus();
            switch (deliveryStatus) {
                case DELIVERED:
                    return OrderItemStatus.DELIVERED;
                case SHIPPED:
                    return OrderItemStatus.SHIPPED;
                case READY:
                    return OrderItemStatus.READY;
                default:
                    break;
            }
        }
        
        // 발주 확인 확인
        if (this.confirmedAt != null) {
            return OrderItemStatus.CONFIRMED;
        }
        
        // 기본 상태: 발주 확인 대기
        return OrderItemStatus.PENDING_CONFIRMATION;
    }
    
    /**
     * 주문 상품의 현재 상태를 한글로 반환 (읽기 전용)
     * 
     * @return 주문 상품의 현재 상태를 한글로 나타내는 문자열
     */
    public String getStatusLabel() {
        return getStatus().getLabel();
    }
}
