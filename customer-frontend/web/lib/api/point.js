import { api } from "./http";

/**
 * 포인트 잔액 조회
 * @returns {Promise<Object>} 포인트 잔액 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getPointBalance() {
  try {
    const { data } = await api.get("/api/customer/points/balance");
    return data;
  } catch (error) {
    console.error("포인트 잔액 조회 실패:", error);
    if (error.response?.status === 401) {
      return null;
    }
    throw error;
  }
}

/**
 * 포인트 내역 조회
 * @returns {Promise<Object[]>} 포인트 내역 목록
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getPointHistory({ page = 1, size = 10 } = {}) {
  try {
    const qp = { page: Math.max(0, Number(page) - 1), size };
    const { data } = await api.get("/api/customer/points/history", { params: qp });
    return data?.data || data;
  } catch (error) {
    console.error("포인트 내역 조회 실패:", error);
    if (error.response?.status === 401) {
      return { items: [], total: 0, page, size };
    }
    throw error;
  }
}
