# 리뷰 AI 분석 (Review Analysis)

파트너가 자기 상품 리뷰 여러 건을 모아 OpenAI로 요약·이슈·개선점을 받는 보조 기능이다.

리뷰 작성·수정·답변·적립 같은 일반 기능은 `ReviewService`를 본다. 적립과의 연계만 [points.md](../points/points.md)에 적어 두었다. 이 문서는 AI 분석만 다룬다.

QnA의 AI 답변 초안(`draft-assist`)과 API·로그·한도가 다르다. QnA 쪽은 [qna.md](../qna/qna.md) §6을 본다.

AI가 만든 결과를 리뷰나 답변에 자동으로 올리지는 않는다. 화면에 보여 줄 분석 JSON만 돌려준다.

---

## 1. 무엇을 하는 기능인가

| 항목 | 내용 |
|------|------|
| 누가 쓰나 | 파트너 로그인 세션만 |
| 무엇을 넣나 | 같은 상품(`productNo`) 리뷰 3건 이상 |
| 무엇이 나오나 | 요약, 이슈, 개선 제안, 키워드, 대표 인용, 평점 통계, 알림 |
| 실패할 때 | 리뷰 수가 부족하면 `400`. OpenAI 오류는 `200`이지만 본문은 "리뷰 분석 일시 불가" 같은 대체 응답 |

주요 코드: `ReviewAnalysisController`, `ReviewAnalysisService`, `OpenAiReviewAnalysisClient`, `ReviewController`의 `ai-candidates`.

---

## 2. 사용 순서

1. 파트너 로그인 — `POST /api/auth/login` (`portal=ADMIN`. 파트너 계정도 ADMIN 포털로 로그인한다.)
2. 후보 조회 — `GET /api/reviews/partner/ai-candidates?productNo=…`
   - 옵션·기간·개수는 쿼리로 줄일 수 있다.
   - 자기 상품이 아니면 `403`.
3. 분석 요청 — `POST /api/ai/review-analysis/partner`
   - 2번에서 받은 리뷰 목록을 body의 `reviews`에 넣어 보낸다.
4. 서버가 일일 한도·상품 수·최소 건수를 검사한 뒤 OpenAI를 호출하고, 결과를 `review_analysis_log`에 남긴다. 로그 저장이 실패해도 분석 응답 자체는 그대로 돌려준다.

`partnerId`는 요청 body에 넣어도 무시된다. 컨트롤러가 세션의 파트너 ID로 덮어 쓴다.

---

## 3. API

### 후보 조회

| 메서드 | 경로 |
|--------|------|
| GET | `/api/reviews/partner/ai-candidates` |

쿼리: `productNo`(필수), `optionNos`, `limit`, `fromAt`, `toAt`.

응답: `reviews` 배열과 `meta`(기본 개수, 최소 필요 건수, 상한, 실제 조회 건수 등).

`limit`를 안 주면 `ai.review-analysis.recent-count`(기본 3)만큼, 최대 `hard-cap`(기본 10)까지 조회한다.

### 분석 요청

| 메서드 | 경로 |
|--------|------|
| POST | `/api/ai/review-analysis/partner` |

body 필드: `fromAt`, `toAt`, `productNos`, `maxReviews`, `reviews`.

`reviews` 한 건당: `reviewNo`, `productNo`, `optionNo`, `rating`, `content`, `createdAt`.

### 검사 규칙

| 상황 | 결과 |
|------|------|
| `reviews`가 `minimum-required`(기본 3)보다 적음 | `400` `REVIEW_ANALYSIS_NOT_ENOUGH_REVIEWS` |
| `productNos`가 2개 이상이거나, `reviews`에 서로 다른 상품이 섞임 | 요청 거부(현재 `IllegalArgumentException` → 공통 예외 처리) |
| 파트너 일일 한도 초과 | `429` `AI_DAILY_LIMIT_EXCEEDED` |
| OpenAI 키 없음, 호출 실패, 응답 파싱 실패 | HTTP `200` + 대체 응답 (`summary`에 "리뷰 분석 일시 불가" 등) |

OpenAI 호출은 네트워크 오류·5xx·429일 때 최대 1번 더 시도한다(총 2회).

