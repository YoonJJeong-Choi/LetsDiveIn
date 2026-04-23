import fetch from 'auth/FetchInterceptor'

const ReturnService = {}

/**
 * 전체 반품 목록 조회 (관리자/파트너용)
 * 관리자: 모든 반품 조회
 * 파트너: 자신의 상품 반품만 조회
 * @returns {Promise} 반품 목록
 */
ReturnService.getAllReturns = function () {
	return fetch({
		url: '/returns',
		method: 'get'
	})
}

/**
 * 반품 상세 조회
 * @param {Number} returnNo - 반품 번호
 * @returns {Promise} 반품 상세 정보
 */
ReturnService.getReturn = function (returnNo) {
	return fetch({
		url: `/returns/${returnNo}`,
		method: 'get'
	})
}

/**
 * 관리자 반품 AI 보조 결과 조회 (관리자 전용)
 * @param {Number} returnNo - 반품 번호
 * @returns {Promise} AI 보조 결과
 */
ReturnService.getReturnAiAssist = function (returnNo) {
	return fetch({
		url: `/returns/${returnNo}/ai-assist`,
		method: 'get'
	})
}

/**
 * 반품 상태 변경 (관리자/파트너용)
 * @param {Number} returnNo - 반품 번호
 * @param {Object} updateData - 업데이트 데이터
 * @param {String} updateData.returnStatus - 반품 상태 (PICKUP_COMPLETED, REFUNDED)
 * @param {String} [updateData.returnTrackingNumber] - 반품 송장번호 (선택)
 * @param {String} [updateData.returnCourier] - 반품 택배사 (선택)
 * @returns {Promise} 반품 정보
 */
ReturnService.updateReturnStatus = function (returnNo, updateData) {
	return fetch({
		url: `/returns/${returnNo}`,
		method: 'patch',
		data: updateData
	})
}

/**
 * 반품 승인 (관리자/파트너용)
 * @param {Number} returnNo - 반품 번호
 * @returns {Promise} 반품 정보
 */
ReturnService.approveReturn = function (returnNo) {
	return fetch({
		url: `/returns/${returnNo}/approve`,
		method: 'post'
	})
}

/**
 * 반품 거절 (관리자/파트너용)
 * @param {Number} returnNo - 반품 번호
 * @param {String} rejectionReason - 거절 사유
 * @returns {Promise} 반품 정보
 */
ReturnService.rejectReturn = function (returnNo, rejectionReason) {
	return fetch({
		url: `/returns/${returnNo}/reject`,
		method: 'post',
		data: {
			rejectionReason: rejectionReason
		}
	})
}

/**
 * 반품 변경 이력 조회 (관리자/파트너용)
 * @param {Number} returnNo - 반품 번호
 * @returns {Promise} 반품 변경 이력 목록
 */
ReturnService.getReturnHistory = function (returnNo) {
	return fetch({
		url: `/returns/${returnNo}/history`,
		method: 'get'
	})
}

export default ReturnService
