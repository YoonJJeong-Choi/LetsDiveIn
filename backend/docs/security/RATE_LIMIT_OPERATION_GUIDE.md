# Rate Limit 운영 가이드

## 목적
- 공개 API의 악의적 반복 호출(회원가입/로그인/업로드/AI 호출)을 줄인다.

## 현재 적용된 제한 (구현 완료)

| API | 제한 | 한도 | 적용 위치 |
|-----|------|------|-----------|
| `POST /api/customer/join` | IP | 분당 5회 | `ApiRateLimitInterceptor` |
| `POST /api/customer/join` | 이메일 | 시간당 3회 | `CustomerService.join` |
| `POST /api/customer/join` | 중복 이메일 | 1계정 | `CustomerService.join` → `400` `EMAIL_ALREADY_EXISTS` (인증 전이어도 동일) |
| `POST /api/auth/login` | IP | 분당 10회 | `ApiRateLimitInterceptor` |
| `POST /api/auth/login` | 이메일(실패 누적) | 10분 내 오류 5회 → 10분 잠금 | `LoginLockoutService` (`AuthService`, 성공 시 초기화) |
| `POST /api/files/upload` | IP | 분당 20회 | `ApiRateLimitInterceptor` |
| `POST /api/files/upload` | 계정(세션) | 분당 10회 | `ApiRateLimitInterceptor` |
| AI draft-assist / 리뷰 분석 | IP | 분당 20회 | `ApiRateLimitInterceptor` |
| AI draft-assist / 리뷰 분석 | 계정(세션) | 분당 5회 | `ApiRateLimitInterceptor` |

### AI API 경로
- `POST /api/admin/qna/{qnaNo}/draft-assist`
- `POST /api/partner/qna/{qnaNo}/draft-assist`
- `POST /api/ai/review-analysis/partner`

### 로그인 잠금 상세
- `INVALID_CREDENTIALS`(비밀번호 오류·없는 계정)만 실패로 카운트.
- `EMAIL_NOT_VERIFIED`, `ACCOUNT_INACTIVE` 등 비밀번호가 맞은 경우는 카운트하지 않음.
- 잠금 중에도 IP 분당 10회 제한은 별도로 동작.

### 회원가입 중복 이메일 vs 이메일당 rate limit
- 중복 검사(`400`): 비즈니스 규칙 — 이메일당 계정 1개. 2번째 성공 가입은 불가.
- 이메일 rate limit(`429`): API 남용 방지 — `join` 호출 자체를 시간당 3회까지 카운트(성공 여부 무관, 중복 시도도 포함).
- 흐름 예: 1회 성공 → 2~3회 `400` → 4번째 `429` (같은 이메일로 성공 3번은 중복 때문에 불가능).
- IP 제한 테스트: 서로 다른 미가입 이메일 사용.

### 비밀번호 찾기(셀프서비스)
- 고객 로그인 화면 링크 제거됨. 공개 API 없음.
- 관리자 고객 비밀번호 초기화: `POST /api/admin/customers/{customerId}/reset-password` (관리자 세션 필수).

### 관련 코드 위치
- `common/ratelimit/InMemoryRateLimiterService.java`
- `common/ratelimit/ApiRateLimitInterceptor.java`
- `common/ratelimit/LoginLockoutService.java`
- `config/RateLimitConfig.java`
- `account/service/AuthService.java`
- `customer/service/CustomerService.java`
- `common/error/ErrorCode.java` (`RATE_LIMIT_EXCEEDED`, `EMAIL_ALREADY_EXISTS`)

## AI 과금 방어 관련 상태
- 일일 한도 방어: 구현 완료 (QnA Draft, 리뷰 분석)
- 분당 한도 방어: 구현 완료 (AI API 계정/IP)
- 운영 권장: OpenAI 대시보드 예산 알림(월 예산/사용량) 설정

## 아직 미구현 (우선순위 순)
1. CAPTCHA 연동 (예: 로그인 실패 3회 이상)
2. 비밀번호 찾기(셀프서비스) API + 해당 경로 rate limit
3. 공개 조회(GET) 공통 제한 (예: IP 분당 120회, page size 상한 50)
4. CloudWatch/WAF 알림/정책 자동화

## 운영 주의사항
- Rate limit·로그인 잠금 저장소는 인메모리.
  - 서버 재시작 시 카운터·잠금 초기화
  - 멀티 인스턴스 시 인스턴스별로 별도 카운트

## 배포 후 최소 확인
- [ ] 회원가입: 다른 이메일 6회차 `429` (IP)
- [ ] 회원가입: 같은 이메일로 `join` 1회 성공 후 재시도 3회(`400`) → 4번째 `429` (이메일)
- [ ] 로그인: 틀린 비밀번호 6회차 `429` (이메일 잠금)
- [ ] 로그인: 기존 계정 11회차 `429` (IP)
- [ ] 업로드·AI: 각 한도 1회씩 확인
- [ ] `429` 메시지가 각 프론트 UI에 표시
- [ ] 정상 1~2회 요청은 여전히 성공 (회귀)

## 장애 대응 (간단)
- `429` 급증 시: 경로 확인 → 제한값·WAF 조정 → 정상 트래픽 영향 확인
