# QnA 개발 스토리 (바이브 코딩 대표 사례)

이 문서는 1:1 QnA 기능을 AI 보조로 기획·구현한 과정과 당시 스펙을 남긴다. 현재 동작의 정본은 [qna.md](qna.md)를 본다.

---

## 1. 왜 QnA를 대표 사례로 두었는가

- 멀티몰 구조(관리자 CS vs 파트너)와 FAQ·주문·반품 도메인이 한 기능에 모인다.
- 1단계 CRUD·라우팅을 먼저 고정하고, 2단계 AI draft를 붙이는 **단계적 확장**이 명확하다.
- ticket 등 레거시 용어 대신 QnA로 화면·API·DB를 통일했다.

---

## 2. 구현 단계

| 단계 | 범위 | 상태 |
|------|------|------|
| 1단계 | QnA·메시지 CRUD, `QnaRoutingService`, 고객·관리자·파트너 API·화면, FAQ 카테고리 통일 | 완료 |
| 2단계 | `draft-assist`(OpenAI), FAQ 참조, 일일·분당 한도, `qna_draft_log`, admin AI ops | 완료 |

의도적으로 미구현(1단계 스펙에서 제외):

- 채팅형 다회 메시지, `IN_PROGRESS`/`CLOSED`, 동일 QnA 재오픈
- `returnNo` 연동(스키마 nullable만 준비)
- AI 자동 reply·메일 발송

---

## 3. 1단계 설계 결정

### 3.1 대화 모델

```
고객 POST /api/qna  →  QnaMessage(CUSTOMER) 1건, PENDING
관리자/파트너 POST .../reply  →  QnaMessage(ADMIN|PARTNER) 1건, ANSWERED
```

- QnA당 답변 1회. 추가 질문 = 새 QnA.
- 본문은 `qna_message`에만 저장(`qna` 테이블에 body 없음).

### 3.2 FAQ와 카테고리 공유

| 코드 | 화면 라벨 | FAQ | QnA |
|------|-----------|-----|-----|
| ORDER_PAYMENT | 주문/결제 | ○ | ○ |
| DELIVERY | 배송 | ○ | ○ |
| RETURN_EXCHANGE | 취소/반품/교환 | ○ | ○ (+ scope) |
| MEMBER | 회원정보 | ○ | ○ |
| PRODUCT | 상품 | ○ | ○ |
| POINT | 포인트 | ○ | ○ |
| ETC | 기타 | ○ | ○ |

- 백엔드: `InquiryCategory.java`, FAQ `faq_category`는 동일 코드 문자열(`FaqCategoryMigration`).
- 프론트: `inquiryCategories.js`(admin·customer), 또는 `GET /api/inquiry-categories`.
- 쿠폰 카테고리 없음 — 포인트만 운영.

### 3.3 라우팅 (당시 스펙 메모)

고객이 파트너를 고르지 않는다. `orderItemNo`로 판매 파트너를 자동 지정한다.

| scope (RETURN_EXCHANGE) | 화면 문구 예 | 담당 |
|-------------------------|--------------|------|
| POLICY | 이용 방법·기간·정책 | 관리자 |
| ORDER_ITEM | 주문한 상품의 취소/반품/교환 건 | 파트너 |

안내 문구 예:

- 상품·배송: 「주문과 상품을 선택해 주세요. 선택한 판매자에게 답변드립니다.」
- 취소/반품/교환: 「이용 방법·기간은 고객센터, 주문 건은 상품 선택 후 해당 판매자」

---

## 4. 고객 UI·서버 검증 (구현 기준)

### 항상 입력

- category, title, body

### 유형별 UI

| category | 추가 UI |
|----------|---------|
| PRODUCT, DELIVERY | orderNo → orderItem 목록 → orderItemNo 필수 |
| RETURN_EXCHANGE | scope 라디오 → ORDER_ITEM이면 위와 동일 |
| ORDER_PAYMENT, POINT | orderNo 선택(권장) |
| MEMBER, ETC | 주문 필드 숨김 |

### 서버 검증

- 로그인 고객만 POST
- orderNo/orderItemNo 본인 주문 검증
- partnerNo는 요청 본문에 받지 않음

---

## 5. API 본문 예 (Postman)

배송:

```json
{
  "category": "DELIVERY",
  "title": "배송이 늦어요",
  "body": "언제 출고되나요?",
  "orderNo": 10023,
  "orderItemNo": 50001
}
```

