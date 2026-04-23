# 결제 API 테스트 시나리오

## 수정된 기능 테스트 가이드

### 변경사항 요약
1. ✅ 클라이언트 금액 검증 추가
2. ✅ `paidAt` 필드 추가 (결제 승인 시점)
3. ✅ 응답에 `orderStatus` 포함

---

## 테스트 준비

### 1. 주문 생성
먼저 결제 대기 상태의 주문을 생성합니다.

```bash
POST http://localhost:8080/api/orders
Cookie: JSESSIONID=your-session-id

{
  "cartItemNos": [1, 2],
  "recipientName": "홍길동",
  "recipientPhone": "010-1234-5678",
  "deliveryAddress": "서울시 강남구 테헤란로 123",
  "deliveryAddressDetail": "456호",
  "deliveryZipCode": "06234",
  "paymentMethod": "CARD",
  "orderMemo": "문 앞에 놔주세요"
}
```

**응답에서 `orderNo`와 `orderTotalPrice`를 기록하세요.**

예: `orderNo: 1`, `orderTotalPrice: 150000`

---

## 테스트 시나리오

### ✅ 시나리오 1: 정상 결제 승인 (금액 없이)

**요청:**
```json
POST /api/payments/approve
{
  "orderNo": 1
}
```

**예상 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentNo": 1,
    "orderNo": 1,
    "paymentAmount": 150000,
    "paymentMethod": "CARD",
    "paymentStatus": "APPROVED",
    "orderStatus": "ACTIVE",
    "paymentCreatedAt": "2024-01-15T10:30:00",
    "paidAt": "2024-01-15T10:30:05",
    "paymentCancelYn": false
  }
}
```

**확인 사항:**
- ✅ `paymentStatus`가 "APPROVED"
- ✅ `orderStatus`가 "ACTIVE"
- ✅ `paidAt` 필드가 있고 현재 시간과 비슷함
- ✅ `paymentAmount`가 주문 금액과 일치

---

### ✅ 시나리오 2: 정상 결제 승인 (금액 포함, 일치)

**요청:**
```json
POST /api/payments/approve
{
  "orderNo": 1,
  "paymentAmount": 150000
}
```

**예상 응답 (200 OK):**
- 시나리오 1과 동일

**확인 사항:**
- ✅ 금액이 일치하여 정상 처리됨

---

### ❌ 시나리오 3: 금액 불일치 (에러)

**요청:**
```json
POST /api/payments/approve
{
  "orderNo": 1,
  "paymentAmount": 100000
}
```
(주문 금액이 150000인 경우)

**예상 응답 (400 Bad Request):**
```json
{
  "success": false,
  "message": "결제 금액이 주문 금액과 일치하지 않습니다. 주문 금액: 150000, 요청 금액: 100000"
}
```

**확인 사항:**
- ✅ 에러 메시지에 주문 금액과 요청 금액이 명확히 표시됨
- ✅ 결제가 처리되지 않음 (주문 상태가 여전히 PENDING_PAYMENT)

---

### ❌ 시나리오 4: 중복 결제 시도 (에러)

**전제 조건:** 이미 결제된 주문 (orderNo: 1)

**요청:**
```json
POST /api/payments/approve
{
  "orderNo": 1
}
```

**예상 응답 (400 Bad Request):**
```json
{
  "success": false,
  "message": "이미 결제가 완료된 주문입니다. (현재 상태: ACTIVE)"
}
```

**확인 사항:**
- ✅ 중복 결제가 차단됨
- ✅ 명확한 에러 메시지

---

### ❌ 시나리오 5: 다른 사용자의 주문 결제 시도 (에러)

**전제 조건:** 다른 사용자가 생성한 주문 (orderNo: 2)

**요청:**
```json
POST /api/payments/approve
{
  "orderNo": 2
}
```

**예상 응답 (403 Forbidden):**
```json
{
  "success": false,
  "message": "접근 권한이 없습니다."
}
```

**확인 사항:**
- ✅ 소유자 검증이 정상 작동

---

### ❌ 시나리오 6: 결제 실패 상태인 주문 결제 시도 (에러)

**전제 조건:** 결제 실패 처리된 주문

**요청:**
```json
POST /api/payments/approve
{
  "orderNo": 3
}
```

**예상 응답 (400 Bad Request):**
```json
{
  "success": false,
  "message": "결제 실패한 주문입니다. 재결제 기능은 아직 구현되지 않았습니다. (현재 상태: PAYMENT_FAILED)"
}
```

---

## 추가 테스트 시나리오 (결제 기능 강화)

### 시나리오 7: 중복 결제 방지 강화 (Payment 레코드 체크)

**전제 조건:** 이미 결제 성공한 주문 (orderNo: 1, paymentNo: 1, paidAt이 있음)

**요청:**
```json
POST /api/payments/approve
{
  "orderNo": 1
}
```

**예상 응답 (400 Bad Request):**
```json
{
  "success": false,
  "message": "이미 결제가 완료된 주문입니다. 결제 번호: 1, 결제 시점: 2024-01-15T10:30:05"
}
```

**확인 사항:**
- ✅ Payment 레코드 존재 여부로도 중복 결제 차단
- ✅ 에러 메시지에 결제 번호와 결제 시점 포함

---

### 시나리오 8: 조기 리턴 (이미 결제 완료 시)

**전제 조건:** 이미 결제 완료된 주문 (orderNo: 1, 상태: ACTIVE, paymentNo: 1)

**요청:**
```json
POST /api/payments/approve
{
  "orderNo": 1
}
```

**예상 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentNo": 1,
    "orderNo": 1,
    "paymentAmount": 150000,
    "paymentMethod": "CARD",
    "paymentStatus": "APPROVED",
    "orderStatus": "ACTIVE",
    "paymentCreatedAt": "2024-01-15T10:30:00",
    "paidAt": "2024-01-15T10:30:05",
    "paymentCancelYn": false
  }
}
```

