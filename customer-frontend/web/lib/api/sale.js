import axios from "axios";

const baseURL =
  typeof window !== "undefined"
    ? process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
    : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

/**
 * 주문 생성 직전(시뮬레이션) 세일 적용 조회
 * - 백엔드에서 `at` 미지정 시, price-lock 세션이 활성화되어 있으면 락 시작 시각으로 판정합니다.
 *
 * @param {Object} params
 * @param {number} params.productNo
 * @param {number|null} params.optionNo
 * @param {string|null} [params.at] ISO 8601 datetime string (optional)
 */
export async function getApplicableSale({ productNo, optionNo = null, at = null }) {
  try {
    const params = {
      productNo,
      optionNo: optionNo === null ? undefined : optionNo,
      at: at ?? undefined,
    };
    const { data } = await api.get("/api/sales/applicable", { params });
    return data;
  } catch (error) {
    console.error("세일 적용 조회 실패:", error);
    const errorMessage =
      error.response?.data?.message || "세일 적용 조회에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

