# 배송 기능 구현 계획

## 현재 상태 분석

### ✅ 이미 있는 것
1. DeliveryEntity - 배송 엔티티 존재
   - OrderItem과 1:1 관계
   - DeliveryStatus enum (READY, SHIPPED, DELIVERED)
   - 배송 추적 정보 필드 (trackingNumber, courier)
   - 배송 시작/완료 날짜 필드

2. DeliveryStatus Enum - 배송 상태
   - READY("배송 준비")
   - SHIPPED("배송 중")
   - DELIVERED("배송 완료")

### ❌ 없는 것
1. DeliveryRepository
2. DeliveryService
3. DeliveryController
4. Delivery DTO (Request/Response)
5. 주문 생성 시 Delivery 자동 생성 로직
6. 배송 상태 업데이트 로직

---

## 구현 계획

### Phase 1: 기본 구조 생성 (필수)

#### 1.1 DeliveryRepository 생성
```java
public interface DeliveryRepository extends JpaRepository<DeliveryEntity, Long> {
    Optional<DeliveryEntity> findByOrderItem_OrderItemNo(Long orderItemNo);
    List<DeliveryEntity> findByOrderItem_Order_OrderNo(Long orderNo);
}
```

#### 1.2 DeliveryEntity에 Builder 추가
- 현재 AllArgsConstructor만 private로 있음
- Builder 패턴 추가 필요

#### 1.3 Delivery DTO 생성
- `DeliveryResponseDto` - 배송 정보 응답
- `DeliveryUpdateRequestDto` - 배송 상태/송장번호 업데이트 요청

---

### Phase 2: 배송 생성 (결제 완료 시)

#### 2.1 주문 생성 시 Delivery 자동 생성
시점: 결제 승인 완료 후 (`OrderStatus.ACTIVE`)

로직:
- 주문의 모든 OrderItem에 대해 Delivery 생성
- 초기 상태: `DeliveryStatus.READY` (배송 준비)
- 배송 시작일/완료일은 null

위치: `PaymentService.approvePayment()` 메서드 내
- 결제 승인 성공 후 → OrderItem별 Delivery 생성

---

### Phase 3: 배송 조회 API

#### 3.1 주문별 배송 조회
```
GET /api/orders/{orderNo}/deliveries
```
- 주문의 모든 OrderItem의 배송 정보 반환
- 고객: 본인 주문만 조회 가능
- 관리자/파트너: 모든 주문 조회 가능 (나중에)

#### 3.2 주문 아이템별 배송 조회
```
GET /api/order-items/{orderItemNo}/delivery
```
- 특정 주문 아이템의 배송 정보 반환

#### 3.3 배송 번호로 조회
```
GET /api/deliveries/{deliveryNo}
```
- 배송 번호로 직접 조회

---

### Phase 4: 배송 상태 업데이트 API

⚠️ 중요: 테스트 환경에서 배송 상태 업데이트 방법

실제 배송이 일어나지 않는 테스트 환경에서는 수동으로 배송 상태를 업데이트해야 합니다.

#### 방법 1: 관리자/파트너용 API (권장 - 실제 운영과 동일)
- 관리자나 파트너가 송장번호를 입력하고 배송 상태를 수동으로 업데이트
- 실제 운영 환경과 동일한 방식

#### 방법 2: ~~테스트용 고객 업데이트 API~~ (불필요)
- ~~고객이 자신의 주문 배송 상태를 업데이트~~ → 제거
- 실제 운영 환경과 다르게 동작하게 되어 혼란만 가중됨
- 관리자/파트너만 업데이트하는 것이 실제 운영 환경과 동일

#### 4.1 배송 시작 (배송 준비 → 배송 중)
```
PATCH /api/deliveries/{deliveryNo}/start
권한: ADMIN 또는 PARTNER
Body: {
  "trackingNumber": "1234567890",
  "courier": "CJ대한통운"
}
```
- 상태: `READY` → `SHIPPED`
- `deliveryStartDate` 설정 (현재 시간)
- 송장번호, 택배사 저장

#### 4.2 배송 완료 (배송 중 → 배송 완료)
```
PATCH /api/deliveries/{deliveryNo}/complete
권한: ADMIN 또는 PARTNER
```
- 상태: `SHIPPED` → `DELIVERED`
- `deliveryEndDate` 설정 (현재 시간)

#### 4.3 배송 정보 수정
```
PATCH /api/deliveries/{deliveryNo}
권한: ADMIN 또는 PARTNER
Body: {
  "trackingNumber": "9876543210",
  "courier": "한진택배"
}
```
- 송장번호, 택배사 수정 (배송 시작 전/중)

