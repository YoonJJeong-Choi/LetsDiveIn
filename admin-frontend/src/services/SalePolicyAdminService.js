import fetch from 'auth/FetchInterceptor'

const SalePolicyAdminService = {}

// 관리자 전체 세일 정책 목록 조회 (상태별 정렬은 백엔드에서 처리)
SalePolicyAdminService.listAdminSales = function () {
	return fetch({
		url: '/admin/sales',
		method: 'get'
	})
}

// 세일 승인/거절/취소
SalePolicyAdminService.approveSale = function (id) {
	return fetch({
		url: `/admin/sales/${id}/approve`,
		method: 'patch'
	})
}

SalePolicyAdminService.rejectSale = function (id, rejectionReason) {
	return fetch({
		url: `/admin/sales/${id}/reject`,
		method: 'patch',
		data: { rejectionReason }
	})
}

SalePolicyAdminService.cancelSale = function (id, cancelReason) {
	return fetch({
		url: `/admin/sales/${id}/cancel`,
		method: 'patch',
		data: { cancelReason }
	})
}

export default SalePolicyAdminService

