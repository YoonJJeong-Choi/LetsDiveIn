## 최종 결제/웹훅 시나리오 (표준화 완료)

### 환경 변수
- payments.toss.secret-key: 백엔드 서버 환경에 설정 필수(기본키 제거됨)
- Postman 환경: `TOSS_SECRET_KEY` (서명 생성용) 값은 서버와 동일

### 공통 헤더
- Content-Type: application/json
- Toss-Signature: 비워둠 (Pre-request Script가 자동 주입)

### Pre-request Script (서명)
```javascript
const raw = (pm.request.body && pm.request.body.mode === 'raw') ? pm.request.body.raw : '';
const resolved = pm.variables.replaceIn(raw);
const secret = pm.environment.get('TOSS_SECRET_KEY') || '';
const sig = CryptoJS.HmacSHA256(resolved, secret).toString(CryptoJS.enc.Hex);
pm.request.headers.upsert({ key: 'Toss-Signature', value: sig });
console.log('[webhook-sign]', { len: resolved.length, sig, hasSecret: !!secret });
```

### 1) 승인 웹훅
- URL: POST /api/payments/webhook
- Body(raw, 한 줄 권장):
  {"eventId":"evt_1","type":"PAYMENT_APPROVED","data":{"orderId":"ORD-54-1234567890","paymentKey":"tgen_abc","amount":15000}}
- 기대 결과:
  - 200 OK
  - orders.order_no=54 → order_status=PAID
  - payment(order_no=54) → status=PAID, paymentKey/pgOrderId/amount/rawApprovePayload 저장
  - payment_webhook_log(eventId=evt_1) → processed=true
  - 동일 본문 재전송 → 200 OK, 상태/로그 변화 없음(멱등)

### 2) 취소 웹훅
- URL: POST /api/payments/webhook
- Body:
  {"eventId":"evt_2","type":"PAYMENT_CANCELED","data":{"orderId":"ORD-54-1234567890","paymentKey":"tgen_abc"}}
- 기대 결과:
  - 200 OK
  - payment(order_no=54) → status=CANCELED, rawApprovePayload 저장
  - 주문은 정책상 취소 시도(불가 상태면 유지)
  - 동일 본문 재전송 → 200 OK, 상태/로그 변화 없음(멱등)

### 유효성/보안
- HMAC 서명 검증 필수: 서버 환경에 toss.secret-key가 없으면 401
- eventId 필수: 누락 시 400 (임시 fallback 제거)

### 디버깅
- Postman Console에서 `[webhook-sign]` 로그 확인(hasSecret=true, len>0, 64자 hex 서명)
- 401이면 키/본문 일치 여부 점검 (환경 선택, 변환 옵션 OFF)

# 포스트맨 테스트 가이드

## 결제/주문 가이드 – 업데이트(변경됨)

이 섹션은 기존 결제/주문 가이드를 보완하는 업데이트입니다. 기존 문서는 유지하되, 결제 승인과 테스트 방법은 아래 절차를 우선 적용하세요. 과거 “임시 주문-결제 로직”은 사용 중지 권장(Deprecated)으로 표기합니다.

### 변경 요약
- 결제 승인은 이제 `POST /api/payments/confirm` 로 처리합니다. 성공 시 주문 상태가 `PENDING_PAYMENT → PAID` 로 전이됩니다.
- 금액 불일치 차단이 적용되었습니다. `amount` 가 주문 금액과 다르면 실패 처리됩니다.
- 프론트는 v2 결제위젯 기준으로 동작합니다.
- 중요: 이전 “임시 주문-결제 로직(가짜 승인, v1 결제창, 더미 카드 입력 등)”은 권장되지 않습니다. 아래 “임시 로직(변경됨)” 섹션에 참고용으로만 남겨둡니다.

### Postman 환경 변수(권장)
- `baseUrl`: `http://localhost:8080`
- `orderNo`: 주문 생성 응답값
- `orderTotalPrice`: 주문 생성 응답값
- `paymentKey`: 결제 성공 리다이렉트에서 받은 값
- `timestamp`: `{{$timestamp}}`
- `orderId`: `ORD-{{orderNo}}-{{timestamp}}`

### 1) 고객 로그인(세션 유지)
- POST `{{baseUrl}}/api/auth/login`
- Body(JSON)
```
{ "email": "user@example.com", "password": "******" }
```
- Postman에서 쿠키 자동저장 켜기(세션 유지)

### 2) 주문 생성(변경 없음)
- POST `{{baseUrl}}/api/orders`
- Body(JSON)
```
{
  "cartItemNos": [42],
  "recipientName": "홍길동",
  "recipientPhone": "01012345678",
  "deliveryAddress": "서울시 OO구",
  "deliveryAddressDetail": "101동 1001호",
  "deliveryZipCode": "01234",
  "paymentMethod": "CARD",
  "orderMemo": null,
  "usePointAmount": null
}
```
- 기대: `success=true`, `data.orderNo`, `data.orderTotalPrice` → 환경 변수에 저장

### 3) (선택) 결제 사전 생성 로그
- POST `{{baseUrl}}/api/payments`
- Body(JSON)
```
{ "orderId": "ORD-{{orderNo}}-{{timestamp}}", "amount": {{orderTotalPrice}}, "orderNo": {{orderNo}} }
```
- 참고: 필수 아님(로깅/검증용)

### 4) 결제 승인(confirm) – 정상 시나리오
- POST `{{baseUrl}}/api/payments/confirm`
- Body(JSON)
```
{
  "paymentKey": "{{paymentKey}}",
  "orderId": "ORD-{{orderNo}}-{{timestamp}}",
  "amount": {{orderTotalPrice}}
}
```
- 기대: `success=true`, `data.approved=true`, `data.orderNo`
- 주문 상태가 `PAID` 로 전이되었는지 확인

### 5) 결제 승인(confirm) – 금액 불일치(음성) 시나리오
- POST `{{baseUrl}}/api/payments/confirm`
- Body(JSON)
```
{
  "paymentKey": "{{paymentKey}}",
  "orderId": "ORD-{{orderNo}}-{{timestamp}}",
  "amount": 999999
}
```
- 기대: 실패(`success=false`, `code=INVALID_REQUEST`, message에 “금액 불일치”), 주문 상태 변화 없음

### 6) 멱등 재시도 확인(선택)
- 4)의 승인 요청을 동일 파라미터로 재호출
- 기대: 중복 INSERT 없이 정상 응답(상태는 `PAID` 유지), 장바구니 중복 삭제 없음

### 컬렉션 권장 구성
- Auth
  - POST `/api/auth/login`
- Orders
  - POST `/api/orders`
  - GET `/api/orders`
- Payments
  - POST `/api/payments` (선택)
  - POST `/api/payments/confirm` (정상)
  - POST `/api/payments/confirm` (amount mismatch)

---

## [AI] 리뷰 분석(파트너)

### 엔드포인트
```
POST http://localhost:8080/api/ai/review-analysis/partner
```

### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (파트너 로그인 후 받은 세션 ID)
```

### 요청 예시
```json
{
  "partnerId": "partner-1001",
  "fromAt": "2026-04-01T00:00:00",
  "toAt": "2026-04-08T23:59:59",
  "productNos": [101, 102],
  "maxReviews": 50,
  "reviews": [
    {
      "reviewNo": 1,
      "productNo": 101,
      "optionNo": 10001,
      "rating": 3,
      "content": "사이즈가 조금 작아요",
      "createdAt": "2026-04-07T10:30:00"
    }
  ]
}
```

### 성공 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "summary": "사이즈 관련 불만이 일부 있으나 전반적 만족도가 보통 이상입니다.",
    "sentiment": {
      "positiveRatio": 0.45,
      "negativeRatio": 0.30,
      "neutralRatio": 0.25
    },
    "issues": [
      {
        "type": "사이즈편차",
        "frequency": 3,
        "severity": "MEDIUM",
        "evidenceReviewNos": [1, 2]
      }
    ],
    "actions": [
      {
        "area": "SIZE_GUIDE",
        "recommendation": "사이즈 가이드 표를 보강하세요.",
        "expectedImpact": "HIGH"
      }
    ],
    "topKeywords": [
      {
        "keyword": "사이즈",
        "weight": 0.8
      }
    ],
    "representativeQuotes": [
      {
        "reviewNo": 1,
        "quote": "사이즈가 조금 작아요"
      }
    ],
    "stats": {
      "count": 10,
      "avgRating": 3.8,
      "ratingDist1": 1,
      "ratingDist2": 1,
      "ratingDist3": 2,
      "ratingDist4": 3,
      "ratingDist5": 3
    },
    "alerts": {
      "pii": false,
      "spam": false,
      "policyViolation": false,
      "policyTypes": []
    },
    "confidence": "MEDIUM"
  },
  "errors": null
}
```

### fallback 응답 예시 (GPT 장애/쿼터 부족/파싱 실패)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "summary": "리뷰 분석 일시 불가",
    "sentiment": {
      "positiveRatio": 0.0,
      "negativeRatio": 0.0,
      "neutralRatio": 1.0
    },
    "issues": [],
    "actions": [
      {
        "area": "FAQ",
        "recommendation": "잠시 후 다시 시도해주세요.",
        "expectedImpact": "LOW"
      }
    ],
    "topKeywords": [],
    "representativeQuotes": [],
    "stats": {
      "count": 0,
      "avgRating": 0.0,
      "ratingDist1": 0,
      "ratingDist2": 0,
      "ratingDist3": 0,
      "ratingDist4": 0,
      "ratingDist5": 0
    },
    "alerts": {
      "pii": false,
      "spam": false,
      "policyViolation": false,
      "policyTypes": ["AI_UNAVAILABLE"]
    },
    "confidence": "LOW"
  },
  "errors": null
}
```

### 테스트 시나리오 3개
1. 정상: 유효 API 키 + 정상 입력으로 요청 시 구조화된 분석 응답 반환
2. 입력 이상: `null`/빈 리스트 포함 요청 시 서버 오류 없이 응답 반환(내부 null-safe 처리)
3. GPT 장애: API 키 오입력/쿼터 초과 시 fallback 응답 반환(`summary=리뷰 분석 일시 불가`)

## [AI] 반품 보조(관리자 전용) 전환 가이드

### 변경 핵심
- 고객 화면의 반품 AI 예측 기능은 운영 대상에서 제거/비활성화되었습니다.
- 반품 AI는 관리자 반품 상세에서만 사용하는 보조 기능으로 전환되었습니다.
- 관리자 보조는 GPT 호출 없이 규칙 기반(`fraud + evidence + checklist`)으로 동작합니다.

### 권한/보안 정책
- 관리자 보조 엔드포인트: `GET /api/returns/{returnNo}/ai-assist`
  - 권한: `ADMIN`만 허용
  - `CUSTOMER`, `PARTNER`는 `FORBIDDEN`
- 기존 고객측 리스크 점수 API: `POST /api/ai/return-risk/score`
  - 현재 권한: `ADMIN`만 허용 (고객 경로로 사용 금지)

### Feature Flag
- 설정 키: `AI_FEATURE_ENABLED`
- 기본값: `true` (`@Value("${AI_FEATURE_ENABLED:true}")`)
- 동작:
  - `true`: 규칙 기반 보조 분석 응답 반환
  - `false`: fallback 응답 반환 (`featureEnabled=false`, `riskFactors=["AI_FEATURE_DISABLED"]`)

설정 예시 (`application.properties`):
```properties
AI_FEATURE_ENABLED=false
```

### 응답 스키마 (관리자 AI 보조)
`ApiResponse<ReturnAiAssistResponseDto>` 기준 `data`:

```json
{
  "featureEnabled": true,
  "fraudScore": 40,
  "riskLevel": "MEDIUM",
  "riskFactors": ["RECENT_RETURN_FREQUENCY", "SAME_ADDRESS_REPEAT_HIGH"],
  "evidenceTags": ["IMAGE_ATTACHED", "HAS_REASON_TEXT"],
  "evidenceGaps": ["ADDITIONAL_IMAGE_RECOMMENDED"],
  "recommendedActions": [
    "주문/배송/반품 이력을 수동으로 재확인하세요.",
    "중위험 건으로 분류되어 증빙과 사유 일치 여부를 우선 확인하세요."
  ]
}
```

### 샘플 요청/응답 (관리자 전용)
요청:
```http
GET /api/returns/1001/ai-assist
Cookie: JSESSIONID=...
```

성공 응답 예시:
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "featureEnabled": true,
    "fraudScore": 40,
    "riskLevel": "MEDIUM",
    "riskFactors": ["RECENT_RETURN_FREQUENCY", "SAME_ADDRESS_REPEAT_HIGH"],
    "evidenceTags": ["IMAGE_ATTACHED", "HAS_REASON_TEXT"],
    "evidenceGaps": ["ADDITIONAL_IMAGE_RECOMMENDED"],
    "recommendedActions": [
      "주문/배송/반품 이력을 수동으로 재확인하세요.",
      "중위험 건으로 분류되어 증빙과 사유 일치 여부를 우선 확인하세요."
    ]
  },
  "errors": null
}
```

feature off 응답 예시:
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "featureEnabled": false,
    "fraudScore": 0,
    "riskLevel": "LOW",
    "riskFactors": ["AI_FEATURE_DISABLED"],
    "evidenceTags": [],
    "evidenceGaps": [],
    "recommendedActions": [
      "AI 기능이 비활성화되어 기존 관리자 검토 절차를 진행하세요."
    ]
  },
  "errors": null
}
```

### 테스트 가이드 (최신)
자동 테스트(권장):
```bash
cd backend
.\gradlew.bat test --tests "com.swimshop.swim_mall.return_order.service.ReturnServiceAiAssistTest" --tests "com.swimshop.swim_mall.return_order.service.ReturnFraudServiceTest" --tests "com.swimshop.swim_mall.return_order.service.ReturnImageAndChecklistTest"
```

필수 검증 포인트:
1. customer 접근 차단 (`FORBIDDEN`)
2. admin 접근 허용
3. rule 기반 fraudScore 계산
4. evidence 태그/갭 매핑
5. 권장 조치에 신뢰도 문구 미포함(신뢰도 제거 반영)
6. `AI_FEATURE_ENABLED` on/off 분기

수동 테스트:
1. `AI_FEATURE_ENABLED=true`로 관리자 반품 상세 진입 → AI 보조 패널 정상 표시
2. `AI_FEATURE_ENABLED=false`로 재시작 후 동일 화면 진입 → fallback 문구 표시
3. 고객 세션으로 `GET /api/returns/{returnNo}/ai-assist` 호출 시 `FORBIDDEN` 확인

---

## 임시 주문-결제 로직(변경됨, 참고용)

- 상태: 사용 중지 권장(Deprecated). 신규 테스트는 상단 “업데이트” 섹션을 따르세요.
- 변경 내용
  - 임시 가짜 승인 API(`/api/payments/approve`)는 제거되었습니다.
  - 프론트의 더미 결제 UI(v1 결제창/가짜 카드 입력 등)는 제거되었습니다.
- 남겨두는 이유
  - 과거 테스트 기록/문서 추적용. 실 운영/검증에는 사용하지 않습니다.

## 관리자 이벤트 실적 조회 (스냅샷/보상 기반)

관리자 권한으로 특정 이벤트의 종료 실적(구매확정 시점 기준)을 조회합니다.  
포인트 보상 발생 기록을 근거로 귀속하며, 이벤트 수정의 영향을 받지 않도록 스냅샷/보상 테이블을 사용합니다.

- 메서드: GET  
- URL: `/api/admin/events/{eventNo}/performance`  
- 권한: ADMIN 로그인 세션 필요  
- 쿼리 파라미터(옵션)
  - `from`: ISO_DATETIME, 예) `2026-03-01T00:00:00`
  - `to`: ISO_DATETIME, 예) `2026-03-31T23:59:59`
  - 미지정 시 전체 기간 집계

### 관리자: 종료 이벤트 실적 목록
- 메서드: GET  
- URL: `/api/admin/events/performance/list`  
- 권한: ADMIN  
- 쿼리 파라미터(옵션): `from`, `to`  
- 설명: 종료(ENDED) 이벤트들의 실적을 목록으로 조회

### 관리자: 일자별 추이
- 메서드: GET  
- URL: `/api/admin/events/performance/timeseries`  
- 권한: ADMIN  
- 쿼리 파라미터(옵션): `eventNo`, `from`, `to`  
- 설명: 일자별 주문/매출 추이. `eventNo` 미지정 시 전체 종료 이벤트 합산

### 관리자: Top N
- 메서드: GET  
- URL: `/api/admin/events/performance/top`  
- 권한: ADMIN  
- 쿼리 파라미터: `type`=partner|product, `limit`(옵션), `eventNo`(옵션), `from`(옵션), `to`(옵션)  
- 설명: 파트너/상품 기준 Top N (매출 기준)

집계 기준
- 포인트 성과(추가 적립):  
  - 관리자 단독 이벤트 보상: `admin_event_reward` 합계  
  - 파트너 참여 이벤트 보상: `partner_event_reward` 합계  
  - 기간 필터는 `order_item.completedAt` 기준
- 주문/매출 지표: 보상이 발생한 `order_item_no` 집합을 기준으로  
  - `totalOrderItems`: 아이템 수  
  - `totalOrders`: 고유 주문 수  
  - `totalNetAmount`: `order_item.itemTotalPrice` 합

예시 요청 1) 기간 미지정 전체

```bash
curl -X GET \
  -H "Cookie: JSESSIONID=..." \
  "http://localhost:8080/api/admin/events/123/performance"
```

예시 요청 2) 기간 지정

```bash
curl -X GET \
  -H "Cookie: JSESSIONID=..." \
  "http://localhost:8080/api/admin/events/123/performance?from=2026-03-01T00:00:00&to=2026-03-31T23:59:59"
```

예시 응답

```json
{
  "success": true,
  "data": {
    "eventNo": 123,
    "fromAt": "2026-03-01T00:00:00",
    "toAt": "2026-03-31T23:59:59",
    "totalOrders": 18,
    "totalOrderItems": 24,
    "totalNetAmount": 1523000,
    "adminRewardPoint": 42000,
    "partnerRewardPoint": 17000,
    "sampleOrderItemNos": [101, 102, 115]
  }
}
```

Postman 사용 팁
- Authorization: 세션 쿠키 기반이라면 미리 관리자 로그인 요청 후 Cookie를 자동 전파되게 설정
- Params 탭에 `from`, `to`를 ISO_DATETIME 형식으로 입력
- Tests 탭에서 핵심 필드 검증 예시:

```javascript
pm.test("응답 스키마 체크", function () {
  pm.expect(pm.response.json().data).to.have.keys([
    "eventNo","fromAt","toAt","totalOrders","totalOrderItems",
    "totalNetAmount","adminRewardPoint","partnerRewardPoint","rewardToNetRatio","sampleOrderItemNos"
  ]);
});
```

## 파트너 이벤트 실적 조회

### 파트너: 단일 이벤트 실적
- 메서드: GET  
- URL: `/api/partner/events/{eventNo}/performance`  
- 권한: PARTNER 로그인 세션 필요  
- 쿼리 파라미터(옵션): `from`, `to`  
- 설명: 현재 로그인한 파트너 기준 실적(주문/매출/보상) 요약

### 파트너: 종료 이벤트 실적 목록
- 메서드: GET  
- URL: `/api/partner/events/performance/list`  
- 권한: PARTNER 로그인 세션 필요  
- 쿼리 파라미터(옵션): `from`, `to`  
- 설명: 현재 파트너의 보상 실적이 있는 종료 이벤트 목록 조회

## 1. 파트너 상품 등록 테스트

### 엔드포인트
```

---

## 상품 검색/필터 가이드 (색상/사이즈/가격)

### 엔드포인트
```
GET /api/product/search
```

### 파라미터
- `keyword`(선택): 상품명/설명 부분 일치
- `productType`(선택): 대분류 Enum
- `productSubType`(선택): 소분류 Enum
- `minPrice`(선택): 최소 가격
- `maxPrice`(선택): 최대 가격
- `optionName`(선택): 옵션명(색상 또는 사이즈)
- `partnerBrandCode`(선택): 파트너 대표 브랜드 코드(예: `SPEEDO`)
  - 색상: 컬러 테이블(`color`, `color_synonym`)에 등록된 표준 컬러/유사어가 있으면 우선 정규화 매칭
  - 사이즈: 문자열 부분 일치

### 색상 정규화 동작
- 컬러 기준
  - `color`에 표준 컬러(code/label), `color_synonym`에 유사어를 등록해두면
  - `optionName` 입력값을 label/유사어로 정규화하여 해당 컬러로 매칭
  - 매칭 실패 시 기존처럼 부분 일치(contains)로 검색
- 예시
  - `color`: (code=BLACK, label=블랙)
  - `color_synonym`: (BLACK, "black"), (BLACK, "검정")
  - 요청: `/api/product/search?optionName=검정` → BLACK으로 정규화 매칭

### 테스트 시나리오
1) 사이즈 필터
```
GET /api/product/search?optionName=M
```
- 옵션 사이즈에 "M"을 포함하는 상품만 반환

2) 색상 유사어 등록 전
```
GET /api/product/search?optionName=navy
```
- 부분 일치로 동작(옵션 색상 문자열에 "navy" 포함 시 매칭)

3) 색상 유사어 등록 후
- `color`에 (code=NAVY, label=네이비)
- `color_synonym`에 (NAVY, "navy"), (NAVY, "남색") 추가
```
GET /api/product/search?optionName=남색
```
- NAVY로 정규화되어 매칭(정확도 향상)

주의
- 컬러 정규화는 운영 테이블(`color`, `color_synonym`)에 등록된 값 기준으로 동작합니다.
- 유사어가 없으면 기존 부분 일치로 동작합니다.

---

## 파일 업로드 API (공통)

### 업로드
```
POST /api/files/upload
Content-Type: multipart/form-data
```

- form-data
  - `file` (필수): 이미지(jpg/jpeg/png/gif/webp) 또는 PDF
  - `category` (선택): `product`, `event`, `partner-doc` 등

