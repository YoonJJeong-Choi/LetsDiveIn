# QnA (1:1 문의) 도메인

고객 1:1 문의와 관리자·파트너 답변이다. FAQ(사전 Q&A)와 테이블·API가 분리되어 있다.

개발 과정·단계별 스펙·체크리스트: [qna-development.md](qna-development.md)

---

## 1. 상태·대화 구조

`QnaStatus`(`QnaEntity`): `PENDING` → `ANSWERED` 두 값만 사용한다. `IN_PROGRESS`·`CLOSED`·동일 QnA 재오픈·추가 메시지는 현재 코드에 없다.

| 값 | 의미 |
|----|------|
| PENDING | 고객 등록, 공식 답변 없음 |
| ANSWERED | 관리자 또는 파트너가 reply 1회 등록 |

QnA 1건 = 고객 메시지 1개 + 담당자 답변 1개(`qna_message` 행 2개). 재문의는 새 `POST /api/qna`로 새 QnA를 등록한다. `ANSWERED` 이후 같은 `qnaNo`에 reply 재시도 시 `409`(`QNA_ALREADY_ANSWERED`).

---

## 2. 카테고리·담당 라우팅

문의 카테고리는 FAQ와 공통 `InquiryCategory` enum 코드를 쓴다. 목록: `GET /api/inquiry-categories`. 프론트는 `inquiryCategories.js`와 동일 라벨.

`RETURN_EXCHANGE`만 QnA 전용 `QnaScope`가 있다: `POLICY`(정책·절차 → 관리자) / `ORDER_ITEM`(특정 주문 건 → 파트너, `orderItemNo` 필수).

등록 시 서버가 `partnerNo`를 계산한다. 고객·요청 본문에서 파트너를 받지 않는다.

| 코드 | 담당 | 주문 상품 |
|------|------|-----------|
| ORDER_PAYMENT, MEMBER, POINT, ETC | 관리자 | — (POINT·ORDER_PAYMENT는 orderNo 선택 가능) |
| PRODUCT, DELIVERY | 파트너 | 필수 |
| RETURN_EXCHANGE + POLICY | 관리자 | 불필요 |
| RETURN_EXCHANGE + ORDER_ITEM | 파트너 | 필수 |

파트너 지정: `QnaRoutingService.resolvePartnerId` — `OrderItem`의 옵션 파트너 우선, 없으면 상품 파트너, 없으면 `partnerNo` null(관리자). 배송·반품과 동일한 판매자 해석. 한 주문에 파트너가 여러 명이면 `orderItemNo` 단위로 갈린다.

---

## 3. 데이터 모델

- `qna` — 메타: `customer_id`, `category`, `scope`(nullable), `status`, `title`, `order_no`, `order_item_no`, `partner_no`(서버 계산), `return_no`(nullable, 미사용), `answered_at` 등. 본문 컬럼 없음.
- `qna_message` — `author_type`(CUSTOMER|ADMIN|PARTNER), `body`, 작성자 FK(admin/partner nullable).

---

## 4. REST API

### 고객 — `/api/qna` (`QnaController`)

| 메서드 | 경로 | 역할 |
|--------|------|------|
| POST | /api/qna | 등록 → PENDING + 고객 메시지 |
| GET | /api/qna | 내 목록 (`status`, `category` optional) |
| GET | /api/qna/{qnaNo} | 상세 + 메시지 (본인만) |

로그인 필수. `PRODUCT`/`DELIVERY`/`RETURN_EXCHANGE`+`ORDER_ITEM`에서 `orderItemNo` 없으면 `400`. 연결 주문·상품은 본인 소유 검증.

### 관리자 — `/api/admin/qna` (`AdminQnaController`)

| 메서드 | 경로 | 역할 |
|--------|------|------|
| GET | /api/admin/qna | 목록 (`all=true` 시 파트너 건 포함·조회만) |
| GET | /api/admin/qna/{qnaNo} | 상세 |
| POST | /api/admin/qna/{qnaNo}/reply | `partnerNo == null` 건만 → ANSWERED |
| POST | /api/admin/qna/{qnaNo}/draft-assist | AI 답변 초안 (자동 reply 없음) |

파트너 건(`partnerNo != null`)에 관리자 reply → `403`.

### 파트너 — `/api/partner/qna` (`PartnerQnaController`)

| 메서드 | 경로 | 역할 |
|--------|------|------|
| GET | /api/partner/qna | 세션 파트너 건만 |
| GET | /api/partner/qna/{qnaNo} | 상세 (본인 건만) |
| POST | /api/partner/qna/{qnaNo}/reply | 답변 → ANSWERED |
| POST | /api/partner/qna/{qnaNo}/draft-assist | AI 답변 초안 |

권한: QnA의 `partnerNo`와 `OrderItem` 소유 파트너 일치 (`DeliveryService`·`ReturnService`와 같은 패턴).

reply 본문: `{ "body": "..." }`.

---

## 5. 권한·오류

| 상황 | 코드 |
|------|------|
| 비로그인 고객 등록 | 401 |
| 타인 QnA 조회 | 403 |
| 파트너가 타 파트너 QnA | 403 |
| 관리자가 파트너 QnA에 reply | 403 |
| ANSWERED QnA에 reply | 409 |
| 없는 qnaNo | 404 |

---

## 6. AI 답변 초안 (`draft-assist`)

`QnaDraftAssistService`: QnA 본문·메타·(허용 시) 주문·배송·반품 요약·FAQ Top N으로 OpenAI 초안 생성. 응답 `draftBody`, `referencedFaqs[]`, `confidence`. 실패·비활성 시 FAQ 참조만 폴백.

- 설정: `ai.qna-draft.enabled`, `daily-limit-admin`(30), `daily-limit-partner`(20)
- 일일 한도 초과: 429 `AI_DAILY_LIMIT_EXCEEDED`
- 분당 한도: [security/RATE_LIMIT_OPERATION_GUIDE.md](../../security/RATE_LIMIT_OPERATION_GUIDE.md)
- 운영: `/api/admin/ai/overview`, `qna_draft_log`

리뷰 분석(`POST /api/ai/review-analysis/partner`)은 QnA와 별도 API다.

---

## 7. 연관 도메인

| 도메인 | 연계 |
|--------|------|
| [주문/OrderItem](../../architecture/status/status-model.md) | orderNo, orderItemNo, 파트너 해석 |
| [배송](../delivery/delivery.md) | DELIVERY QnA |
| [반품](../../architecture/status/status-model.md) | RETURN_EXCHANGE + ORDER_ITEM |
| FAQ `/api/faq` | 정책 안내, AI FAQ 참조 |
| [포인트](../points/points.md) | POINT 카테고리 |

---

## 8. 구현 위치 참고

- 컨트롤러: `qna/controller/QnaController`, `AdminQnaController`, `PartnerQnaController`, `InquiryCategoryController`
- 서비스: `qna/service/QnaService`, `QnaRoutingService`, `ai/qnadraft/QnaDraftAssistService`
- 엔티티: `qna/entity/QnaEntity`, `QnaMessageEntity`
- 테스트: `QnaRoutingServiceTest`
