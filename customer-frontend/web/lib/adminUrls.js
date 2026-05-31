/**
 * 고객몰에서 관리자(파트너) 앱으로 보낼 때 사용.
 * 배포 환경에서는 반드시 NEXT_PUBLIC_ADMIN_FRONTEND_URL 을 설정하세요 (예: https://admin.example.com).
 * 미설정 시 로컬 기본값 admin-frontend 포트(http://localhost:3001)를 사용합니다.
 */
export function getPartnerAdminLoginUrl() {
  const raw = process.env.NEXT_PUBLIC_ADMIN_FRONTEND_URL?.trim();
  const base = raw ? raw.replace(/\/$/, "") : "http://localhost:3001";
  return `${base}/auth/login`;
}
