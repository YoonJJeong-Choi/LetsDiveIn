# Postman 현행 참조 (URL 스냅샷)

역할: 이 저장소의 `*Controller`에 선언된 베이스 경로와, Postman에서 자주 쓰는 핵심 전체 URL만 정리한다. 긴 JSON·단계별 시나리오·과거 절차는 [POSTMAN_LEGACY_REFERENCE.md](POSTMAN_LEGACY_REFERENCE.md)에 두고, 최소 흐름·도메인 링크는 [POSTMAN_TEST_GUIDE.md](POSTMAN_TEST_GUIDE.md)를 본다.

갱신: 컨트롤러 매핑이 바뀌면 이 파일의 표를 수동으로 맞춘다(또는 추출 스크립트를 도입한다).

---

## 1. 컨트롤러별 베이스 경로 (`@RequestMapping`)

| 컨트롤러(요약) | 베이스 경로 |
|----------------|-------------|
| `AuthController` | `/api/auth` |
| `OrderController` | `/api/orders` |
| `OrderPriceLockController` | `/api/orders/price-lock` |
| `OrderItemController` | `/api/order-items` |
| `PaymentController` | `/api/payments` |
| `DeliveryController` | `/api/deliveries` |
| `ReturnController` | `/api/returns` |
| `CartController` | `/api/cart` |
| `ProductController` | `/api/product` |
| `ProductAdminController` | `/api/admin/products` |
| `PartnerController` | `/api/partner` |
| `AdminController` | `/api/admin` |
| `CustomerController` | `/api/customer` |
| `PointController` | `/api` |
| `SalePolicyAdminController` | `/api` |
| `SettlementController` | `/api/partner/settlement` |
| `FileController` | `/api/files` |
| `BrandController` | `/api/brands` |
| `ColorController` | `/api/colors` |
| `ColorAdminController` | `/api/admin/colors` |
| `SizeController` | `/api/sizes` |
| `SizeAdminController` | `/api/admin/sizes` |
| `InventoryController` | `/api/partner/inventory` |
| `FaqController` | `/api/faq` |
| `EventController` | `/api/events` |
| `EventAdminController` | `/api/admin/events` |
| `PartnerEventController` | `/api/partner/events` |
| `AdminEventPerformanceController` | `/api/admin/events` |
| `PartnerEventPerformanceController` | `/api/partner/events` |
| `ReviewController` | `/api/reviews` |
| `ReviewAnalysisController` | `/api/ai/review-analysis` |
| `QnaController` | `/api/qna` |
| `AdminQnaController` | `/api/admin/qna` |
| `PartnerQnaController` | `/api/partner/qna` |
| `InquiryCategoryController` | `/api/inquiry-categories` |

`PointController`·`SalePolicyAdminController`는 베이스가 `/api`이므로, 실제 URL은 메서드의 `/customer/...`, `/admin/...`, `/partner/...`, `/sales/...` 등과 합쳐서 읽는다.

---

## 2. 자주 쓰는 전체 URL (복붙 빈도 높음)

### 인증

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

