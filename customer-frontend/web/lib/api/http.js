import axios from "axios";

/** SSR·서버에서 백엔드 직접 호출 시 사용할 절대 URL */
export function resolveServerApiBaseURL() {
  return (
    process.env.INTERNAL_API_URL ||
    process.env.NEXT_PUBLIC_API_URL ||
    "http://localhost:8080"
  );
}

/**
 * 브라우저: NEXT_PUBLIC_API_URL을 쓰지 않으면 상대경로("") → Next rewrites가 백엔드로 프록시.
 * 프로덕션에서 API 호스트가 따로면 .env에 NEXT_PUBLIC_API_URL만 두면 됨.
 */
export function resolveBrowserApiBaseURL() {
  const u = process.env.NEXT_PUBLIC_API_URL;
  if (typeof u === "string" && u.trim() !== "") return u.trim();
  return "";
}

export const api = axios.create({
  baseURL:
    typeof window === "undefined"
      ? resolveServerApiBaseURL()
      : resolveBrowserApiBaseURL(),
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});
