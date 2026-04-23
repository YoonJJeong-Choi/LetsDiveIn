import axios from "axios";

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

/**
 * 통합 로그인 (고객/파트너/관리자)
 * @returns {{ role, subjectId, email, name }}
 */
export async function login(email, password) {
  const { data } = await api.post("/api/auth/login", { email, password });
  return data;
}

/**
 * 로그아웃
 */
export async function logout() {
  await api.post("/api/auth/logout");
}

/**
 * 현재 로그인 사용자 확인
 * @returns {{ role, subjectId, email, name }} 또는 401 시 null
 */
export async function getMe() {
  try {
    const { data } = await api.get("/api/auth/me");
    return data;
  } catch (err) {
    if (err.response?.status === 401) return null;
    throw err;
  }
}
