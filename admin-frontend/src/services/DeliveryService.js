import fetch from 'auth/FetchInterceptor'

const DeliveryService = {}

/**
 * 전체 배송 목록을 조회합니다.
 * 관리자: 모든 배송 조회
 * 파트너: 자신의 상품 배송만 조회
 * @returns {Promise} 배송 목록
 */
DeliveryService.getAllDeliveries = function () {
	return fetch({
		url: '/deliveries',
		method: 'get'
	})
}

/**
 * 주문 번호로 배송 목록을 조회합니다.
 * @param {Number} orderNo - 주문 번호
 * @returns {Promise} 배송 목록
 */
DeliveryService.getDeliveriesByOrderNo = function (orderNo) {
	return fetch({
		url: `/orders/${orderNo}/deliveries`,
		method: 'get'
	})
}

/**
 * 주문 아이템 번호로 배송 정보를 조회합니다.
 * @param {Number} orderItemNo - 주문 아이템 번호
 * @returns {Promise} 배송 정보
 */
DeliveryService.getDeliveryByOrderItemNo = function (orderItemNo) {
	return fetch({
		url: `/order-items/${orderItemNo}/delivery`,
		method: 'get'
	})
}

/**
 * 배송 번호로 배송 정보를 조회합니다.
 * @param {Number} deliveryNo - 배송 번호
 * @returns {Promise} 배송 정보
 */
DeliveryService.getDelivery = function (deliveryNo) {
	return fetch({
		url: `/deliveries/${deliveryNo}`,
		method: 'get'
	})
}

/**
 * 배송을 시작합니다 (READY -> SHIPPED).
 * @param {Number} deliveryNo - 배송 번호
 * @param {Object} deliveryData - 배송 데이터 (trackingNumber, courier)
 * @returns {Promise} 배송 정보
 */
DeliveryService.startDelivery = function (deliveryNo, deliveryData) {
	return fetch({
		url: `/deliveries/${deliveryNo}/start`,
		method: 'patch',
		data: deliveryData
	})
}

/**
 * 배송을 완료 처리합니다 (SHIPPED -> DELIVERED).
 * @param {Number} deliveryNo - 배송 번호
 * @returns {Promise} 배송 정보
 */
DeliveryService.completeDelivery = function (deliveryNo) {
	return fetch({
		url: `/deliveries/${deliveryNo}/complete`,
		method: 'patch'
	})
}

/**
 * 배송 정보를 수정합니다.
 * @param {Number} deliveryNo - 배송 번호
 * @param {Object} deliveryData - 배송 데이터 (trackingNumber, courier)
 * @returns {Promise} 배송 정보
 */
DeliveryService.updateDelivery = function (deliveryNo, deliveryData) {
	return fetch({
		url: `/deliveries/${deliveryNo}`,
		method: 'patch',
		data: deliveryData
	})
}

export default DeliveryService;
