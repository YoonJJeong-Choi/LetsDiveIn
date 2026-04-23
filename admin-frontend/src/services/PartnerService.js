import fetch from 'auth/FetchInterceptor'

const PartnerService = {}

/**
 * 현재 로그인한 파트너의 상품 목록을 조회합니다.
 * @returns {Promise} 상품 목록 (각 상품에 옵션 포함)
 */
PartnerService.getMyProducts = function () {
	return fetch({
		url: '/partner/products',
		method: 'get'
	})
}

/**
 * 파트너 상품을 등록합니다.
 * @param {Object} productData - 상품 등록 데이터
 * @returns {Promise} 등록된 상품 정보
 */
PartnerService.createProduct = function (productData) {
	return fetch({
		url: '/partner/products',
		method: 'post',
		data: productData
	})
}

/**
 * 파트너 상품을 수정합니다.
 * @param {Number} productNo - 수정할 상품 번호
 * @param {Object} productData - 상품 수정 데이터
 * @returns {Promise} 수정된 상품 정보
 */
PartnerService.updateProduct = function (productNo, productData) {
	return fetch({
		url: `/partner/products/${productNo}`,
		method: 'put',
		data: productData
	})
}

/**
 * 파트너 상품을 삭제합니다 (소프트 삭제).
 * @param {Number} productNo - 삭제할 상품 번호
 * @returns {Promise} 삭제 성공 여부
 */
PartnerService.deleteProduct = function (productNo) {
	return fetch({
		url: `/partner/products/${productNo}`,
		method: 'delete'
	})
}

/**
 * 파트너 상품 수정 신청 취소 (PENDING_UPDATE → ACTIVE)
 * @param {Number} productNo - 취소할 상품 번호
 * @returns {Promise} 취소된 상품 정보
 */
PartnerService.cancelProductUpdate = function (productNo) {
	return fetch({
		url: `/partner/products/${productNo}/cancel-update`,
		method: 'patch'
	})
}

/**
 * 파트너 목록 조회 (상태별 필터링 및 휴업 신청 필터)
 * @param {Object} params - 쿼리 파라미터
 * @param {string} params.status - 필터링할 상태 (PENDING, APPROVED, REJECTED, INACTIVE)
 * @param {boolean} params.hasDeactivationRequest - 휴업 신청이 있는 파트너만 조회 (true)
 * @returns {Promise} 파트너 목록
 */
PartnerService.getAllPartners = function (params = {}) {
	const queryParams = new URLSearchParams();
	if (params.status) queryParams.append('status', params.status);
	if (params.hasDeactivationRequest) queryParams.append('hasDeactivationRequest', 'true');
	
	const queryString = queryParams.toString();
	const url = `/admin/partners${queryString ? `?${queryString}` : ''}`;
	
	return fetch({
		url: url,
		method: 'get'
	})
}

/**
 * 파트너 신청 목록 조회 (PENDING 상태만)
 * @returns {Promise} 파트너 신청 목록
 */
PartnerService.getPendingPartners = function () {
	return fetch({
		url: '/admin/partners/pending',
		method: 'get'
	})
}

/**
 * 파트너 상세 조회
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 파트너 상세 정보
 */
PartnerService.getPartnerDetail = function (partnerId) {
	return fetch({
		url: `/admin/partners/${partnerId}`,
		method: 'get'
	})
}

/**
 * 파트너 승인
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 승인된 파트너 정보
 */
PartnerService.approvePartner = function (partnerId) {
	return fetch({
		url: `/admin/partners/${partnerId}/approve`,
		method: 'patch'
	})
}

/**
 * 파트너 거절
 * @param {Number} partnerId - 파트너 ID
 * @param {Object} requestData - 거절 정보
 * @param {string} requestData.rejectionReason - 거절 사유
 * @returns {Promise} 거절 결과
 */
PartnerService.rejectPartner = function (partnerId, requestData) {
	return fetch({
		url: `/admin/partners/${partnerId}/reject`,
		method: 'patch',
		data: requestData
	})
}

