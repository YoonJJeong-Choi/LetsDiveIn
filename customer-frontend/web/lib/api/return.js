import { api } from "./http";

/**
 * 반품 신청 (고객용)
 * @param {Object} payload
 * @param {number} payload.orderItemNo - 주문 아이템 번호
 * @param {string} payload.returnReasonType - 반품 사유 유형 (CHANGE_OF_MIND | DEFECT | WRONG_ITEM | ORDER_MISTAKE | OTHER)
 * @param {string} [payload.returnReason] - 상세 사유 (유형에 따라 필수, 최대 200자)
 * @param {number[]} [payload.imageFileIds=[]] - 반품 이미지 파일 ID (불량·쇼핑몰 측 오배송·기타는 최소 1장)
 * @returns {Promise<Object>} 반품 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function requestReturn({ orderItemNo, returnReasonType, returnReason = "", imageFileIds = [] }) {
  try {
    const { data } = await api.post("/api/returns", {
      orderItemNo,
      returnReasonType,
      returnReason,
      imageFileIds,
    });
    return data;
  } catch (error) {
    console.error("반품 신청 실패:", error);
    const errorMessage =
      error.response?.data?.message || "반품 신청에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 고객 반품 목록 조회
 * @returns {Promise<Object[]>} 반품 목록
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getReturnsByCustomer() {
  try {
    const { data } = await api.get("/api/returns/customer");
    return data;
  } catch (error) {
    console.error("반품 목록 조회 실패:", error);
    throw error;
  }
}

/**
 * 반품 상세 조회
 * @param {number} returnNo - 반품 번호
 * @returns {Promise<Object>} 반품 상세 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getReturn(returnNo) {
  try {
    const { data } = await api.get(`/api/returns/${returnNo}`);
    return data;
  } catch (error) {
    console.error("반품 상세 조회 실패:", error);
    throw error;
  }
}

/**
 * 반품 상태 한글 변환 헬퍼 함수
 * @param {string} status - 반품 상태
 * @returns {string} 한글 상태명
 */
export function getReturnStatusLabel(status) {
  if (!status) return "";
  switch (status) {
    case "REQUESTED":
      return "반품신청";
    case "APPROVED":
      return "반품승인";
    case "REJECTED":
      return "반품거절";
    case "PICKUP_COMPLETED":
      return "수거완료";
    case "REFUNDED":
      return "환불완료";
    default:
      return status;
  }
}