**확인 사항:**
- ✅ 에러 대신 기존 결제 정보 반환
- ✅ 불필요한 처리 없이 즉시 반환

---

### 시나리오 9: 결제 번호로 결제 조회

**요청:**
```
GET /api/payments/1
```

**예상 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentNo": 1,
    "orderNo": 1,
    "paymentAmount": 150000,
    "paymentMethod": "CARD",
    "paymentStatus": "APPROVED",
    "orderStatus": "ACTIVE",
    "paymentCreatedAt": "2024-01-15T10:30:00",
    "paidAt": "2024-01-15T10:30:05",
    "paymentCancelYn": false
  }
}
```

**확인 사항:**
- ✅ 결제 번호로 결제 정보 조회 성공
- ✅ 모든 결제 정보 필드 포함

---

### 시나리오 10: 주문 번호로 결제 조회

**요청:**
```
GET /api/orders/1/payment
```

**예상 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentNo": 1,
    "orderNo": 1,
    "paymentAmount": 150000,
    "paymentMethod": "CARD",
    "paymentStatus": "APPROVED",
    "orderStatus": "ACTIVE",
    "paymentCreatedAt": "2024-01-15T10:30:00",
    "paidAt": "2024-01-15T10:30:05",
    "paymentCancelYn": false
  }
}
```

**확인 사항:**
- ✅ 주문 번호로 결제 정보 조회 성공
- ✅ 주문 상세 페이지에서 결제 정보 표시 용이

---

### 시나리오 11: 결제 정보 없는 주문 조회

**전제 조건:** 결제 대기 상태인 주문 (orderNo: 2, 상태: PENDING_PAYMENT)

**요청:**
```
GET /api/orders/2/payment
```

**예상 응답 (404 Not Found):**
```json
{
  "success": false,
  "message": "이 주문에 대한 결제 정보가 없습니다."
}
```

**확인 사항:**
- ✅ 결제가 아직 진행되지 않은 주문은 404 에러 반환

---

## Postman 컬렉션 예시

### 1. 결제 승인 (금액 없이)
```
POST http://localhost:8080/api/payments/approve
Headers:
  Content-Type: application/json
  Cookie: JSESSIONID={{sessionId}}
Body:
{
  "orderNo": {{orderNo}}
}
```

### 2. 결제 승인 (금액 포함)
```
POST http://localhost:8080/api/payments/approve
Headers:
  Content-Type: application/json
  Cookie: JSESSIONID={{sessionId}}
Body:
{
  "orderNo": {{orderNo}},
  "paymentAmount": {{orderTotalPrice}}
}
```

### 3. 금액 불일치 테스트
```
POST http://localhost:8080/api/payments/approve
Headers:
  Content-Type: application/json
  Cookie: JSESSIONID={{sessionId}}
Body:
{
  "orderNo": {{orderNo}},
  "paymentAmount": 99999
}
```

### 4. 결제 번호로 결제 조회
```
GET http://localhost:8080/api/payments/{{paymentNo}}
Headers:
  Cookie: JSESSIONID={{sessionId}}
```

### 5. 주문 번호로 결제 조회
```
GET http://localhost:8080/api/orders/{{orderNo}}/payment
Headers:
  Cookie: JSESSIONID={{sessionId}}
```

---

## 체크리스트

### 결제 승인 테스트
- [ ] 시나리오 1: 금액 없이 결제 승인 성공
- [ ] 시나리오 2: 금액 포함 결제 승인 성공
- [ ] 시나리오 3: 금액 불일치 에러 확인
- [ ] 시나리오 4: 중복 결제 방지 확인 (OrderStatus 체크)
- [ ] 시나리오 4-1: 중복 결제 방지 확인 (Payment 레코드 체크)
- [ ] 시나리오 4-2: 조기 리턴 확인 (이미 결제 완료 시 기존 결제 정보 반환)
- [ ] 시나리오 5: 소유자 검증 확인
- [ ] 시나리오 6: 결제 실패 상태 주문 처리 확인
- [ ] 응답에 `orderStatus` 필드 확인
- [ ] 응답에 `paidAt` 필드 확인 (결제 성공 시)
- [ ] `paidAt`이 null인지 확인 (결제 실패 시)

### 결제 조회 테스트
- [ ] 결제 번호로 결제 조회 성공
- [ ] 주문 번호로 결제 조회 성공
- [ ] 다른 고객의 결제 정보 조회 시도 (403 에러)
- [ ] 존재하지 않는 결제 번호 조회 (404 에러)
- [ ] 결제 정보가 없는 주문 조회 (404 에러)
