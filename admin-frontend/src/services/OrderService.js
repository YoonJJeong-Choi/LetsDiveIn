import fetch from 'auth/FetchInterceptor'

const OrderService = {}

/**
 * 주문 목록을 조회합니다.
 * @returns {Promise} 주문 목록
 */
OrderService.getOrders = function () {
	return fetch({
		url: '/orders',
		method: 'get'
	})
}

/**
 * 주문 상세 정보를 조회합니다.
 * @param {Number} orderNo - 주문 번호
 * @returns {Promise} 주문 상세 정보
 */
OrderService.getOrder = function (orderNo) {
	return fetch({
		url: `/orders/${orderNo}`,
		method: 'get'
	})
}

/**
 * 전체 주문 목록 조회 (관리자/파트너용) - 서버 페이지네이션/필터 지원
 * @param {{page?: number, size?: number, status?: string}} params
 * @returns {Promise<{items: any[], total: number, page: number, size: number}>}
 */
OrderService.getAllOrders = function (params = {}) {
	const qp = { ...params };
	if (qp.page !== undefined && qp.page !== null) {
		qp.page = Math.max(0, Number(qp.page) - 1);
	}
	return fetch({
		url: '/orders/admin',
		method: 'get',
		params: qp
	})
}

// getAllOrdersPaged deprecated: 제거됨. getAllOrders(params)를 사용하세요.

/**
 * 주문 상세 조회 (관리자/파트너용)
 * @param {Number} orderNo - 주문 번호
 * @returns {Promise} 주문 상세 정보
 */
OrderService.getOrderForAdmin = function (orderNo) {
	return fetch({
		url: `/orders/${orderNo}/admin`,
		method: 'get'
	})
}

/**
 * 주문 상태 변경 (관리자만)
 * @param {Number} orderNo - 주문 번호
 * @param {String} orderStatus - 변경할 주문 상태 (예: "ACTIVE", "CANCELLED")
 * @returns {Promise} 업데이트된 주문 정보
 */
OrderService.updateOrderStatus = function (orderNo, orderStatus) {
	return fetch({
		url: `/orders/${orderNo}/status`,
		method: 'patch',
		data: {
			orderStatus: orderStatus
		}
	})
}

/**
 * 발주 확인 (파트너/관리자용) - 주문 전체 발주 확인 (레거시)
 * @deprecated 주문 상품별 발주 확인을 사용하세요. (confirmOrderItem)
 * @param {Number} orderNo - 주문 번호
 * @returns {Promise} 업데이트된 주문 정보
 */
OrderService.confirmOrder = function (orderNo) {
	return fetch({
		url: `/orders/${orderNo}/confirm`,
		method: 'post'
	})
}

/**
 * 주문 상품별 발주 확인 (파트너/관리자용)
 * @param {Number} orderItemNo - 주문 상품 번호
 * @returns {Promise} 업데이트된 주문 상품 정보
 */
OrderService.confirmOrderItem = function (orderItemNo) {
	return fetch({
		url: `/orders/order-items/${orderItemNo}/confirm`,
		method: 'post'
	})
}

export default OrderService;