/**
 * 파트너 비활성화 (APPROVED → INACTIVE)
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 비활성화된 파트너 정보
 */
PartnerService.deactivatePartner = function (partnerId) {
	return fetch({
		url: `/admin/partners/${partnerId}/deactivate`,
		method: 'patch'
	})
}

/**
 * 파트너 재활성화 (INACTIVE → APPROVED)
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 재활성화된 파트너 정보
 */
PartnerService.activatePartner = function (partnerId) {
	return fetch({
		url: `/admin/partners/${partnerId}/activate`,
		method: 'patch'
	})
}

/**
 * 휴업 신청 승인
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 비활성화된 파트너 정보
 */
PartnerService.approveDeactivationRequest = function (partnerId) {
	return fetch({
		url: `/admin/partners/${partnerId}/deactivation-request/approve`,
		method: 'patch'
	})
}

/**
 * 휴업 신청 거절
 * @param {Number} partnerId - 파트너 ID
 * @param {Object} requestData - 거절 정보
 * @param {string} requestData.rejectionReason - 거절 사유
 * @returns {Promise} 거절 결과
 */
PartnerService.rejectDeactivationRequest = function (partnerId, requestData) {
	return fetch({
		url: `/admin/partners/${partnerId}/deactivation-request/reject`,
		method: 'patch',
		data: requestData
	})
}

/**
 * 재활성화 신청 거절
 * @param {Number} partnerId - 파트너 ID
 * @param {Object} requestData - 거절 정보
 * @param {string} requestData.rejectionReason - 거절 사유
 * @returns {Promise} 거절 결과
 */
PartnerService.rejectReactivationRequest = function (partnerId, requestData) {
	return fetch({
		url: `/admin/partners/${partnerId}/reactivation-request/reject`,
		method: 'patch',
		data: requestData
	})
}

/**
 * 파트너 이력 조회 (관리자용)
 * @param {Number} partnerId - 파트너 ID
 * @param {Object} params - 쿼리 파라미터
 * @param {string} params.actionType - 액션 타입 필터 (APPLICATION, APPROVAL, REJECTION, DEACTIVATION_REQUEST, DEACTIVATION_APPROVED, DEACTIVATION_REJECTION, REACTIVATION_REQUEST, REACTIVATION_APPROVED, REACTIVATION_REJECTION, DEACTIVATED, ACTIVATED)
 * @returns {Promise} 파트너 이력 목록
 */
PartnerService.getPartnerHistory = function (partnerId, params = {}) {
	const queryParams = new URLSearchParams();
	if (params.actionType) queryParams.append('actionType', params.actionType);
	
	const queryString = queryParams.toString();
	const url = `/admin/partners/${partnerId}/history${queryString ? `?${queryString}` : ''}`;
	
	return fetch({
		url: url,
		method: 'get'
	})
}

/**
 * 현재 로그인한 파트너의 정보를 조회합니다.
 * @returns {Promise} 파트너 정보
 */
PartnerService.getMyPartnerInfo = function () {
	return fetch({
		url: '/partner/me',
		method: 'get'
	})
}

/**
 * 현재 로그인한 파트너의 저위험 프로필 조회
 * @returns {Promise} 파트너 프로필
 */
PartnerService.getMyProfile = function () {
	return fetch({
		url: '/partner/profile',
		method: 'get'
	})
}

/**
 * 현재 로그인한 파트너의 저위험 프로필 수정
 * @param {Object} requestData
 * @returns {Promise} 수정된 파트너 프로필
 */
PartnerService.updateMyProfile = function (requestData) {
	return fetch({
		url: '/partner/profile',
		method: 'patch',
		data: requestData
	})
}

/**
 * 현재 로그인한 파트너의 고위험 정보 변경 신청 생성
 * @param {Object} requestData
 * @returns {Promise} 생성된 변경 신청
 */
PartnerService.createProfileChangeRequest = function (requestData) {
	return fetch({
		url: '/partner/profile-change-requests',
		method: 'post',
		data: requestData
	})
}