### 주문·결제 (고객)

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{orderNo}`
- `GET /api/orders/{orderNo}/payment`
- `GET /api/orders/{orderNo}/deliveries`
- `PATCH /api/orders/{orderNo}/cancel`
- `POST /api/orders/order-items/{orderItemNo}/complete`
- `POST /api/orders/order-items/{orderItemNo}/confirm` (발주 확인)
- `POST /api/orders/{orderNo}/confirm` (deprecated 전체 발주 확인)

### 결제

- `POST /api/payments` (스켈레톤/준비)
- `POST /api/payments/confirm`
- `POST /api/payments/webhook`

### 배송

- `GET /api/deliveries`
- `GET /api/deliveries/{deliveryNo}`
- `PATCH /api/deliveries/{deliveryNo}/start`
- `PATCH /api/deliveries/{deliveryNo}/complete`
- `PATCH /api/deliveries/{deliveryNo}` (송장·택배사 수정)

### 반품

- `POST /api/returns`
- `GET /api/returns/customer`
- `GET /api/returns`
- `GET /api/returns/{returnNo}`
- `POST /api/returns/{returnNo}/approve`
- `POST /api/returns/{returnNo}/reject`
- `PATCH /api/returns/{returnNo}`
- `GET /api/returns/{returnNo}/history`
- `GET /api/returns/{returnNo}/return-assist` (하위 호환: `review-assist`, `ai-assist`)

### 장바구니

- `GET /api/cart`
- `POST /api/cart/items`
- `PUT /api/cart/items/{cartItemNo}`
- `DELETE /api/cart/items/{cartItemNo}`

### 주문 상품 단건

- `GET /api/order-items/{orderItemNo}/delivery`

### 파트너 상품(일부)

- `GET /api/partner/products`
- `POST /api/partner/products`
- `PUT /api/partner/products/{productNo}`

### FAQ

- `GET /api/faq` (공개, `?category=` 선택)
- `GET /api/faq/{faqNo}` (공개)
- `POST /api/faq` (관리자 세션)
- `PUT /api/faq/{faqNo}` (관리자 세션)
- `DELETE /api/faq/{faqNo}` (관리자 세션)

시드: [reference/SEED_DEMO_SPEC.md](../reference/SEED_DEMO_SPEC.md) §6.

### QnA (1:1 문의)

- `GET /api/inquiry-categories`
- `POST /api/qna` (고객 세션)
- `GET /api/qna`, `GET /api/qna/{qnaNo}`
- `GET /api/admin/qna`, `GET /api/admin/qna/{qnaNo}`
- `POST /api/admin/qna/{qnaNo}/reply` (플랫폼 건, `partnerNo == null` 만)
- `POST /api/admin/qna/{qnaNo}/draft-assist`
- `GET /api/partner/qna`, `GET /api/partner/qna/{qnaNo}`
- `POST /api/partner/qna/{qnaNo}/reply`
- `POST /api/partner/qna/{qnaNo}/draft-assist`

도메인·라우팅·시드: [domains/qna/qna.md](../domains/qna/qna.md).

### 리뷰 AI 분석 (파트너)

- `GET /api/reviews/partner/ai-candidates?productNo=…`
- `POST /api/ai/review-analysis/partner`

도메인·한도·fallback: [domains/review/review-analysis.md](../domains/review/review-analysis.md). 상세 요청 예: [POSTMAN_LEGACY_REFERENCE.md](POSTMAN_LEGACY_REFERENCE.md) § [AI] 리뷰 분석.

---

## 3. 관리자 `/api/admin/*` 등

`AdminController` 등에 다수의 하위 매핑이 있다. 전체 표를 이 파일에 모두 넣지 않는다. 필요 시 소스 `AdminController.java`를 보거나, 긴 요청 예는 [POSTMAN_LEGACY_REFERENCE.md](POSTMAN_LEGACY_REFERENCE.md)의 해당 절을 쓴다.

---

## 4. 포인트·세일 등 (`/api` 베이스)

`PointController` 예:

- `GET /api/customer/points/balance`
- `GET /api/customer/points/history`
- `POST /api/admin/customers/{customerId}/points/add`
- `POST /api/admin/customers/{customerId}/points/deduct`
- `GET /api/admin/customers/{customerId}/points/history`

`SalePolicyAdminController` 예:

- `GET /api/sales/applicable`
- `GET /api/admin/sales`, `GET /api/admin/sales/{id}`
- `PATCH /api/admin/sales/{id}/approve` 등 (전체는 소스 참고)

---

## 5. 정본 문서

- [domains/payment/payment.md](../domains/payment/payment.md)
- [domains/delivery/delivery.md](../domains/delivery/delivery.md)
- [domains/qna/qna.md](../domains/qna/qna.md)
- [domains/review/review-analysis.md](../domains/review/review-analysis.md)
- [architecture/status/status-model.md](../architecture/status/status-model.md)
