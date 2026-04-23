import axios from "axios";

const baseURL =
  typeof window !== "undefined"
    ? (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080")
    : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

export async function createPayment(payload) {
  try {
    const { data } = await api.post("/api/payments", payload);
    return data?.data || data;
  } catch (error) {
    console.error("결제 요청 생성 실패:", error);
    throw error;
  }
}

export async function confirmPayment(payload) {
  try {
    const { data } = await api.post("/api/payments/confirm", payload);
    return data?.data || data;
  } catch (error) {
    console.error("결제 승인(confirm) 실패:", error);
    throw error;
  }
}

// 임시 승인/실패 API는 제거되었습니다.
