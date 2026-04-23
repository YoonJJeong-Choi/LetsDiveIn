# 주문 상태 전이 규칙

## 주문 상태 목록

1. **PENDING_PAYMENT** (결제 대기중)
   - 주문 생성 직후 초기 상태
   - 결제 승인 대기 중

2. **PAID** (결제 완료)
   - 결제 성공 후 상태
   - 결제 승인 완료

3. **PAYMENT_FAILED** (결제 실패)
   - 결제 실패 또는 취소 시 상태
   - 재결제 가능

4. **ACTIVE** (주문 진행중)
   - 결제 완료 후 주문 처리 중
   - 배송 준비, 배송 중 등

5. **CANCELLED** (주문 취소)
   - 주문 전체 취소
   - 환불 처리 완료

## 상태 전이 규칙

```
주문 생성
    ↓
PENDING_PAYMENT (결제 대기중)
    ↓
    ├─→ PAID (결제 성공)
    │       ↓
    │   ACTIVE (주문 진행중)
    │       ↓
    │   CANCELLED (주문 취소)
    │
    └─→ PAYMENT_FAILED (결제 실패)
            ↓
        (재결제 시도 가능 → PENDING_PAYMENT로 복귀)
```

## 상태 전이 조건

### PENDING_PAYMENT → PAID
- 결제 승인 성공 시
- 결제 API에서 성공 응답 수신 시

### PENDING_PAYMENT → PAYMENT_FAILED
- 결제 승인 실패 시
- 결제 취소 시
- 결제 타임아웃 시

### PAID → ACTIVE
- 결제 완료 후 주문 처리 시작 시
- 배송 준비 시작 시

### ACTIVE → CANCELLED
- 주문 취소 요청 시
- 환불 처리 완료 시

### PAYMENT_FAILED → PENDING_PAYMENT
- 재결제 시도 시
- 결제 재진행 시

## 구현 예시

```java
// 주문 생성 시
OrderEntity order = OrderEntity.builder()
    .orderStatus(OrderStatus.PENDING_PAYMENT) // 초기 상태
    .build();

// 결제 성공 시
order.updateStatus(OrderStatus.PAID);

// 결제 실패 시
order.updateStatus(OrderStatus.PAYMENT_FAILED);

// 주문 처리 시작 시
order.updateStatus(OrderStatus.ACTIVE);

// 주문 취소 시
order.updateStatus(OrderStatus.CANCELLED);
```
