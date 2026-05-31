import fetch from 'auth/FetchInterceptor'

const ReviewService = {}

/** Spring @RequestParam List<Long> optionNos 에 맞춤: optionNos=1&optionNos=2 (axios 기본 optionNos[]=… 는 바인딩 실패 가능) */
function serializePartnerAiCandidatesParams(params) {
	const parts = []
	if (params.productNo != null && params.productNo !== '') {
		parts.push(`productNo=${encodeURIComponent(params.productNo)}`)
	}
	if (Array.isArray(params.optionNos) && params.optionNos.length > 0) {
		params.optionNos.forEach((n) => {
			if (n != null && n !== '') parts.push(`optionNos=${encodeURIComponent(n)}`)
		})
	}
	if (params.fromAt) parts.push(`fromAt=${encodeURIComponent(params.fromAt)}`)
	if (params.toAt) parts.push(`toAt=${encodeURIComponent(params.toAt)}`)
	if (params.limit != null && params.limit !== '') parts.push(`limit=${encodeURIComponent(params.limit)}`)
	return parts.join('&')
}

/**
 * 전체 리뷰 목록 조회 (관리자/파트너용) - 서버 페이지네이션
 * @param {{page?: number, size?: number}} params
 * @returns {Promise<{items: any[], total: number, page: number, size: number}>}
 */
ReviewService.getAllReviews = function (params = {}) {
	const qp = { ...params };
	if (qp.page !== undefined && qp.page !== null) {
		qp.page = Math.max(0, Number(qp.page) - 1);
	}
	return fetch({
		url: '/reviews',
		method: 'get',
		params: qp
	})
}

/**
 * 리뷰 상세 조회
 * @param {Number} reviewNo - 리뷰 번호
 * @returns {Promise} 리뷰 상세 정보
 */
ReviewService.getReview = function (reviewNo) {
	return fetch({
		url: `/reviews/${reviewNo}`,
		method: 'get'
	})
}

/**
 * 상품별 리뷰 목록 조회
 * @param {Number} productNo - 상품 번호
 * @returns {Promise} 리뷰 목록
 */
ReviewService.getReviewsByProduct = function (productNo) {
	return fetch({
		url: `/reviews/product/${productNo}`,
		method: 'get'
	})
}

/**
 * 리뷰 삭제 (관리자만)
 * @param {Number} reviewNo - 리뷰 번호
 * @returns {Promise}
 */
ReviewService.deleteReview = function (reviewNo) {
	return fetch({
		url: `/reviews/${reviewNo}`,
		method: 'delete'
	})
}

/**
 * 파트너 리뷰 목록 조회 (파트너의 상품 리뷰만) - 서버 페이지네이션
 * @param {{page?: number, size?: number}} params
 * @returns {Promise<{items: any[], total: number, page: number, size: number}>}
 */
ReviewService.getReviewsByPartner = function (params = {}) {
	const qp = { ...params };
	if (qp.page !== undefined && qp.page !== null) {
		qp.page = Math.max(0, Number(qp.page) - 1);
	}
	return fetch({
		url: '/reviews/partner',
		method: 'get',
		params: qp
	})
}

/**
 * 파트너 리뷰 AI 분석용: 상품·옵션·기간 기준 최신 N건 후보 (DB 직접 조회)
 * @param {{ productNo: number, optionNos?: number[], fromAt?: string, toAt?: string, limit?: number }} params
 */
ReviewService.getPartnerReviewAiCandidates = function (params = {}) {
	const qp = { ...params };
	if (qp.productNo === undefined || qp.productNo === null) {
		return Promise.reject(new Error('productNo is required'));
	}
	return fetch({
		url: '/reviews/partner/ai-candidates',
		method: 'get',
		params: qp,
		paramsSerializer: serializePartnerAiCandidatesParams
	})
}

/**
 * 리뷰 답변 작성 (파트너만)
 * @param {Number} reviewNo - 리뷰 번호
 * @param {String} reviewReply - 답변 내용
 * @returns {Promise} 리뷰 정보
 */
ReviewService.addReviewReply = function (reviewNo, reviewReply) {
	return fetch({
		url: `/reviews/${reviewNo}/reply`,
		method: 'post',
		data: {
			reviewReply: reviewReply
		}
	})
}

/**
 * 리뷰 답변 수정 (파트너만)
 * @param {Number} reviewNo - 리뷰 번호
 * @param {String} reviewReply - 답변 내용
 * @returns {Promise} 리뷰 정보
 */
ReviewService.updateReviewReply = function (reviewNo, reviewReply) {
	return fetch({
		url: `/reviews/${reviewNo}/reply`,
		method: 'put',
		data: {
			reviewReply: reviewReply
		}
	})
}

/**
 * 리뷰 답변 삭제 (파트너만)
 * @param {Number} reviewNo - 리뷰 번호
 * @returns {Promise} 리뷰 정보
 */
ReviewService.deleteReviewReply = function (reviewNo) {
	return fetch({
		url: `/reviews/${reviewNo}/reply`,
		method: 'delete'
	})
}

export default ReviewService
