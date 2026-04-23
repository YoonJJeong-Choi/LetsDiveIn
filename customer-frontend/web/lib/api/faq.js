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

/**
 * FAQ 목록 조회 (공개 API)
 * @param {string} category - 카테고리 (선택, 예: "주문/결제", "배송", "취소/반품/교환", "회원정보", "상품", "포인트/쿠폰", "기타")
 * @returns {Promise<{success: boolean, data: Array}>} FAQ 목록
 */
export async function getFaqList(category = null) {
  try {
    const params = category ? { category } : {};
    const { data } = await api.get("/api/faq", { params });
    return data;
  } catch (error) {
    console.error("FAQ 목록 조회 실패:", error);
    throw error;
  }
}

/**
 * FAQ 상세 조회 (공개 API)
 * @param {number} faqNo - FAQ 번호
 * @returns {Promise<{success: boolean, data: Object}>} FAQ 정보
 */
export async function getFaqDetail(faqNo) {
  try {
    const { data } = await api.get(`/api/faq/${faqNo}`);
    return data;
  } catch (error) {
    console.error("FAQ 상세 조회 실패:", error);
    throw error;
  }
}
