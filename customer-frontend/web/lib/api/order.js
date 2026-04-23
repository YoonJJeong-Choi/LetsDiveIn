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
 * 주문 생성
 * @param {Object} orderData - 주문 데이터
 * @param {number[]} orderData.cartItemNos - 주문할 장바구니 아이템 번호 리스트
 * @param {string} orderData.recipientName - 수령인 이름
 * @param {string} orderData.recipientPhone - 수령인 전화번호
 * @param {string} orderData.deliveryAddress - 배송지 주소
 * @param {string} [orderData.deliveryAddressDetail] - 배송지 상세 주소 (선택)
 * @param {string} [orderData.deliveryZipCode] - 우편번호 (선택)
 * @param {string} orderData.paymentMethod - 결제 방법 (예: "CARD", "BANK_TRANSFER", "VIRTUAL_ACCOUNT")
 * @param {string} [orderData.orderMemo] - 주문 메모 (선택)
 * @returns {Promise<Object>} 주문 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function createOrder(orderData) {
  try {
    const { data } = await api.post("/api/orders", orderData);
    return data;
  } catch (error) {
    console.error("주문 생성 실패:", error);
    const errorMessage = error.response?.data?.message || "주문 생성에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 주문 목록 조회
 * @returns {Promise<Object[]>} 주문 목록
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getOrders({ page = 1, size = 10 } = {}) {
  try {
    // 1-based(UI) -> 0-based(server)
    const qp = { page: Math.max(0, Number(page) - 1), size };
    const { data } = await api.get("/api/orders", { params: qp });
    return data?.data || data;
  } catch (error) {
    console.error("주문 목록 조회 실패:", error);
    throw error;
  }
}

/**
 * 주문 상세 조회
 * @param {number} orderNo - 주문 번호
 * @returns {Promise<Object>} 주문 상세 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getOrder(orderNo) {
  try {
    const { data } = await api.get(`/api/orders/${orderNo}`);
    return data;
  } catch (error) {
    console.error("주문 상세 조회 실패:", error);
    throw error;
  }
}

/**
 * 주문 취소 (고객용)
 * @param {number} orderNo - 주문 번호
 * @returns {Promise<Object>} 주문 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function cancelOrder(orderNo) {
  try {
    const { data } = await api.patch(`/api/orders/${orderNo}/cancel`);
    return data;
  } catch (error) {
    console.error("주문 취소 실패:", error);
    const errorMessage =
      error.response?.data?.message || "주문 취소에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 주문 상품별 구매 확정 (고객이 수령 확인)
 * @param {number} orderItemNo - 주문 상품 번호
 * @returns {Promise<Object>} 주문 상품 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function completeOrderItem(orderItemNo) {
  try {
    const { data } = await api.post(`/api/orders/order-items/${orderItemNo}/complete`);
    return data;
  } catch (error) {
    console.error("구매 확정 실패:", error);
    const errorMessage =
      error.response?.data?.message || "구매 확정에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 주문서 진입 시 15분 가격 보장(price-lock) 시작
 * @returns {Promise<Object>}
 */
export async function startPriceLock() {
  try {
    const { data } = await api.post("/api/orders/price-lock/start");
    return data;
  } catch (error) {
    console.error("price-lock start 실패:", error);
    const errorMessage =
      error.response?.data?.message || "가격 보장 시작에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 현재 price-lock 상태 조회
 * @returns {Promise<Object>}
 */
export async function getPriceLockStatus() {
  try {
    const { data } = await api.get("/api/orders/price-lock/status");
    return data;
  } catch (error) {
    console.error("price-lock status 실패:", error);
    const errorMessage =
      error.response?.data?.message || "가격 보장 상태 조회에 실패했습니다.";
    throw new Error(errorMessage);
  }
}