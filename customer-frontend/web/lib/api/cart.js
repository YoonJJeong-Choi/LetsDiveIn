import { api } from "./http";

/**
 * 장바구니 조회
 * @returns {Promise<Object>} 장바구니 정보 (아이템 목록 포함)
 * @throws {Error} 에러 발생 시 에러 메시지 (401, 403 에러는 조용히 처리)
 */
export async function getCart() {
  try {
    const { data } = await api.get("/api/cart");
    return data;
  } catch (error) {
    // 401(인증 필요) 또는 403(권한 없음) 에러는 비로그인 상태에서 정상 동작이므로 조용히 처리
    if (error.response?.status === 401 || error.response?.status === 403) {
      // 에러를 throw하지 않고 조용히 실패 처리 (프론트엔드 상태는 유지됨)
      return Promise.reject(error);
    }
    console.error("장바구니 조회 실패:", error);
    throw error;
  }
}

/**
 * 장바구니에 아이템 추가
 * @param {Object} itemData - 장바구니 아이템 데이터
 * @param {number} itemData.productNo - 상품 번호
 * @param {number|null} itemData.optionNo - 옵션 번호 (선택)
 * @param {number} itemData.quantity - 수량
 * @param {number} itemData.itemPrice - 가격 (옵션 포함)
 * @returns {Promise<string>} 성공 메시지
 * @throws {Error} 에러 발생 시 에러 메시지 (401 에러는 throw하지 않음)
 */
export async function addCartItem(itemData) {
  try {
    const { data } = await api.post("/api/cart/items", itemData);
    return data;
  } catch (error) {
    // 401 에러(로그인 필요)는 비로그인 상태에서 정상 동작이므로 조용히 처리
    if (error.response?.status === 401) {
      // 에러를 throw하지 않고 조용히 실패 처리 (프론트엔드 상태는 유지됨)
      return Promise.reject(error);
    }
    console.error("장바구니 추가 실패:", error);
    throw error;
  }
}

/**
 * 장바구니 아이템 수정 (수량, 옵션 동시 변경 가능)
 * @param {number} cartItemNo - 장바구니 아이템 번호
 * @param {Object} updateData - 수정할 데이터
 * @param {number|null} updateData.quantity - 수량 (null이면 수정 안 함)
 * @param {number|null} updateData.optionNo - 옵션 번호 (null이면 옵션 변경 안 함)
 * @returns {Promise<string>} 성공 메시지
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function updateCartItem(cartItemNo, updateData) {
  try {
    const { data } = await api.put(`/api/cart/items/${cartItemNo}`, {
      quantity: updateData.quantity !== undefined ? updateData.quantity : null,
      optionNo: updateData.optionNo !== undefined ? updateData.optionNo : null,
    });
    return data;
  } catch (error) {
    console.error("장바구니 아이템 수정 실패:", error);
    throw error;
  }
}

/**
 * 장바구니 아이템 삭제
 * @param {number} cartItemNo - 장바구니 아이템 번호
 * @returns {Promise<string>} 성공 메시지
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function removeCartItem(cartItemNo) {
  try {
    const { data } = await api.delete(`/api/cart/items/${cartItemNo}`);
    return data;
  } catch (error) {
    console.error("장바구니 삭제 실패:", error);
    throw error;
  }
}
