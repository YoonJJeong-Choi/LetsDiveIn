# 배송 기능 테스트 가이드

## 테스트 환경에서 배송 상태 업데이트 방법

실제 배송이 일어나지 않는 테스트 환경에서는 **수동으로 배송 상태를 업데이트**해야 합니다.

---

## 테스트 시나리오

### 시나리오 1: 관리자로 배송 상태 업데이트 (권장)

#### 1.1 관리자 로그인
```
POST /api/auth/login
Body: {
  "email": "admin@example.com",
  "password": "admin123"
}
```

#### 1.2 배송 시작 (READY → SHIPPED)
```
PATCH /api/deliveries/{deliveryNo}/start
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
Body: {
  "trackingNumber": "1234567890",
  "courier": "CJ대한통운"
}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "deliveryNo": 1,
    "orderItemNo": 1,
    "deliveryStatus": "SHIPPED",
    "deliveryStartDate": "2024-01-15T10:30:00",
    "deliveryEndDate": null,
    "deliveryTrackingNumber": "1234567890",
    "deliveryCourier": "CJ대한통운"
  }
}
```

#### 1.3 배송 완료 (SHIPPED → DELIVERED)
```
PATCH /api/deliveries/{deliveryNo}/complete
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "deliveryNo": 1,
    "orderItemNo": 1,
    "deliveryStatus": "DELIVERED",
    "deliveryStartDate": "2024-01-15T10:30:00",
    "deliveryEndDate": "2024-01-17T14:20:00",
    "deliveryTrackingNumber": "1234567890",
    "deliveryCourier": "CJ대한통운"
  }
}
```

---

### 시나리오 2: 고객으로 배송 조회 (조회만 가능)

#### 2.1 고객 로그인
```
POST /api/auth/login
Body: {
  "email": "customer@example.com",
  "password": "customer123"
}
```

#### 2.2 본인 주문의 배송 조회
```
GET /api/orders/{orderNo}/deliveries
Headers:
  Cookie: JSESSIONID={{customerSessionId}}
```

**권한 검증:**
- 본인 주문의 배송만 조회 가능
- 다른 고객의 주문 배송 조회 시도 시 403 Forbidden

**⚠️ 주의:** 고객은 배송 상태를 업데이트할 수 없습니다. 조회만 가능합니다.

---

## 전체 테스트 플로우

### 1. 주문 생성 및 결제
```
1. 주문 생성 (PENDING_PAYMENT)
2. 결제 승인 (ACTIVE)
   → 각 OrderItem에 대해 Delivery 자동 생성 (상태: READY)
```

### 2. 배송 상태 확인
```
GET /api/orders/{orderNo}/deliveries
→ 모든 OrderItem의 배송 정보 확인
→ 초기 상태: READY (배송 준비)
```

### 3. 배송 시작 (관리자)
```
PATCH /api/deliveries/{deliveryNo}/start
→ 상태: READY → SHIPPED
→ deliveryStartDate 설정
→ 송장번호, 택배사 저장
```

### 4. 배송 완료 (관리자)
```
PATCH /api/deliveries/{deliveryNo}/complete
→ 상태: SHIPPED → DELIVERED
→ deliveryEndDate 설정
```

### 5. 배송 상태 확인
```
GET /api/orders/{orderNo}/deliveries
→ 배송 상태: DELIVERED
→ 배송 완료일 확인
```

---

## Postman 테스트 순서

### Step 1: 주문 생성 및 결제
```
POST /api/orders
→ orderNo 기록

POST /api/payments/approve
→ paymentNo 기록
→ 각 OrderItem에 Delivery 생성됨
```

### Step 2: 배송 정보 확인
```
GET /api/orders/{orderNo}/deliveries
→ deliveryNo 기록
→ 초기 상태: READY 확인
```

### Step 3: 관리자 로그인
```
POST /api/auth/login (관리자 계정)
→ adminSessionId 기록
```

### Step 4: 배송 시작
```
PATCH /api/deliveries/{deliveryNo}/start
Body: {
  "trackingNumber": "1234567890",
  "courier": "CJ대한통운"
}
→ 상태: SHIPPED 확인
→ deliveryStartDate 확인
```

### Step 5: 배송 완료
```
PATCH /api/deliveries/{deliveryNo}/complete
→ 상태: DELIVERED 확인
→ deliveryEndDate 확인
```

---

## 체크리스트

- [ ] 주문 생성 및 결제 완료
- [ ] 배송 자동 생성 확인 (각 OrderItem별)
- [ ] 초기 배송 상태: READY 확인
- [ ] 관리자 로그인
- [ ] 배송 시작 API 호출 (송장번호 입력)
- [ ] 배송 상태: SHIPPED 확인
- [ ] 배송 시작일 설정 확인
- [ ] 배송 완료 API 호출
- [ ] 배송 상태: DELIVERED 확인
- [ ] 배송 완료일 설정 확인
- [ ] 고객으로 배송 조회 (본인 주문만)
- [ ] 다른 고객의 배송 조회 시도 (403 에러)
- [ ] 고객으로 배송 상태 업데이트 시도 (403 에러 - 업데이트 불가)

---

## 주의사항

1. **배송 상태 전이 순서**
   - READY → SHIPPED → DELIVERED 순서만 가능
   - 역방향 전이 불가

2. **권한 검증**
   - 관리자: 모든 배송 상태 업데이트 가능
   - 고객: 본인 주문의 배송만 조회 가능 (업데이트 불가)

3. **송장번호 입력**
   - 배송 시작 시 필수
   - 배송 완료 후에는 수정 불가 (또는 관리자만 수정 가능)

4. **날짜 자동 설정**
   - 배송 시작: `deliveryStartDate` 자동 설정 (현재 시간)
   - 배송 완료: `deliveryEndDate` 자동 설정 (현재 시간)

5. **반품 신청 기간 정책**
   - 배송 완료 후 반품 신청은 `deliveryEndDate` 기준 7일 이내만 가능합니다.
   - 7일이 초과되면 반품 신청 API에서 반품 불가 에러가 반환됩니다.