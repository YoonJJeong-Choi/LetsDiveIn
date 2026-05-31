import { api } from "./http";

/**
 * 리뷰 작성
 * @param {Object} reviewData - 리뷰 데이터
 * @param {number} reviewData.orderItemNo - 주문 아이템 번호
 * @param {string} reviewData.reviewContent - 리뷰 내용
 * @param {number} reviewData.reviewRating - 평점 (1-5)
 * @returns {Promise<Object>} 리뷰 응답 데이터
 */
export async function createReview(reviewData) {
  try {
    const orderItemNo = Number(reviewData.orderItemNo);
    const { data } = await api.post("/api/reviews", {
      orderItemNo: Number.isFinite(orderItemNo) ? orderItemNo : reviewData.orderItemNo,
      reviewContent: reviewData.reviewContent,
      reviewRating: reviewData.reviewRating,
      ...(Array.isArray(reviewData.imageFileIds) && reviewData.imageFileIds.length > 0
        ? { imageFileIds: reviewData.imageFileIds }
        : {}),
    });
    return data.data || data;
  } catch (error) {
    throw new Error(
      error.response?.data?.message || error.message || "리뷰 작성에 실패했습니다."
    );
  }
}

/**
 * 리뷰 수정 (고객용 - 제한됨)
 * 주의: 리뷰 작성 후 수정/삭제는 불가능합니다. (리뷰 신뢰성 보장)
 * 관리자만 리뷰 삭제가 가능합니다.
 * @deprecated 고객은 리뷰 수정 불가
 */
// export async function updateReview(reviewNo, reviewData) {
//   // 리뷰 작성 후 수정 불가
//   throw new Error("리뷰 작성 후 수정할 수 없습니다.");
// }

/**
 * 리뷰 삭제 (고객용 - 제한됨)
 * 주의: 리뷰 작성 후 수정/삭제는 불가능합니다. (리뷰 신뢰성 보장)
 * 관리자만 리뷰 삭제가 가능합니다.
 * @deprecated 고객은 리뷰 삭제 불가
 */
// export async function deleteReview(reviewNo) {
//   // 리뷰 작성 후 삭제 불가
//   throw new Error("리뷰 작성 후 삭제할 수 없습니다.");
// }

/**
 * 상품별 리뷰 목록 조회
 * @param {number} productNo - 상품 번호
 * @returns {Promise<Array>} 리뷰 목록
 */
export async function getReviewsByProduct(productNo, { page, size, sort } = {}) {
  try {
    const qp = {};
    if (page !== undefined && page !== null) qp.page = Math.max(0, Number(page) - 1);
    if (size !== undefined && size !== null) qp.size = size;
    if (sort !== undefined && sort !== null) qp.sort = sort;
    const { data } = await api.get(`/api/reviews/product/${productNo}`, { params: qp });
    return data.data || data || [];
  } catch (error) {
    throw new Error(
      error.response?.data?.message || error.message || "리뷰 목록 조회에 실패했습니다."
    );
  }
}

/**
 * 내 리뷰 목록 조회 (고객용)
 * @returns {Promise<Array>} 리뷰 목록
 */
export async function getMyReviews({ page = 1, size = 10 } = {}) {
  try {
    const qp = { page: Math.max(0, Number(page) - 1), size };
    const { data } = await api.get("/api/reviews/customer", { params: qp });
    return data?.data || data;
  } catch (error) {
    throw new Error(
      error.response?.data?.message || error.message || "리뷰 목록 조회에 실패했습니다."
    );
  }
}

export async function getWritableReviews({ page = 1, size = 10 } = {}) {
  try {
    const qp = { page: Math.max(0, Number(page) - 1), size };
    const { data } = await api.get("/api/reviews/writable", { params: qp });
    return data?.data || data;
  } catch (error) {
    throw new Error(
      error.response?.data?.message || error.message || "작성 가능 리뷰 조회에 실패했습니다."
    );
  }
}

/**
 * 리뷰 상세 조회
 * @param {number} reviewNo - 리뷰 번호
 * @returns {Promise<Object>} 리뷰 상세 데이터
 */
export async function getReview(reviewNo) {
  try {
    const { data } = await api.get(`/api/reviews/${reviewNo}`);
    return data.data || data;
  } catch (error) {
    throw new Error(
      error.response?.data?.message || error.message || "리뷰 조회에 실패했습니다."
    );
  }
}
