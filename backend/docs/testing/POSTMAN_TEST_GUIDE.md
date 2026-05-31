# Postman 테스트 가이드

규칙·필드·웹훅·배송 등 나머지는 [domains/payment/payment.md](../domains/payment/payment.md) 등 도메인 문서와 [POSTMAN_CURRENT_REFERENCE.md](POSTMAN_CURRENT_REFERENCE.md)·[POSTMAN_LEGACY_REFERENCE.md](POSTMAN_LEGACY_REFERENCE.md)를 본다.

---

## Postman에서 맞출 공통 값

| 항목 | 내용 |
|------|------|
| baseUrl | `http://localhost:8080` (로컬 기준) |
| 세션 | 로그인 응답 후 쿠키 저장(JSESSIONID 등)이 켜져 있어야 한다. |
| 로그인 URL | `POST {{baseUrl}}/api/auth/login` |
| portal | JSON에 넣는다. 고객: `CUSTOMER`. 관리자·파트너 계정: `ADMIN` (`AuthPortal` enum에는 `PARTNER` 없음. 파트너도 `ADMIN` 포털로 로그인한다.) |
| 웹훅 서명용 변수 | 서버 `payments.toss.secret-key`와 같은 값을 Postman 환경에 `TOSS_SECRET_KEY` 등으로 두고, 서명은 [payment.md](../domains/payment/payment.md)와 레거시 파일 맨 앞 절을 참고한다. |

---

## 호출 순서 예시 (고객 → 결제 확정)

1. `POST {{baseUrl}}/api/auth/login`  
   예: `{"email":"…","password":"…","portal":"CUSTOMER"}`

2. `POST {{baseUrl}}/api/orders`  
   `cartItemNos`, 배송지, `paymentMethod` 등. 응답의 `orderNo`, `orderTotalPrice`를 환경 변수에 저장해 두면 다음 단계에 쓰기 쉽다.

3. `POST {{baseUrl}}/api/payments/confirm`  
   `paymentKey`, `orderId`(규칙은 payment.md), `amount`는 주문 합계와 맞아야 한다.

4. 기대 상태는 [status-model.md](../architecture/status/status-model.md)·[payment.md](../domains/payment/payment.md)와 대조한다. 금액 불일치·멱등 재호출 등은 payment.md에 맡긴다.
