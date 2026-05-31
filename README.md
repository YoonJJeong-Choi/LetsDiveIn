# Let's Dive In

수영 용품 멀티 벤더 쇼핑몰 모노레포입니다. 고객몰(Next.js), 관리자·파트너 백오피스(React), API(Spring Boot)로 구성됩니다.

| 구분 | 기술 | 로컬 포트 |
|------|------|-----------|
| 고객몰 | Next.js, React | 3000 |
| 관리자·파트너 | Create React App, Ant Design | 3001 |
| API | Spring Boot, PostgreSQL, Flyway | 8080 |

## 빠른 시작

### 1. 데이터베이스

PostgreSQL에 로컬 DB를 만듭니다 (예: `LetsDiveIn`). 마이그레이션은 앱 기동 시 Flyway가 실행합니다.

### 2. 백엔드

```bash
cd backend
cp .env.example .env   # Windows: copy .env.example .env
# .env 에 DB 비밀번호, 메일, Toss 시크릿 등 실제 값 입력
./gradlew bootRun
```

- 환경 변수 키 목록: [`backend/.env.example`](backend/.env.example) (공개 가능한 template, **실제 값은 `.env`에만**)
- 시드 데이터: `app.data.init.enabled=true` + 빈 DB 기동 시 자동 생성. 계정·시나리오는 [시드 스펙](backend/docs/reference/SEED_DEMO_SPEC.md) 참고 (데모 비밀번호는 Git/README에 넣지 않음)

### 3. 프론트엔드

```bash
# 고객몰
cd customer-frontend/web
npm install && npm run dev

# 관리자·파트너 (별 터미널)
cd admin-frontend
npm install && npm start
```

로컬에서는 고객몰 Next가 `/api`를 백엔드(8080)로 프록시합니다. 배포 시 API·관리자 URL은 각 프론트 설정을 따릅니다.

## 프로젝트 구조

```
swim-mall/
├── customer-frontend/web/   # 고객용
├── admin-frontend/          # 관리자·파트너
└── backend/                 # Spring Boot API + docs/
```

## 문서

상세 설계·API·Postman·시드·보안은 [`backend/docs/README.md`](backend/docs/README.md)가 목차입니다.

| 바로가기 | 내용 |
|----------|------|
| [domains/](backend/docs/README.md#domains) | 배송·결제·포인트·QnA·정산 |
| [testing/POSTMAN_TEST_GUIDE.md](backend/docs/testing/POSTMAN_TEST_GUIDE.md) | Postman 테스트 |
| [reference/SEED_DEMO_SPEC.md](backend/docs/reference/SEED_DEMO_SPEC.md) | 시드·데모 계정 |
| [security/RATE_LIMIT_OPERATION_GUIDE.md](backend/docs/security/RATE_LIMIT_OPERATION_GUIDE.md) | Rate limit |

## 환경 변수 (요약)

| 앱 | 설정 |
|----|------|
| **backend** | `backend/.env` (Git 제외). [`backend/.env.example`](backend/.env.example) 복사 후 값 입력 |
| **customer-frontend** | 선택: `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_ADMIN_FRONTEND_URL`, `NEXT_PUBLIC_TOSS_CLIENT_KEY` 등. 미설정 시 로컬 프록시·기본 URL 사용 |
| **admin-frontend** | [`src/configs/EnvironmentConfig.js`](admin-frontend/src/configs/EnvironmentConfig.js) — 개발 시 `http://localhost:8080/api` |