반품 정책:

```json
{
  "category": "RETURN_EXCHANGE",
  "scope": "POLICY",
  "title": "반품 기간 문의",
  "body": "구매 확정 후 며칠 이내 반품 가능한가요?"
}
```

reply:

```json
{ "body": "안녕하세요. 확인 결과 ..." }
```

---

## 6. 응답 DTO (요지)

`QnaResponseDto`: qnaNo, category, scope, status, title, orderNo, orderItemNo, partnerNo, partnerName, createdAt, answeredAt, messages[](authorType, body, createdAt, 표시명). 목록에 lastMessagePreview, productName(optional).

`QnaDraftAssistResponseDto`: draftBody, referencedFaqs[](faqNo, question), confidence.

---

## 7. 2단계 AI draft-assist

| 항목 | 내용 |
|------|------|
| API | POST /api/admin/qna/{qnaNo}/draft-assist, POST /api/partner/qna/{qnaNo}/draft-assist |
| 맥락 | QnA 본문, 메타, 주문·배송·반품 요약(허용 시), FAQ Top N |
| 설정 | ai.qna-draft.*, ai.openai.* |
| 로그 | qna_draft_log (V6), AdminAiOpsService 연동 |
| 한도 | AiDailyLimitService + ApiRateLimitInterceptor |
| 폴백 | buildFallback — FAQ 참조만 |

리뷰 분석 API는 파트너·리뷰 전용으로 QnA와 분리 유지.

---

## 8. 프론트 연동

| 앱 | 경로 |
|----|------|
| customer-frontend | `/qna` — QnaPage.jsx, lib/api/qna.js |
| admin-frontend | qna-management — reply + AI 초안 버튼 |
| partner | partner/qna — 동일 |

FAQ `/FAQs`에서 QnA 등록 링크. `/contact`는 별도 템플릿(1:1 QnA API 미연동).

---

## 9. 시드

`app.data.init.enabled=true` 이고 qna 비어 있을 때 `DataInitializer.createQnaSeeds()`.

| 건 | 담당 | status | 비고 |
|----|------|--------|------|
| 1 | 관리자 | PENDING | MEMBER |
| 1 | 파트너1 | PENDING | DELIVERY + orderItem |
| 1 | 파트너1 | ANSWERED | PRODUCT + 파트너 reply |

고객: testCustomer1@example.com 우선. 주문 상품 없으면 파트너 건 생략.

상세: [reference/SEED_DEMO_SPEC.md](../../reference/SEED_DEMO_SPEC.md) §6.

---

## 10. Postman 호출 순서

1. 고객 login → POST /api/qna
2. GET /api/qna, GET /api/qna/{qnaNo}
3. 관리자 login → GET /api/admin/qna → 플랫폼 건 POST .../reply
4. 파트너 login → GET /api/partner/qna → POST .../reply
5. POST .../draft-assist (OpenAI 키·일일 한도 확인)

URL 목록: [testing/POSTMAN_CURRENT_REFERENCE.md](../../testing/POSTMAN_CURRENT_REFERENCE.md) § QnA.

---

## 11. 완료 체크리스트 (기록)

### 1단계

- [x] Flyway V5 (qna, qna_message)
- [x] 고객 등록·목록·상세 + QnaRoutingServiceTest
- [x] 관리자·파트너 목록·reply
- [x] customer / admin / partner UI
- [x] InquiryCategory + inquiryCategories.js + GET /api/inquiry-categories
- [x] FAQ 카테고리 enum 통일 + FaqCategoryMigration
- [x] docs/README.md 링크
- [x] QnA 시드
- [x] POSTMAN_CURRENT_REFERENCE QnA 절

### 2단계

- [x] draft-assist API
- [x] qna_draft_log (V6)
- [x] 일일 한도·rate limit·AI ops

---

## 12. 코드 위치 (전체)

| 레이어 | 경로 |
|--------|------|
| 마이그레이션 | V5__add_qna_tables.sql, V6__add_qna_draft_log.sql |
| qna | entity, repository, service, controller |
| ai/qnadraft | QnaDraftAssistService, OpenAiQnaDraftClient, QnaDraftLogEntity |
| 프론트 | customer web/app/(other-pages)/qna, admin qna-management, partner qna |
| 시드 | DataInitializer.createQnaSeeds() |
