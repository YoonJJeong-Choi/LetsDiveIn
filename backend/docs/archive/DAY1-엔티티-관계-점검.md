# Day1 엔티티·관계 점검 결과 (엔티티 수정 없음, 메모만)

---

## A. 계정/권한 (로그인 기반)

| 항목 | 있다/없다 | 비고 |
|------|-----------|------|
| Customer 로그인 식별자(이메일) + 비밀번호 | 있다 | `customerEmail`, `customerPassword` |
| Partner 로그인 식별자(이메일) + 비밀번호 | 없다 | 추가 필요 (PartnerEntity에 이메일/비밀번호 없음) |
| Admin 로그인 식별자(이메일) + 비밀번호 | 일부 | `adminPassword` 있음, 로그인 식별자(이메일 등) 없음 → 추가 필요 |
| Partner 승인/활성/정지 상태값 | 있다 | `partnerStatus` (String). enum `PartnerStatus`(PENDING/APPROVED/INACTIVE) 존재 → 타입을 enum으로 통일 권장 |
| Admin 상태값 | 있다 | `adminStatus` (String). “승인/활성/정지” 표현 가능 (enum 없음, 필요 시 추가) |
| status 개념 위치 | 있음 | Customer: 인증은 `emailChecked`. Partner: `partnerStatus`, Admin: `adminStatus` |

---

## B. 상품 도메인 (파트너 핵심)

| 항목 | 있다/없다 | FK 위치 | 관계 | 삭제 시 | 조회 |
|------|-----------|---------|------|---------|------|
| Product ↔ Partner (상품이 어느 파트너 소속인지) | 애매 | Product에 partner FK 없음. Option에만 `partner_no` 있음 | Option이 Product·Partner 모두 참조. 상품 단위 소유는 없고, 옵션 단위로만 파트너 구분 | - | 파트너별 상품은 Option 기준으로만 가능. 상품 1개를 여러 파트너가 옵션으로 나눠 가질 수 있는 구조 → “상품 1건 = 파트너 1명”이 필요하면 추가 필요 (Product에 partner FK 또는 1:N 역방향) |
| Product ↔ Option | 있다 | Option 쪽 `product_no` | Option N:1 Product (옵션은 상품에 귀속) | Option 삭제 시 Product는 유지. Product 삭제 시 Option 고아 가능 → Product 쪽 cascade 정책 확인 권장 | 상품별 옵션 조회: Product → Option 리스트 (ProductEntity에 OneToMany 없음, Repository에서 옵션 조회) |
| Inventory ↔ Option | 있다 | Inventory 쪽 `option_no` | Inventory N:1 Option. 옵션별 재고 수량 관리 가능 | Option 삭제 시 Inventory 고아 가능 | 옵션별 재고 조회 가능 (Inventory → Option) |

---

## C. 장바구니/주문 (고객 핵심 플로우)

| 항목 | 있다/없다 | 비고 |
|------|-----------|------|
| Cart ↔ Customer | 없다 | 추가 필요 (Cart 엔티티 없음) |
| CartItem ↔ Product/Option | 없다 | 추가 필요 (CartItem 엔티티 없음. 장바구니 아이템이 상품/옵션/수량 표현 필요) |
| Order ↔ Customer | 있다 | Order 쪽 `customer_id`. Order N:1 Customer. 고객별 주문 조회 가능 |
| Order ↔ OrderItem | 있다 | Order 1:N OrderItem. FK는 OrderItem 쪽 `order_no`. cascade ALL + orphanRemoval → 삭제 시 고아 없음. 주문별 아이템 조회 쉬움 |
| OrderItem ↔ Product / Option | 있다 | OrderItem에 `product_no`, `option_no`(nullable). “무슨 상품(옵션)을 몇 개 샀는지” 표현 가능 |

---

## D. 배송/결제 (최소 동작 기준)

| 항목 | 있다/없다 | FK 위치 | 관계 | 비고 |
|------|-----------|---------|------|------|
| Order ↔ Delivery | 간접 연결 | Delivery 쪽 `order_item_no`. OrderItem 1:1 Delivery | Order 1:N OrderItem 1:1 Delivery. 주문이 배송과 연결될 수 있는 구조임 (OrderItem 경유) | 주문 단위가 아니라 주문 아이템 단위 배송 (아이템별 배송 상태 관리에 적합) |
| Order ↔ Payment | 있다 | Order 쪽 `payment_no` (unique). Payment 쪽 mappedBy로 Order 역참조 | Order 1:1 Payment. 결제가 주문에 귀속 | PG 연동 없이 “주문–결제 연결 구조”만 확인 → 가능 |

