# 배송 조회 빈 배열 반환 - 정상 여부 확인 가이드

## 빈 배열이 반환되는 경우

### ✅ 정상 케이스 1: 주문이 아직 결제되지 않음
```
상황: 주문을 생성했지만 아직 결제를 하지 않음
주문 상태: PENDING_PAYMENT
결과: 빈 배열 [] 반환 (정상)
```

**이유**: 배송은 **결제 승인 시점**에 자동 생성됩니다.
- 주문 생성 → 배송 없음
- 결제 승인 → 배송 자동 생성 (각 OrderItem마다 Delivery 생성)

---

### ✅ 정상 케이스 2: 주문이 존재하지 않음
```
상황: 존재하지 않는 주문 번호로 조회
결과: 빈 배열 [] 반환 (정상)
```

**이유**: 주문이 없으면 배송도 없으므로 빈 배열 반환

---

### ✅ 정상 케이스 3: 파트너 권한으로 조회했지만 자신의 상품이 아님
```
상황: 파트너가 로그인하여 다른 파트너의 상품 배송 조회
결과: 빈 배열 [] 반환 (정상)
```

**이유**: 파트너는 자신의 상품(Option)이 포함된 배송만 조회 가능

---

### ⚠️ 문제 케이스: 결제는 했는데 배송이 생성되지 않음
```
상황: 결제는 완료했지만 배송이 없음
주문 상태: ACTIVE 또는 PAID
결과: 빈 배열 [] 반환 (비정상 - 버그 가능성)
```

**이유**: 결제 승인 시 `createDeliveriesForOrder()` 메서드가 호출되어야 하는데 실행되지 않았을 수 있음

---

## 확인 방법

### 1. 주문 상태 확인
```sql
SELECT order_no, order_status FROM orders WHERE order_no = {주문번호};
```

**예상 결과**:
- `PENDING_PAYMENT` → 빈 배열 정상 ✅
- `PAID` 또는 `ACTIVE` → 배송이 있어야 함 ⚠️

### 2. 주문 아이템 확인
```sql
SELECT order_item_no, order_no FROM order_item WHERE order_no = {주문번호};
```

주문 아이템이 있어야 배송이 생성됩니다.

### 3. 배송 존재 여부 확인
```sql
SELECT d.delivery_no, d.delivery_status, oi.order_item_no
FROM delivery d
JOIN order_item oi ON d.order_item_no = oi.order_item_no
WHERE oi.order_no = {주문번호};
```

### 4. 결제 여부 확인
```sql
SELECT payment_no, paid_at FROM payment WHERE payment_no = (
    SELECT payment_no FROM orders WHERE order_no = {주문번호}
);
```

`paid_at`이 있으면 결제 완료, 배송이 있어야 합니다.

---

## 테스트 시나리오

### 시나리오 1: 정상 플로우
```
1. 주문 생성 → GET /api/orders/{orderNo}/deliveries → [] ✅
2. 결제 승인 → GET /api/orders/{orderNo}/deliveries → [배송1, 배송2, ...] ✅
```

### 시나리오 2: 결제 전 조회
```
1. 주문 생성 (결제 안 함)
2. GET /api/orders/{orderNo}/deliveries
3. 결과: [] ✅ (정상 - 아직 결제 안 했으므로)
```

### 시나리오 3: 결제 후 조회
```
1. 주문 생성
2. 결제 승인 (POST /api/payments/approve)
3. GET /api/orders/{orderNo}/deliveries
4. 결과: [배송 정보들] ✅ (정상)
```

---

## 디버깅 체크리스트

빈 배열이 반환될 때 확인할 사항:

- [ ] 주문이 존재하는가?
- [ ] 주문 상태가 무엇인가? (PENDING_PAYMENT면 정상)
- [ ] 결제를 완료했는가? (`paid_at`이 있는가?)
- [ ] 주문 아이템이 있는가?
- [ ] 어떤 권한으로 조회했는가? (파트너면 자신의 상품인지 확인)
- [ ] 결제 승인 시 배송 생성 로직이 실행되었는가?

---

## 결론

**빈 배열이 반환되는 것이 정상인 경우**:
- 주문이 아직 결제되지 않았을 때 (PENDING_PAYMENT)
- 주문이 존재하지 않을 때
- 파트너가 자신의 상품이 아닌 배송을 조회할 때

**빈 배열이 반환되는 것이 비정상인 경우**:
- 결제를 완료했는데도 배송이 없을 때 (버그 가능성)
