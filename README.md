# Swim Mall 프로젝트

쇼핑몰 프로젝트입니다.

## 프로젝트 구조

```
swim-mall/
├── customer-frontend/    # 고객용 프론트엔드 (Next.js)
├── admin-frontend/        # 관리자용 프론트엔드 (추후 개발 예정)
├── backend/               # 백엔드 (Spring Boot)
└── template/              # 템플릿 원본 (참고용)
    └── customer-frontend/
```

## 각 프로젝트 설명

### customer-frontend
고객용 웹 애플리케이션입니다.
- 기술 스택: Next.js, React
- 위치: `customer-frontend/web/`

### admin-frontend
관리자용 웹 애플리케이션입니다. (추후 개발 예정)

### backend
백엔드 서버입니다.
- 기술 스택: Spring Boot, Java
- 위치: `backend/`

### template
템플릿 원본 파일입니다. 참고용으로 보관합니다.

## 개발 환경 설정

### customer-frontend
```bash
cd customer-frontend/web
npm install
npm run dev
```

### backend
```bash
cd backend
./gradlew bootRun
```

## 환경 변수

각 프로젝트의 환경 변수 설정은 해당 프로젝트의 README를 참고하세요.