---

## E. 반품/리뷰

| 항목 | 있다/없다 | FK 위치 | 관계 | 삭제 시 | 조회 |
|------|-----------|---------|------|---------|------|
| Return ↔ OrderItem | 있다 | Return 쪽 `order_item_no` (unique) | 1:1. “어떤 주문 아이템을 반품하는지” 명확 | OrderItem 삭제 시 Return 고아 가능. Return 쪽 cascade는 OrderItem이 아님 | 반품 → 주문 아이템 조회 가능 |
| Review ↔ Product | 있다 | Review 쪽 `product_no` | Review N:1 Product | - | 상품별 리뷰 조회 가능 |
| Review 작성자(Customer) | 있다 | Review 쪽 `customer_id` | Review N:1 Customer | - | 고객별 리뷰 조회 가능 |

---

## F. 관리자/파트너 운영 (승인/정산/관리)

| 항목 | 있다/없다 | 비고 |
|------|-----------|------|
| PartnerApplication(입점신청) / 신청서 엔티티 | 없다 | 추가 필요 (SYS-010 구현 시 “신청서” 저장 테이블 필요. 승인 상태 PENDING/APPROVED/REJECTED 표현 가능하도록) |
| ProductRequest(상품 등록/수정 요청) | 없다 | 추가 필요 (PRT-001/PRT-005용 “요청” 엔티티. 승인 상태 표현 가능해야 함) |
| Settlement ↔ Partner | 있다 | Settlement 쪽 `partner_id`. N:1 Partner. 파트너별 정산 조회 가능, 기간 집계는 서비스 레이어에서 처리 |
| Settlement ↔ Admin | 있다 | Settlement 쪽 `admin_id`. Admin이 정산 처리 주체로 남음 |
| 승인/반려/정지 시 “처리자(admin)” 기록 | 일부 | PartnerEntity에 `admin` (승인한 관리자) 있음. 입점 신청서·상품 요청서에 “처리자(admin)” 필드 있으면 ADM 기능 구현 쉬움 → 현재는 Partner만 admin 보유, 신청서/요청서 엔티티 없음 |

---

## 관계 품질 요약 (필수 4개 체크)

### B. Product ↔ Partner
- FK: 없음 (Product에 partner 없음. Option에만 있음)
- 관계: 상품 단위 1:N은 없음. 옵션 단위로만 파트너 구분
- 삭제/조회: 상품 소유 파트너가 없어 “파트너별 상품”은 옵션 기준으로만 가능. 상품 1건 = 파트너 1명이 필요하면 추가 필요

### Order ↔ OrderItem
- FK: OrderItem 쪽 `order_no`
- 관계: 1:N
- 삭제: Order 삭제 시 OrderItem cascade + orphanRemoval → 고아 없음
- 조회: 주문별 아이템 조회 쉬움 (Order → orderItems)

### Order ↔ Payment
- FK: Order 쪽 `payment_no`
- 관계: 1:1
- 삭제: Order 삭제 시 Payment 쪽 cascade 미설정 → Payment 고아 가능 (정책 결정 후 cascade 옵션 검토)
- 조회: 주문 → 결제 조회 가능

### Return ↔ OrderItem
- FK: Return 쪽 `order_item_no`
- 관계: 1:1
- 삭제: OrderItem에 Return 쪽 cascade(ALL, orphanRemoval) 있음 → OrderItem 삭제 시 Return 함께 삭제
- 조회: 반품 → 주문 아이템 명확

---

## “추가 필요” 체크 리스트 (Day2 전 확인)

| 구분 | 항목 |
|------|------|
| 계정/권한 | Partner 로그인용 이메일·비밀번호 필드, Admin 로그인 식별자(이메일 등). PartnerStatus를 PartnerEntity에서 enum으로 사용 권장 |
| 상품 | “상품 1건 = 파트너 1명”이 요구사항이면 Product ↔ Partner 연결 추가 (또는 현재처럼 Option 단위 파트너만 유지하고 명세에 반영) |
| 장바구니 | Cart 엔티티 (Customer 연결), CartItem 엔티티 (Product/Option, 수량) |
| 운영 | PartnerApplication(입점 신청서) 엔티티, ProductRequest(상품 등록/수정 요청) 엔티티. 필요 시 “처리자(admin)” 필드 포함 |

---

*엔티티 코드는 수정하지 않았고, “있다/없다” 및 “추가 필요”만 기록함.*