/**
 * 현재 로그인한 파트너의 고위험 정보 변경 신청 목록 조회
 * @returns {Promise} 변경 신청 목록
 */
PartnerService.getMyProfileChangeRequests = function () {
	return fetch({
		url: '/partner/profile-change-requests',
		method: 'get'
	})
}

/**
 * 현재 로그인한 파트너의 이력을 조회합니다.
 * @param {Object} params - 쿼리 파라미터
 * @param {string} params.actionType - 액션 타입 필터 (APPLICATION, APPROVAL, REJECTION, DEACTIVATION_REQUEST, DEACTIVATION_APPROVED, DEACTIVATION_REJECTION, REACTIVATION_REQUEST, REACTIVATION_APPROVED, REACTIVATION_REJECTION, DEACTIVATED, ACTIVATED)
 * @returns {Promise} 파트너 이력 목록
 */
PartnerService.getMyHistory = function (params = {}) {
	const queryParams = new URLSearchParams();
	if (params.actionType) queryParams.append('actionType', params.actionType);
	
	const queryString = queryParams.toString();
	const url = `/partner/history${queryString ? `?${queryString}` : ''}`;
	
	return fetch({
		url: url,
		method: 'get'
	})
}

/**
 * 휴업 신청
 * @param {Object} requestData - 휴업 신청 데이터
 * @param {string} requestData.deactivationReason - 휴업 신청 사유
 * @returns {Promise} 신청 결과
 */
PartnerService.requestDeactivation = function (requestData) {
	return fetch({
		url: '/partner/deactivation/request',
		method: 'post',
		data: requestData
	})
}

/**
 * 재활성화 신청
 * @param {Object} requestData - 재활성화 신청 데이터
 * @param {string} requestData.reactivationReason - 재활성화 신청 사유
 * @returns {Promise} 신청 결과
 */
PartnerService.requestReactivation = function (requestData) {
	return fetch({
		url: '/partner/reactivation/request',
		method: 'post',
		data: requestData
	})
}

/**
 * 파트너의 전체 재고 목록 조회
 * @returns {Promise} 재고 목록
 */
PartnerService.getMyInventories = function () {
	return fetch({
		url: '/partner/inventory',
		method: 'get'
	})
}

/**
 * 특정 옵션의 재고 조회
 * @param {Number} optionNo - 옵션 번호
 * @returns {Promise} 재고 정보
 */
PartnerService.getInventoryByOptionNo = function (optionNo) {
	return fetch({
		url: `/partner/inventory/${optionNo}`,
		method: 'get'
	})
}

/**
 * 특정 상품의 재고 조회 (옵션이 없는 상품의 경우)
 * @param {Number} productNo - 상품 번호
 * @returns {Promise} 재고 정보
 */
PartnerService.getInventoryByProductNo = function (productNo) {
	return fetch({
		url: `/partner/inventory/product/${productNo}`,
		method: 'get'
	})
}

/**
 * 재고 생성 (옵션이 있는 상품의 경우)
 * @param {Number} optionNo - 옵션 번호
 * @param {Object} inventoryData - 재고 데이터
 * @param {Number} inventoryData.stockQuantity - 재고 수량
 * @returns {Promise} 생성된 재고 정보
 */
PartnerService.createInventory = function (optionNo, inventoryData) {
	return fetch({
		url: `/partner/inventory/${optionNo}`,
		method: 'post',
		data: inventoryData
	})
}

/**
 * 재고 생성 (옵션이 없는 상품의 경우)
 * @param {Number} productNo - 상품 번호
 * @param {Object} inventoryData - 재고 데이터
 * @param {Number} inventoryData.stockQuantity - 재고 수량
 * @returns {Promise} 생성된 재고 정보
 */
PartnerService.createInventoryForProduct = function (productNo, inventoryData) {
	return fetch({
		url: `/partner/inventory/product/${productNo}`,
		method: 'post',
		data: inventoryData
	})
}