예시 응답:
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "fileId": 12,
    "fileUrl": "http://localhost:8080/uploads/product/0c0c9e5d-3f7b-4c56-9f6d-25f8a873ec56.png",
    "originalName": "sample.png",
    "storedName": "0c0c9e5d-3f7b-4c56-9f6d-25f8a873ec56.png",
    "contentType": "image/png",
    "size": 12345,
    "category": "product"
  },
  "errors": null
}
```

### 파일 다운로드(권한형)
```
GET /api/files/{fileId}/download
```

- `partner-doc` 파일: 업로드한 파트너 본인 또는 관리자만 조회 가능
- 그 외 파일: 로그인 사용자 조회 가능

주의:
- public 파일은 `/uploads/**` 경로로 정적 접근됩니다.
- `partner-doc`는 private 저장되며 `GET /api/files/{fileId}/download`로만 접근됩니다.
- 기본 저장소는 로컬이며, 이후 스토리지 구현 교체로 S3 연동 가능합니다.
- 업로드는 로그인 필수이며 category별 권한이 다릅니다.
  - `product`: PARTNER, ADMIN
  - `event`: ADMIN
  - `partner-doc`: PARTNER, ADMIN
  - `review`: CUSTOMER
  - `review-image`: CUSTOMER
  - `return-image`: CUSTOMER
  - `common`(기본값): ADMIN

---

## 컬러 관리 API (관리자)

### 컬러 목록 조회
```
GET /api/admin/colors
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
```
- 활성 컬러만 정렬 순서로 반환. 각 컬러의 유사어 목록 포함.

### 컬러 생성/업서트
```
POST /api/admin/colors
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
Body: {
  "code": "BLACK",
  "label": "블랙",
  "hex": "#000000",
  "sortOrder": 10,
  "isActive": true
}
```
- 동일 code가 있으면 label/sortOrder/isActive를 업데이트.

### 컬러 활성/비활성
```
PATCH /api/admin/colors/{code}/status
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
Body: { "isActive": false }
```

### 유사어 추가
```
POST /api/admin/colors/{code}/synonyms
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
Body: { "synonym": "검정" }
```

### 유사어 삭제
```
DELETE /api/admin/colors/synonyms/{synonymId}
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
```

---

## 사이즈/브랜드 관리 및 공개 API

### 사이즈 (관리자)
- 생성/업서트:
```
POST /api/admin/sizes
Body: {
  "code": "M",
  "label": "미디움",
  "sortOrder": 20,
  "isActive": true
}
```
- 활성/비활성:
```
PATCH /api/admin/sizes/{code}/status
Body: { "isActive": false }
```
- 유사어 추가/삭제:
```
POST /api/admin/sizes/{code}/synonyms
Body: { "synonym": "medium" }
DELETE /api/admin/sizes/synonyms/{synonymId}
```
- 공개 목록:
```
GET /api/sizes
→ [{ "code":"S","label":"스몰","sortOrder":10 }, ...]
```

### 브랜드 (관리자)
- 현재 브랜드 관리자 쓰기 API는 사용하지 않습니다. (정책: 1파트너 1대표브랜드 고정)
- 브랜드 필터 데이터는 `GET /api/brands`(공개 API) 기준으로만 사용합니다.
- 공개 목록:
```
GET /api/brands
→ [{ "code":"SPEEDO","displayName":"SPEEDO","slug":"speedo","logoUrl":null,"country":"UK","sortOrder":10 }, ...]
```
POST http://localhost:8080/api/partner/products
```

### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (파트너 로그인 후 받은 세션 ID)
```

### 요청 바디 예시 (색상 + 사이즈)

```json
{
  "productName": "테스트 수영모자",
  "productType": "SWIM_CAP",
  "productSubType": "CAP_SILICONE",
  "productPrice": "25000",
  "productDescription": "테스트용 수영모자입니다.",
  "productImageUrl": "/images/products/test-cap.jpg",
  "options": [
    {
      "color": "빨강",
      "size": "M",
      "optionAddPrice": 0
    },
    {
      "color": "파랑",
      "size": "L",
      "optionAddPrice": 2000
    },
    {
      "color": "검정",
      "size": "M",
      "optionAddPrice": 0
    }
  ]
}
```

### 요청 바디 예시 (색상만)

```json
{
  "productName": "테스트 수영안경",
  "productType": "SWIM_GOGGLES",
  "productSubType": "NONE",
  "productPrice": "35000",
  "productDescription": "테스트용 수영안경입니다.",
  "productImageUrl": "/images/products/test-goggles.jpg",
  "options": [
    {
      "color": "투명",
      "size": null,
      "optionAddPrice": 0
    },
    {
      "color": "미러",
      "size": null,
      "optionAddPrice": 5000
    }
  ]
}
```

### 요청 바디 예시 (사이즈만)

```json
{
  "productName": "테스트 수영복",
  "productType": "SWIMSUIT_WOMEN",
  "productSubType": "ONE_PIECE",
  "productPrice": "89000",
  "productDescription": "테스트용 수영복입니다.",
  "productImageUrl": "/images/products/test-suit.jpg",
  "options": [
    {
      "color": null,
      "size": "S",
      "optionAddPrice": 0
    },
    {
      "color": null,
      "size": "M",
      "optionAddPrice": 0
    },
    {
      "color": null,
      "size": "L",
      "optionAddPrice": 2000
    }
  ]
}
```

### 요청 바디 예시 (optionName 수동 입력 - color/size가 없을 경우)

```json
{
  "productName": "테스트 상품",
  "productType": "ETC",
  "productSubType": "NONE",
  "productPrice": "10000",
  "productDescription": "테스트용 상품입니다.",
  "productImageUrl": "/images/products/test.jpg",
  "options": [
    {
      "color": null,
      "size": null,
      "optionName": "기본 옵션",
      "optionAddPrice": 0
    }
  ]
}
```

## 2. 파트너 로그인 (세션 필요)

### 엔드포인트
```
POST http://localhost:8080/api/auth/login
```

### 요청 바디
```json
{
  "email": "partner1@swim-mall.com",
  "password": "partner123"
}
```

### 응답에서 세션 쿠키 확인
응답 헤더의 `Set-Cookie`에서 `JSESSIONID` 값을 복사하여 다음 요청에 사용

## 3. 상품 목록 조회 (고객용)

### 엔드포인트
```
GET http://localhost:8080/api/product/list/active
```

### 응답 예시
```json
[
  {
    "productNo": 1,
    "productName": "실리콘 수영모자",
    "productType": "SWIM_CAP",
    "productSubType": "CAP_SILICONE",
    "productPrice": "25000",
    "productDescription": "...",
    "productImageUrl": "...",
    "productCreatedAt": "2026-01-01T00:00:00",
    "minPrice": 25000,
    "maxPrice": 27000,
    "options": [
      {
        "optionNo": 1,
        "color": "빨강",
        "size": "M",
        "optionName": "빨강 / M",
        "optionAddPrice": 0,
        "totalPrice": 25000
      },
      {
        "optionNo": 2,
        "color": "파랑",
        "size": "L",
        "optionName": "파랑 / L",
        "optionAddPrice": 2000,
        "totalPrice": 27000
      }
    ]
  }
]
```

## 4. 상품 상세 조회

### 엔드포인트
```
GET http://localhost:8080/api/product/detail/{productNo}
```

예: `GET http://localhost:8080/api/product/detail/1`

## 5. 장바구니 아이템 수정

### 엔드포인트
```
PUT http://localhost:8080/api/cart/items/{cartItemNo}
```

### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

### 요청 바디 예시 (수량만 수정)

```json
{
  "quantity": 3,
  "optionNo": null
}
```

또는

```json
{
  "quantity": 3
}
```

### 요청 바디 예시 (옵션만 변경)

```json
{
  "quantity": null,
  "optionNo": 2
}
```

또는

```json
{
  "optionNo": 2
}
```

### 요청 바디 예시 (수량과 옵션 동시 변경)

```json
{
  "quantity": 5,
  "optionNo": 3
}
```

### 주의사항
- `quantity`: 수량 (Integer, null이면 수정 안 함, 1 이상이어야 함)
- `optionNo`: 옵션 번호 (Long, null이면 옵션 변경 안 함)
- 둘 다 null이면 아무것도 수정되지 않음
- 옵션 변경 시 같은 상품의 옵션이어야 함
- 같은 상품+새 옵션 조합이 이미 장바구니에 있으면 수량이 합쳐지고 기존 아이템은 삭제됨

## 6. 장바구니 조회

### 엔드포인트
```
GET http://localhost:8080/api/cart
```

### 헤더
```
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

### 응답 예시
```json
{
  "totalPrice": 75000,
  "items": [
    {
      "cartItemNo": 1,
      "productNo": 1,
      "productName": "실리콘 수영모자",
      "productImageUrl": "/images/products/cap.jpg",
      "optionNo": 1,
      "color": "빨강",
      "size": "M",
      "quantity": 2,
      "itemPrice": 25000
    },
    {
      "cartItemNo": 2,
      "productNo": 2,
      "productName": "여성 원피스 수영복",
      "productImageUrl": "/images/products/suit.jpg",
      "optionNo": 5,
      "color": "블랙",
      "size": "L",
      "quantity": 1,
      "itemPrice": 25000
    }
  ]
}
```

## 7. 장바구니에 아이템 추가

### 엔드포인트
```
POST http://localhost:8080/api/cart/items
```

### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

### 요청 바디 예시 (옵션 있는 상품)
```json
{
  "productNo": 1,
  "optionNo": 1,
  "quantity": 2
}
```

### 요청 바디 예시 (옵션 없는 상품)
```json
{
  "productNo": 3,
  "optionNo": null,
  "quantity": 1
}
```

## 8. 주문 생성

### 엔드포인트
```
POST http://localhost:8080/api/orders
```

### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

### 요청 바디 예시
```json
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

### 필수 필드
- `cartItemNos`: 주문할 장바구니 아이템 번호 리스트 (배열, 비어있으면 안 됨)
- `recipientName`: 수령인 이름 (null 또는 빈 문자열 불가)
- `recipientPhone`: 수령인 전화번호 (null 또는 빈 문자열 불가, 하이픈 있든 없든 통과, 저장 시 정규화됨)
- `deliveryAddress`: 배송지 주소 (null 또는 빈 문자열 불가)
- `paymentMethod`: 결제 방법 (예: "CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT")

### 선택 필드
- `deliveryAddressDetail`: 배송지 상세 주소 (선택)
- `deliveryZipCode`: 우편번호 (선택)
- `orderMemo`: 주문 메모 (선택)

### 전화번호 형식 예시 (모두 가능)
다음 형식 모두 가능하며, 저장 시 자동으로 정규화됩니다:
```json
{
  "recipientPhone": "010-1234-5678"    // 하이픈 포함
}
```
또는
```json
{
  "recipientPhone": "01012345678"      // 하이픈 없음
}
```
또는
```json
{
  "recipientPhone": "010 1234 5678"    // 공백 포함
}
```
→ 모두 저장 시 `"01012345678"`로 정규화되어 저장되고, 응답에서도 정규화된 형식으로 반환됩니다.

### 응답 예시
```json
{
  "success": true,
  "data": {
    "orderNo": 1,
    "orderTotalPrice": 75000,
    "orderCreatedAt": "2026-02-27T16:00:00",
    "orderStatus": "ACTIVE",
    "recipientName": "홍길동",
    "recipientPhone": "010-1234-5678",
    "deliveryAddress": "서울시 강남구 테헤란로 123",
    "deliveryAddressDetail": "456호",
    "deliveryZipCode": "06234",
    "paymentMethod": "CARD",
    "paymentAmount": 75000,
    "orderMemo": "문 앞에 놔주세요",
    "orderItems": [
      {
        "orderItemNo": 1,
        "productNo": 1,
        "productName": "실리콘 수영모자",
        "productImageUrl": "/images/products/cap.jpg",
        "optionNo": 1,
        "color": "빨강",
        "size": "M",
        "quantity": 2,
        "itemPrice": 25000,
        "itemDiscountAmount": 0,
        "itemTotalPrice": 50000,
        "isCancelled": false
      },
      {
        "orderItemNo": 2,
        "productNo": 2,
        "productName": "여성 원피스 수영복",
        "productImageUrl": "/images/products/suit.jpg",
        "optionNo": 5,
        "color": "블랙",
        "size": "L",
        "quantity": 1,
        "itemPrice": 25000,
        "itemDiscountAmount": 0,
        "itemTotalPrice": 25000,
        "isCancelled": false
      }
    ]
  }
}
```

### 에러 응답 예시

#### 필수 필드가 비어있는 경우 (400 Bad Request)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "잘못된 요청입니다"
}
```
- `recipientName`, `recipientPhone`, `deliveryAddress`가 null이거나 빈 문자열("")이면 에러 발생

#### 장바구니 아이템이 없는 경우 (400 Bad Request)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "장바구니 아이템이 필요합니다"
}
```

#### 로그인하지 않은 경우 (401 Unauthorized)
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "로그인이 필요합니다"
}
```

#### 다른 고객의 장바구니 아이템인 경우 (403 Forbidden)
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "접근 권한이 없습니다"
}
```
- `cartItemNos`에 포함된 아이템이 현재 로그인한 고객의 것이 아니면 에러 발생

#### 장바구니 아이템을 찾을 수 없는 경우 (404 Not Found)
```json
{
  "success": false,
  "code": "CART_ITEM_NOT_FOUND",
  "message": "장바구니 아이템을 찾을 수 없습니다"
}
```

### 주의사항
- **주문 생성 후 해당 장바구니 아이템들은 자동으로 삭제됩니다.**
- **`cartItemNos`에 포함된 모든 아이템은 현재 로그인한 고객의 장바구니에 있어야 합니다. (보안 검증)**
- **금액 계산은 서버에서 수행됩니다.** 프론트엔드에서 전달한 금액은 무시되고, 서버에서 장바구니 아이템의 가격을 기반으로 계산합니다.
- **전화번호는 저장 시 자동 정규화됩니다.** 하이픈과 공백이 제거되어 저장됩니다.
- **필수 필드는 빈 문자열도 허용하지 않습니다.** null뿐만 아니라 빈 문자열("")도 검증에서 제외됩니다.

## 9. 주문 목록 조회

### 엔드포인트
```
GET http://localhost:8080/api/orders
```

### 헤더
```
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

### 응답 예시
```json
{
  "success": true,
  "data": [
    {
      "orderNo": 1,
      "orderTotalPrice": 75000,
      "orderCreatedAt": "2026-02-27T16:00:00",
      "orderStatus": "ACTIVE",
      "recipientName": "홍길동",
      "recipientPhone": "010-1234-5678",
      "deliveryAddress": "서울시 강남구 테헤란로 123",
      "deliveryAddressDetail": "456호",
      "deliveryZipCode": "06234",
      "paymentMethod": "CARD",
      "paymentAmount": 75000,
      "orderMemo": "문 앞에 놔주세요",
      "orderItems": [...]
    }
  ]
}
```

### 주의사항
- 최신 주문이 먼저 나옵니다 (생성일시 내림차순).
- 현재 로그인한 고객의 주문만 조회됩니다.

## 10. 주문 상세 조회

### 엔드포인트
```
GET http://localhost:8080/api/orders/{orderNo}
```

### 헤더
```
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

예: `GET http://localhost:8080/api/orders/1`

### 응답 예시
```json
{
  "success": true,
  "data": {
    "orderNo": 1,
    "orderTotalPrice": 75000,
    "orderCreatedAt": "2026-02-27T16:00:00",
    "orderStatus": "ACTIVE",
    "recipientName": "홍길동",
    "recipientPhone": "010-1234-5678",
    "deliveryAddress": "서울시 강남구 테헤란로 123",
    "deliveryAddressDetail": "456호",
    "deliveryZipCode": "06234",
    "paymentMethod": "CARD",
    "paymentAmount": 75000,
    "orderMemo": "문 앞에 놔주세요",
    "orderItems": [
      {
        "orderItemNo": 1,
        "productNo": 1,
        "productName": "실리콘 수영모자",
        "productImageUrl": "/images/products/cap.jpg",
        "optionNo": 1,
        "color": "빨강",
        "size": "M",
        "quantity": 2,
        "itemPrice": 25000,
        "itemDiscountAmount": 0,
        "itemTotalPrice": 50000,
        "isCancelled": false
      }
    ]
  }
}
```

### 주의사항
- 현재 로그인한 고객의 주문만 조회할 수 있습니다.
- 다른 고객의 주문을 조회하려고 하면 403 Forbidden 에러가 발생합니다.

## 주문 생성 테스트 순서

1. **고객 로그인**
   ```
   POST http://localhost:8080/api/auth/login
   Body: { "email": "customer1@swim-mall.com", "password": "customer123" }
   ```
   → 응답 헤더에서 `JSESSIONID` 복사

2. **장바구니에 아이템 추가** (여러 개 추가 가능)
   ```
   POST http://localhost:8080/api/cart/items
   Body: { "productNo": 1, "optionNo": 1, "quantity": 2 }
   ```

3. **장바구니 조회** (cartItemNo 확인)
   ```
   GET http://localhost:8080/api/cart
   ```
   → 응답에서 `cartItemNo` 값들을 확인 (예: [1, 2])

4. **주문 생성**
   ```
   POST http://localhost:8080/api/orders
   Body: {
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

5. **장바구니 재조회** (주문한 아이템들이 삭제되었는지 확인)
   ```
   GET http://localhost:8080/api/cart
   ```

6. **주문 목록 조회**
   ```
   GET http://localhost:8080/api/orders
   ```

7. **주문 상세 조회**
   ```
   GET http://localhost:8080/api/orders/1
   ```

## 8. 주문 생성

### 엔드포인트
```
POST http://localhost:8080/api/orders
```

### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

### 사전 준비
1. 고객 로그인 (세션 ID 획득)
2. 장바구니에 아이템 추가 (cartItemNo 확인)

### 요청 바디 예시

```json
{
  "cartItemNos": [1, 2],
  "recipientName": "홍길동",
  "recipientPhone": "010-1234-5678",
  "deliveryAddress": "서울특별시 강남구 테헤란로 123",
  "deliveryAddressDetail": "456호",
  "deliveryZipCode": "06142",
  "paymentMethod": "CARD",
  "orderMemo": "문 앞에 놔주세요"
}
```

### 필수 필드
- `cartItemNos`: 주문할 장바구니 아이템 번호 리스트 (배열, 비어있으면 안 됨)
- `recipientName`: 수령인 이름
- `recipientPhone`: 수령인 전화번호
- `deliveryAddress`: 배송지 주소
- `paymentMethod`: 결제 방법 (예: "CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT")

### 선택 필드
- `deliveryAddressDetail`: 배송지 상세 주소
- `deliveryZipCode`: 우편번호
- `orderMemo`: 주문 메모

### 성공 응답 예시 (200 OK)

```json
{
  "success": true,
  "data": {
    "orderNo": 1,
    "orderTotalPrice": 104000,
    "orderCreatedAt": "2024-01-15T10:30:00",
    "orderStatus": "ACTIVE",
    "recipientName": "홍길동",
    "recipientPhone": "010-1234-5678",
    "deliveryAddress": "서울특별시 강남구 테헤란로 123",
    "deliveryAddressDetail": "456호",
    "deliveryZipCode": "06142",
    "paymentMethod": "CARD",
    "paymentAmount": 104000,
    "orderMemo": "문 앞에 놔주세요",
    "orderItems": [
      {
        "orderItemNo": 1,
        "productNo": 1,
        "productName": "실리콘 수영모자",
        "productImageUrl": "/images/products/womens/women-1.jpg",
        "optionNo": 1,
        "color": "블랙",
        "size": "M",
        "quantity": 1,
        "itemPrice": 15000,
        "itemDiscountAmount": 0,
        "itemTotalPrice": 15000,
        "isCancelled": false
      },
      {
        "orderItemNo": 2,
        "productNo": 2,
        "productName": "여성 원피스 수영복",
        "productImageUrl": "/images/products/womens/women-20.jpg",
        "optionNo": 3,
        "color": "블랙",
        "size": "L",
        "quantity": 1,
        "itemPrice": 89000,
        "itemDiscountAmount": 0,
        "itemTotalPrice": 89000,
        "isCancelled": false
      }
    ]
  }
}
```

### 에러 응답 예시

#### 장바구니 아이템이 없는 경우 (400 Bad Request)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "장바구니 아이템이 필요합니다"
}
```

#### 로그인하지 않은 경우 (401 Unauthorized)
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "로그인이 필요합니다"
}
```

#### 장바구니 아이템이 다른 고객의 것인 경우 (403 Forbidden)
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "접근 권한이 없습니다"
}
```

### 주의사항
1. **장바구니 아이템 확인**: 주문 전에 장바구니 조회 API로 `cartItemNo`를 확인하세요.
2. **주문 생성 후 장바구니 삭제**: 주문 생성 시 해당 장바구니 아이템들은 자동으로 삭제됩니다.
3. **결제 방법**: `paymentMethod`는 문자열로 입력 (예: "CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT")
4. **총액 계산**: 백엔드에서 자동으로 계산되어 `orderTotalPrice`에 반영됩니다.

---

## 19. 세일/이벤트 적용 규칙 확정안 (문서/코드/QA 공통)

### 19-1. 가격 적용 순서 (고정)

아래 순서를 문서, 서버 코드, QA 기대값에서 동일하게 사용합니다.

1. 상품 원가(옵션가 포함)
2. 세일 할인 적용
3. 이벤트 혜택 적용 (쿠폰/적립/사은품)
4. 포인트 사용 차감
5. 최종 결제액 확정

---

### 19-2. 기준 시점 통일 (price-lock 기준)

- 세일/이벤트 적용 판정 시점은 주문 생성 시점 기준으로 통일합니다.
- 가격 보장(price-lock) 세션이 유효하면, 세일/이벤트 판정 기준 시각도 락 시작 시각으로 고정합니다.
- 락 이후 정책(세일/이벤트) 변경이 발생해도 해당 주문은 스냅샷 기준으로 처리합니다.

---

### 19-3. 중복/배타 규칙

- 기본 원칙: 세일 + 이벤트 동시 허용
- 단, 특정 이벤트 정책에서 "세일 상품 제외"가 정의된 경우 해당 규칙 우선
- 쿠폰/포인트와 이벤트 우선순위는 19-1 순서를 따름

### 19-3-1. 세일 생성 권한/이벤트 연결 규칙 (확정)

- 세일 정책 생성 책임: 파트너
- 관리자 세일 생성 API(`POST /api/admin/sales`)는 정책상 비허용
- 파트너가 `eventNo`를 세일에 연결하려면 아래 조건을 모두 만족해야 함
  - 이벤트 모드가 `PARTNER_PARTICIPATION`
  - 이벤트 타입이 `SALE`
  - 이벤트 상태가 `SCHEDULED` 또는 `ACTIVE`
  - 해당 파트너의 이벤트 참여 레코드가 `active=true` 상태
  - 현재 시각이 이벤트의 파트너 신청 기간(`partnerApplyStartAt ~ partnerApplyEndAt`) 범위 내
  - 세일 기간이 이벤트 고객 노출 기간 범위를 벗어나지 않음
  - 파트너와 이벤트가 같은 운영 주체(admin) 소속
- `ADMIN_ONLY` 이벤트에는 파트너 세일 연결 불가
- `eventType=SALE`로 연결된 세일은 파트너 긴급 중단 불가(관리자만 중단 가능)
- `eventType=SALE`로 연결해 생성할 때 할인값/기간은 이벤트 기준으로 강제 적용됩니다. (파트너 입력값 무시)
- 동일 파트너가 동일 `eventNo`에서 같은 대상(상품/옵션)을 중복 등록하는 것은 차단됩니다.
- 파트너가 `SALE` 이벤트 참여를 해제해도 이미 생성된 `sale_policy`(승인/거절/중단)는 자동으로 취소/중단되지 않습니다.
- 참여 해제는 이후 **신규** `eventType=SALE` 연동 세일(캠페인) 생성만 제한합니다. (컨테이너=참여, 실행 레코드=sale_policy)
- `eventType=SALE` 이벤트 목록/상세에서 표시되는 `participating` 값은 실제 `sale_policy`가 등록된 경우에만 `true`로 내려갑니다.

---

### 19-4. 주문 스냅샷 저장 점검

#### 주문(order) 단위
- `usedPointAmount`: 주문 시 사용 포인트 금액
- `saleDiscountTotal`: 주문 전체 세일 할인 합계 (권장 추가)
- `eventDiscountTotal`: 주문 전체 이벤트 할인 합계 (권장 추가)
- `finalPayAmount`: 최종 결제 금액

#### 주문아이템(order_item) 단위
- `appliedSalePolicyNo`
- `appliedSaleCampaignId`
- `saleEvaluatedAt`
- (필요 시) 이벤트 스냅샷 필드

---

### 19-5. 취소/반품 환원 규칙

- 취소: 스냅샷 기준 금액으로 환불
- 부분 취소: 아이템 비율 기준으로 세일/이벤트 할인 재배분
- 적립금 회수/재지급:
  - 미확정 적립은 지급하지 않음
  - 이미 지급된 적립은 취소/반품 확정 시 회수

---

### 19-6. E2E 검증 시나리오 (필수)

1. 세일만 적용
2. 이벤트만 적용
3. 세일 + 이벤트 동시 적용
4. 주문 직전 이벤트 종료
5. price-lock 이후 정책 변경
6. 부분 취소/부분 반품

각 시나리오에서 아래 일치 여부를 확인합니다.
- 화면 금액
- 결제 요청 금액
- DB 스냅샷(`orders`, `order_item`)
- 포인트/이벤트 이력
- 정산 기준 금액

---

### 19-7. 포인트 정책 분리(상시/이벤트) 검증 시나리오

#### 목표
- 기본 포인트(상시): 구매확정/리뷰 작성은 이벤트와 무관하게 항상 적용
- 이벤트 포인트(캠페인): `EventType=POINT` 이벤트 조건 충족 시에만 추가 지급
- 최종 지급식: `최종 지급 = 기본 포인트 + 이벤트 추가 포인트`

#### 현재 백엔드 규칙
- 구매확정 기본 포인트
  - 트리거: `POST /api/order-items/{orderItemNo}/complete` 또는 자동 구매확정 스케줄러
  - 계산: `orderItem.itemTotalPrice * 고객등급 pointAccumulationRate(%)`
- 리뷰 작성 기본 포인트
  - 트리거: `POST /api/reviews`
  - 고정값: `100` 포인트
  - 중복 방지: `review.order_item_no` 유니크로 1회만 적립
- 이벤트 추가 포인트
  - 관리자 단독 이벤트: `admin_event_reward`
  - 파트너 참여형 이벤트: `partner_event_reward`
  - 대상 매핑 테이블: `event_point_target`
    - 유니크: `(event_no, target_type, target_value)`
    - 포인트 대상은 CSV가 아니라 매핑 row 기준으로 판정
  - 주문 횟수 차감 테이블:
    - 관리자 단독: `admin_event_reward_order_cap`
    - 파트너 참여형: `partner_event_reward_order_cap`
  - 횟수 차감 단위: 주문 기준(`event_no + customer_id + order_no`), 최대 3회
  - `EventType=POINT`만 "배수/가산" 계산으로 이벤트 포인트 지급
    - `saleDiscountType=PERCENT`: `basePoint * saleDiscountValue / 100`
    - `saleDiscountType=FIXED`: `saleDiscountValue` 고정 포인트
  - `EventType=SALE`은 추가 포인트 지급 없음

#### POINT 이벤트 선택 적용 옵션(관리자 이벤트 생성/수정 바디)
- `pointEventTargetType` (선택, 기본값 `ALL`)
  - `ALL | PARTNER | PRODUCT | OPTION | CATEGORY | MIN_ORDER_AMOUNT`
- `pointEventTargetValues` (필수: `ALL/MIN_ORDER_AMOUNT` 제외 시)
  - 예: `PARTNER`면 `["1","2"]`, `PRODUCT`면 `["10","11"]`, `OPTION`이면 `["101","102"]`, `CATEGORY`면 `["SWIMSUIT","BIKINI"]`
- `pointEventMinOrderAmount` (`MIN_ORDER_AMOUNT`에서 필수)

> 참고: `pointEventTargetValue`(CSV)는 더 이상 사용하지 않습니다.

예시 1) 전체 대상
```json
{
  "eventTitle": "포인트 이벤트 - 전체",
  "eventContent": "구매확정 시 추가 포인트",
  "eventStatus": "ACTIVE",
  "eventMode": "ADMIN_ONLY",
  "eventType": "POINT",
  "saleDiscountType": "PERCENT",
  "saleDiscountValue": 20,
  "customerEventStartAt": "2026-03-27T00:00:00",
  "customerEventEndAt": "2026-04-30T23:59:59",
  "pointEventTargetType": "ALL"
}
```

예시 2) 상품 선택 대상
```json
{
  "eventTitle": "포인트 이벤트 - 특정 상품",
  "eventContent": "지정 상품 구매확정 추가 포인트",
  "eventStatus": "ACTIVE",
  "eventMode": "ADMIN_ONLY",
  "eventType": "POINT",
  "saleDiscountType": "FIXED",
  "saleDiscountValue": 300,
  "customerEventStartAt": "2026-03-27T00:00:00",
  "customerEventEndAt": "2026-04-30T23:59:59",
  "pointEventTargetType": "PRODUCT",
  "pointEventTargetValues": ["1", "2", "3"]
}
```

#### 케이스 A: 구매확정 기본 포인트만 지급
1. POINT 이벤트를 만들지 않거나, 대상이 아닌 주문아이템으로 구매확정
2. `POST /api/order-items/{orderItemNo}/complete` 호출
3. 확인
   - `point_history`에 "구매 확정 적립" 1건 생성
   - `admin_event_reward`, `partner_event_reward`는 생성되지 않거나 해당 주문아이템 없음

#### 케이스 B: 구매확정 + POINT 이벤트 추가 포인트(관리자 단독)
1. 관리자 이벤트 생성
   - `eventType=POINT`
   - `eventMode=ADMIN_ONLY`
   - `eventStatus=ACTIVE`
   - `customerEventStartAt <= now <= customerEventEndAt`
   - `saleDiscountType/saleDiscountValue` 입력
2. 대상 주문아이템 구매확정
3. 확인
   - 기본 포인트 1건 + 관리자 이벤트 포인트 1건이 `point_history`에 각각 생성
   - `admin_event_reward`에 `(event_no, order_item_no)` 1건 생성
   - 동일 주문아이템 재호출 시 reward 중복 생성 없음(유니크 제약)
   - `event_point_target`에 대상 row가 있어야만 지급됨

#### 케이스 C: 구매확정 + POINT 이벤트 추가 포인트(파트너 참여형)
1. 관리자 이벤트 생성
   - `eventType=POINT`
   - `eventMode=PARTNER_PARTICIPATION`
   - 신청기간/고객기간/상태 유효
2. 파트너 참여
   - `POST /api/partner/events/{eventNo}/participation`
3. 참여 파트너 상품 주문 후 구매확정
4. 확인
   - 기본 포인트 + 파트너 이벤트 포인트가 `point_history`에 생성
   - `partner_event_reward`에 `(event_no, order_item_no)` 1건 생성

#### 케이스 D: 대상 필터 미충족 주문아이템 미지급
1. `pointEventTargetType=PRODUCT`, `pointEventTargetValues=["1"]` 로 이벤트 생성
2. 상품 `2` 주문아이템 구매확정
3. 확인
   - 기본 포인트만 지급
   - `admin_event_reward` / `partner_event_reward`에 해당 주문아이템 보상 row 없음
   - `event_point_target`에는 상품 `1`만 저장

#### 케이스 E: MIN_ORDER_AMOUNT 경계값 검증
1. `pointEventTargetType=MIN_ORDER_AMOUNT`, `pointEventMinOrderAmount=50000`으로 이벤트 생성
2. 주문총액 49,999 주문아이템 구매확정 -> 이벤트 포인트 미지급 확인
3. 주문총액 50,000 주문아이템 구매확정 -> 이벤트 포인트 지급 확인

#### 케이스 F: 리뷰 작성 기본 포인트
1. 구매확정된 주문아이템으로 `POST /api/reviews` 호출
2. 확인
   - `point_history`에 설명 `"리뷰 작성 적립"` 생성
   - 동일 주문아이템으로 재리뷰 시도 시 리뷰 생성/포인트 적립 모두 불가

#### 점검 SQL 예시
```sql
-- 주문아이템 기준 포인트 이력 확인
SELECT history_id, order_item_no, point_amount, description, created_at
FROM point_history
WHERE order_item_no = :orderItemNo
ORDER BY history_id DESC;

-- 관리자 이벤트 보상 생성 확인
SELECT reward_no, event_no, order_item_no, point_amount, created_at
FROM admin_event_reward
WHERE order_item_no = :orderItemNo
ORDER BY reward_no DESC;

-- 파트너 이벤트 보상 생성 확인
SELECT reward_no, event_no, order_item_no, point_amount, created_at
FROM partner_event_reward
WHERE order_item_no = :orderItemNo
ORDER BY reward_no DESC;

-- 이벤트 대상 매핑 확인
SELECT id, event_no, target_type, target_value, created_at
FROM event_point_target
WHERE event_no = :eventNo
ORDER BY id DESC;

-- 주문 기준 횟수 차감(관리자 단독)
SELECT cap_no, event_no, customer_id, order_no, created_at
FROM admin_event_reward_order_cap
WHERE event_no = :eventNo AND customer_id = :customerId
ORDER BY cap_no DESC;

-- 주문 기준 횟수 차감(파트너 참여형)
SELECT cap_no, event_no, customer_id, order_no, created_at
FROM partner_event_reward_order_cap
WHERE event_no = :eventNo AND customer_id = :customerId
ORDER BY cap_no DESC;
```

### 19-8. 이벤트 정책 스냅샷(주문 시점 고정) 안내

- 목적: 주문 생성 이후 이벤트 정책이 변경되어도 기존 주문의 포인트/대상 판정에 영향이 없도록, 주문 시점의 정책을 스냅샷으로 고정합니다.
- 저장 컬럼(`order_item`):
  - `admin_event_policy_snapshot` (TEXT, JSON 직렬화)
  - `partner_event_policy_snapshot` (TEXT, JSON 직렬화)
- 스냅샷 구조(JSON):
  - `policies[]`: 각 원소는
    - `eventNo`, `eventType`(POINT/SALE)
    - `rewardType`(PERCENT|FIXED), `rewardValue`
    - `pointEventTargetType`, `pointEventTargetValues[]`, `pointEventMinOrderAmount`
  - `snapshotAt`: ISO 문자열
- 지급 시 판정 순서:
  1) 스냅샷 정책(주문 시점)을 사용해 대상/금액 판정
  2) 스냅샷이 없는 과거 주문은 하위호환으로 런타임 판정
- TEXT 선택 사유:
  - 디버깅/운영 점검 시 SQL로 원문 확인 용이
  - 인덱싱/부분 질의 요구 없음(원본 보관용)
  - DB별 JSON/BLOB 의존 최소화(이식성 확보)
- 점검 SQL:
```sql
SELECT order_item_no,
       admin_event_policy_snapshot,
       partner_event_policy_snapshot
FROM order_item
WHERE order_item_no = :orderItemNo;
```

---

## 20. 시즌 세일 운영 체크리스트 (권한/이력/정산)

### 20-1. 정책/권한

- SALE 이벤트 연동 세일은 파트너 중단 불가(UI에서 버튼 비노출 + 안내문 노출)
- SALE 이벤트 연동 세일 중단은 관리자만 가능
- 관리자 중단 시 사유 입력 필수(`PATCH /api/admin/sales/{id}/cancel` body: `cancelReason`)
- 시즌 세일 일부 파트너 제외 요청은 "예외 승인" 프로세스로 처리
  - 파트너 요청 접수
  - 운영/정산 영향 검토
  - 관리자 승인 후 해당 파트너 정책만 관리자 중단(사유 기록)

### 20-2. 이력 기록 점검

- 세일-이벤트 연결 변경 시 이력 저장(`EVENT_LINK_CHANGED`)
- 관리자 승인 시 이력 저장(`APPROVED`)
- 관리자 거절 시 이력 저장(`REJECTED`, 거절 사유 포함)
- 관리자 중단 시 이력 저장(`CANCELLED`, 중단 사유 포함)
- 이력 필수값: 누가/언제/사유(`changedByRole`, `changedById`, `changedAt`, `reason`)

### 20-3. 주문 스냅샷 점검

- 주문아이템에 아래 스냅샷이 저장되는지 확인
  - `appliedSalePolicyNo`
  - `appliedSaleCampaignId`
  - `appliedSaleEventNo`
  - `appliedSaleEventType`
  - `saleEvaluatedAt`
- CS/정산 확인 시 실시간 정책 조회 대신 스냅샷 기준으로 판단

---


## 17. 세일 기능 테스트

### 17-0. 파트너 단독 세일 vs 관리자 파트너 참여형 SALE (차이점 요약)

| 구분 | 파트너 단독 세일 | 관리자 파트너 참여형 SALE(eventType=SALE) |
|---|---|---|
| 생성 API | `POST /api/partner/sales/campaign` | `POST /api/partner/sales/campaign` (동일, `eventNo` 필수) |
| 필수 전제 | 파트너 권한 | 파트너 권한 + 이벤트 참여(active=true) + 파트너 신청 기간 내 |
| 할인값/타입 | 파트너 입력값 사용 | 이벤트 정책값으로 강제 덮어쓰기 |
| 세일 기간 | 파트너 입력값 사용 | 이벤트 고객 노출 기간으로 강제 덮어쓰기 |
| 상태 결정 | 기본 하이브리드(저위험 ACTIVE, 고위험 PENDING_APPROVAL) | `eventType=SALE` 연동은 항상 `ACTIVE` |
| 대상 중복 | 일반 중복 검증(기간/타겟 충돌) | 일반 중복 검증 + 동일 `eventNo` 동일 타겟 중복 차단 |
| 파트너 중단 권한 | 가능(단건/캠페인) | 불가(관리자만 중단 가능) |
| 보상(reward) 조건 | 이벤트 미연동이면 해당 없음 | 추가 포인트 보상 없음 (세일가 적용만 수행) |

### 17-1. 파트너 세일 캠페인 생성 (다건)

#### 엔드포인트
```
POST http://localhost:8080/api/partner/sales/campaign
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (파트너 로그인 후 받은 세션 ID)
```

#### 요청 바디 예시 (상품 단위)
```json
{
  "eventNo": 1,
  "scope": "PRODUCT",
  "startAt": "2026-03-26T00:00:00",
  "endAt": "2026-03-31T23:59:59",
  "targets": [
    {
      "targetProductNo": 1,
      "discountType": "PERCENT",
      "discountValue": 20,
      "maxDiscountAmount": 10000
    },
    {
      "targetProductNo": 2,
      "discountType": "FIXED",
      "discountValue": 3000,
      "maxDiscountAmount": null
    }
  ]
}
```

#### 요청 바디 예시 (옵션 단위 다건)
```json
{
  "eventNo": 1,
  "scope": "OPTION",
  "startAt": "2026-03-26T00:00:00",
  "endAt": "2026-03-31T23:59:59",
  "targets": [
    {
      "targetOptionNo": 101,
      "discountType": "PERCENT",
      "discountValue": 15
    },
    {
      "targetOptionNo": 102,
      "discountType": "PERCENT",
      "discountValue": 10
    }
  ]
}
```

#### 성공 응답 체크 포인트
- `data[].campaignId`가 동일하면 한 캠페인으로 묶여 생성됨
- 저위험 항목은 `ACTIVE`, 고위험 항목은 `PENDING_APPROVAL`로 생성될 수 있음
  - 단, `eventType=SALE`로 강제형 시즌 세일을 연결해 생성하는 경우에는 할인/기간은 이벤트 기준으로 덮어쓰고, 상태는 항상 `ACTIVE`로 처리됩니다.

#### 강제형 시즌 SALE 연결 시 주의
- `eventNo`에 연결된 이벤트가 `eventType=SALE`이면, 백엔드에서 `targets[].discountType/discountValue/maxDiscountAmount`는 이벤트 정책값으로 덮어씌워집니다.
- `eventType=SALE`이면 요청의 `startAt/endAt` 값은 검증/사용하지 않고 이벤트 기간(`customerEventStartAt/endAt`)으로 강제 고정됩니다.
- 다만 DTO validation 통과를 위해 필수 필드는 요청에 포함되어야 합니다.
- 세일 기간은 기본 날짜 기준(시작 00:00:00 / 종료 23:59:59)이며, 프론트에서 "시간 포함"을 켜면 날짜+시간 기준으로 저장됩니다.

#### 에러 응답 체크 포인트 (중복 차단)
- 같은 상품 기간 중복: `"같은 상품에 기간이 겹치는 세일이 이미 존재합니다."`
- 같은 옵션 기간 중복: `"같은 옵션에 기간이 겹치는 세일이 이미 존재합니다."`
- 상품/옵션 교차 중복: `"같은 상품(옵션 포함)에 기간이 겹치는 세일이 이미 존재합니다."`

---

### 17-2. 파트너 세일 목록/상세 조회

#### 엔드포인트
```
GET http://localhost:8080/api/partner/sales
GET http://localhost:8080/api/partner/sales/{id}
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (파트너 로그인 후 받은 세션 ID)
```

---

### 17-3. 파트너 세일 단건/캠페인 긴급 중단

#### 엔드포인트
```
PATCH http://localhost:8080/api/partner/sales/{id}/cancel
PATCH http://localhost:8080/api/partner/sales/campaigns/{campaignId}/cancel
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (파트너 로그인 후 받은 세션 ID)
```

---

### 17-4. 관리자 세일 승인/거절/중단

#### 엔드포인트
```
GET http://localhost:8080/api/admin/sales
GET http://localhost:8080/api/admin/sales/{id}
PATCH http://localhost:8080/api/admin/sales/{id}/approve
PATCH http://localhost:8080/api/admin/sales/{id}/reject
PATCH http://localhost:8080/api/admin/sales/{id}/cancel
```

#### 거절 요청 바디 예시
```json
{
  "rejectionReason": "승인 기준(할인율/기간) 검토 결과 반려"
}
```

#### 중단 요청 바디 예시 (필수)
```json
{
  "cancelReason": "시즌 세일 운영 정책 변경으로 중단"
}
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

---

### 17-5. 고객 세일 적용 조회(판정 API)

#### 엔드포인트
```
GET http://localhost:8080/api/sales/applicable?productNo=1&optionNo=101&at=2026-03-27T12:00:00
```

#### 설명
- 주문/장바구니/상품목록에서 특정 시점 기준 적용 가능한 세일 1건을 조회할 때 사용

---

### 17-6. 주문 스냅샷 필드 확인

#### 체크 API
```
GET http://localhost:8080/api/orders/{orderNo}
```

#### 확인 포인트
- 주문(`orders`) 단위: `usedPointAmount`
- 주문아이템(`order_item`) 단위: `appliedSalePolicyNo`, `appliedSaleCampaignId`, `saleEvaluatedAt`

#### 가격 적용 순서(고정)
1. 상품 원가
2. 세일 할인
3. 이벤트/쿠폰/포인트

---

## 18. 이벤트 기능 테스트

### 18-1. 고객 이벤트 조회

#### 엔드포인트
```
GET http://localhost:8080/api/events
GET http://localhost:8080/api/events/{eventNo}
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (선택)
```

---

### 18-2. 관리자 이벤트 생성/수정/상태변경

#### 엔드포인트
```
POST  http://localhost:8080/api/admin/events
PUT   http://localhost:8080/api/admin/events/{eventNo}
PATCH http://localhost:8080/api/admin/events/{eventNo}/status
GET   http://localhost:8080/api/events/admin
GET   http://localhost:8080/api/events/{eventNo}/admin
GET   http://localhost:8080/api/events/{eventNo}/admin/participants
```

#### 생성/수정 요청 바디 예시
```json
{
  "eventTitle": "봄맞이 수영대전",
  "eventContent": "봄 시즌 이벤트",
  "eventStatus": "SCHEDULED",
  "customerEventStartAt": "2026-04-01T00:00:00",
  "customerEventEndAt": "2026-04-14T23:59:59",
  "partnerApplyEnabled": true,
  "partnerApplyStartAt": "2026-03-25T00:00:00",
  "partnerApplyEndAt": "2026-03-31T23:59:59",
  "thumbnailUrl": "/images/events/spring.jpg",
  "eventType": "SALE",
  "saleDiscountType": "PERCENT",
  "saleDiscountValue": 30,
  "saleMaxDiscountAmount": 20000,
  "eventMode": "PARTNER_PARTICIPATION",
  "adminMemo": "운영팀 검토 완료"
}
```

`eventType=SALE`인 경우 `saleDiscountType`, `saleDiscountValue`는 필수입니다.  
강제형 정책으로, 파트너는 이벤트 참여 후 세일 대상(상품/옵션)만 선택하고 할인값은 이벤트 기준을 따릅니다.

#### 상태 변경 요청 바디 예시
```json
{
  "eventStatus": "ACTIVE"
}
```

---

### 18-3. 파트너 이벤트 참여/해제/참여여부 조회

#### 엔드포인트
```
GET    http://localhost:8080/api/partner/events?upcomingDays=14
POST   http://localhost:8080/api/partner/events/{eventNo}/participation
DELETE http://localhost:8080/api/partner/events/{eventNo}/participation
GET    http://localhost:8080/api/partner/events/{eventNo}/participation
```

`DELETE /participation` 시 선택 파라미터:
- `deactivateLinkedSales=true` : 해당 이벤트에 연동된 기존 세일 정책을 함께 `INACTIVE`로 전환
- 미지정(기본값 `false`) : 참여만 해제하고 기존 세일 정책은 유지

#### 파트너 이벤트 목록 필드 의미 (중요)
- `participationEnabled`: 파트너가 해당 이벤트에 참여 신청(active=true)했는지 여부
- `participating`: (특히 `eventType=SALE`) 실제 연동 `sale_policy`가 존재하는지 여부
  - 즉, SALE 이벤트는 참여 신청만 하고 세일 등록을 안 하면 `participationEnabled=true`, `participating=false`가 될 수 있습니다.

#### 관리자 이벤트 참여 파트너 조회 응답 필드
- `linkedSalePolicyCount`: 해당 파트너가 이 이벤트에 연동해 등록한 세일 정책 수
- `linkedSaleTargets`: 해당 파트너가 등록한 세일 대상 요약(예: `상품#12, 옵션#104`)

#### 헤더
```
Cookie: JSESSIONID=세션ID (파트너 로그인 후 받은 세션 ID)
```

#### 참여 여부 조회 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "participating": true
  }
}
```


## 9. 주문 목록 조회

### 엔드포인트
```
GET http://localhost:8080/api/orders
```

### 헤더
```
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

### 성공 응답 예시 (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "orderNo": 1,
      "orderTotalPrice": 104000,
      "orderCreatedAt": "2024-01-15T10:30:00",
      "orderStatus": "ACTIVE",
      "recipientName": "홍길동",
      "recipientPhone": "010-1234-5678",
      "deliveryAddress": "서울특별시 강남구 테헤란로 123",
      "deliveryAddressDetail": "456호",
      "deliveryZipCode": "06142",
      "paymentMethod": "CARD",
      "paymentAmount": 104000,
      "orderMemo": "문 앞에 놔주세요",
      "orderItems": [...]
    }
  ]
}
```

### 주의사항
- 본인의 주문만 조회됩니다.
- 최신순으로 정렬되어 반환됩니다.

---

## 10. 주문 상세 조회

### 엔드포인트
```
GET http://localhost:8080/api/orders/{orderNo}
```

### 헤더
```
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

### 경로 변수
- `orderNo`: 주문 번호 (예: 1)

### 성공 응답 예시 (200 OK)

```json
{
  "success": true,
  "data": {
    "orderNo": 1,
    "orderTotalPrice": 104000,
    "orderCreatedAt": "2024-01-15T10:30:00",
    "orderStatus": "ACTIVE",
    "recipientName": "홍길동",
    "recipientPhone": "010-1234-5678",
    "deliveryAddress": "서울특별시 강남구 테헤란로 123",
    "deliveryAddressDetail": "456호",
    "deliveryZipCode": "06142",
    "paymentMethod": "CARD",
    "paymentAmount": 104000,
    "orderMemo": "문 앞에 놔주세요",
    "orderItems": [
      {
        "orderItemNo": 1,
        "productNo": 1,
        "productName": "실리콘 수영모자",
        "productImageUrl": "/images/products/womens/women-1.jpg",
        "optionNo": 1,
        "color": "블랙",
        "size": "M",
        "quantity": 1,
        "itemPrice": 15000,
        "itemDiscountAmount": 0,
        "itemTotalPrice": 15000,
        "isCancelled": false
      }
    ]
  }
}
```

### 에러 응답 예시

#### 주문을 찾을 수 없는 경우 (404 Not Found)
```json
{
  "success": false,
  "code": "ORDER_NOT_FOUND",
  "message": "주문을 찾을 수 없습니다"
}
```

#### 다른 고객의 주문인 경우 (403 Forbidden)
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "접근 권한이 없습니다"
}
```

### 주의사항
- 본인의 주문만 조회할 수 있습니다.

---

## 5. 결제 승인 API

### 5.1 결제 승인

**엔드포인트:** `POST /api/payments/approve`

**설명:** 주문 생성 후 결제를 승인합니다. 주문 상태가 `PENDING_PAYMENT` → `PAID` → `ACTIVE`로 변경됩니다.

**⚠️ 중요 변경사항 (2024-01-XX):**
- 클라이언트가 `paymentAmount`를 보내면 서버 금액과 비교 검증합니다.
- 응답에 `orderStatus`와 `paidAt` 필드가 추가되었습니다.

**요청 헤더:**
```
Content-Type: application/json
Cookie: JSESSIONID=your-session-id
```

**요청 바디:**
```json
{
  "orderNo": 1,
  "paymentId": "test-payment-id-123",
  "paymentKey": "test-payment-key-456",
  "paymentAmount": 150000
}
```

**요청 파라미터:**
- `orderNo` (필수): 주문 번호
- `paymentId` (선택): 결제 ID (PG사에서 발급, 테스트용으로는 선택사항)
- `paymentKey` (선택): 결제 키 (PG사에서 발급, 테스트용으로는 선택사항)
- `paymentAmount` (선택): 클라이언트가 보낸 결제 금액 (보내면 서버 금액과 비교 검증)

**성공 응답 (200 OK):**
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

**응답 필드 설명:**
- `paymentNo`: 결제 번호 (paymentId)
- `orderNo`: 주문 번호 (orderId)
- `paymentAmount`: 결제 금액 (amount)
- `paymentStatus`: 결제 상태 (APPROVED, FAILED, CANCELED, PENDING)
- `orderStatus`: 주문 상태 (PENDING_PAYMENT, PAID, ACTIVE, PAYMENT_FAILED, CANCELLED)
- `paidAt`: 결제 승인 시점 (결제 성공 시에만 값 있음)

### 5.2 결제 실패 처리

**엔드포인트:** `POST /api/payments/{orderNo}/fail`

**설명:** 결제 실패를 처리합니다. 주문 상태가 `PENDING_PAYMENT` → `PAYMENT_FAILED`로 변경됩니다.

**요청 헤더:**
```
Content-Type: application/json
Cookie: JSESSIONID=your-session-id
```

**경로 변수:**
- `orderNo`: 주문 번호

**요청 바디 (선택):**
```json
"결제 카드 한도 초과"
```

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "paymentNo": 1,
    "orderNo": 1,
    "paymentAmount": 150000,
    "paymentMethod": "CARD",
    "paymentStatus": "FAILED",
    "orderStatus": "PAYMENT_FAILED",
    "paymentCreatedAt": "2024-01-15T10:30:00",
    "paidAt": null,
    "paymentCancelYn": false
  }
}
```

### 결제 API 주의사항

1. **주문 상태 전이:**
   - 결제 승인: `PENDING_PAYMENT` → `PAID` → `ACTIVE`
   - 결제 실패: `PENDING_PAYMENT` → `PAYMENT_FAILED`

2. **권한 검증:**
   - 결제 승인/실패는 해당 주문의 소유자(고객)만 가능합니다.

3. **중복 결제 방지 (강화):**
   - `PENDING_PAYMENT` 상태인 주문만 결제 승인/실패가 가능합니다.
   - 이미 `PAID` 또는 `ACTIVE` 상태인 주문은 "이미 결제가 완료된 주문입니다" 에러 반환
   - **Payment 레코드 존재 여부로도 체크**: 같은 orderId에 결제 성공 기록(`paidAt`이 있음)이 이미 있으면 재결제 차단
   - **조기 리턴**: 이미 결제 완료 상태면 에러 대신 기존 결제 정보를 즉시 반환

4. **금액 검증:**
   - 클라이언트가 `paymentAmount`를 보내면 서버의 주문 금액(`orderTotalPrice`)과 비교 검증합니다.
   - 금액이 일치하지 않으면 "결제 금액이 주문 금액과 일치하지 않습니다" 에러 반환
   - 클라이언트가 금액을 보내지 않아도 정상 처리됩니다 (서버 금액 사용)

5. **테스트 모드:**
   - 현재는 실제 PG사 연동 없이 결제 승인이 처리됩니다.

### 결제 승인 API 테스트 시나리오

#### 시나리오 1: 정상 결제 승인 (금액 없이)
```json
POST /api/payments/approve
{
  "orderNo": 1
}
```
**예상 결과:** 성공, 서버 금액으로 결제 처리

#### 시나리오 2: 정상 결제 승인 (금액 포함, 일치)
```json
POST /api/payments/approve
{
  "orderNo": 1,
  "paymentAmount": 150000
}
```
**예상 결과:** 성공 (주문 금액이 150000인 경우)

#### 시나리오 3: 금액 불일치 (에러)
```json
POST /api/payments/approve
{
  "orderNo": 1,
  "paymentAmount": 100000
}
```
**예상 결과:** 400 Bad Request
```json
{
  "success": false,
  "message": "결제 금액이 주문 금액과 일치하지 않습니다. 주문 금액: 150000, 요청 금액: 100000"
}
```

#### 시나리오 4: 중복 결제 시도 (에러)
```json
POST /api/payments/approve
{
  "orderNo": 1
}
```
**예상 결과:** 400 Bad Request (이미 결제된 주문인 경우)
```json
{
  "success": false,
  "message": "이미 결제가 완료된 주문입니다. (현재 상태: ACTIVE)"
}
```

#### 시나리오 5: 다른 사용자의 주문 결제 시도 (에러)
```json
POST /api/payments/approve
{
  "orderNo": 2
}
```
**예상 결과:** 403 Forbidden (다른 사용자의 주문인 경우)

---

## 6. 결제 조회 API

### 6.1 결제 번호로 결제 조회

**엔드포인트:** `GET /api/payments/{paymentNo}`

**설명:** 결제 번호로 결제 정보를 조회합니다.

**요청 헤더:**
```
Cookie: JSESSIONID=your-session-id
```

**경로 변수:**
- `paymentNo`: 결제 번호 (예: 1)

**성공 응답 (200 OK):**
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

**에러 응답 (404 Not Found):**
```json
{
  "success": false,
  "message": "결제 정보를 찾을 수 없습니다."
}
```

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "message": "접근 권한이 없습니다."
}
```
(다른 고객의 결제 정보 조회 시도 시)

---

### 6.2 주문 번호로 결제 조회

**엔드포인트:** `GET /api/orders/{orderNo}/payment`

**설명:** 주문 번호로 해당 주문의 결제 정보를 조회합니다.

**요청 헤더:**
```
Cookie: JSESSIONID=your-session-id
```

**경로 변수:**
- `orderNo`: 주문 번호 (예: 1)

**성공 응답 (200 OK):**
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

**에러 응답 (404 Not Found):**
```json
{
  "success": false,
  "message": "이 주문에 대한 결제 정보가 없습니다."
}
```
(결제가 아직 진행되지 않은 주문인 경우)

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "message": "접근 권한이 없습니다."
}
```
(다른 고객의 주문 조회 시도 시)

---

### 결제 조회 API 주의사항

1. **권한 검증:**
   - 본인의 결제 정보만 조회 가능합니다.
   - 다른 고객의 결제 정보 조회 시도 시 403 Forbidden 에러 반환

2. **결제 정보 없음:**
   - 아직 결제가 진행되지 않은 주문(`PENDING_PAYMENT` 상태)은 결제 정보가 없을 수 있습니다.
   - 이 경우 404 Not Found 에러 반환

3. **사용 사례:**
   - 디버깅: 결제 문제 발생 시 결제 정보 확인
   - 관리자 화면: 주문 상세에서 결제 정보 표시
   - 주문 상세 페이지: 고객이 자신의 주문 결제 정보 확인

---

## 10. 주문 관리 API (Admin/Partner용)

**⚠️ 중요: Order와 OrderItem의 관계 및 상태 관리**

### 엔티티 관계 구조

```
Order (주문) 1:N OrderItem (주문 상품)
├─ Order 1:1 Payment (결제 정보)
└─ OrderItem 1:1 Delivery (배송 정보)
   OrderItem 1:1 Return (반품 정보)
   OrderItem 1:1 Review (리뷰 정보)
```

**관계 설명:**
- **Order (주문)**: 하나의 주문은 여러 주문 상품을 포함할 수 있음 (1:N)
- **OrderItem (주문 상품)**: 각 주문 상품은 하나의 주문에 속함 (N:1)
- **Payment (결제)**: 주문 전체에 대한 결제 정보 (Order 1:1)
- **Delivery (배송)**: 각 주문 상품마다 별도의 배송 정보 (OrderItem 1:1)
- **Return (반품)**: 각 주문 상품마다 별도의 반품 정보 (OrderItem 1:1)
- **Review (리뷰)**: 각 주문 상품마다 별도의 리뷰 정보 (OrderItem 1:1)

### 상태 관리 책임 분리

**Order.orderStatus (주문 전체의 진행 단계):**
- `PENDING_PAYMENT`: 결제 대기
- `PAID`: 결제 완료 (발주 확인 대기)
- `ACTIVE`: 주문 처리 중 (일부 또는 전체 상품 발주 확인됨)
- `CANCELLED`: 주문 취소
- **주의:** `COMPLETED` 상태는 없습니다. 구매 확정은 `OrderItem.completedAt`으로 관리합니다.

**OrderItem 상태 필드 (개별 주문 상품의 상세 상태):**
- `confirmedAt`: 발주 확인일시 (파트너/관리자가 발주 확인 시 설정)
- `completedAt`: 구매 확정일시 (고객이 구매 확정 시 설정)
- `delivery.deliveryStatus`: 배송 상태 (`READY`, `SHIPPED`, `DELIVERED`)
- `returnEntity.returnStatus`: 반품 상태 (`REQUESTED`, `APPROVED`, `REJECTED`, `PICKUP_COMPLETED`, `REFUNDED`)

### 상태 전이 규칙

- **발주 확인**: 모든 주문 상품이 발주 확인(`confirmedAt` 설정)되면 → `Order.orderStatus`가 `PAID` → `ACTIVE`로 자동 변경
- **구매 확정**: 구매 확정(`OrderItem.completedAt` 설정)은 `Order.orderStatus`와 무관하게 각 주문 상품별로 독립적으로 처리
- **배송/반품/리뷰**: 각 주문 상품별로 독립적으로 관리 (OrderItem 단위)

### API 변경 사항

**발주 확인:**
- ✅ **새로운 API (권장)**: `POST /api/orders/order-items/{orderItemNo}/confirm` - 주문 상품별 발주 확인
- ⚠️ **레거시 API**: `POST /api/orders/{orderNo}/confirm` - 주문 전체 발주 확인 (레거시 호환용, 사용 권장하지 않음)

**구매 확정:**
- ✅ **새로운 API**: `POST /api/orders/order-items/{orderItemNo}/complete` - 주문 상품별 구매 확정
- ❌ **기존 API 제거**: `POST /api/orders/{orderNo}/complete` - 주문 전체 구매 확정 (더 이상 사용 불가)

---

### 10.1 전체 주문 목록 조회 (Admin/Partner)

**엔드포인트:** `GET /api/orders/admin`

**설명:** 
- **관리자(ADMIN)**: 모든 주문 목록 조회 가능
- **파트너(PARTNER)**: 자신의 상품 주문만 조회 가능 (option이 있는 경우 option의 partner, option이 없는 경우 product의 partner로 필터링)

**요청 헤더:**
```
Cookie: JSESSIONID=세션ID (관리자 또는 파트너 로그인 후 받은 세션 ID)
```

**성공 응답 (200 OK) - 관리자:**
```json
{
  "success": true,
  "data": [
    {
      "orderNo": 1,
      "orderTotalPrice": 75000,
      "orderCreatedAt": "2024-01-15T10:30:00",
      "orderStatus": "ACTIVE",
      "recipientName": "홍길동",
      "recipientPhone": "010-1234-5678",
      "deliveryAddress": "서울시 강남구 테헤란로 123",
      "paymentMethod": "CARD",
      "paymentAmount": 75000,
      "orderItems": [...]
    }
  ]
}
```

**성공 응답 (200 OK) - 파트너:**
```json
{
  "success": true,
  "data": [
    {
      "orderNo": 1,
      "orderTotalPrice": 75000,
      "orderCreatedAt": "2024-01-15T10:30:00",
      "orderStatus": "ACTIVE",
      "recipientName": "홍길동",
      "recipientPhone": "010-1234-5678",
      "deliveryAddress": "서울시 강남구 테헤란로 123",
      "paymentMethod": "CARD",
      "paymentAmount": 75000,
      "orderItems": [...]
    }
  ]
}
```
(파트너는 자신의 상품 주문만 조회됨)

---

### 10.2 주문 상세 조회 (Admin/Partner)

**엔드포인트:** `GET /api/orders/{orderNo}/admin`

**설명:** 주문 번호로 주문 상세 정보를 조회합니다. 주문 정보, 결제 정보, 배송 정보가 모두 포함됩니다.

**요청 헤더:**
```
Cookie: JSESSIONID=세션ID (관리자 또는 파트너 로그인 후 받은 세션 ID)
```

**경로 변수:**
- `orderNo`: 주문 번호 (예: 1)

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "orderNo": 1,
    "orderTotalPrice": 75000,
    "orderCreatedAt": "2024-01-15T10:30:00",
    "orderStatus": "ACTIVE",
    "recipientName": "홍길동",
    "recipientPhone": "010-1234-5678",
    "deliveryAddress": "서울시 강남구 테헤란로 123",
    "deliveryAddressDetail": "456호",
    "deliveryZipCode": "06234",
    "paymentNo": 1,
    "paymentMethod": "CARD",
    "paymentAmount": 75000,
    "paymentCreatedAt": "2024-01-15T10:30:00",
    "paidAt": "2024-01-15T10:30:05",
    "orderItems": [
      {
        "orderItemNo": 1,
        "productNo": 1,
        "productName": "실리콘 수영모자",
        "productImageUrl": "/images/products/caps/silicone-cap-1.jpg",
        "optionNo": 1,
        "color": "블랙",
        "size": "M",
        "quantity": 2,
        "itemPrice": 25000,
        "itemTotalPrice": 50000,
        "deliveryNo": 1,
        "deliveryStatus": "READY",
        "deliveryTrackingNumber": null,
        "deliveryCourier": null
      }
    ]
  }
}
```

**권한 검증:**
- **관리자**: 모든 주문 조회 가능
- **파트너**: 자신의 상품 주문만 조회 가능
- **고객**: 본인 주문만 조회 가능 (이 API 사용 불가, `/api/orders/{orderNo}` 사용)

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "자신의 상품 주문만 조회할 수 있습니다."
}
```
(파트너가 다른 파트너의 주문 조회 시도)

---

### 10.3 주문 상품별 발주 확인 (파트너/관리자)

**엔드포인트:** `POST /api/orders/order-items/{orderItemNo}/confirm`

**설명:** 결제 완료된 주문 상품을 발주 확인하여 배송을 생성합니다. 각 파트너가 자신의 상품만 발주 확인할 수 있으며, 모든 주문 상품이 발주 확인되면 주문 상태가 `PAID` → `ACTIVE`로 자동 변경됩니다.

**요청 헤더:**
```
Cookie: JSESSIONID=세션ID (관리자 또는 파트너 로그인 후 받은 세션 ID)
```

**경로 변수:**
- `orderItemNo`: 주문 상품 번호 (예: 1)

**요청 바디:** 없음

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "orderItemNo": 1,
    "productNo": 1,
    "productName": "실리콘 수영모자",
    "productImageUrl": "/images/products/cap-1.jpg",
    "optionNo": 1,
    "color": "빨강",
    "size": "M",
    "quantity": 1,
    "itemPrice": 25000,
    "itemTotalPrice": 25000,
    "isCancelled": false,
    "confirmedAt": "2024-01-15T10:30:00",
    "deliveryNo": 1,
    "deliveryStatus": "READY"
  }
}
```

**권한 검증:**
- **관리자**: 모든 주문 상품 발주 확인 가능
- **파트너**: 자신의 상품 주문만 발주 확인 가능
- **고객**: 발주 확인 불가 (403 Forbidden)

**발주 확인 조건:**
- 주문 상태가 `PAID` (결제 완료) 상태여야 함
- 아직 발주 확인되지 않은 주문 상품이어야 함
- 파트너는 자신의 상품만 발주 확인 가능

**주문 상태 자동 변경:**
- 모든 주문 상품이 발주 확인되면 주문 상태가 자동으로 `PAID` → `ACTIVE`로 변경됨
- 각 주문 상품별로 개별 발주 확인 가능

**에러 응답 (400 Bad Request):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "결제 완료된 주문 상품만 발주 확인할 수 있습니다. 현재 주문 상태: 주문 진행중"
}
```
(주문 상태가 `PAID`가 아닌 경우)

**에러 응답 (400 Bad Request):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "이미 발주 확인된 주문 상품입니다."
}
```
(이미 발주 확인된 경우)

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "자신의 상품 주문만 발주 확인할 수 있습니다."
}
```
(파트너가 다른 파트너의 주문 상품 발주 확인 시도)

**예시 시나리오:**
```
주문 #1 (PAID 상태)
├─ 상품 A (파트너 A) → 파트너 A가 발주 확인 → 배송 생성, confirmedAt 설정
└─ 상품 B (파트너 B) → 파트너 B가 발주 확인 → 배송 생성, confirmedAt 설정

모든 상품 발주 확인 완료 → 주문 상태 자동 변경: PAID → ACTIVE
```

---

### 10.3.1 주문 전체 발주 확인 (레거시 호환)

**엔드포인트:** `POST /api/orders/{orderNo}/confirm`

**설명:** 결제 완료된 주문을 발주 확인하여 주문 처리 시작합니다. 주문 상태가 `PAID` → `ACTIVE`로 변경되고, 각 주문 아이템에 대해 배송이 생성됩니다 (`READY` 상태). 모든 주문 상품의 `confirmedAt`이 설정됩니다.

**⚠️ 주의:** 이 API는 레거시 호환을 위해 유지되지만, **주문 상품별 발주 확인 API 사용을 권장합니다.** (`POST /api/orders/order-items/{orderItemNo}/confirm`)

**주요 동작:**
- 모든 주문 상품의 `OrderItem.confirmedAt` 설정
- 주문 상태 전이: `PAID` → `ACTIVE`
- 각 주문 상품에 대해 배송 생성 (`READY` 상태)

**요청 헤더:**
```
Cookie: JSESSIONID=세션ID (관리자 또는 파트너 로그인 후 받은 세션 ID)
```

**경로 변수:**
- `orderNo`: 주문 번호 (예: 1)

**요청 바디:** 없음

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "orderNo": 1,
    "orderStatus": "ACTIVE",
    "orderItems": [
      {
        "orderItemNo": 1,
        "productName": "실리콘 수영모자",
        "productImageUrl": "/images/products/cap-1.jpg",
        "optionNo": 1,
        "color": "빨강",
        "size": "M",
        "quantity": 1,
        "itemPrice": 25000,
        "itemTotalPrice": 25000,
        "isCancelled": false,
        "confirmedAt": "2024-01-15T10:30:00",
        "deliveryNo": 1,
        "deliveryStatus": "READY"
      }
    ]
  }
}
```

**응답 필드 설명:**
- `confirmedAt`: 발주 확인일시 (레거시 API 사용 시에도 모든 주문 상품에 설정됨)
- `deliveryStatus`: 배송 상태 (발주 확인 시 `READY`로 생성됨)

**권한 검증:**
- **관리자**: 모든 주문 발주 확인 가능
- **파트너**: 자신의 상품 주문만 발주 확인 가능
- **고객**: 발주 확인 불가 (403 Forbidden)

**에러 응답 (400 Bad Request):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "결제 완료된 주문만 발주 확인할 수 있습니다. 현재 상태: 주문 진행중"
}
```
(`PAID` 상태가 아닌 경우)

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "자신의 상품 주문만 발주 확인할 수 있습니다."
}
```
(파트너가 다른 파트너의 주문 발주 확인 시도)

---

### 10.4 주문 상태 변경 (관리자만)

**엔드포인트:** `PATCH /api/orders/{orderNo}/status`

**설명:** 주문 상태를 변경합니다. **관리자만 가능**하며, 예외 상황 처리용으로 사용됩니다 (결제 실패 재시도, 주문 취소 등).

**⚠️ 중요:**
- **파트너는 주문 상태 변경 불가** (발주 확인 API만 사용)
- **구매 확정된 주문 상품이 있거나 취소(`CANCELLED`)된 주문은 상태 변경 불가**
- **주의:** 구매 확정은 `OrderItem.completedAt`으로 관리합니다. `Order.orderStatus`에는 `COMPLETED` 상태가 없습니다.

**요청 헤더:**
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**경로 변수:**
- `orderNo`: 주문 번호 (예: 1)

**요청 바디:**
```json
{
  "orderStatus": "CANCELLED"
}
```

**요청 파라미터:**
- `orderStatus` (필수): 변경할 주문 상태
  - `PENDING_PAYMENT`: 결제 대기
  - `PAID`: 결제 완료
  - `ACTIVE`: 주문 진행중
  - `PAYMENT_FAILED`: 결제 실패
  - `CANCELLED`: 주문 취소
  - **주의:** `COMPLETED` 상태는 없습니다. 구매 확정은 `OrderItem.completedAt`으로 관리합니다.

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "orderNo": 1,
    "orderStatus": "CANCELLED",
    "orderTotalPrice": 75000,
    "orderCreatedAt": "2024-01-15T10:30:00",
    "orderItems": [...]
  }
}
```

**권한 검증:**
- **관리자**: 모든 주문 상태 변경 가능
- **파트너**: 주문 상태 변경 불가 (403 Forbidden)
- **고객**: 주문 상태 변경 불가 (403 Forbidden)

**상태 전이 규칙:**
- `PENDING_PAYMENT` → `PAID`, `PAYMENT_FAILED`, `CANCELLED`만 가능
- `PAID` → `ACTIVE`, `CANCELLED`만 가능
- `ACTIVE` → `CANCELLED`만 가능 (취소는 배송 시작 전만)
- `PAYMENT_FAILED` → `PENDING_PAYMENT`만 가능 (재시도)
- `CANCELLED` → 변경 불가
- **주의:** 구매 확정은 `OrderItem.completedAt`으로 관리하므로 `Order.orderStatus`에는 `COMPLETED` 상태가 없습니다.

**에러 응답 (400 Bad Request):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "결제 대기 상태에서는 결제 완료, 결제 실패, 취소만 가능합니다. 요청한 상태: 주문 진행중"
}
```
(허용되지 않은 상태 전이인 경우)

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "관리자만 접근 가능합니다."
}
```
(파트너 또는 고객이 주문 상태 변경 시도)

**에러 응답 (400 Bad Request):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "구매 확정된 주문의 상태는 변경할 수 없습니다."
}
```
(`COMPLETED` 또는 `CANCELLED` 상태인 주문 변경 시도)

---

### 주문 관리 API 주의사항

1. **권한별 접근 범위:**
   - **관리자(ADMIN)**: 모든 주문 조회 및 상태 변경 가능
   - **파트너(PARTNER)**: 자신의 상품 주문만 조회 가능, 발주 확인 가능, **주문 상태 변경 불가**
   - **고객(CUSTOMER)**: 본인 주문만 조회 가능 (이 API 사용 불가)

2. **발주 확인 vs 주문 상태 변경:**
   - **발주 확인** (`POST /api/orders/order-items/{orderItemNo}/confirm`): 파트너/관리자 모두 가능, 주문 상품별 발주 확인 + 배송 생성, 모든 상품 발주 확인 시 `PAID` → `ACTIVE` 자동 전환
   - **주문 상태 변경** (`PATCH /api/orders/{orderNo}/status`): 관리자만 가능, 예외 상황 처리용

3. **주문 상태 변경 제한:**
   - 구매 확정(`COMPLETED`) 또는 취소(`CANCELLED`)된 주문은 상태 변경 불가
   - 상태 전이 규칙에 따라 허용된 상태로만 변경 가능

4. **파트너 주문 필터링:**
   - 옵션이 있는 상품: `OrderItem.option.partner.partnerId`로 필터링
   - 옵션이 없는 상품: `OrderItem.product.partner.partnerId`로 필터링

---

---

## 11. 배송 관리 API (Admin/Partner용)

### ⚠️ 중요: Postman에서 세션 쿠키 설정 방법

배송 API는 세션 기반 인증을 사용합니다. Postman에서 테스트할 때는 반드시 세션 쿠키를 설정해야 합니다.

#### 방법 1: 헤더에 직접 Cookie 추가 (권장)

1. **파트너/관리자 로그인**:
   ```
   POST http://localhost:8080/api/auth/login
   Body: { "email": "partner1@swim-mall.com", "password": "partner123" }
   ```

2. **응답 헤더에서 세션 ID 확인**:
   - 응답의 `Headers` 탭에서 `Set-Cookie` 헤더 확인
   - 예: `Set-Cookie: JSESSIONID=A1B2C3D4E5F6G7H8; Path=/; HttpOnly`
   - `JSESSIONID=A1B2C3D4E5F6G7H8` 부분만 복사

3. **배송 API 요청 시 헤더에 추가**:
   - `Headers` 탭에서 `Cookie` 키 추가
   - 값: `JSESSIONID=A1B2C3D4E5F6G7H8` (복사한 세션 ID)

#### 방법 2: Postman Cookie Manager 사용 (자동 관리)

1. **Postman 설정**:
   - `File` → `Settings` → `General` 탭
   - `Automatically follow redirects` 체크 해제 (필요시)
   - `Send cookies` 체크 확인

2. **로그인 요청**:
   ```
   POST http://localhost:8080/api/auth/login
   ```
   - 응답 후 Postman이 자동으로 쿠키 저장

3. **배송 API 요청**:
   - 같은 도메인(`localhost:8080`)에서 요청 시 자동으로 쿠키 전송
   - `Headers` 탭에서 `Cookie` 헤더 확인 가능

#### 방법 3: Postman Variables 사용 (권장 - 여러 요청 관리)

1. **환경 변수 설정**:
   - Postman 우측 상단 `Environments` 클릭
   - `+` 버튼으로 새 환경 생성 (예: "Swim Mall Local")
   - 변수 추가:
     - Variable: `sessionId`
     - Initial Value: (비워둠)
     - Current Value: (비워둠)

2. **로그인 요청에 Test Script 추가**:
   ```javascript
   // 로그인 응답 후 세션 ID 추출
   const cookies = pm.response.headers.get("Set-Cookie");
   if (cookies) {
       const sessionIdMatch = cookies.match(/JSESSIONID=([^;]+)/);
       if (sessionIdMatch) {
           pm.environment.set("sessionId", sessionIdMatch[1]);
           console.log("Session ID saved:", sessionIdMatch[1]);
       }
   }
   ```

3. **배송 API 요청 헤더 설정**:
   - `Headers` 탭에서 `Cookie` 키 추가
   - 값: `JSESSIONID={{sessionId}}`

#### 🔍 문제 해결

**문제**: 프론트엔드에서는 작동하는데 Postman에서는 안 됨

**원인**:
- Postman에서 세션 쿠키(`JSESSIONID`)를 전달하지 않음
- 세션이 만료됨 (일정 시간 후 자동 만료)
- 로그인하지 않았거나 다른 사용자로 로그인함

**해결**:
1. ✅ 파트너/관리자로 먼저 로그인
2. ✅ 응답 헤더에서 `JSESSIONID` 복사
3. ✅ 배송 API 요청 헤더에 `Cookie: JSESSIONID=복사한값` 추가
4. ✅ 세션이 만료되면 다시 로그인

**확인 방법**:
- 배송 API 요청 시 `Headers` 탭에서 `Cookie` 헤더가 있는지 확인
- 응답이 `403 Forbidden` 또는 `401 Unauthorized`면 세션 문제

---

### 11.1 전체 배송 목록 조회 (Admin/Partner)

**엔드포인트:** `GET /api/deliveries`

**설명:** 
- **관리자(ADMIN)**: 모든 배송 목록 조회 가능
- **파트너(PARTNER)**: 자신의 상품 배송만 조회 가능 (option이 있는 경우 option의 partner, option이 없는 경우 product의 partner로 필터링)

**요청 헤더:**
```
Cookie: JSESSIONID=세션ID (관리자 또는 파트너 로그인 후 받은 세션 ID)
```

**⚠️ 주의**: 반드시 파트너 또는 관리자로 로그인한 후 받은 세션 ID를 사용해야 합니다.

**성공 응답 (200 OK) - 관리자:**
```json
{
  "success": true,
  "data": [
    {
      "deliveryNo": 1,
      "orderNo": 1,
      "orderItemNo": 1,
      "productNo": 1,
      "productName": "실리콘 수영모자",
      "productImageUrl": "/images/products/caps/silicone-cap-1.jpg",
      "optionNo": 1,
      "color": "블랙",
      "size": "M",
      "quantity": 2,
      "itemPrice": 25000,
      "itemTotalPrice": 50000,
      "deliveryStatus": "READY",
      "deliveryStartDate": null,
      "deliveryEndDate": null,
      "deliveryTrackingNumber": null,
      "deliveryCourier": null
    },
    {
      "deliveryNo": 2,
      "orderNo": 1,
      "orderItemNo": 2,
      "productNo": 2,
      "productName": "여성 원피스 수영복",
      "productImageUrl": "/images/products/womens/one-piece-1.jpg",
      "optionNo": 3,
      "color": "블랙",
      "size": "L",
      "quantity": 1,
      "itemPrice": 89000,
      "itemTotalPrice": 89000,
      "deliveryStatus": "SHIPPED",
      "deliveryStartDate": "2024-01-15T10:30:00",
      "deliveryEndDate": null,
      "deliveryTrackingNumber": "1234567890",
      "deliveryCourier": "CJ대한통운"
    }
  ]
}
```

**성공 응답 (200 OK) - 파트너:**
```json
{
  "success": true,
  "data": [
    {
      "deliveryNo": 1,
      "orderNo": 1,
      "orderItemNo": 1,
      "productNo": 1,
      "productName": "실리콘 수영모자",
      "productImageUrl": "/images/products/caps/silicone-cap-1.jpg",
      "optionNo": 1,
      "color": "블랙",
      "size": "M",
      "quantity": 2,
      "itemPrice": 25000,
      "itemTotalPrice": 50000,
      "deliveryStatus": "READY",
      "deliveryStartDate": null,
      "deliveryEndDate": null,
      "deliveryTrackingNumber": null,
      "deliveryCourier": null
    }
  ]
}
```
(파트너는 자신의 상품 배송만 조회됨)

**응답 필드 설명:**
- `productNo`: 상품 번호
- `productName`: 상품명
- `productImageUrl`: 상품 이미지 URL (프론트엔드에서 이미지 표시용)
- `optionNo`: 옵션 번호 (옵션이 없는 상품의 경우 `null`)
- `color`: 색상 (옵션이 없는 상품의 경우 `null`)
- `size`: 사이즈 (옵션이 없는 상품의 경우 `null`)
- `quantity`: 주문 수량
- `itemPrice`: 단위 가격 (주문 당시 가격)
- `itemTotalPrice`: 총 가격 (수량 * 단위가격 - 할인금액)

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "관리자 또는 파트너만 접근 가능합니다."
}
```
(고객(CUSTOMER)이 접근 시도 시)

---

### 11.2 배송 번호로 배송 조회

**엔드포인트:** `GET /api/deliveries/{deliveryNo}`

**설명:** 배송 번호로 특정 배송 정보를 조회합니다.

**요청 헤더:**
```
Cookie: JSESSIONID=세션ID (관리자, 파트너, 또는 고객 로그인 후 받은 세션 ID)
```

**경로 변수:**
- `deliveryNo`: 배송 번호 (예: 1)

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "deliveryNo": 1,
    "orderNo": 1,
    "orderItemNo": 1,
    "productNo": 1,
    "productName": "실리콘 수영모자",
    "optionNo": 1,
    "color": "블랙",
    "size": "M",
    "deliveryStatus": "READY",
    "deliveryStartDate": null,
    "deliveryEndDate": null,
    "deliveryTrackingNumber": null,
    "deliveryCourier": null
  }
}
```

**권한 검증:**
- **관리자**: 모든 배송 조회 가능
- **파트너**: 자신의 상품 배송만 조회 가능
- **고객**: 본인 주문의 배송만 조회 가능

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "접근 권한이 없습니다."
}
```
(파트너가 다른 파트너의 배송 조회 시도, 또는 고객이 다른 고객의 배송 조회 시도)

**에러 응답 (404 Not Found):**
```json
{
  "success": false,
  "code": "DELIVERY_NOT_FOUND",
  "message": "배송 정보를 찾을 수 없습니다."
}
```

---

### 11.3 배송 시작 (READY → SHIPPED)

**엔드포인트:** `PATCH /api/deliveries/{deliveryNo}/start`

**설명:** 배송을 시작합니다. 상태가 `READY`에서 `SHIPPED`로 변경되고, 송장번호와 택배사 정보가 저장됩니다.

**요청 헤더:**
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (관리자 또는 파트너 로그인 후 받은 세션 ID)
```

**경로 변수:**
- `deliveryNo`: 배송 번호 (예: 1)

**요청 바디:**
```json
{
  "trackingNumber": "1234567890",
  "courier": "CJ대한통운"
}
```

**요청 파라미터:**
- `trackingNumber` (필수): 배송 송장번호
- `courier` (필수): 배송 택배사 (예: "CJ대한통운", "한진택배", "로젠택배" 등)

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "deliveryNo": 1,
    "orderNo": 1,
    "orderItemNo": 1,
    "productNo": 1,
    "productName": "실리콘 수영모자",
    "productImageUrl": "/images/products/caps/silicone-cap-1.jpg",
    "optionNo": 1,
    "color": "블랙",
    "size": "M",
    "quantity": 2,
    "itemPrice": 25000,
    "itemTotalPrice": 50000,
    "deliveryStatus": "SHIPPED",
    "deliveryStartDate": "2024-01-15T10:30:00",
    "deliveryEndDate": null,
    "deliveryTrackingNumber": "1234567890",
    "deliveryCourier": "CJ대한통운"
  }
}
```

**권한 검증:**
- **관리자**: 모든 배송 시작 가능
- **파트너**: 자신의 상품 배송만 시작 가능
- **고객**: 배송 시작 불가 (403 Forbidden)

**에러 응답 (400 Bad Request):**
```json
{
  "success": false,
  "code": "INVALID_DELIVERY_STATUS",
  "message": "배송 시작은 READY 상태에서만 가능합니다."
}
```
(이미 SHIPPED 또는 DELIVERED 상태인 경우)

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "접근 권한이 없습니다."
}
```
(파트너가 다른 파트너의 배송 시작 시도, 또는 고객이 배송 시작 시도)

---

### 11.4 배송 완료 (SHIPPED → DELIVERED)

**엔드포인트:** `PATCH /api/deliveries/{deliveryNo}/complete`

**설명:** 배송을 완료합니다. 상태가 `SHIPPED`에서 `DELIVERED`로 변경되고, 배송 완료일이 설정됩니다.

**요청 헤더:**
```
Cookie: JSESSIONID=세션ID (관리자 또는 파트너 로그인 후 받은 세션 ID)
```

**경로 변수:**
- `deliveryNo`: 배송 번호 (예: 1)

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "deliveryNo": 1,
    "orderNo": 1,
    "orderItemNo": 1,
    "productNo": 1,
    "productName": "실리콘 수영모자",
    "productImageUrl": "/images/products/caps/silicone-cap-1.jpg",
    "optionNo": 1,
    "color": "블랙",
    "size": "M",
    "quantity": 2,
    "itemPrice": 25000,
    "itemTotalPrice": 50000,
    "deliveryStatus": "DELIVERED",
    "deliveryStartDate": "2024-01-15T10:30:00",
    "deliveryEndDate": "2024-01-17T14:20:00",
    "deliveryTrackingNumber": "1234567890",
    "deliveryCourier": "CJ대한통운"
  }
}
```

**권한 검증:**
- **관리자**: 모든 배송 완료 가능
- **파트너**: 자신의 상품 배송만 완료 가능
- **고객**: 배송 완료 불가 (403 Forbidden)

**에러 응답 (400 Bad Request):**
```json
{
  "success": false,
  "code": "INVALID_DELIVERY_STATUS",
  "message": "배송 완료는 SHIPPED 상태에서만 가능합니다."
}
```
(READY 상태이거나 이미 DELIVERED 상태인 경우)

---

### 11.5 배송 정보 수정 (송장번호, 택배사)

**엔드포인트:** `PATCH /api/deliveries/{deliveryNo}`

**설명:** 배송 송장번호와 택배사 정보를 수정합니다. (배송 시작 후 수정 가능)

**요청 헤더:**
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (관리자 또는 파트너 로그인 후 받은 세션 ID)
```

**경로 변수:**
- `deliveryNo`: 배송 번호 (예: 1)

**요청 바디:**
```json
{
  "trackingNumber": "9876543210",
  "courier": "한진택배"
}
```

**요청 파라미터:**
- `trackingNumber` (선택): 배송 송장번호 (null 가능)
- `courier` (선택): 배송 택배사 (null 가능)

**성공 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "deliveryNo": 1,
    "orderNo": 1,
    "orderItemNo": 1,
      "productNo": 1,
      "productName": "실리콘 수영모자",
      "productImageUrl": "/images/products/caps/silicone-cap-1.jpg",
      "optionNo": 1,
      "color": "블랙",
      "size": "M",
      "quantity": 2,
      "itemPrice": 25000,
      "itemTotalPrice": 50000,
      "deliveryStatus": "SHIPPED",
      "deliveryStartDate": "2024-01-15T10:30:00",
      "deliveryEndDate": null,
      "deliveryTrackingNumber": "9876543210",
      "deliveryCourier": "한진택배"
  }
}
```

**권한 검증:**
- **관리자**: 모든 배송 정보 수정 가능
- **파트너**: 자신의 상품 배송 정보만 수정 가능
- **고객**: 배송 정보 수정 불가 (403 Forbidden)

**에러 응답 (403 Forbidden):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "접근 권한이 없습니다."
}
```

---

## 배송 관리 API 테스트 순서 (Admin/Partner)

### Step 1: 관리자 로그인
```
POST /api/auth/login
Body: {
  "email": "admin@swim-mall.com",
  "password": "admin123"
}
→ adminSessionId 기록
```

### Step 2: 전체 배송 목록 조회 (관리자)
```
GET /api/deliveries
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
→ 모든 배송 목록 확인
```

### Step 3: 파트너 로그인
```
POST /api/auth/login
Body: {
  "email": "partner1@swim-mall.com",
  "password": "partner123"
}
→ partnerSessionId 기록
```

### Step 4: 전체 배송 목록 조회 (파트너)
```
GET /api/deliveries
Headers:
  Cookie: JSESSIONID={{partnerSessionId}}
→ 자신의 상품 배송만 조회됨 (option이 있는 경우 option의 partner, option이 없는 경우 product의 partner로 필터링)
```

### Step 5: 배송 시작 (파트너)
```
PATCH /api/deliveries/{deliveryNo}/start
Headers:
  Cookie: JSESSIONID={{partnerSessionId}}
Body: {
  "trackingNumber": "1234567890",
  "courier": "CJ대한통운"
}
→ 상태: READY → SHIPPED
```

### Step 6: 배송 정보 수정 (파트너)
```
PATCH /api/deliveries/{deliveryNo}
Headers:
  Cookie: JSESSIONID={{partnerSessionId}}
Body: {
  "trackingNumber": "9876543210",
  "courier": "한진택배"
}
→ 송장번호와 택배사 수정
```

### Step 7: 배송 완료 (파트너)
```
PATCH /api/deliveries/{deliveryNo}/complete
Headers:
  Cookie: JSESSIONID={{partnerSessionId}}
→ 상태: SHIPPED → DELIVERED
```

---

## 배송 관리 API 주의사항

1. **권한별 접근 범위:**
   - **관리자(ADMIN)**: 모든 배송 조회 및 수정 가능
   - **파트너(PARTNER)**: 자신의 상품 배송만 조회 및 수정 가능
     - 옵션이 있는 상품: `option.partner`로 필터링
     - 옵션이 없는 상품: `product.partner`로 필터링
   - **고객(CUSTOMER)**: 본인 주문의 배송만 조회 가능 (수정 불가)

2. **배송 상태 전이:**
   - `READY` → `SHIPPED` → `DELIVERED` 순서만 가능
   - 역방향 전이 불가

3. **배송 시작 시 필수 정보:**
   - `trackingNumber`: 배송 송장번호 (필수)
   - `courier`: 배송 택배사 (필수)

4. **배송 정보 수정:**
   - 배송 시작 후(`SHIPPED` 또는 `DELIVERED` 상태)에만 수정 가능
   - `trackingNumber`와 `courier` 모두 선택 사항 (null 가능)

5. **파트너 배송 필터링:**
   - 옵션이 있는 상품: `OrderItem.option.partner.partnerId`로 필터링
   - 옵션이 없는 상품: `OrderItem.product.partner.partnerId`로 필터링
   - 따라서 `ProductEntity`에 `partner` 필드가 설정되어 있어야 옵션이 없는 상품도 파트너에게 보입니다.

---

## 주의사항

1. **세션 필요**: 파트너 상품 등록/수정/삭제는 파트너 로그인 후 세션이 필요합니다.
2. **고객 세션 필요**: 주문 생성/조회는 고객 로그인 후 세션이 필요합니다.
3. **ProductType/ProductSubType**: enum 값은 대문자로 정확히 입력해야 합니다.
4. **optionName 자동 생성**: color와 size가 있으면 자동으로 `"색상 / 사이즈"` 형식으로 생성됩니다.
5. **create 모드**: `spring.jpa.hibernate.ddl-auto=create`로 설정하면 서버 재시작 시 모든 데이터가 삭제되고 새로 생성됩니다.
6. **주문 생성 후 장바구니 삭제**: 주문 생성 시 해당 장바구니 아이템들은 자동으로 삭제됩니다.
7. **결제 승인**: 주문 생성 후 자동으로 결제 승인이 처리됩니다 (프론트엔드에서).
8. **Product.partner 필드**: 옵션이 없는 단일 상품의 경우 `ProductEntity`에 `partner` 필드를 설정해야 파트너가 해당 상품의 배송을 조회할 수 있습니다.

---

## 11. 반품 관리 API (고객/관리자/파트너용)

### 인증 방법
모든 반품 API는 세션 기반 인증을 사용합니다. 먼저 로그인하여 `JSESSIONID` 쿠키를 받아야 합니다.

**Postman에서 세션 쿠키 사용 방법:**
1. **방법 1: Cookie 헤더에 직접 추가**
   ```
   Cookie: JSESSIONID=세션ID값
   ```

2. **방법 2: Postman Cookie Manager 사용**
   - Postman 하단의 "Cookies" 탭 클릭
   - `localhost:8080` 도메인 선택
   - "Add Cookie" 클릭
   - Name: `JSESSIONID`, Value: 세션ID값 입력

3. **방법 3: Environment Variable 사용**
   - Postman에서 Environment 생성
   - Variable: `JSESSIONID`, Initial Value: (비워둠)
   - 로그인 후 받은 세션ID를 변수에 저장
   - 헤더에서 `Cookie: JSESSIONID={{JSESSIONID}}` 사용

### Step 1: 고객 로그인
```
POST /api/auth/login
Body: {
  "email": "customer1@swim-mall.com",
  "password": "customer123"
}
→ customerSessionId 기록 (Response Headers의 Set-Cookie에서 JSESSIONID 추출)
```

### Step 2: 주문 생성 및 배송 완료까지 진행
반품 신청을 하려면 배송이 완료된 주문이 필요합니다.
1. 주문 생성 (고객)
2. 발주 확인 (파트너/관리자)
3. 배송 시작 (파트너/관리자)
4. 배송 완료 (파트너/관리자)

### Step 3: 반품 신청 (고객용)
사전 준비(조건부): `DEFECT`·`WRONG_ITEM`·`OTHER`는 증빙 이미지가 필수이므로 먼저 업로드 후 `fileId`를 확보합니다. `CHANGE_OF_MIND`·`ORDER_MISTAKE`는 이미지 없이 신청할 수 있습니다.

```
POST /api/files/upload
Headers:
  Cookie: JSESSIONID={{customerSessionId}}
Body (form-data):
  file: [이미지 파일]
  category: return-image
```

업로드 응답의 `data.fileId`를 반품 요청의 `imageFileIds`에 전달하면 반품 이미지로 연결됩니다.

```
POST /api/returns
Headers:
  Cookie: JSESSIONID={{customerSessionId}}
Body: {
  "orderItemNo": 1,
  "returnReasonType": "CHANGE_OF_MIND",
  "returnReason": "",
  "imageFileIds": [101, 102]
}
```

**요청 바디 필드:**
- `orderItemNo` (필수): 주문 아이템 번호
- `returnReasonType` (필수): 반품 사유 유형 — `CHANGE_OF_MIND` | `ORDER_MISTAKE` | `DEFECT` | `WRONG_ITEM` | `OTHER`
- `returnReason` (조건부): 상세 사유. `DEFECT`·`WRONG_ITEM`·`OTHER`일 때 필수(서버 검증: 불량/쇼핑몰 측 오배송 5자 이상, 기타 10자 이상). `CHANGE_OF_MIND`·`ORDER_MISTAKE`는 선택. (`OTHER`도 별도 코드값 없이 이 필드에만 입력하면 됨.)
- `imageFileIds` (조건부): `/api/files/upload` (`category=return-image`)로 업로드한 파일 ID 목록(최대 5장). `DEFECT`·`WRONG_ITEM`·`OTHER`일 때 **최소 1장 필수**. `CHANGE_OF_MIND`·`ORDER_MISTAKE`는 **생략 가능**(`[]` 전송).

**위험도 스냅샷 (`returnRiskScore` / `returnRiskTier`):**
- 반품 **신청 시점**에 계산되어 응답과 DB에 저장되는 값입니다(이후 정책이 바뀌어도 과거 건은 변하지 않음).
- **등급**: 점수 70 이상 `HIGH`, 40 이상 `MEDIUM`, 그 미만 `LOW`.
- **점수 구성(요약)**:
  - 사유 유형: `CHANGE_OF_MIND`·`ORDER_MISTAKE` → 0, `OTHER` → 15, `DEFECT`·`WRONG_ITEM` → 30
  - 반품 금액(해당 주문 아이템 총액)이 10만 원 이상이면 +20
  - 해당 고객 기준 **최근 90일** 반품 신청 건수가 **2건 이상**이면 +25(이번 요청 직전까지의 건수로 판단)
- **동일 배송지·180일·주소 반복 등은 점수에 반영하지 않습니다.** (과거에 논의되던 지표는 제거됨.)

**반품 신청 조건:**
- 배송 완료(DELIVERED) 상태여야 함
- 주문 진행중(ACTIVE) 상태여야 함 (구매 확정은 OrderItem.completedAt으로 관리)
- 구매 확정되지 않은 주문 상품이어야 함 (`OrderItem.completedAt`이 null이어야 함)
- 이미 반품 신청이 없어야 함

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "returnNo": 1,
    "orderItemNo": 1,
    "orderNo": 1,
    "productNo": 1,
    "productName": "테스트 수영모자",
    "productImageUrl": "/images/products/test-cap.jpg",
    "optionNo": 1,
    "color": "빨강",
    "size": "M",
    "quantity": 1,
    "itemPrice": 25000,
    "itemTotalPrice": 25000,
    "returnStatus": "REQUESTED",
    "returnRequestedAt": "2024-01-15T10:30:00",
    "returnReasonType": "CHANGE_OF_MIND",
    "returnRiskScore": 0,
    "returnRiskTier": "LOW",
    "returnReason": null,
    "returnAmount": 25000,
    "returnTrackingNumber": null,
    "returnCourier": null,
    "imageUrls": [
      "http://localhost:8080/uploads/return-image/xxxx-xxxx-xxxx.png"
    ]
  }
}
```

### Step 4: 고객 반품 목록 조회
```
GET /api/returns/customer
Headers:
  Cookie: JSESSIONID={{customerSessionId}}
```

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "returnNo": 1,
      "orderItemNo": 1,
      "orderNo": 1,
      "productName": "테스트 수영모자",
      "returnStatus": "REQUESTED",
      "returnRequestedAt": "2024-01-15T10:30:00",
      "returnReason": "상품이 마음에 들지 않습니다.",
      "returnAmount": 25000
    }
  ]
}
```

### Step 5: 반품 상세 조회
```
GET /api/returns/1
Headers:
  Cookie: JSESSIONID={{customerSessionId}}
```

**주의:** `{returnNo}`를 실제 반품 번호로 치환하세요. 예: `/api/returns/1`, `/api/returns/2` 등

**권한:**
- 고객: 본인 반품만 조회 가능
- 관리자: 모든 반품 조회 가능
- 파트너: 자신의 상품 반품만 조회 가능

### Step 6: 관리자/파트너 로그인
```
POST /api/auth/login
Body: {
  "email": "admin@swim-mall.com",  // 또는 partner1@swim-mall.com
  "password": "admin123"  // 또는 partner123
}
→ adminSessionId 또는 partnerSessionId 기록
```

### Step 7: 전체 반품 목록 조회 (관리자/파트너용)
```
GET /api/returns
Headers:
  Cookie: JSESSIONID={{adminSessionId}}  // 또는 {{partnerSessionId}}
```

**권한별 조회 범위:**
- **관리자**: 모든 반품 조회
- **파트너**: 자신의 상품 반품만 조회
  - 옵션이 있는 상품: `option.partner`로 필터링
  - 옵션이 없는 상품: `product.partner`로 필터링

### Step 8: 반품 승인 (관리자/파트너용)
```
POST /api/returns/1/approve
Headers:
  Cookie: JSESSIONID={{adminSessionId}}  // 또는 {{partnerSessionId}}
```

**주의:** `{returnNo}`를 실제 반품 번호로 치환하세요. 예: `/api/returns/1/approve`, `/api/returns/2/approve` 등

**상태 전이:**
- `REQUESTED` → `APPROVED`로 변경
- 반품 신청 상태인 반품만 승인 가능

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "returnNo": 1,
    "returnStatus": "APPROVED",
    "returnRequestedAt": "2024-01-15T10:30:00",
    "returnReason": "상품이 마음에 들지 않습니다.",
    "returnAmount": 25000
  }
}
```

### Step 9: 반품 거절 (관리자/파트너용)
```
POST /api/returns/1/reject
Headers:
  Cookie: JSESSIONID={{adminSessionId}}  // 또는 {{partnerSessionId}}
Body: {
  "rejectionReason": "반품 기간이 지났습니다."  // 선택 (없으면 기본 메시지 사용)
}
```

**주의:** `{returnNo}`를 실제 반품 번호로 치환하세요. 예: `/api/returns/1/reject`, `/api/returns/2/reject` 등

**상태 전이:**
- `REQUESTED` → `REJECTED`: 신청 단계에서 거절 (예: 반품 기간 초과 등)
- `PICKUP_COMPLETED` → `REJECTED`: 수거 후 상품 확인 결과 이상으로 거절
- `APPROVED` 상태에서는 거절 불가 (승인 = 반품을 받겠다는 의미이므로 수거 진행해야 함)
- 거절된 반품은 더 이상 변경 불가

**거절 사유:**
- `rejectionReason`을 제공하지 않으면 기본 메시지 사용
  - `REQUESTED` 상태: "반품 거절"
  - `PICKUP_COMPLETED` 상태: "상품 확인 결과 반품 불가 (상품 상태 불량)"

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "returnNo": 1,
    "returnStatus": "REJECTED",
    "returnRequestedAt": "2024-01-15T10:30:00",
    "returnReason": "상품이 마음에 들지 않습니다.",
    "rejectionReason": "반품 기간이 지났습니다.",
    "returnAmount": 25000
  }
}
```

### Step 10: 반품 상태 변경 - 수거 완료 (관리자/파트너용)
```
PATCH /api/returns/1
Headers:
  Cookie: JSESSIONID={{adminSessionId}}  // 또는 {{partnerSessionId}}
Body: {
  "returnStatus": "PICKUP_COMPLETED",
  "returnTrackingNumber": "9876543210",  // 선택
  "returnCourier": "한진택배"  // 선택
}
```

**주의:** `{returnNo}`를 실제 반품 번호로 치환하세요. 예: `/api/returns/1`, `/api/returns/2` 등

**상태 전이:**
- `APPROVED` → `PICKUP_COMPLETED`만 가능
- 승인된 반품만 수거 완료 처리 가능

### Step 11: 반품 상태 변경 - 환불 완료 (관리자/파트너용)
```
PATCH /api/returns/1
Headers:
  Cookie: JSESSIONID={{adminSessionId}}  // 또는 {{partnerSessionId}}
Body: {
  "returnStatus": "REFUNDED"
}
```

**주의:** `{returnNo}`를 실제 반품 번호로 치환하세요. 예: `/api/returns/1`, `/api/returns/2` 등

**상태 전이:**
- `PICKUP_COMPLETED` → `REFUNDED`: 수거 후 상품 확인 결과 정상 → 환불 완료
- `REFUNDED` 상태에서는 더 이상 변경 불가

**참고:** 수거 완료 후 상품에 이상이 있으면 `POST /api/returns/{returnNo}/reject`로 거절 처리

### Step 12: 반품 거절 (수거 후 상품 확인 결과 이상) (관리자/파트너용)
```
POST /api/returns/1/reject
Headers:
  Cookie: JSESSIONID={{adminSessionId}}  // 또는 {{partnerSessionId}}
Body: {
  "rejectionReason": "상품이 손상되어 반품 불가합니다."  // 선택
}
```

**주의:** `{returnNo}`를 실제 반품 번호로 치환하세요.

**상태 전이:**
- `PICKUP_COMPLETED` → `REJECTED`: 수거 후 상품 확인 결과 이상 → 반품 거절
- 거절된 반품은 더 이상 변경 불가

---

## 반품 관리 API 주의사항

1. **권한별 접근 범위:**
   - **고객(CUSTOMER)**: 본인 반품만 신청 및 조회 가능
   - **관리자(ADMIN)**: 모든 반품 조회 및 상태 변경 가능
   - **파트너(PARTNER)**: 자신의 상품 반품만 조회 및 상태 변경 가능
     - 옵션이 있는 상품: `option.partner`로 필터링
     - 옵션이 없는 상품: `product.partner`로 필터링

2. **반품 신청 조건:**
   - 배송 완료(DELIVERED) 상태여야 함
   - 주문 진행중(ACTIVE) 상태여야 함 (구매 확정은 OrderItem.completedAt으로 관리)
   - 구매 확정되지 않은 주문 상품이어야 함 (`OrderItem.completedAt`이 null이어야 함)
   - 이미 반품 신청이 없어야 함 (주문 아이템당 1개의 반품만 가능)

3. **반품 상태 전이:**
   ```
   REQUESTED (반품신청)
       ↓
       ├─→ APPROVED (반품승인) → PICKUP_COMPLETED (수거완료)
       │                           ↓ [상품 확인 후]
       │                           ├─→ REFUNDED (환불완료) [상품 정상]
       │                           └─→ REJECTED (반품거절) [상품 이상]
       └─→ REJECTED (반품거절) [신청 단계에서 거절]
   ```
   - `REQUESTED` → `APPROVED` 또는 `REJECTED`만 가능
   - `APPROVED` → `PICKUP_COMPLETED`만 가능 (승인 = 반품을 받겠다는 의미이므로 수거 진행해야 함)
   - `PICKUP_COMPLETED` → `REFUNDED` 또는 `REJECTED` 가능
     - 상품이 정상이면 → `REFUNDED` (환불 완료)
     - 상품에 이상이 있으면 → `REJECTED` (반품 거절)
   - `REJECTED` 상태에서는 더 이상 변경 불가 (두 가지 경우 모두)
   - `REFUNDED` 상태에서는 더 이상 변경 불가
   - 역방향 전이 불가

4. **반품 승인/거절:**
   - 반품 신청(`REQUESTED`) 후 관리자/파트너가 승인 또는 거절 처리 필요
   - 승인된 반품만 수거 진행 가능
   - 거절은 두 가지 경우만 가능:
     - 신청 단계에서 거절: `REQUESTED` → `REJECTED` (예: 반품 기간 초과)
     - 수거 후 거절: `PICKUP_COMPLETED` → `REJECTED` (예: 상품 상태 불량)
   - `APPROVED` 상태에서는 거절 불가 (승인 = 반품을 받겠다는 의미이므로 수거 진행해야 함)
   - 거절 사유(`rejectionReason`)는 선택적으로 입력 가능

5. **반품 금액:**
   - 반품 금액은 주문 아이템의 총 가격(`itemTotalPrice`)으로 자동 설정됩니다.

6. **반품 송장번호 및 택배사:**
   - 수거 완료 처리 시 선택적으로 입력 가능
   - 고객이 반품 상품을 보낼 때 사용하는 송장번호와 택배사 정보

---

## 12. 주문 취소 API (고객용)

### Step 1: 고객 로그인
```
POST /api/auth/login
Body: {
  "email": "customer1@swim-mall.com",
  "password": "customer123"
}
→ customerSessionId 기록
```

### Step 2: 주문 취소
```
PATCH /api/orders/1/cancel
Headers:
  Cookie: JSESSIONID={{customerSessionId}}
```

**주의:** `{orderNo}`를 실제 주문 번호로 치환하세요. 예: `/api/orders/1/cancel`, `/api/orders/2/cancel` 등

**주문 취소 조건:**
- 고객만 취소 가능 (본인 주문만)
- 다음 상태에서 취소 가능:
  - `PENDING_PAYMENT` (결제 대기)
  - `PAID` (결제 완료)
  - `ACTIVE` (주문 진행중) - 단, 배송이 시작되지 않은 경우만
- 배송이 시작된 경우(SHIPPED 또는 DELIVERED) 취소 불가
- 이미 취소된 주문은 취소 불가
- 구매 확정(COMPLETED)된 주문은 취소 불가

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "orderNo": 1,
    "orderStatus": "CANCELLED",
    "orderTotalPrice": 25000,
    "orderCreatedAt": "2024-01-15T10:00:00",
    "orderItems": [...]
  }
}
```

**에러 응답 예시 (배송 시작된 경우):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "배송이 시작된 주문은 취소할 수 없습니다.",
  "data": null
}
```

---

## 주문 취소 API 주의사항

1. **취소 가능한 상태:**
   - `PENDING_PAYMENT`: 언제든지 취소 가능
   - `PAID`: 언제든지 취소 가능
   - `ACTIVE`: 배송이 시작되지 않은 경우만 취소 가능
     - 배송이 없거나
     - 모든 배송이 `READY` 상태인 경우

2. **취소 불가능한 경우:**
   - 배송이 시작된 경우 (`SHIPPED` 또는 `DELIVERED` 상태)
   - 이미 취소된 주문
   - 구매 확정된 주문 (`COMPLETED` 상태)

3. **보안:**
   - 본인 주문만 취소 가능 (서버에서 검증)
   - 다른 사용자의 주문 취소 시도 시 403 Forbidden 반환

---

## 13. 주문 상품별 구매 확정 API (고객용)

### Step 1: 고객 로그인
```
POST /api/auth/login
Body: {
  "email": "customer1@swim-mall.com",
  "password": "customer123"
}
→ customerSessionId 기록
```

### Step 2: 주문 상품별 구매 확정
```
POST /api/orders/order-items/{orderItemNo}/complete
Headers:
  Cookie: JSESSIONID={{customerSessionId}}
```

**주의:** `{orderItemNo}`를 실제 주문 상품 번호로 치환하세요. 예: `/api/orders/order-items/1/complete`, `/api/orders/order-items/2/complete` 등

**구매 확정 조건:**
- 고객만 구매 확정 가능 (본인 주문 상품만)
- 배송 완료(`DELIVERED`)된 주문 상품만 구매 확정 가능
- 반품 진행 중이거나 환불 완료된 주문 상품은 구매 확정 불가
  - 반품 상태가 `REQUESTED`, `APPROVED`, `PICKUP_COMPLETED`, `REFUNDED`인 경우 불가
  - 반품 상태가 `REJECTED`인 경우만 구매 확정 가능
- 이미 구매 확정된 주문 상품은 구매 확정 불가

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "orderItemNo": 1,
    "productNo": 1,
    "productName": "테스트 수영모자",
    "productImageUrl": "/images/products/test-cap.jpg",
    "optionNo": 1,
    "color": "빨강",
    "size": "M",
    "quantity": 1,
    "itemPrice": 25000,
    "itemTotalPrice": 25000,
    "isCancelled": false,
    "completedAt": "2024-01-15T10:00:00",
    "deliveryStatus": "DELIVERED",
    "returnStatus": null
  }
}
```

**에러 응답 예시 (배송 미완료):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "배송 완료된 주문 상품만 구매 확정할 수 있습니다. 현재 배송 상태: SHIPPED",
  "data": null
}
```

**에러 응답 예시 (반품 진행 중):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "반품 진행 중이거나 환불 완료된 주문 상품은 구매 확정할 수 없습니다. 현재 반품 상태: REQUESTED",
  "data": null
}
```

**에러 응답 예시 (이미 구매 확정):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "이미 구매 확정된 주문 상품입니다.",
  "data": null
}
```

### 주문 상품별 구매 확정 API 주의사항

1. **구매 확정 가능한 조건:**
   - 배송 상태가 `DELIVERED` (배송 완료)
   - 반품 신청이 없거나, 반품 상태가 `REJECTED` (반품 거절)인 경우
   - 아직 구매 확정되지 않은 경우

2. **구매 확정 불가능한 경우:**
   - 배송이 완료되지 않은 경우 (`READY`, `SHIPPED` 상태)
   - 반품 진행 중인 경우 (`REQUESTED`, `APPROVED`, `PICKUP_COMPLETED` 상태)
   - 환불 완료된 경우 (`REFUNDED` 상태)
   - 이미 구매 확정된 경우

3. **여러 상품 주문 시:**
   - 각 주문 상품별로 개별 구매 확정 가능
   - 한 주문에 여러 상품이 있어도 각각 따로 구매 확정 처리
   - 예: 주문에 상품 A, B가 있을 때, A만 구매 확정하고 B는 나중에 구매 확정 가능

4. **보안:**
   - 본인 주문 상품만 구매 확정 가능 (서버에서 검증)
   - 다른 사용자의 주문 상품 구매 확정 시도 시 403 Forbidden 반환

5. **레거시 호환:**
   - 기존 주문 전체 구매 확정 API (`POST /api/orders/{orderNo}/complete`)는 제거되었습니다.
   - 주문 상품별 구매 확정 API (`POST /api/orders/order-items/{orderItemNo}/complete`)만 사용 가능합니다.

6. **주문 상태와의 관계:**
   - 구매 확정(`OrderItem.completedAt`)은 `Order.orderStatus`와 무관합니다.
   - `Order.orderStatus`에는 `COMPLETED` 상태가 없으며, 구매 확정 여부는 `OrderItem.completedAt`으로만 판단합니다.
   - 주문 상태는 주문 전체의 진행 단계만 관리합니다 (`PENDING_PAYMENT`, `PAID`, `ACTIVE`, `CANCELLED`).

---

## 14. 리뷰 작성 API (고객용)

### Step 1: 고객 로그인
```
POST /api/auth/login
Body: {
  "email": "customer1@swim-mall.com",
  "password": "customer123"
}
→ customerSessionId 기록
```

### Step 2: 리뷰 작성
```
POST /api/reviews
Headers:
  Content-Type: application/json
  Cookie: JSESSIONID={{customerSessionId}}
Body: {
  "orderItemNo": 1,
  "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
  "reviewRating": 5
}
```

**주의:** `{orderItemNo}`를 실제 주문 아이템 번호로 치환하세요.

**리뷰 작성 조건:**
- 고객만 리뷰 작성 가능 (본인 주문 상품만)
- 구매 확정된 주문 상품만 리뷰 작성 가능 (주문 상품의 `completedAt`이 설정된 경우)
- 같은 OrderItem에 리뷰 1개만 작성 가능 (중복 방지)
- 주문 상품이 구매 확정되지 않으면 리뷰 작성 불가

**요청 필드:**
- `orderItemNo` (필수): 주문 아이템 번호 (구매 인증, 중복 방지)
- `reviewContent` (필수): 리뷰 내용
- `reviewRating` (필수): 평점 (1~5)

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "reviewNo": 1,
    "orderItemNo": 1,
    "orderNo": 1,
    "productNo": 1,
    "productName": "테스트 수영모자",
    "productImageUrl": "/images/products/test-cap.jpg",
    "optionNo": 1,
    "color": "빨강",
    "size": "M",
    "customerId": 1,
    "customerName": "홍길동",
    "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
    "reviewRating": 5,
    "reviewCreatedAt": "2024-01-15T10:00:00"
  }
}
```

**에러 응답 예시 (구매 확정 안 된 경우):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "구매 확정된 주문 상품만 리뷰 작성이 가능합니다.",
  "data": null
}
```

**에러 응답 예시 (중복 리뷰):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "이미 리뷰를 작성한 주문 상품입니다.",
  "data": null
}
```

**에러 응답 예시 (본인 주문 아님):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "본인의 주문 상품만 리뷰 작성이 가능합니다.",
  "data": null
}
```

### Step 3: 내 리뷰 목록 조회
```
GET /api/reviews/customer
Headers:
  Cookie: JSESSIONID={{customerSessionId}}
```

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "reviewNo": 1,
      "orderItemNo": 1,
      "orderNo": 1,
      "productNo": 1,
      "productName": "테스트 수영모자",
      "productImageUrl": "/images/products/test-cap.jpg",
      "optionNo": 1,
      "color": "빨강",
      "size": "M",
      "customerId": 1,
      "customerName": "홍길동",
      "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
      "reviewRating": 5,
      "reviewCreatedAt": "2024-01-15T10:00:00"
    }
  ]
}
```

### Step 4: 상품별 리뷰 목록 조회
```
GET /api/reviews/product/1
```

**주의:** `{productNo}`를 실제 상품 번호로 치환하세요.

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "reviewNo": 1,
      "orderItemNo": 1,
      "orderNo": 1,
      "productNo": 1,
      "productName": "테스트 수영모자",
      "productImageUrl": "/images/products/test-cap.jpg",
      "optionNo": 1,
      "color": "빨강",
      "size": "M",
      "customerId": 1,
      "customerName": "홍길동",
      "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
      "reviewRating": 5,
      "reviewCreatedAt": "2024-01-15T10:00:00"
    }
  ]
}
```

### Step 5: 리뷰 상세 조회
```
GET /api/reviews/1
```

**주의:** `{reviewNo}`를 실제 리뷰 번호로 치환하세요.

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "reviewNo": 1,
    "orderItemNo": 1,
    "orderNo": 1,
    "productNo": 1,
    "productName": "테스트 수영모자",
    "productImageUrl": "/images/products/test-cap.jpg",
    "optionNo": 1,
    "color": "빨강",
    "size": "M",
    "customerId": 1,
    "customerName": "홍길동",
    "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
    "reviewRating": 5,
    "reviewCreatedAt": "2024-01-15T10:00:00"
  }
}
```

### Step 6: 전체 리뷰 목록 조회 (관리자용)
```
GET /api/reviews
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
```

**주의:** 관리자로 로그인한 세션 ID를 사용해야 합니다.

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "reviewNo": 1,
      "orderItemNo": 1,
      "orderNo": 1,
      "productNo": 1,
      "productName": "테스트 수영모자",
      "productImageUrl": "/images/products/test-cap.jpg",
      "optionNo": 1,
      "color": "빨강",
      "size": "M",
      "customerId": 1,
      "customerName": "홍길동",
      "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
      "reviewRating": 5,
      "reviewCreatedAt": "2024-01-15T10:00:00"
    }
  ]
}
```

---

## 리뷰 작성 API 주의사항

1. **리뷰 작성 조건:**
   - 구매 확정된 주문 상품만 리뷰 작성 가능 (주문 상품의 `completedAt`이 설정된 경우)
   - 주문 상품이 구매 확정되지 않으면 리뷰 작성 불가
   - 같은 OrderItem에 리뷰 1개만 작성 가능 (중복 방지)
   - 본인 주문 상품만 리뷰 작성 가능

2. **필수 필드:**
   - `orderItemNo`: 주문 아이템 번호 (구매 인증, 중복 방지)
   - `reviewContent`: 리뷰 내용
   - `reviewRating`: 평점 (1~5)

3. **리뷰 작성 단위:**
   - OrderItem 기준 1회 작성
   - 같은 주문에서 옵션/수량이 다르면 각각 리뷰 작성 가능

4. **보안:**
   - 본인 주문 상품만 리뷰 작성 가능 (서버에서 검증)
   - 다른 사용자의 주문 상품에 리뷰 작성 시도 시 403 Forbidden 반환
   - 고객만 리뷰 작성 가능 (관리자/파트너는 불가)

5. **리뷰 조회:**
   - 고객: 본인 리뷰만 조회 가능 (`/api/reviews/customer`)
   - 상품별 리뷰: 누구나 조회 가능 (`/api/reviews/product/{productNo}`)
   - 관리자: 전체 리뷰 조회 가능 (`/api/reviews`)
   - 파트너: 자신의 상품 리뷰만 조회 가능 (`/api/reviews/partner`)

---

## 14. 리뷰 관리 API (관리자/파트너용)

### 14-1. 리뷰 삭제 (관리자만)

#### Step 1: 관리자 로그인
```
POST /api/auth/login
Body: {
  "email": "admin@swim-mall.com",
  "password": "admin123"
}
→ adminSessionId 기록
```

#### Step 2: 리뷰 삭제
```
DELETE /api/reviews/{reviewNo}
Headers:
  Cookie: JSESSIONID={{adminSessionId}}
```

**주의:** `{reviewNo}`를 실제 리뷰 번호로 치환하세요.

**권한:** 관리자만 삭제 가능

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": "리뷰가 삭제되었습니다."
}
```

**에러 응답 예시 (권한 없음):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "관리자만 리뷰 삭제가 가능합니다.",
  "data": null
}
```

**에러 응답 예시 (리뷰 없음):**
```json
{
  "success": false,
  "code": "REVIEW_NOT_FOUND",
  "message": "리뷰를 찾을 수 없습니다.",
  "data": null
}
```

---

### 14-2. 파트너 리뷰 조회 (파트너용)

#### Step 1: 파트너 로그인
```
POST /api/auth/login
Body: {
  "email": "partner1@swim-mall.com",
  "password": "partner123"
}
→ partnerSessionId 기록
```

#### Step 2: 자신의 상품 리뷰 조회
```
GET /api/reviews/partner
Headers:
  Cookie: JSESSIONID={{partnerSessionId}}
```

**권한:** 파트너만 조회 가능 (자신의 상품 리뷰만)

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "reviewNo": 1,
      "orderItemNo": 1,
      "orderNo": 1,
      "productNo": 1,
      "productName": "테스트 수영모자",
      "productImageUrl": "/images/products/test-cap.jpg",
      "optionNo": 1,
      "color": "빨강",
      "size": "M",
      "customerId": 1,
      "customerName": "홍길동",
      "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
      "reviewRating": 5,
      "reviewCreatedAt": "2024-01-15T10:00:00",
      "reviewReply": null,
      "reviewReplyCreatedAt": null
    }
  ]
}
```

**에러 응답 예시 (권한 없음):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "파트너만 자신의 상품 리뷰 조회가 가능합니다.",
  "data": null
}
```

---

### 14-3. 리뷰 답변 작성 (파트너만)

#### Step 1: 파트너 로그인
```
POST /api/auth/login
Body: {
  "email": "partner1@swim-mall.com",
  "password": "partner123"
}
→ partnerSessionId 기록
```

#### Step 2: 리뷰 답변 작성
```
POST /api/reviews/{reviewNo}/reply
Headers:
  Content-Type: application/json
  Cookie: JSESSIONID={{partnerSessionId}}
Body: {
  "reviewReply": "감사합니다! 좋은 평가 감사드립니다. 앞으로도 더 좋은 상품으로 찾아뵙겠습니다."
}
```

**주의:** 
- `{reviewNo}`를 실제 리뷰 번호로 치환하세요.
- 본인의 상품 리뷰에만 답변 작성 가능
- 이미 답변이 있는 경우 수정 API 사용

**권한:** 파트너만 답변 작성 가능 (본인 상품 리뷰만)

**요청 필드:**
- `reviewReply` (필수): 답변 내용 (최대 1000자)

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "reviewNo": 1,
    "orderItemNo": 1,
    "orderNo": 1,
    "productNo": 1,
    "productName": "테스트 수영모자",
    "productImageUrl": "/images/products/test-cap.jpg",
    "optionNo": 1,
    "color": "빨강",
    "size": "M",
    "customerId": 1,
    "customerName": "홍길동",
    "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
    "reviewRating": 5,
    "reviewCreatedAt": "2024-01-15T10:00:00",
    "reviewReply": "감사합니다! 좋은 평가 감사드립니다. 앞으로도 더 좋은 상품으로 찾아뵙겠습니다.",
    "reviewReplyCreatedAt": "2024-01-15T11:00:00"
  }
}
```

**에러 응답 예시 (권한 없음):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "파트너만 리뷰 답변 작성이 가능합니다.",
  "data": null
}
```

**에러 응답 예시 (본인 상품 아님):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "본인의 상품 리뷰에만 답변을 작성할 수 있습니다.",
  "data": null
}
```

**에러 응답 예시 (이미 답변 있음):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "이미 답변이 작성된 리뷰입니다. 수정 기능을 사용해주세요.",
  "data": null
}
```

---

### 14-4. 리뷰 답변 수정 (파트너만)

#### Step 1: 파트너 로그인
```
POST /api/auth/login
Body: {
  "email": "partner1@swim-mall.com",
  "password": "partner123"
}
→ partnerSessionId 기록
```

#### Step 2: 리뷰 답변 수정
```
PUT /api/reviews/{reviewNo}/reply
Headers:
  Content-Type: application/json
  Cookie: JSESSIONID={{partnerSessionId}}
Body: {
  "reviewReply": "수정된 답변 내용입니다. 감사합니다!"
}
```

**주의:** 
- `{reviewNo}`를 실제 리뷰 번호로 치환하세요.
- 본인의 상품 리뷰에만 답변 수정 가능
- 답변이 없는 경우 작성 API 사용

**권한:** 파트너만 답변 수정 가능 (본인 상품 리뷰만)

**요청 필드:**
- `reviewReply` (필수): 수정할 답변 내용 (최대 1000자)

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "reviewNo": 1,
    "orderItemNo": 1,
    "orderNo": 1,
    "productNo": 1,
    "productName": "테스트 수영모자",
    "productImageUrl": "/images/products/test-cap.jpg",
    "optionNo": 1,
    "color": "빨강",
    "size": "M",
    "customerId": 1,
    "customerName": "홍길동",
    "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
    "reviewRating": 5,
    "reviewCreatedAt": "2024-01-15T10:00:00",
    "reviewReply": "수정된 답변 내용입니다. 감사합니다!",
    "reviewReplyCreatedAt": "2024-01-15T11:00:00"
  }
}
```

**에러 응답 예시 (답변 없음):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "작성된 답변이 없습니다. 작성 기능을 사용해주세요.",
  "data": null
}
```

---

### 14-5. 리뷰 답변 삭제 (파트너만)

#### Step 1: 파트너 로그인
```
POST /api/auth/login
Body: {
  "email": "partner1@swim-mall.com",
  "password": "partner123"
}
→ partnerSessionId 기록
```

#### Step 2: 리뷰 답변 삭제
```
DELETE /api/reviews/{reviewNo}/reply
Headers:
  Cookie: JSESSIONID={{partnerSessionId}}
```

**주의:** `{reviewNo}`를 실제 리뷰 번호로 치환하세요.

**권한:** 파트너만 답변 삭제 가능 (본인 상품 리뷰만)

**응답 예시:**
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "reviewNo": 1,
    "orderItemNo": 1,
    "orderNo": 1,
    "productNo": 1,
    "productName": "테스트 수영모자",
    "productImageUrl": "/images/products/test-cap.jpg",
    "optionNo": 1,
    "color": "빨강",
    "size": "M",
    "customerId": 1,
    "customerName": "홍길동",
    "reviewContent": "상품이 정말 좋아요! 배송도 빨랐습니다.",
    "reviewRating": 5,
    "reviewCreatedAt": "2024-01-15T10:00:00",
    "reviewReply": null,
    "reviewReplyCreatedAt": null
  }
}
```

**에러 응답 예시 (답변 없음):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "삭제할 답변이 없습니다.",
  "data": null
}
```

---

## 리뷰 관리 API 주의사항

1. **리뷰 삭제 (관리자만):**
   - 관리자만 리뷰 삭제 가능
   - 파트너는 리뷰 삭제 불가 (리뷰 조작 방지)
   - 삭제된 리뷰는 복구 불가

2. **파트너 리뷰 조회:**
   - 파트너는 자신의 상품 리뷰만 조회 가능
   - 다른 파트너의 상품 리뷰는 조회 불가
   - 관리자는 전체 리뷰 조회 가능 (`/api/reviews`)

3. **리뷰 답변 (파트너만):**
   - 파트너만 자신의 상품 리뷰에 답변 작성/수정/삭제 가능
   - 본인의 상품 리뷰에만 답변 가능 (다른 파트너 상품 리뷰 불가)
   - 답변은 1개만 작성 가능 (수정/삭제로 관리)
   - 답변 작성일시는 최초 작성 시점 유지 (수정 시에도 변경되지 않음)

4. **권한 검증:**
   - 모든 API는 서버에서 권한 검증 수행
   - 관리자/파트너가 아닌 경우 403 Forbidden 반환
   - 본인 상품이 아닌 경우 403 Forbidden 반환

5. **답변 필드:**
   - `reviewReply`: 답변 내용 (nullable, 최대 1000자)
   - `reviewReplyCreatedAt`: 답변 작성일시 (nullable)
   - 답변이 없는 경우 두 필드 모두 `null`

---

## 15. 고객 마이페이지 관리 API

### 15-1. 프로필 정보 수정

#### 엔드포인트
```
PUT http://localhost:8080/api/customer/me
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

#### 요청 바디 예시
```json
{
  "customerName": "홍길동"
}
```

**주의사항:**
- 이메일은 수정 불가 (보안상 계정 식별자 역할)
- 생년월일은 수정 불가 (결제 시스템과 연관된 법적 요구사항 및 데이터 무결성 유지)
- `customerName`: 필수, 고객 이름만 수정 가능

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "customerId": 1,
    "customerName": "홍길동",
    "customerEmail": "customer1@swim-mall.com",
    "customerBirth": "1990-01-15",
    "customerCreateAt": "2024-01-01T10:00:00"
  }
}
```

**에러 응답 예시 (로그인 안 됨):**
```json
{
  "success": false,
  "code": "CUSTOMER_NOT_FOUND",
  "message": "고객을 찾을 수 없습니다.",
  "data": null
}
```

---

### 15-2. 비밀번호 변경

#### 엔드포인트
```
PUT http://localhost:8080/api/customer/password
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

#### 요청 바디 예시
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**주의사항:**
- `currentPassword`: 필수, 현재 비밀번호
- `newPassword`: 필수, 새 비밀번호 (최소 8자 이상)
- `confirmPassword`: 필수, 새 비밀번호 확인 (newPassword와 일치해야 함)

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": "비밀번호가 변경되었습니다."
}
```

**에러 응답 예시 (현재 비밀번호 불일치):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "현재 비밀번호가 일치하지 않습니다.",
  "data": null
}
```

**에러 응답 예시 (비밀번호 확인 불일치):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "새 비밀번호와 확인 비밀번호가 일치하지 않습니다.",
  "data": null
}
```

---

### 15-3. 주소 목록 조회

#### 엔드포인트
```
GET http://localhost:8080/api/customer/addresses
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "addressNo": 1,
      "recipientName": "홍길동",
      "recipientPhone": "010-1234-5678",
      "deliveryAddress": "서울특별시 강남구 테헤란로 123",
      "deliveryAddressDetail": "101동 101호",
      "deliveryZipCode": "06142",
      "isDefault": true,
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:00:00"
    },
    {
      "addressNo": 2,
      "recipientName": "홍길동",
      "recipientPhone": "010-9876-5432",
      "deliveryAddress": "서울특별시 서초구 서초대로 456",
      "deliveryAddressDetail": "202동 202호",
      "deliveryZipCode": "06511",
      "isDefault": false,
      "createdAt": "2024-01-05T14:30:00",
      "updatedAt": "2024-01-05T14:30:00"
    }
  ]
}
```

**주의사항:**
- 기본 주소(`isDefault: true`)가 먼저 정렬되고, 그 다음 생성일시 내림차순으로 정렬됩니다.
- 현재 로그인한 고객의 주소만 조회됩니다.

---

### 15-4. 주소 추가

#### 엔드포인트
```
POST http://localhost:8080/api/customer/addresses
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

#### 요청 바디 예시
```json
{
  "recipientName": "홍길동",
  "recipientPhone": "010-1234-5678",
  "deliveryAddress": "서울특별시 강남구 테헤란로 123",
  "deliveryAddressDetail": "101동 101호",
  "deliveryZipCode": "06142",
  "isDefault": true
}
```

**필수 필드:**
- `recipientName`: 수령인 이름
- `recipientPhone`: 수령인 전화번호
- `deliveryAddress`: 배송지 주소

**선택 필드:**
- `deliveryAddressDetail`: 배송지 상세 주소
- `deliveryZipCode`: 우편번호
- `isDefault`: 기본 주소 여부 (기본값: `false`)

**주의사항:**
- `isDefault`를 `true`로 설정하면, 기존 기본 주소는 자동으로 해제됩니다.
- 한 고객당 하나의 기본 주소만 유지됩니다.

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "addressNo": 1,
    "recipientName": "홍길동",
    "recipientPhone": "010-1234-5678",
    "deliveryAddress": "서울특별시 강남구 테헤란로 123",
    "deliveryAddressDetail": "101동 101호",
    "deliveryZipCode": "06142",
    "isDefault": true,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
}
```

---

### 15-5. 주소 수정

#### 엔드포인트
```
PUT http://localhost:8080/api/customer/addresses/{addressNo}
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

**주의:** `{addressNo}`를 실제 주소 번호로 치환하세요.

#### 요청 바디 예시
```json
{
  "recipientName": "홍길동",
  "recipientPhone": "010-9999-8888",
  "deliveryAddress": "서울특별시 강남구 테헤란로 456",
  "deliveryAddressDetail": "201동 201호",
  "deliveryZipCode": "06143",
  "isDefault": false
}
```

**필수 필드:**
- `recipientName`: 수령인 이름
- `recipientPhone`: 수령인 전화번호
- `deliveryAddress`: 배송지 주소

**선택 필드:**
- `deliveryAddressDetail`: 배송지 상세 주소
- `deliveryZipCode`: 우편번호
- `isDefault`: 기본 주소 여부

**주의사항:**
- 본인의 주소만 수정할 수 있습니다.
- `isDefault`를 `true`로 변경하면, 기존 기본 주소는 자동으로 해제됩니다.

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "addressNo": 1,
    "recipientName": "홍길동",
    "recipientPhone": "010-9999-8888",
    "deliveryAddress": "서울특별시 강남구 테헤란로 456",
    "deliveryAddressDetail": "201동 201호",
    "deliveryZipCode": "06143",
    "isDefault": false,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-10T15:30:00"
  }
}
```

**에러 응답 예시 (본인 주소 아님):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "본인의 주소만 수정할 수 있습니다.",
  "data": null
}
```

---

### 15-6. 주소 삭제

#### 엔드포인트
```
DELETE http://localhost:8080/api/customer/addresses/{addressNo}
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (고객 로그인 후 받은 세션 ID)
```

**주의:** `{addressNo}`를 실제 주소 번호로 치환하세요.

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": "주소가 삭제되었습니다."
}
```

**에러 응답 예시 (본인 주소 아님):**
```json
{
  "success": false,
  "code": "FORBIDDEN",
  "message": "본인의 주소만 삭제할 수 있습니다.",
  "data": null
}
```

**에러 응답 예시 (주소 없음):**
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "주소를 찾을 수 없습니다.",
  "data": null
}
```

---

## 고객 마이페이지 관리 API 주의사항

1. **프로필 정보 수정:**
   - 이메일은 수정 불가 (보안상 계정 식별자 역할)
   - 생년월일은 수정 불가 (결제 시스템과 연관된 법적 요구사항 및 데이터 무결성 유지)
   - 이름만 수정 가능
   - 로그인한 고객만 본인 정보 수정 가능

2. **비밀번호 변경:**
   - 현재 비밀번호 확인 필수
   - 새 비밀번호는 최소 8자 이상
   - 새 비밀번호와 확인 비밀번호가 일치해야 함

3. **주소 관리:**
   - 본인의 주소만 조회/추가/수정/삭제 가능
   - 기본 주소는 한 고객당 하나만 유지
   - 기본 주소로 설정하면 기존 기본 주소는 자동 해제
   - 주소 삭제 시 복구 불가

4. **권한 검증:**
   - 모든 API는 서버에서 권한 검증 수행
   - 로그인하지 않은 경우 401 Unauthorized 반환
   - 본인 데이터가 아닌 경우 403 Forbidden 반환

---

## 16. 파트너 입점 신청 API

### 16-1. 파트너 입점 신청

#### 엔드포인트
```
POST http://localhost:8080/api/partner/apply
```

#### 헤더
```
Content-Type: application/json
```

**주의:** 로그인 없이도 신청 가능 (비회원/회원 모두 가능)

#### 요청 바디 예시
```json
{
  "email": "newpartner@example.com",
  "partnerName": "새로운 수영용품샵",
  "partnerContact": "010-1234-5678",
  "partnerBankAccount": "111-222-456789",
  "businessRegistrationNumber": "123-45-67890",
  "representativeBrandCode": "SPEEDO"
}
```

#### 필수 필드
- `email`: 파트너 이메일 (중복 불가)
- `partnerName`: 파트너명 (상점명)
- `partnerContact`: 연락처 (형식: 010-XXXX-XXXX)
- `partnerBankAccount`: 정산계좌 (형식: XXX-XXX-XXXXXX)
- `businessRegistrationNumber`: 사업자등록번호 (형식: XXX-XX-XXXXX)
- `representativeBrandCode`: 대표 브랜드 코드 (신청 시 저장, 승인 시 `brand.code` 자동 생성/활성화)

**주의:** 비밀번호는 신청 시 입력하지 않음. 승인 시 임시 비밀번호가 이메일로 발송됨

#### 성공 응답 예시 (201 Created)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "partnerId": 3,
    "partnerName": "새로운 수영용품샵",
    "partnerContact": "010-1234-5678",
    "partnerBankAccount": "111-222-456789",
    "representativeBrandCode": "SPEEDO",
    "partnerStatus": "PENDING",
    "partnerApprovedAt": null,
    "email": "newpartner@example.com"
  }
}
```

#### 에러 응답 예시 (이메일 중복)
```json
{
  "success": false,
  "code": "DUPLICATE_EMAIL",
  "message": "이미 사용 중인 이메일입니다.",
  "data": null
}
```

#### 에러 응답 예시 (이미 신청 중)
```json
{
  "success": false,
  "code": "DUPLICATE_EMAIL",
  "message": "이미 신청 중인 이메일입니다.",
  "data": null
}
```

#### 에러 응답 예시 (유효성 검증 실패)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "연락처 형식은 010-XXXX-XXXX입니다.",
  "data": null
}
```

**주의사항:**
- 신청 시 Account는 생성되지 않음 (승인 시에만 생성)
- 신청 후 PENDING 상태로 관리자 검토 대기
- 승인 시 임시 비밀번호가 이메일로 발송됨
- 거절 시 거절 사유가 이메일로 발송됨

---

### 16-x. 파트너 프로필(저위험 즉시 수정)

- 조회
```
GET /api/partner/profile
```

- 수정(즉시 반영)
```
PATCH /api/partner/profile
Body: {
  "contactPersonName": "홍길동",
  "customerServicePhone": "010-1111-2222",
  "profileImageUrl": "http://localhost:8080/uploads/public/profile/a.png",
  "introduction": "파트너 소개 문구"
}
```

### 16-y. 파트너 고위험 정보 수정 신청(관리자 승인)

- 파트너 신청 생성
```
POST /api/partner/profile-change-requests
Body: {
  "requestedPartnerName": "Swimming Store",
  "requestedPartnerBankAccount": "123-456-789012",
  "requestedBusinessRegistrationNumber": "123-45-67890",
  "requestedRepresentativeBrandCode": "SWIMMING_STORE",
  "requestReason": "브랜드/정산 정보 변경"
}
```

- 파트너 본인 신청 목록
```
GET /api/partner/profile-change-requests
```

- 관리자 목록/승인/거절
```
GET /api/admin/partners/change-requests?status=PENDING
PATCH /api/admin/partners/change-requests/{requestId}/approve
PATCH /api/admin/partners/change-requests/{requestId}/reject
Body: { "rejectReason": "증빙서류 불충분" }
```

정책:
- 저위험 정보: 즉시 수정
- 고위험 정보(상호/정산계좌/사업자번호/대표브랜드): 수정 신청 후 관리자 승인 시 반영

---

### 16-2. 파트너 신청 목록 조회 (관리자용)

#### 엔드포인트
```
GET http://localhost:8080/api/admin/partners/pending
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** 관리자만 접근 가능 (현재는 권한 체크 주석 처리됨)

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "partnerId": 3,
      "partnerName": "새로운 수영용품샵",
      "partnerContact": "010-1234-5678",
      "partnerBankAccount": "111-222-456789",
      "businessRegistrationNumber": "123-45-67890",
      "partnerStatus": "PENDING",
      "partnerApprovedAt": null,
      "email": "newpartner@example.com"
    },
    {
      "partnerId": 4,
      "partnerName": "비치웨어 전문점",
      "partnerContact": "010-9876-5432",
      "partnerBankAccount": "333-444-789012",
      "partnerStatus": "PENDING",
      "partnerApprovedAt": null,
      "email": "beachwear@example.com"
    }
  ]
}
```

**주의사항:**
- PENDING 상태인 파트너만 조회됨
- 승인된 파트너(APPROVED), 거절된 파트너(REJECTED), 비활성화된 파트너(INACTIVE)는 목록에 포함되지 않음

---

### 16-3. 파트너 승인 (관리자용)

#### 엔드포인트
```
PATCH http://localhost:8080/api/admin/partners/{partnerId}/approve
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

**브랜드 자동 생성 정책:** 승인 시 `partner.representativeBrandCode`가 `brand`에 없으면 자동 생성됩니다.

#### 요청 바디
없음 (경로 변수만 사용)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "partnerId": 3,
    "partnerName": "새로운 수영용품샵",
      "partnerContact": "010-1234-5678",
      "partnerBankAccount": "111-222-456789",
      "businessRegistrationNumber": "123-45-67890",
      "partnerStatus": "APPROVED",
      "partnerApprovedAt": "2024-01-15T10:30:00",
      "email": "newpartner@example.com"
  }
}
```

#### 에러 응답 예시 (파트너 없음)
```json
{
  "success": false,
  "code": "PARTNER_NOT_FOUND",
  "message": "파트너를 찾을 수 없습니다.",
  "data": null
}
```

#### 에러 응답 예시 (이미 승인됨)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "대기 중인 파트너만 승인할 수 있습니다. 현재 상태: 승인",
  "data": null
}
```

**주의사항:**
- 승인 시 Account가 자동 생성됨
- 임시 비밀번호가 생성되어 파트너 이메일로 발송됨
- 파트너는 이메일로 받은 임시 비밀번호로 로그인 가능
- 승인 후 파트너 상태는 APPROVED로 변경됨

---

### 16-4. 파트너 거절 (관리자용)

#### 엔드포인트
```
PATCH http://localhost:8080/api/admin/partners/{partnerId}/reject
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

#### 요청 바디 예시
```json
{
  "rejectionReason": "사업자 등록증이 불명확합니다. 다시 제출해주세요."
}
```

#### 필수 필드
- `rejectionReason`: 거절 사유 (필수)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": "파트너 신청이 거절되었습니다."
}
```

#### 에러 응답 예시 (파트너 없음)
```json
{
  "success": false,
  "code": "PARTNER_NOT_FOUND",
  "message": "파트너를 찾을 수 없습니다.",
  "data": null
}
```

#### 에러 응답 예시 (이미 처리됨)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "대기 중인 파트너만 거절할 수 있습니다. 현재 상태: 승인",
  "data": null
}
```

#### 에러 응답 예시 (거절 사유 없음)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "거절 사유는 필수입니다.",
  "data": null
}
```

**주의사항:**
- 거절 시 Account는 생성되지 않음
- 거절 사유가 파트너 이메일로 발송됨
- 거절 후 파트너 상태는 REJECTED로 변경됨
- 거절된 파트너는 재신청 가능 (같은 이메일로 재신청 시 중복 체크됨)

---

### 16-5. 파트너 목록 조회 (관리자용, 상태별 필터링)

#### 엔드포인트
```
GET http://localhost:8080/api/admin/partners?status=PENDING|APPROVED|REJECTED|INACTIVE
``` 

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

#### 쿼리 파라미터
- `status` (선택): 필터링할 상태
  - `PENDING`: 입점 신청됨
  - `APPROVED`: 승인됨
  - `REJECTED`: 거절됨
  - `INACTIVE`: 비활성/정지
  - 없음: 전체 조회

#### 응답 예시 (전체 조회)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "partnerId": 1,
      "partnerName": "수영용품 전문샵",
      "partnerContact": "010-1234-5678",
      "partnerBankAccount": "111-222-456789",
      "businessRegistrationNumber": "123-45-67890",
      "partnerStatus": "APPROVED",
      "partnerApprovedAt": "2024-01-10T10:00:00",
      "email": "partner1@swim-mall.com"
    },
    {
      "partnerId": 2,
      "partnerName": "비치웨어 스토어",
      "partnerContact": "010-9876-5432",
      "partnerBankAccount": "333-444-789012",
      "businessRegistrationNumber": "987-65-43210",
      "partnerStatus": "APPROVED",
      "partnerApprovedAt": "2024-01-10T10:00:00",
      "email": "partner2@swim-mall.com"
    }
  ]
}
```

#### 응답 예시 (상태별 필터링)
```
GET http://localhost:8080/api/admin/partners?status=APPROVED
```

**주의사항:**
- 상태별로 필터링하여 조회 가능
- status 파라미터 없이 호출 시 전체 파트너 조회

---

### 16-6. 파트너 상세 조회 (관리자용)

#### 엔드포인트
```
GET http://localhost:8080/api/admin/partners/{partnerId}
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "partnerId": 1,
    "partnerName": "수영용품 전문샵",
    "partnerContact": "010-1234-5678",
    "partnerBankAccount": "111-222-456789",
    "businessRegistrationNumber": "123-45-67890",
    "partnerStatus": "APPROVED",
    "partnerApprovedAt": "2024-01-10T10:00:00",
    "email": "partner1@swim-mall.com"
  }
}
```

#### 에러 응답 예시 (파트너 없음)
```json
{
  "success": false,
  "code": "PARTNER_NOT_FOUND",
  "message": "파트너를 찾을 수 없습니다.",
  "data": null
}
```

---

### 16-7. 파트너 비활성화 (관리자용)

#### 엔드포인트
```
PATCH http://localhost:8080/api/admin/partners/{partnerId}/deactivate
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

#### 요청 바디
없음 (경로 변수만 사용)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "partnerId": 1,
    "partnerName": "수영용품 전문샵",
    "partnerContact": "010-1234-5678",
    "partnerBankAccount": "111-222-456789",
    "businessRegistrationNumber": "123-45-67890",
    "partnerStatus": "INACTIVE",
    "partnerApprovedAt": "2024-01-10T10:00:00",
    "email": "partner1@swim-mall.com"
  }
}
```

#### 에러 응답 예시 (승인되지 않은 파트너)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "승인된 파트너만 비활성화할 수 있습니다. 현재 상태: 입점 신청됨",
  "data": null
}
```

**주의사항:**
- APPROVED 상태의 파트너만 비활성화 가능
- 비활성화 시 Account 상태도 INACTIVE로 변경됨
- 비활성화된 파트너는 로그인 불가
- **비활성화 시 해당 파트너의 모든 상품과 옵션이 자동으로 DELETE 상태로 변경되어 고객 목록에 표시되지 않음**

---

### 16-8. 파트너 재활성화 (관리자용)

#### 엔드포인트
```
PATCH http://localhost:8080/api/admin/partners/{partnerId}/activate
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

#### 요청 바디
없음 (경로 변수만 사용)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "partnerId": 1,
    "partnerName": "수영용품 전문샵",
    "partnerContact": "010-1234-5678",
    "partnerBankAccount": "111-222-456789",
    "businessRegistrationNumber": "123-45-67890",
    "partnerStatus": "APPROVED",
    "partnerApprovedAt": "2024-01-10T10:00:00",
    "email": "partner1@swim-mall.com"
  }
}
```

#### 에러 응답 예시 (비활성화되지 않은 파트너)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "비활성화된 파트너만 재활성화할 수 있습니다. 현재 상태: 승인됨",
  "data": null
}
```

**주의사항:**
- INACTIVE 상태의 파트너만 재활성화 가능
- 재활성화 시 Account 상태도 ACTIVE로 변경됨
- 재활성화된 파트너는 다시 로그인 가능
- **재활성화 시 해당 파트너의 모든 상품과 옵션이 자동으로 ACTIVE 상태로 변경되어 고객 목록에 다시 표시됨**

---

### 16-9. 파트너 휴업 신청 (파트너용)

#### 엔드포인트
```
POST http://localhost:8080/api/partner/deactivation/request
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (파트너 로그인 후 받은 세션 ID)
```

**주의:** 로그인한 파트너만 신청 가능 (APPROVED 상태만)

#### 요청 바디 예시
```json
{
  "deactivationReason": "일시적인 재고 부족으로 인한 휴업 신청"
}
```

#### 필수 필드
- `deactivationReason`: 휴업 신청 사유 (필수)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": null
}
```

#### 에러 응답 예시 (로그인 안 됨)
```json
{
  "success": false,
  "code": "UNAUTHORIZED",
  "message": "로그인이 필요합니다.",
  "data": null
}
```

#### 에러 응답 예시 (운영 중이 아님)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "운영 중인 파트너만 휴업 신청이 가능합니다.",
  "data": null
}
```

#### 에러 응답 예시 (이미 신청함)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "이미 휴업 신청이 접수되었습니다.",
  "data": null
}
```

**주의사항:**
- APPROVED 상태인 파트너만 휴업 신청 가능
- 한 번에 하나의 휴업 신청만 가능 (이미 신청한 경우 재신청 불가)
- 휴업 신청 후 관리자 승인 대기 상태
- 관리자가 승인하면 INACTIVE 상태로 변경됨
- 관리자가 거절하면 APPROVED 상태 유지 및 신청 정보 초기화

---

### 16-10. 휴업 신청 목록 조회 (관리자용)

#### 엔드포인트
```
GET http://localhost:8080/api/admin/partners/deactivation-requests
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** 관리자만 접근 가능

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "partnerId": 1,
      "partnerName": "수영용품 전문샵",
      "partnerContact": "010-1234-5678",
      "partnerBankAccount": "111-222-456789",
      "businessRegistrationNumber": "123-45-67890",
      "partnerStatus": "APPROVED",
      "partnerApprovedAt": "2024-01-10T10:00:00",
      "email": "partner1@swim-mall.com"
    },
    {
      "partnerId": 2,
      "partnerName": "비치웨어 전문점",
      "partnerContact": "010-9876-5432",
      "partnerBankAccount": "333-444-789012",
      "businessRegistrationNumber": "234-56-78901",
      "partnerStatus": "APPROVED",
      "partnerApprovedAt": "2024-01-12T14:30:00",
      "email": "partner2@swim-mall.com"
    }
  ]
}
```

**주의사항:**
- APPROVED 상태이면서 휴업 신청이 있는 파트너만 조회됨
- 휴업 신청 사유와 신청일시는 상세 조회 API에서 확인 가능

---

### 16-11. 휴업 신청 승인 (관리자용)

#### 엔드포인트
```
PATCH http://localhost:8080/api/admin/partners/{partnerId}/deactivation-request/approve
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

#### 요청 바디
없음 (경로 변수만 사용)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "partnerId": 1,
    "partnerName": "수영용품 전문샵",
    "partnerContact": "010-1234-5678",
    "partnerBankAccount": "111-222-456789",
    "businessRegistrationNumber": "123-45-67890",
    "partnerStatus": "INACTIVE",
    "partnerApprovedAt": "2024-01-10T10:00:00",
    "email": "partner1@swim-mall.com"
  }
}
```

#### 에러 응답 예시 (파트너 없음)
```json
{
  "success": false,
  "code": "PARTNER_NOT_FOUND",
  "message": "파트너를 찾을 수 없습니다.",
  "data": null
}
```

#### 에러 응답 예시 (운영 중이 아님)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "운영 중인 파트너만 휴업 신청 승인이 가능합니다. 현재 상태: 입점 신청됨",
  "data": null
}
```

#### 에러 응답 예시 (휴업 신청 없음)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "휴업 신청이 존재하지 않습니다.",
  "data": null
}
```

**주의사항:**
- APPROVED 상태이고 휴업 신청이 있는 파트너만 승인 가능
- 승인 시 파트너 상태가 INACTIVE로 변경됨
- Account 상태도 INACTIVE로 변경되어 로그인 불가
- **승인 시 해당 파트너의 모든 상품과 옵션이 자동으로 INACTIVE 상태로 변경되어 고객 목록에 표시되지 않음**
- 휴업 신청 정보는 승인 후 자동으로 초기화됨

---

### 16-12. 휴업 신청 거절 (관리자용)

#### 엔드포인트
```
PATCH http://localhost:8080/api/admin/partners/{partnerId}/deactivation-request/reject
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

#### 요청 바디 예시
```json
{
  "rejectionReason": "현재 시즌이므로 휴업 신청을 거절합니다."
}
```

#### 필수 필드
- `rejectionReason`: 거절 사유 (필수)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": "휴업 신청이 거절되었습니다."
}
```

#### 에러 응답 예시 (파트너 없음)
```json
{
  "success": false,
  "code": "PARTNER_NOT_FOUND",
  "message": "파트너를 찾을 수 없습니다.",
  "data": null
}
```

#### 에러 응답 예시 (운영 중이 아님)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "운영 중인 파트너만 휴업 신청 거절이 가능합니다. 현재 상태: 입점 신청됨",
  "data": null
}
```

#### 에러 응답 예시 (휴업 신청 없음)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "휴업 신청이 존재하지 않습니다.",
  "data": null
}
```

**주의사항:**
- APPROVED 상태이고 휴업 신청이 있는 파트너만 거절 가능
- 거절 시 파트너 상태는 APPROVED 유지
- 휴업 신청 정보(사유, 신청일시)가 초기화됨
- 파트너는 다시 휴업 신청 가능

---

### 16-12. 파트너 재활성화 신청 (파트너용)

#### 엔드포인트
```
POST http://localhost:8080/api/partner/reactivation/request
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (파트너 로그인 후 받은 세션 ID)
```

**주의:** 로그인한 파트너만 신청 가능 (INACTIVE 상태만)

#### 요청 바디 예시
```json
{
  "reactivationReason": "재고 확보 완료 및 재운영 준비 완료"
}
```

#### 필수 필드
- `reactivationReason`: 재활성화 신청 사유 (필수)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": null
}
```

#### 에러 응답 예시 (비활성화되지 않음)
```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "비활성화된 파트너만 재활성화 신청이 가능합니다.",
  "data": null
}
```

**주의사항:**
- INACTIVE 상태인 파트너만 재활성화 신청 가능
- **INACTIVE 상태에서도 로그인은 가능** (제한된 기능만 사용 가능)
- 한 번에 하나의 재활성화 신청만 가능 (이미 신청한 경우 재신청 불가)
- 재활성화 신청 후 관리자 승인 대기 상태
- 관리자가 승인하면 APPROVED 상태로 변경됨
- 관리자가 거절하면 INACTIVE 상태 유지 및 신청 정보 초기화
- **INACTIVE 상태에서는 상품 등록/수정/삭제 불가, 상품 목록 조회와 재활성화 신청만 가능**

---

**참고: 재활성화 신청 목록/승인은 기존 파트너 관리 API를 사용합니다:**
- 재활성화 신청 목록: `GET /api/admin/partners?status=INACTIVE`로 조회 후, `reactivationRequestedAt`이 null이 아닌 파트너만 필터링
- 재활성화 신청 승인: 기존 `PATCH /api/admin/partners/{partnerId}/activate` 사용 (재활성화 신청 정보는 자동으로 초기화됨)
- 재활성화 신청 거절: `PATCH /api/admin/partners/{partnerId}/reactivation-request/reject` 사용

---

### 16-14. 재활성화 신청 목록 조회 (관리자용) - 기존 API 사용

**기존 파트너 관리 API를 사용합니다:**

#### 엔드포인트
```
GET http://localhost:8080/api/admin/partners?status=INACTIVE
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** 관리자만 접근 가능

#### 응답 예시
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": [
    {
      "partnerId": 1,
      "partnerName": "수영용품 전문샵",
      "partnerContact": "010-1234-5678",
      "partnerBankAccount": "111-222-456789",
      "businessRegistrationNumber": "123-45-67890",
      "partnerStatus": "INACTIVE",
      "partnerApprovedAt": "2024-01-10T10:00:00",
      "email": "partner1@swim-mall.com",
      "reactivationRequestReason": "재고 확보 완료 및 재운영 준비 완료",
      "reactivationRequestedAt": "2024-01-20T14:30:00"
    }
  ]
}
```

**주의사항:**
- `GET /api/admin/partners?status=INACTIVE`로 INACTIVE 상태 파트너 목록 조회
- 응답의 `reactivationRequestedAt` 필드가 null이 아니면 재활성화 신청이 있는 파트너
- 프론트엔드에서 `reactivationRequestedAt`이 null이 아닌 파트너만 필터링하여 표시

---

### 16-15. 재활성화 신청 승인 (관리자용) - 기존 API 사용

**기존 파트너 재활성화 API를 사용합니다:**

#### 엔드포인트
```
PATCH http://localhost:8080/api/admin/partners/{partnerId}/activate
```

#### 헤더
```
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

#### 요청 바디
없음 (경로 변수만 사용)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": {
    "partnerId": 1,
    "partnerName": "수영용품 전문샵",
    "partnerContact": "010-1234-5678",
    "partnerBankAccount": "111-222-456789",
    "businessRegistrationNumber": "123-45-67890",
    "partnerStatus": "APPROVED",
    "partnerApprovedAt": "2024-01-10T10:00:00",
    "email": "partner1@swim-mall.com",
    "reactivationRequestReason": null,
    "reactivationRequestedAt": null
  }
}
```

**주의사항:**
- 기존 `PATCH /api/admin/partners/{partnerId}/activate` API 사용
- INACTIVE 상태 파트너를 재활성화하면 재활성화 신청 정보도 자동으로 초기화됨
- 승인 시 파트너 상태가 APPROVED로 변경됨
- Account 상태도 ACTIVE로 변경되어 정상 로그인 가능
- **승인 시 해당 파트너의 모든 상품과 옵션이 자동으로 ACTIVE 상태로 변경되어 고객 목록에 다시 표시됨**

---

### 16-16. 재활성화 신청 거절 (관리자용)

#### 엔드포인트
```
PATCH http://localhost:8080/api/admin/partners/{partnerId}/reactivation-request/reject
```

#### 헤더
```
Content-Type: application/json
Cookie: JSESSIONID=세션ID (관리자 로그인 후 받은 세션 ID)
```

**주의:** `{partnerId}`를 실제 파트너 ID로 치환하세요.

#### 요청 바디 예시
```json
{
  "rejectionReason": "재고 확인이 필요합니다."
}
```

#### 필수 필드
- `rejectionReason`: 거절 사유 (필수)

#### 성공 응답 예시 (200 OK)
```json
{
  "success": true,
  "code": null,
  "message": "성공",
  "data": "재활성화 신청이 거절되었습니다."
}
```

**주의사항:**
- INACTIVE 상태이고 재활성화 신청이 있는 파트너만 거절 가능
- 거절 시 파트너 상태는 INACTIVE 유지
- 재활성화 신청 정보(사유, 신청일시)가 초기화됨
- 파트너는 다시 재활성화 신청 가능

---

## 파트너 입점 신청 API 주의사항

1. **신청 프로세스:**
   - 신청 시 Account는 생성되지 않음 (승인 시에만 생성)
   - 신청 정보는 Partner 엔티티에 저장됨 (PENDING 상태)
   - 비밀번호는 신청 시 입력하지 않음 (승인 시 임시 비밀번호 생성)

2. **승인 프로세스:**
   - 관리자가 승인 시 Account가 자동 생성됨
   - 임시 비밀번호(8자리 랜덤 문자열)가 생성되어 이메일로 발송됨
   - 파트너는 이메일로 받은 임시 비밀번호로 로그인 가능
   - 승인 후 파트너 상태는 APPROVED로 변경됨

3. **거절 프로세스:**
   - 관리자가 거절 사유를 입력하여 거절
   - 거절 사유가 파트너 이메일로 발송됨
   - 거절 후 파트너 상태는 REJECTED로 변경됨
   - Account는 생성되지 않음
   - 거절된 파트너는 재활성화 불가 (재신청만 가능)

4. **비활성화 프로세스:**
   - **관리자 직접 비활성화**: 관리자가 승인된 파트너를 직접 비활성화
   - **파트너 휴업 신청**: 파트너가 휴업 신청 → 관리자 승인 → 비활성화
   - 비활성화 시 파트너 상태는 INACTIVE로 변경됨
   - Account 상태도 INACTIVE로 변경됨
   - **INACTIVE 상태에서도 로그인은 가능** (제한된 기능만 사용 가능)
   - 비활성화된 파트너는 재활성화 가능

5. **재활성화 프로세스:**
   - **관리자 직접 재활성화**: 관리자가 비활성화된 파트너를 직접 재활성화
   - **파트너 재활성화 신청**: 파트너가 재활성화 신청 → 관리자 승인 → 재활성화
   - 재활성화 시 파트너 상태는 APPROVED로 변경됨
   - Account 상태도 ACTIVE로 변경되어 정상 로그인 가능

6. **이메일 발송:**
   - 승인 시: 계정 정보(이메일, 임시 비밀번호) 포함
   - 거절 시: 거절 사유 포함
   - 이메일 발송은 비동기로 처리됨 (실패해도 API는 성공 반환)

7. **권한 검증:**
   - 신청 API: 로그인 없이도 접근 가능
   - 목록 조회/승인/거절/비활성화/재활성화 API: 관리자만 접근 가능

8. **데이터 검증:**
   - 이메일: 중복 확인 (Account 또는 PENDING 상태인 Partner)
   - 연락처: 형식 검증 (010-XXXX-XXXX)
   - 정산계좌: 형식 검증 (XXX-XXX-XXXXXX)
   - 사업자등록번호: 형식 검증 (XXX-XX-XXXXX)

9. **파트너 상태:**
   - `PENDING`: 입점 신청됨 (Account 없음)
   - `APPROVED`: 승인됨 (Account 있음, 로그인 가능)
   - `REJECTED`: 거절됨 (Account 없음, 재신청 가능)
   - `INACTIVE`: 비활성/정지 (Account 있음, 재활성화 가능)

10. **상태 전환 흐름:**
    - PENDING → (승인) → APPROVED → (비활성화) → INACTIVE → (재활성화) → APPROVED
    - PENDING → (거절) → REJECTED (재활성화 불가, 재신청만 가능)
    - APPROVED → (파트너 휴업 신청) → (관리자 승인) → INACTIVE
    - APPROVED → (파트너 휴업 신청) → (관리자 거절) → APPROVED (신청 정보 초기화)
    - INACTIVE → (파트너 재활성화 신청) → (관리자 승인) → APPROVED
    - INACTIVE → (파트너 재활성화 신청) → (관리자 거절) → INACTIVE (신청 정보 초기화)

11. **휴업 신청 프로세스:**
    - 파트너가 APPROVED 상태일 때 휴업 신청 가능
    - 한 번에 하나의 휴업 신청만 가능 (이미 신청한 경우 재신청 불가)
    - 관리자가 승인하면 INACTIVE 상태로 변경되고 상품/옵션도 비활성화됨
    - 관리자가 거절하면 APPROVED 상태 유지 및 신청 정보 초기화

12. **재활성화 신청 프로세스:**
    - 파트너가 INACTIVE 상태일 때 재활성화 신청 가능
    - **INACTIVE 상태에서도 로그인은 가능** (제한된 기능만 사용 가능)
    - 한 번에 하나의 재활성화 신청만 가능 (이미 신청한 경우 재신청 불가)
    - 관리자가 승인하면 APPROVED 상태로 변경되고 상품/옵션도 활성화됨
    - 관리자가 거절하면 INACTIVE 상태 유지 및 신청 정보 초기화

13. **INACTIVE 상태에서의 기능 제한:**
    - **사용 가능한 기능:**
      - 상품 목록 조회 (`GET /api/partner/products`)
      - 재활성화 신청 (`POST /api/partner/reactivation/request`)
    - **사용 불가능한 기능:**
      - 상품 등록 (`POST /api/partner/products`) - "비활성화된 파트너는 상품을 등록할 수 없습니다. 재활성화 신청을 해주세요." 에러
      - 상품 수정 (`PUT /api/partner/products/{productNo}`) - "비활성화된 파트너는 상품을 수정할 수 없습니다. 재활성화 신청을 해주세요." 에러
      - 상품 삭제 (`DELETE /api/partner/products/{productNo}`) - "비활성화된 파트너는 상품을 삭제할 수 없습니다. 재활성화 신청을 해주세요." 에러

---