---

## 4. 성공 응답에 들어 있는 것

| 필드 | 설명 |
|------|------|
| `summary` | 전체 한 줄 요약 |
| `issues` | 불만 유형, 빈도, 심각도(LOW/MEDIUM/HIGH), 근거 리뷰 번호 |
| `actions` | 손볼 영역(상세 페이지, 사이즈 안내, 이미지 등)과 제안 |
| `topKeywords` | 자주 나온 키워드와 비중(0~1) |
| `representativeQuotes` | 대표 인용문과 리뷰 번호 |
| `stats` | 건수, 평균 별점, 별점별 개수 |
| `alerts` | 개인정보·스팸·정책 위반 여부 |

AI가 준 JSON은 서비스에서 다시 읽어 보고, 허용된 값·숫자 범위만 통과시킨다.

---

## 5. QnA AI와 비교

| | QnA draft-assist | 리뷰 분석 |
|---|------------------|-----------|
| 대상 | 문의 1건 | 같은 상품 리뷰 여러 건 |
| API | `/api/admin/qna/{id}/draft-assist`, `/api/partner/qna/{id}/draft-assist` | `/api/ai/review-analysis/partner` |
| 하는 일 | 답변 초안 문장 | 리뷰 묶음 분석·개선점 |
| 자동 저장 | 없음(reply는 따로) | 없음 |
| 로그 테이블 | `qna_draft_log` | `review_analysis_log` |
| 개발 과정 문서 | [qna-development.md](../qna/qna-development.md) | 없음(동작만 본 문서) |

---

## 6. 설정과 한도

`application.yml`의 `ai.openai.*`, `ai.review-analysis.*`:

| 설정 | 기본값 | 설명 |
|------|--------|------|
| `ai.openai.api-key` | 환경변수 `OPENAI_API_KEY` | 비어 있으면 OpenAI 호출 없이 대체 응답만 |
| `ai.openai.model` | `gpt-4o-mini` | |
| `ai.review-analysis.recent-count` | 3 | 후보 조회 기본 개수 |
| `ai.review-analysis.minimum-required` | 3 | 분석에 필요한 최소 리뷰 수 |
| `ai.review-analysis.hard-cap` | 10 | 후보·요청 상한 |
| `ai.review-analysis.daily-limit-partner` | 10 | 파트너 하루 분석 횟수 |

일일 한도는 당일 `review_analysis_log`에 쌓인 건수를 센다(성공·실패 로그 모두 포함).

IP·계정당 분당 호출 제한은 [RATE_LIMIT_OPERATION_GUIDE.md](../../security/RATE_LIMIT_OPERATION_GUIDE.md)를 본다.

관리자는 `GET /api/admin/ai/overview`에서 QnA·리뷰 AI 사용량과 최근 실패 로그를 볼 수 있다.

---

## 7. 어떻게 검증했는지

| 방법 | 위치 |
|------|------|
| 단위 테스트 | `ReviewAnalysisServiceTest`, `ReviewAnalysisPromptTemplateServiceTest` |
| Postman | [POSTMAN_LEGACY_REFERENCE.md](../../testing/POSTMAN_LEGACY_REFERENCE.md)의 [AI] 리뷰 분석 절 |
| 시드 | [SEED_DEMO_SPEC.md](../../reference/SEED_DEMO_SPEC.md) §6 — 텍스트 리뷰가 있으므로 같은 상품 3건 이상 모은 뒤 분석 |

Postman 최소 순서: 파트너 login → `ai-candidates` → `review-analysis/partner`(리뷰 3건 이상).

---

## 8. 코드 위치

- `ai/reviewanalysis/controller/ReviewAnalysisController.java`
- `ai/reviewanalysis/service/ReviewAnalysisService.java`
- `ai/reviewanalysis/service/ReviewAnalysisPromptTemplateService.java`
- `ai/reviewanalysis/client/OpenAiReviewAnalysisClient.java`
- `ai/reviewanalysis/entity/ReviewAnalysisLogEntity.java`
- `review/controller/ReviewController.java` — `GET /partner/ai-candidates`
- `review/service/ReviewService.java` — 후보 조회, 본인 상품인지 확인