/**
 * 재고 수정 (옵션이 있는 상품의 경우)
 * @param {Number} optionNo - 옵션 번호
 * @param {Object} inventoryData - 재고 데이터
 * @param {Number} inventoryData.stockQuantity - 재고 수량
 * @returns {Promise} 수정된 재고 정보
 */
PartnerService.updateInventory = function (optionNo, inventoryData) {
	return fetch({
		url: `/partner/inventory/${optionNo}`,
		method: 'put',
		data: inventoryData
	})
}

/**
 * 재고 수정 (옵션이 없는 상품의 경우)
 * @param {Number} productNo - 상품 번호
 * @param {Object} inventoryData - 재고 데이터
 * @param {Number} inventoryData.stockQuantity - 재고 수량
 * @returns {Promise} 수정된 재고 정보
 */
PartnerService.updateInventoryForProduct = function (productNo, inventoryData) {
	return fetch({
		url: `/partner/inventory/product/${productNo}`,
		method: 'put',
		data: inventoryData
	})
}

/**
 * 파트너 정산 대상 목록 조회
 * @param {Object} params - 쿼리 파라미터
 * @param {string} params.startDate - 정산 기간 시작일 (yyyy-MM-dd 형식)
 * @param {string} params.endDate - 정산 기간 종료일 (yyyy-MM-dd 형식)
 * @param {string} params.status - 필터 상태 ("SETTLEMENT_READY": 정산 가능만, "ALL": 전체)
 * @returns {Promise} 정산 대상 목록 및 합계 정보
 */
PartnerService.getSettlementItems = function (params = {}) {
	const queryParams = new URLSearchParams();
	if (params.startDate) queryParams.append('startDate', params.startDate);
	if (params.endDate) queryParams.append('endDate', params.endDate);
	if (params.status) queryParams.append('status', params.status);
	
	const queryString = queryParams.toString();
	const url = `/partner/settlement${queryString ? `?${queryString}` : ''}`;
	
	return fetch({
		url: url,
		method: 'get'
	})
}

/**
 * 파트너 생성된 정산 목록 조회
 * @param {Object} params - 조회 파라미터
 * @param {string} params.status - 정산 상태 필터 ("PENDING", "COMPLETED", "CANCELLED")
 * @returns {Promise} 생성된 정산 목록
 */
PartnerService.getSettlementList = function (params = {}) {
	const queryParams = new URLSearchParams();
	if (params.status) queryParams.append('status', params.status);
	
	const queryString = queryParams.toString();
	const url = `/partner/settlement/list${queryString ? `?${queryString}` : ''}`;
	
	return fetch({
		url: url,
		method: 'get'
	})
}

/**
 * 파트너 정산 상세 조회
 * @param {number} settlementId - 정산 ID
 * @returns {Promise} 정산 상세 정보
 */
PartnerService.getSettlementDetail = function (settlementId) {
	return fetch({
		url: `/partner/settlement/list/${settlementId}`,
		method: 'get'
	})
}

/**
 * 파트너 정산서 PDF 다운로드
 * @param {number} settlementId - 정산 ID
 * @returns {Promise} PDF 파일 다운로드
 */
PartnerService.downloadSettlementPdf = async function (settlementId) {
	try {
		const blob = await fetch({
			url: `/partner/settlement/list/${settlementId}/pdf`,
			method: 'get',
			responseType: 'blob'
		});
		
		if (blob instanceof Blob) {
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `settlement_${settlementId}.pdf`;
			document.body.appendChild(a);
			a.click();
			window.URL.revokeObjectURL(url);
			document.body.removeChild(a);
			return { success: true };
		} else {
			throw new Error('PDF 다운로드에 실패했습니다.');
		}
	} catch (err) {
		throw new Error('PDF 다운로드에 실패했습니다: ' + (err.message || '알 수 없는 오류'));
	}
}

/**
 * 파트너 정산 목록 Excel 다운로드
 * @param {Object} params - 다운로드 파라미터
 * @param {string} params.status - 정산 상태 필터 ("PENDING", "COMPLETED", "CANCELLED")
 * @returns {Promise} Excel 파일 다운로드
 */
