# 시드·포폴 데모 스펙

`DataInitializer` 구현 기준 문서. 변경 시 코드와 함께 갱신한다.

## 데모 계정

| 역할 | 이메일 | 비고 |
|------|--------|------|
| 관리자 | `testAdmin@example.com` | 메인 테스트 |
| 파트너1 | `testPartner1@example.com` | 메인 테스트 (스피도) |
| 파트너2 | `testPartner2@example.com` | 보조 (아레나) |
| 고객1 | `testCustomer1@example.com` | 메인 테스트 (반품 신청 가능 주문) |
| 고객2 | `testCustomer2@example.com` | 보조 |

메인 테스트 3역할(관리자·파트너1·고객1) 로컬 시드 비밀번호: `test123!`

관련: [DUMMY_DATA_RELATIONS.md](DUMMY_DATA_RELATIONS.md) (생성 순서·엔티티 관계)

---

## 1. 이벤트 구성

| 구분 | 건수 | 시드 시각·상태 (원칙) |
|------|------|------------------------|
| 종료 | 3 | `customerEventEndAt` 과거, `eventStatus` = `ENDED` |
| 진행(장기) | 1 | `customerEventStartAt` ≈ `now - 1일`, `customerEventEndAt` ≈ `now + 365일`, `eventStatus` = `PUBLISHED`, `customerExposeAt` 노출 기준 충족 |
| 예정 | 1 | `customerEventStartAt` 미래, `PRIVATE` + `customerExposeAt` 미래 (고객 목록 비노출, 관리자만 확인) |

주의: `EventEntity`에는 `thumbnailUrl`(문자열)만 있고, 실제 노출은 `eventStatus`·`customerExposeAt`·기간 필드 조합으로 결정된다.

---

## 2. 이미지·파일 시드 방식

| 대상 | DB에 넣는 형태 | 시드 권장 |
|------|----------------|------------|
| 상품 `productImageUrl` | URL 문자열 | 고객 프론트 `public` 기준 경로 (예: `/images/products/woman19.jpg`). 파일은 Git에 포함된 정적 자산이어야 함 |
| 상품 갤러리 `ProductImageEntity` | URL 문자열 | 대표·추가 이미지 목록 (있을 때만 행 생성) |
| 이벤트 `thumbnailUrl` | URL 문자열 | 상품과 동일. 없으면 프론트 폴백 이미지 사용 |
| 리뷰 본문 | 텍스트만 | 이미지 없이도 목록·상세 가능 |
| 리뷰 이미지 `ReviewImageEntity` | `imageUrl` + `UploadedFileEntity` 필수 FK | 시드 없음 (텍스트 리뷰만) |
| 반품 이미지 `ReturnImageEntity` | `imageUrl` + `UploadedFileEntity` 필수 FK | **일반 반품 시드는 없음.** 아래 §5의 return-assist·REFUNDED 데모만 `UploadedFileEntity` + 시드 경로로 생성 |

정리: 상품·이벤트는 DB에 경로 문자열만 넣는다. 반품 증빙은 assist/완료 데모에서만 `UploadedFileEntity`를 함께 만든다. 브라우저는 Next.js가 `customer-frontend/web/public` 아래 파일을 그 경로로 제공한다.

---

## 3. 계정·역할

| 구분 | 개수·역할 | 비고 |
|------|------------|------|
| 관리자 | 1명 (메인) | |
| 파트너 | 2명 고정 | 파트너1 = 메인 테스트, 파트너2 보조 |
| 고객 | 2명 | 고객1 = 메인 테스트, 고객2 보조·리뷰 분산 |
| 비밀번호 | — | README·비공개 문서 (평문 미기재 권장) |

---

## 4. 마스터·카탈로그

| 항목 | 비고 |
|------|------|
| 브랜드 | SPEEDO, ARENA 등 |
| 사이즈 | |
| 컬러 마스터 | 옵션 색상·파트너 등록과 정합 |
| 상품·옵션 | 파트너 2(스피도·아레나), 26종. 상품명·`brandName` 쇼핑몰형 (건수는 아래 표) |
| 재고 | 옵션·단일 상품 각각 연결 |

### 상품·옵션 시드 건수

| 구분 | 개수 |
|------|------|
| 상품 | 26개 |
| 옵션 | 62개 |
| 옵션 없는 단일 상품 | 8개 |

파트너1(스피도): 옵션 있음 12 + 단일 5. 파트너2(아레나): 옵션 있음 6 + 단일 3.

---

## 5. 주문·결제·배송·반품

| 블록 | 건수 | 내용 |
|------|------|------|
| 배송중 | 3 | 결제 완료, `DeliveryStatus.SHIPPED` |
| 취소 | 2 | 결제 없음, `OrderStatus.CANCELLED` |
| 구매확정·정산용 | 28 | `completedAt` 설정, 배송 완료 |
| 반품 완료(REFUNDED) | 1 | 관리자 완료 목록, 증빙 이미지 2장 |
| 반품 신청 가능 | 1 | 고객1, Return 없음 → 앱에서 직접 신청 |
| 반품 검토 보조 데모(REQUESTED) | 3 | DEFECT(이미지2)·WRONG_ITEM(이미지1)·CHANGE_OF_MIND(이미지0) — `GET /returns/{id}/return-assist` |
| 신규 주문·결제 | — | 시드 없음. 데모 중 Toss 결제 (`.env`) |
| 장바구니 | — | 시드 없음. 데모 중 직접 담기 |

주문 합계: **38건** (위 시드 시나리오별 주문 각각 별도 생성).

---

## 6. 리뷰·이벤트·FAQ·QnA

| 항목 | 내용 |
|------|------|
| 리뷰 | 고객별 최대 3건, 텍스트만(2~3문장 본문). **추가**: 파트너1 「실키 핏 프로 실리콘 캡」 동일 상품 구매확정 주문 3건·리뷰 3건(본문 `【리뷰AI시드】` 접두, 리뷰 AI 분석 데모용, 기존 DB에도 부족 시 기동마다 추가) |
| 이벤트 | 1절 조합(종료 3·진행 장기 1·예정 1) |
| FAQ | 7건(주문·배송·반품·회원·상품·포인트·기타 카테고리 각 1) |
| QnA | 3건 — 플랫폼 PENDING 1·파트너 PENDING 1·파트너 ANSWERED 1. 주문 상품 없으면 파트너 건 생략 |

---

## 7. 기동·배포

| 항목 | 값 |
|------|-----|
| 시드 실행 | `app.data.init.enabled=true` + 빈 DB(또는 count 0) + 앱 기동 |
| DB 갈아끼우기 | 논리 DB 비움/재생성 → Flyway → 위 조건으로 기동 |
| 포폴·운영 서버 | `app.data.init.enabled=false` 권장 |

---

## 8. 라이브 데모 플레이북 (역할 3)

| 순서 | 역할 | 예시 |
|------|------|------|
| 1 | 고객1 | 장바구니·반품 신청 가능 주문·또는 신규 결제 |
| 2 | 관리자 | 반품·주문·정산·FAQ/QnA |
| 3 | 파트너1 | 상품/옵션(컬러 사용)·파트너 QnA |

단일 계정 강제 없음 — 시크릿 창 등으로 전환.

---

## 9. 시드 변경 시 갱신 체크

`DataInitializer`·카탈로그·주문 시나리오를 바꿀 때 아래를 함께 맞춘다.

1. 이 문서(건수·시나리오 표)
2. [DUMMY_DATA_RELATIONS.md](DUMMY_DATA_RELATIONS.md) (생성 순서·FK 관계)
3. `DataInitializer` 클래스 주석의 스펙 링크
