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
 * 주문 번호로 배송 목록 조회
 * @param {number} orderNo - 주문 번호
 * @returns {Promise<Object>} 배송 목록
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getDeliveriesByOrderNo(orderNo) {
  try {
    const { data } = await api.get(`/api/orders/${orderNo}/deliveries`);
    return data;
  } catch (error) {
    console.error("배송 목록 조회 실패:", error);
    const errorMessage = error.response?.data?.message || "배송 목록 조회에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 주문 아이템 번호로 배송 조회
 * @param {number} orderItemNo - 주문 아이템 번호
 * @returns {Promise<Object>} 배송 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getDeliveryByOrderItemNo(orderItemNo) {
  try {
    const { data } = await api.get(`/api/order-items/${orderItemNo}/delivery`);
    return data;
  } catch (error) {
    console.error("배송 조회 실패:", error);
    const errorMessage = error.response?.data?.message || "배송 조회에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 배송 번호로 배송 조회
 * @param {number} deliveryNo - 배송 번호
 * @returns {Promise<Object>} 배송 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getDelivery(deliveryNo) {
  try {
    const { data } = await api.get(`/api/deliveries/${deliveryNo}`);
    return data;
  } catch (error) {
    console.error("배송 조회 실패:", error);
    const errorMessage = error.response?.data?.message || "배송 조회에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 배송 시작 (관리자/파트너만 가능)
 * @param {number} deliveryNo - 배송 번호
 * @param {Object} deliveryData - 배송 시작 데이터
 * @param {string} deliveryData.trackingNumber - 송장번호
 * @param {string} deliveryData.courier - 택배사
 * @returns {Promise<Object>} 배송 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function startDelivery(deliveryNo, deliveryData) {
  try {
    const { data } = await api.patch(`/api/deliveries/${deliveryNo}/start`, deliveryData);
    return data;
  } catch (error) {
    console.error("배송 시작 실패:", error);
    const errorMessage = error.response?.data?.message || "배송 시작에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 배송 완료 (관리자/파트너만 가능)
 * @param {number} deliveryNo - 배송 번호
 * @returns {Promise<Object>} 배송 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function completeDelivery(deliveryNo) {
  try {
    const { data } = await api.patch(`/api/deliveries/${deliveryNo}/complete`);
    return data;
  } catch (error) {
    console.error("배송 완료 실패:", error);
    const errorMessage = error.response?.data?.message || "배송 완료 처리에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 배송 정보 수정 (관리자/파트너만 가능)
 * @param {number} deliveryNo - 배송 번호
 * @param {Object} deliveryData - 배송 수정 데이터
 * @param {string} [deliveryData.trackingNumber] - 송장번호 (선택)
 * @param {string} [deliveryData.courier] - 택배사 (선택)
 * @returns {Promise<Object>} 배송 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function updateDelivery(deliveryNo, deliveryData) {
  try {
    const { data } = await api.patch(`/api/deliveries/${deliveryNo}`, deliveryData);
    return data;
  } catch (error) {
    console.error("배송 정보 수정 실패:", error);
    const errorMessage = error.response?.data?.message || "배송 정보 수정에 실패했습니다.";
    throw new Error(errorMessage);
  }
}