PartnerService.downloadSettlementExcel = async function (params = {}) {
	try {
		const queryParams = new URLSearchParams();
		if (params.status) queryParams.append('status', params.status);
		
		const queryString = queryParams.toString();
		const url = `/partner/settlement/list/excel${queryString ? `?${queryString}` : ''}`;
		
		const blob = await fetch({
			url: url,
			method: 'get',
			responseType: 'blob'
		});
		
		if (blob instanceof Blob) {
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `settlements.xlsx`;
			document.body.appendChild(a);
			a.click();
			window.URL.revokeObjectURL(url);
			document.body.removeChild(a);
			return { success: true };
		} else {
			throw new Error('Excel 다운로드에 실패했습니다.');
		}
	} catch (err) {
		throw new Error('Excel 다운로드에 실패했습니다: ' + (err.message || '알 수 없는 오류'));
	}
}

/**
 * 파트너 자신의 매출 통계 조회
 * @param {String} startDate - 시작 날짜 (YYYY-MM-DD 형식, 선택)
 * @param {String} endDate - 종료 날짜 (YYYY-MM-DD 형식, 선택)
 * @returns {Promise} 매출 통계 데이터
 */
PartnerService.getSalesStatistics = function (startDate, endDate) {
	const params = new URLSearchParams();
	if (startDate) params.append('startDate', startDate);
	if (endDate) params.append('endDate', endDate);
	const queryString = params.toString();
	return fetch({
		url: `/partner/sales/statistics${queryString ? '?' + queryString : ''}`,
		method: 'get'
	})
}

/**
 * 파트너 세일 정책 생성
 * @param {Object} data
 * @returns {Promise}
 */
PartnerService.createPartnerSalePolicy = function (data) {
	return fetch({
		url: '/partner/sales',
		method: 'post',
		data
	})
}

/**
 * 파트너 세일 정책 목록 조회
 * @param {Object} params
 * @param {String} params.status
 * @returns {Promise}
 */
PartnerService.getPartnerSalePolicies = function (params = {}) {
	const queryParams = new URLSearchParams();
	if (params.status) queryParams.append('status', params.status);
	const queryString = queryParams.toString();
	return fetch({
		url: `/partner/sales${queryString ? `?${queryString}` : ''}`,
		method: 'get'
	})
}

/**
 * 파트너 세일 정책 취소
 * @param {Number} id
 * @returns {Promise}
 */
PartnerService.cancelPartnerSalePolicy = function (id) {
	return fetch({
		url: `/partner/sales/${id}/cancel`,
		method: 'patch'
	})
}

/**
 * 파트너 세일 "캠페인(묶음)" 생성
 * 캠페인은 내부적으로 SalePolicy를 다건 생성하고 동일 campaignId를 부여합니다.
 * @param {Object} data
 * @returns {Promise}
 */
PartnerService.createPartnerSaleCampaign = function (data) {
	return fetch({
		url: '/partner/sales/campaign',
		method: 'post',
		data
	})
}

/**
 * 파트너 세일 "캠페인(묶음)" 취소 (캠페인에 속한 모든 정책 취소)
 * @param {string} campaignId
 * @returns {Promise}
 */
PartnerService.cancelPartnerSaleCampaign = function (campaignId) {
	return fetch({
		url: `/partner/sales/campaigns/${campaignId}/cancel`,
		method: 'patch'
	})
}

/**
 * 공개 이벤트 목록 조회 (파트너 참여형 필터는 화면에서 처리)
 * @returns {Promise}
 */
PartnerService.getPublicEvents = function () {
	return fetch({
		url: '/events',
		method: 'get'
	})
}

/**
 * 파트너 노출 이벤트 목록 조회 (서버 페이지네이션 지원)
 * @param {Object} params
 * @param {Number} params.upcomingDays - N일 이내 시작 이벤트만 조회 (선택)
 * @param {Number} params.page - 1-based UI 페이지(내부에서 0-based로 변환)
 * @param {Number} params.size - 페이지 크기
 * @param {String} params.status - 상태 필터 (선택)
 * @param {String} params.keyword - 키워드 (선택)
 * @returns {Promise<{items: any[], total: number, page: number, size: number}>}
 */
