# 주문 상태와 배송 상태 연결 관계

## 개요

주문 상태(`OrderStatus`)와 배송 상태(`DeliveryStatus`)는 서로 연동되어 있으며, 각 상태 전이 시 상호 검증이 이루어집니다.

## 상태 정의

### 주문 상태 (OrderStatus)
- `PENDING_PAYMENT`: 결제 대기중
- `PAID`: 결제 완료
- `ACTIVE`: 주문 진행중 (배송 준비 → 배송 중 → 배송 완료 → 구매 확정 대기)
- `COMPLETED`: 구매 확정
- `CANCELLED`: 주문 취소
- `PAYMENT_FAILED`: 결제 실패

### 배송 상태 (DeliveryStatus)
- `READY`: 배송 준비
- `SHIPPED`: 배송 중
- `DELIVERED`: 배송 완료

## 연결 관계

### 1. 발주 확인 시 배송 생성
**시점**: `PAID` → `ACTIVE` 전이 시
- 주문 상태: `PAID` → `ACTIVE`
- 배송 생성: 각 `OrderItem`에 대해 `Delivery` 생성 (상태: `READY`)
- **코드 위치**: `OrderService.confirmOrder()` → `createDeliveriesForOrder()`

```java
// 발주 확인 시 배송 생성
order.updateStatus(OrderStatus.ACTIVE);
createDeliveriesForOrder(order); // 각 OrderItem에 Delivery 생성 (READY 상태)
```

### 2. 배송 시작 시
**시점**: 배송 시작 API 호출 시
- 배송 상태: `READY` → `SHIPPED`
- 주문 상태: `ACTIVE` 유지 (변경 없음)
- **코드 위치**: `DeliveryService.startDelivery()`

**제약사항**:
- 배송 시작 후 주문 취소 불가
- 배송 시작 후 주문 상태는 `ACTIVE` 유지

### 3. 배송 완료 시
**시점**: 배송 완료 API 호출 시
- 배송 상태: `SHIPPED` → `DELIVERED`
- 주문 상태: `ACTIVE` 유지 (변경 없음)
- **코드 위치**: `DeliveryService.completeDelivery()`

**제약사항**:
- 배송 완료 후 주문 취소 불가
- 배송 완료 후 주문 상태는 `ACTIVE` 유지 (구매 확정 대기)

### 4. 구매 확정 시
**시점**: 고객이 구매 확정 API 호출 시
- 주문 상태: `ACTIVE` → `COMPLETED`
- **전제 조건**: 모든 배송이 `DELIVERED` 상태여야 함
- **코드 위치**: `OrderEntity.completeOrder()`

```java
// 구매 확정 검증
boolean allDelivered = this.orderItems.stream()
    .allMatch(orderItem -> 
        orderItem.getDelivery() != null && 
        orderItem.getDelivery().getDeliveryStatus() == DeliveryStatus.DELIVERED
    );
```

### 5. 자동 구매 확정
**시점**: 배송 완료 후 7일 경과 시 (매일 새벽 2시 스케줄러 실행)
- 주문 상태: `ACTIVE` → `COMPLETED`
- **전제 조건**: 
  - 모든 배송이 `DELIVERED` 상태
  - 마지막 배송 완료일로부터 7일 경과
- **코드 위치**: `OrderAutoCompleteScheduler.autoCompleteOrders()`

```java
// 자동 구매 확정 검증
LocalDateTime lastDeliveryDate = this.orderItems.stream()
    .map(orderItem -> orderItem.getDelivery().getDeliveryEndDate())
    .max(LocalDateTime::compareTo)
    .orElse(null);

if (LocalDateTime.now().isAfter(lastDeliveryDate.plusDays(7))) {
    this.orderStatus = OrderStatus.COMPLETED;
}
```

### 6. 주문 취소 시 배송 상태 확인
**시점**: 주문 상태를 `CANCELLED`로 변경 시
- **제약사항**: 배송 상태가 `SHIPPED` 또는 `DELIVERED`인 경우 취소 불가
- **코드 위치**: `OrderEntity.changeStatus()`

```java
// 주문 취소 검증
if (newStatus == OrderStatus.CANCELLED) {
    boolean hasShippedDelivery = this.orderItems.stream()
        .anyMatch(orderItem -> {
            if (orderItem.getDelivery() != null) {
                DeliveryStatus status = orderItem.getDelivery().getDeliveryStatus();
                return status == DeliveryStatus.SHIPPED || 
                       status == DeliveryStatus.DELIVERED;
            }
            return false;
        });
    
    if (hasShippedDelivery) {
        throw new IllegalStateException("배송이 시작된 주문은 취소할 수 없습니다.");
    }
}
```

## 상태 전이 다이어그램

```
PENDING_PAYMENT (결제 대기)
    ↓
PAID (결제 완료)
    ↓ [발주 확인]
ACTIVE (주문 진행중)
    ├─→ 배송 생성: READY (배송 준비)
    ├─→ 배송 시작: SHIPPED (배송 중)
    ├─→ 배송 완료: DELIVERED (배송 완료)
    ├─→ 구매 확정: COMPLETED (구매 확정) [모든 배송 DELIVERED 필요]
    └─→ 자동 구매 확정: COMPLETED (배송 완료 후 7일 경과)

취소 가능 시점:
- PENDING_PAYMENT: 언제든지 취소 가능
- PAID: 발주 확인 전까지 취소 가능
- ACTIVE: 배송 상태가 READY일 때만 취소 가능
- SHIPPED/DELIVERED: 취소 불가 (반품으로 처리)
```

## 주요 제약사항

### 1. 주문 취소 제약
- 배송 상태가 `READY`일 때만 취소 가능
- 배송 상태가 `SHIPPED` 또는 `DELIVERED`인 경우 취소 불가
- 배송 시작 후에는 반품으로 처리해야 함

### 2. 구매 확정 제약
- 모든 배송이 `DELIVERED` 상태여야 함
- 배송이 완료되지 않은 주문은 구매 확정 불가

### 3. 자동 구매 확정 제약
- 모든 배송이 `DELIVERED` 상태여야 함
- 마지막 배송 완료일로부터 7일 경과해야 함
- `ACTIVE` 상태인 주문만 자동 구매 확정 가능

## 프론트엔드 연동

### 주문 관리 화면
- 배송 상태에 따라 취소 옵션 비활성화
- 배송 중/완료 상태에서 취소 시도 시 안내 메시지 표시

### 주문 상세 화면
- 각 주문 상품의 배송 상태 표시
- 배송 상태에 따른 주문 상태 변경 옵션 제한

## 검증 포인트

✅ **적절하게 연결됨**:
1. 발주 확인 시 배송 자동 생성
2. 배송 상태에 따른 주문 취소 제약
3. 배송 완료 상태에 따른 구매 확정 제약
4. 자동 구매 확정 로직 (배송 완료 후 7일)

⚠️ **주의사항**:
- 배송 시작/완료 시 주문 상태는 변경되지 않음 (의도된 동작)
- 주문 상태는 `ACTIVE` 유지, 배송 상태만 변경됨
- 구매 확정은 고객이 직접 하거나 7일 후 자동 처리

## 결론

주문 상태와 배송 상태는 **적절하게 연결**되어 있습니다:
- 발주 확인 시 배송 자동 생성
- 배송 상태에 따른 주문 취소 제약
- 배송 완료 상태에 따른 구매 확정 제약
- 자동 구매 확정 로직 (배송 완료 후 7일)

모든 상태 전이 시 상호 검증이 이루어지며, 비즈니스 로직에 맞게 구현되어 있습니다.