---

### Phase 5: 권한 관리

#### 5.1 고객 권한
- 본인 주문의 배송 정보만 조회 가능
- 배송 상태 업데이트 불가 (관리자/파트너만 가능)
- 실제 운영 환경과 동일하게 유지

#### 5.2 관리자/파트너 권한
- 모든 배송 정보 조회 가능
- 배송 상태 업데이트 가능
- 파트너는 자신의 상품 배송만 관리 (나중에 구현)

#### 5.3 테스트 환경 고려사항
- 테스트 시에도 관리자/파트너 계정으로 배송 상태 업데이트
- 실제 운영 환경과 동일한 방식으로 테스트
- 고객이 업데이트할 수 있게 하면 실제 환경과 다르게 동작하여 혼란만 가중됨

---

## 구현 순서 (우선순위)

### 🔴 최우선 (MVP)
1. ✅ DeliveryEntity에 Builder 추가
2. ✅ DeliveryRepository 생성
3. ✅ Delivery DTO 생성
4. ✅ 결제 승인 시 Delivery 자동 생성
5. ✅ 주문별 배송 조회 API (고객용)

### 🟡 중요 (다음 단계)
6. 주문 아이템별 배송 조회 API
7. 배송 상태 업데이트 API (관리자/파트너용) ⭐ 테스트 필수
   - 배송 시작: 송장번호 입력
   - 배송 완료: 상태 변경
   - 테스트 시에도 관리자 계정으로 업데이트 (실제 운영과 동일)
8. 배송 번호로 조회 API

### 🟢 선택 (나중에)
9. 배송 추적 API (외부 택배사 API 연동)
10. 배송 알림 기능
11. 배송 통계 (관리자 대시보드)

---

## 데이터 흐름

### 배송 생성 흐름
```
주문 생성 (PENDING_PAYMENT)
  ↓
결제 승인 (ACTIVE)
  ↓
각 OrderItem에 대해 Delivery 생성
  - deliveryStatus: READY
  - deliveryStartDate: null
  - deliveryEndDate: null
```

### 배송 상태 전이
```
READY (배송 준비)
  ↓ [배송 시작]
SHIPPED (배송 중)
  ↓ [배송 완료]
DELIVERED (배송 완료)
```

---

## API 엔드포인트 요약

### 고객용
- `GET /api/orders/{orderNo}/deliveries` - 주문별 배송 조회
- `GET /api/order-items/{orderItemNo}/delivery` - 주문 아이템별 배송 조회
- `GET /api/deliveries/{deliveryNo}` - 배송 번호로 조회

### 관리자/파트너용 (나중에)
- `PATCH /api/deliveries/{deliveryNo}/start` - 배송 시작
- `PATCH /api/deliveries/{deliveryNo}/complete` - 배송 완료
- `PATCH /api/deliveries/{deliveryNo}` - 배송 정보 수정

---

## 주의사항

1. OrderItem 단위 배송
   - 주문 단위가 아니라 주문 아이템 단위로 배송 관리
   - 한 주문에 여러 상품이 있으면 각각 별도 배송

2. 배송 생성 시점
   - 결제 완료 후에만 배송 생성 (PENDING_PAYMENT 상태에서는 생성 안 함)

3. 배송 상태 전이 검증
   - READY → SHIPPED → DELIVERED 순서만 가능
   - 역방향 전이 불가
   - 상태 전이 시 날짜 자동 설정 (deliveryStartDate, deliveryEndDate)

4. 권한 검증
   - 고객은 본인 주문의 배송만 조회 가능
   - 배송 상태 업데이트는 관리자/파트너만 가능
   - 실제 운영 환경과 동일하게 유지

5. 테스트 환경 고려
   - 실제 배송이 일어나지 않으므로 수동으로 상태 업데이트 필요
   - 관리자 계정으로 로그인하여 배송 상태 업데이트 (실제 운영과 동일)
   - 실제 운영 환경에서도 관리자가 송장번호를 입력하고 배송 상태를 업데이트하므로, 테스트도 동일한 방식으로 진행
   - 고객이 업데이트할 수 있게 하면 실제 환경과 다르게 동작하여 혼란만 가중됨
   
   참고: 자동화가 필요하면 나중에 추가 가능 (택배사 API 연동, 스케줄러 등)

---

## 다음 단계

1. DeliveryEntity에 Builder 추가
2. DeliveryRepository 생성
3. Delivery DTO 생성
4. PaymentService에서 결제 승인 시 Delivery 생성 로직 추가
5. DeliveryService 생성 (조회 로직)
6. DeliveryController 생성 (조회 API)
7. 테스트