PartnerService.getPartnerVisibleEvents = async function (params = {}) {
	// HMR 시 순환 참조 회피를 위해 동적 import로 fetch 로드
	const { default: http } = await import('auth/FetchInterceptor');
	const qp = {};
	if (params.upcomingDays !== undefined && params.upcomingDays !== null) {
		qp.upcomingDays = String(params.upcomingDays);
	}
	if (params.page !== undefined && params.page !== null) {
		qp.page = Math.max(0, Number(params.page) - 1); // 0-based 변환
	}
	if (params.size !== undefined && params.size !== null) {
		qp.size = Number(params.size);
	}
	if (params.status) qp.status = params.status;
	if (params.keyword) qp.keyword = params.keyword;
	return http({
		url: `/partner/events`,
		method: 'get',
		params: qp
	})
}

/**
 * 이벤트 참여 신청
 * @param {Number} eventNo
 * @returns {Promise}
 */
PartnerService.participateEvent = function (eventNo) {
	return fetch({
		url: `/partner/events/${eventNo}/participation`,
		method: 'post'
	})
}

/**
 * 이벤트 참여 해제
 * @param {Number} eventNo
 * @returns {Promise}
 */
PartnerService.cancelEventParticipation = function (eventNo, options = {}) {
	return fetch({
		url: `/partner/events/${eventNo}/participation`,
		method: 'delete',
		params: {
			deactivateLinkedSales: options.deactivateLinkedSales ? 'true' : 'false'
		}
	})
}

/**
 * 파트너 이벤트 실적 - 단일
 * @param {number} eventNo
 * @param {string|null} from ISO datetime
 * @param {string|null} to ISO datetime
 */
PartnerService.getPartnerEventPerformance = function (eventNo, from = null, to = null) {
	const params = {};
	if (from) params.from = from;
	if (to) params.to = to;
	return fetch({
		url: `/partner/events/${eventNo}/performance`,
		method: 'get',
		params
	})
}

/**
 * 파트너 이벤트 실적 - 종료 목록
 * @param {string|null} from ISO datetime
 * @param {string|null} to ISO datetime
 */
PartnerService.getPartnerEndedEventPerformanceList = function (from = null, to = null) {
	const params = {};
	if (from) params.from = from;
	if (to) params.to = to;
	return fetch({
		url: `/partner/events/performance/list`,
		method: 'get',
		params
	})
}

/**
 * 파트너 실적 - 일자별 추이
 */
PartnerService.getPartnerEventTimeseries = function (params = {}) {
	const q = {};
	if (params.eventNo) q.eventNo = params.eventNo;
	if (params.from) q.from = params.from;
	if (params.to) q.to = params.to;
	return fetch({
		url: `/partner/events/performance/timeseries`,
		method: 'get',
		params: q
	})
}

/**
 * 파트너 실적 - Top N
 */
PartnerService.getPartnerEventTop = function (type, params = {}) {
	const q = { type };
	if (params.limit) q.limit = params.limit;
	if (params.eventNo) q.eventNo = params.eventNo;
	if (params.from) q.from = params.from;
	if (params.to) q.to = params.to;
	return fetch({
		url: `/partner/events/performance/top`,
		method: 'get',
		params: q
	})
}

/**
 * 파트너 상품 상태별 개수 요약
 * @returns {Promise<Record<string, number>>}
 */
PartnerService.getMyProductStatusCounts = function () {
	return fetch({
		url: `/partner/products/status-counts`,
		method: 'get'
	})
}

/**
 * 파트너 리뷰 AI 분석 요청
 * @param {Object} payload - 리뷰 분석 요청 바디
 * @returns {Promise} ApiResponse 래퍼 응답
 */
PartnerService.postPartnerReviewAnalysis = function (payload) {
	return fetch({
		url: '/ai/review-analysis/partner',
		method: 'post',
		data: payload
	})
}

export default PartnerService;
