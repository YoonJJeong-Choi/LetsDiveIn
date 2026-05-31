import fetch from 'auth/FetchInterceptor'

const AdminService = {}

/**
 * 파트너 고위험 변경 신청 목록 조회 (관리자)
 * @param {Object} params
 * @param {string} params.status - 상태 필터 (PENDING|APPROVED|REJECTED)
 */
AdminService.getPartnerChangeRequests = function (params = {}) {
	return fetch({
		url: '/admin/partners/change-requests',
		method: 'get',
		params
	})
}

/**
 * 파트너 고위험 변경 신청 승인 (관리자)
 * @param {number} requestId
 */
AdminService.approvePartnerChangeRequest = function (requestId) {
	return fetch({
		url: `/admin/partners/change-requests/${requestId}/approve`,
		method: 'patch'
	})
}

/**
 * 파트너 고위험 변경 신청 거절 (관리자)
 * @param {number} requestId
 * @param {string} rejectReason
 */
AdminService.rejectPartnerChangeRequest = function (requestId, rejectReason) {
	return fetch({
		url: `/admin/partners/change-requests/${requestId}/reject`,
		method: 'patch',
		data: { rejectReason }
	})
}

/**
 * 대기 중인 파트너 신청 목록을 조회합니다.
 * @returns {Promise} 파트너 신청 목록
 */
AdminService.getPendingPartners = function () {
	return fetch({
		url: '/admin/partners/pending',
		method: 'get'
	})
}

/** 관리자 대시보드 — 바로 처리할 일(대기 건수) 집계 */
AdminService.getDashboardQueue = function () {
	return fetch({
		url: '/admin/dashboard/queue',
		method: 'get'
	})
}

/** 관리자 대시보드 전체 헬스(요약) */
AdminService.getDashboardHealth = function () {
	return fetch({
		url: '/admin/dashboard/health',
		method: 'get'
	})
}

/** 관리자 대시보드 — 추이·랭킹·최근 주문/반품·정산 요약 (days: 14~30) */
AdminService.getDashboardInsights = function (params = {}) {
	return fetch({
		url: '/admin/dashboard/insights',
		method: 'get',
		params
	})
}

/** 관리자 분석 대시보드 — 긴 추이·파트너·시간대·이행 (trendDays: 7~90) */
AdminService.getDashboardAnalytics = function (params = {}) {
	return fetch({
		url: '/admin/dashboard/analytics',
		method: 'get',
		params
	})
}

/** 관리자 AI 운영 모니터링 요약 */
AdminService.getAiOverview = function () {
	return fetch({
		url: '/admin/ai/overview',
		method: 'get'
	})
}

/**
 * 파트너를 승인합니다.
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 승인된 파트너 정보
 */
AdminService.approvePartner = function (partnerId) {
	return fetch({
		url: `/admin/partners/${partnerId}/approve`,
		method: 'patch'
	})
}

/**
 * 파트너를 거절합니다.
 * @param {Number} partnerId - 파트너 ID
 * @param {String} rejectionReason - 거절 사유
 * @returns {Promise} 거절 결과
 */
AdminService.rejectPartner = function (partnerId, rejectionReason) {
	return fetch({
		url: `/admin/partners/${partnerId}/reject`,
		method: 'patch',
		data: { rejectionReason }
	})
}

/**
 * 파트너 목록을 조회합니다 (상태별 필터링 및 휴업 신청 필터 가능).
 * @param {String} status - 필터링할 상태 (PENDING, APPROVED, REJECTED, INACTIVE 또는 null = 전체)
 * @param {Boolean} hasDeactivationRequest - 휴업 신청이 있는 파트너만 조회 (true)
 * @param {Boolean} hasReactivationRequest - 재활성화 신청이 있는 파트너만 조회 (true)
 * @returns {Promise} 파트너 목록
 */
AdminService.getAllPartners = function (status = null, hasDeactivationRequest = null, hasReactivationRequest = null) {
	const params = {};
	if (status) params.status = status;
	if (hasDeactivationRequest === true) params.hasDeactivationRequest = true;
	if (hasReactivationRequest === true) params.hasReactivationRequest = true;
	return fetch({
		url: '/admin/partners',
		method: 'get',
		params
	})
}

/**
 * 파트너 상세 정보를 조회합니다.
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 파트너 상세 정보
 */
AdminService.getPartnerDetail = function (partnerId) {
	return fetch({
		url: `/admin/partners/${partnerId}`,
		method: 'get'
	})
}

/**
 * 파트너를 비활성화합니다 (APPROVED → INACTIVE).
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 비활성화된 파트너 정보
 */
AdminService.deactivatePartner = function (partnerId) {
	return fetch({
		url: `/admin/partners/${partnerId}/deactivate`,
		method: 'patch'
	})
}

/**
 * 파트너를 재활성화합니다 (INACTIVE → APPROVED).
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 재활성화된 파트너 정보
 */
AdminService.activatePartner = function (partnerId) {
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
AdminService.approveDeactivationRequest = function (partnerId) {
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
AdminService.rejectDeactivationRequest = function (partnerId, requestData) {
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
AdminService.rejectReactivationRequest = function (partnerId, requestData) {
	return fetch({
		url: `/admin/partners/${partnerId}/reactivation-request/reject`,
		method: 'patch',
		data: requestData
	})
}

/**
 * 파트너 이력 조회 (관리자용)
 * @param {Number} partnerId - 파트너 ID
 * @param {String} actionType - 액션 타입 필터 (선택)
 * @returns {Promise} 파트너 이력 목록
 */
AdminService.getPartnerHistory = function (partnerId, actionType = null) {
	const params = {};
	if (actionType) params.actionType = actionType;
	return fetch({
		url: `/admin/partners/${partnerId}/history`,
		method: 'get',
		params
	})
}

/**
 * 상품 목록 조회 (상태별 필터링 가능)
 * @param {String} status - 필터링할 상태 (PENDING, ACTIVE, REJECTED, INACTIVE 또는 null = 전체)
 * @returns {Promise} 상품 목록
 */
AdminService.getAllProducts = function (status = null, page = undefined, size = undefined) {
	const params = {};
	if (status) params.status = status;
	if (page !== undefined) params.page = Math.max(0, page - 1);
	if (size !== undefined) params.size = size;
	return fetch({
		url: '/admin/products',
		method: 'get',
		params
	})
}

/**
 * 상품 승인
 * @param {Number} productNo - 상품 번호
 * @returns {Promise} 승인된 상품 정보
 */
AdminService.approveProduct = function (productNo) {
	return fetch({
		url: `/admin/products/${productNo}/approve`,
		method: 'patch'
	})
}

/**
 * 상품 거절
 * @param {Number} productNo - 상품 번호
 * @param {String} rejectionReason - 거절 사유
 * @returns {Promise} 거절된 상품 정보
 */
AdminService.rejectProduct = function (productNo, rejectionReason) {
	return fetch({
		url: `/admin/products/${productNo}/reject`,
		method: 'patch',
		data: { rejectionReason }
	})
}

/**
 * 전체 재고 목록 조회 (관리자용)
 * @returns {Promise} 전체 재고 목록
 */
AdminService.getAllInventories = function () {
	return fetch({
		url: '/admin/inventory',
		method: 'get'
	})
}

/**
 * 특정 파트너의 재고 목록 조회 (관리자용)
 * @param {Number} partnerId - 파트너 ID
 * @returns {Promise} 파트너의 재고 목록
 */
AdminService.getInventoriesByPartner = function (partnerId) {
	return fetch({
		url: `/admin/inventory/partner/${partnerId}`,
		method: 'get'
	})
}

/**
 * 관리자 정산 대상 목록 조회
 * @param {Object} params - 쿼리 파라미터
 * @param {Number} params.partnerId - 파트너 ID (선택, null이면 전체 파트너)
 * @param {string} params.startDate - 정산 기간 시작일 (yyyy-MM-dd 형식)
 * @param {string} params.endDate - 정산 기간 종료일 (yyyy-MM-dd 형식)
 * @param {string} params.status - 필터 상태 ("SETTLEMENT_READY": 정산 가능만, "ALL": 전체)
 * @returns {Promise} 정산 대상 목록 및 합계 정보
 */
AdminService.getSettlementItems = function (params = {}) {
	const queryParams = new URLSearchParams();
	if (params.partnerId) queryParams.append('partnerId', params.partnerId);
	if (params.startDate) queryParams.append('startDate', params.startDate);
	if (params.endDate) queryParams.append('endDate', params.endDate);
	if (params.status) queryParams.append('status', params.status);
	
	const queryString = queryParams.toString();
	const url = `/admin/settlement${queryString ? `?${queryString}` : ''}`;
	
	return fetch({
		url: url,
		method: 'get'
	})
}

/**
 * 정산 생성 (관리자용)
 * @param {Object} requestData - 정산 생성 요청 데이터
 * @param {Number} requestData.partnerId - 파트너 ID
 * @param {Array<Number>} requestData.orderItemIds - 정산 처리할 주문 아이템 ID 목록
 * @param {string} requestData.settlementPeriodStart - 정산 기간 시작일 (yyyy-MM-dd 형식, 선택)
 * @param {string} requestData.settlementPeriodEnd - 정산 기간 종료일 (yyyy-MM-dd 형식, 선택)
 * @returns {Promise} 생성된 정산 정보
 */
AdminService.createSettlement = function (requestData) {
	return fetch({
		url: '/admin/settlement',
		method: 'post',
		data: requestData
	})
}

/**
 * 정산 상태 변경 (관리자용)
 * @param {Number} settlementId - 정산 ID
 * @param {string} status - 변경할 상태 ("PENDING", "PROCESSING", "COMPLETED", "CANCELLED")
 * @returns {Promise} 변경된 정산 정보
 */
AdminService.updateSettlementStatus = function (settlementId, status, paidDate = null) {
	const params = { status };
	if (paidDate) {
		params.paidDate = paidDate;
	}
	return fetch({
		url: `/admin/settlement/${settlementId}/status`,
		method: 'patch',
		params
	})
}

/**
 * 생성된 정산 목록 조회 (관리자용)
 * @param {Object} params - 조회 파라미터
 * @param {Number} params.partnerId - 파트너 ID (선택)
 * @param {string} params.status - 정산 상태 필터 (선택, "PENDING", "PROCESSING", "COMPLETED", "CANCELLED")
 * @returns {Promise} 생성된 정산 목록
 */
AdminService.getSettlementList = function (params = {}) {
	const queryParams = new URLSearchParams();
	if (params.partnerId) queryParams.append('partnerId', params.partnerId);
	if (params.status) queryParams.append('status', params.status);
	
	const queryString = queryParams.toString();
	const url = `/admin/settlements${queryString ? `?${queryString}` : ''}`;
	
	return fetch({
		url: url,
		method: 'get'
	})
}

/**
 * 정산 상세 조회 (관리자용)
 * @param {Number} settlementId - 정산 ID
 * @returns {Promise} 정산 상세 정보 (포함된 주문 아이템 목록 포함)
 */
AdminService.getSettlementDetail = function (settlementId) {
	return fetch({
		url: `/admin/settlements/${settlementId}`,
		method: 'get'
	})
}

/**
 * 정산 변경 이력 조회 (관리자용)
 * @param {Number} settlementId - 정산 ID
 * @returns {Promise} 정산 변경 이력 목록
 */
AdminService.getSettlementHistory = function (settlementId) {
	return fetch({
		url: `/admin/settlements/${settlementId}/history`,
		method: 'get'
	})
}

/**
 * 정산 대시보드 조회 (관리자용)
 * @returns {Promise} 정산 대시보드 데이터 (전체 현황, 파트너별 순위, 월별 현황)
 */
AdminService.getSettlementDashboard = function () {
	return fetch({
		url: '/admin/settlements/dashboard',
		method: 'get'
	})
}

/**
 * 정산서 PDF 다운로드 (관리자용)
 * @param {Number} settlementId - 정산 ID
 * @returns {Promise} PDF 파일 Blob
 */
AdminService.downloadSettlementPdf = async function (settlementId) {
	try {
		// FetchInterceptor는 response.data를 반환하므로, blob 응답의 경우 data가 Blob 객체
		const blob = await fetch({
			url: `/admin/settlements/${settlementId}/pdf`,
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
 * 파일 다운로드 (관리자/권한자)
 * @param {number} fileId
 */
AdminService.downloadFile = async function (fileId) {
	try {
		const blob = await fetch({
			url: `/files/${fileId}/download`,
			method: 'get',
			responseType: 'blob'
		});
		if (blob instanceof Blob) {
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `file_${fileId}`;
			document.body.appendChild(a);
			a.click();
			window.URL.revokeObjectURL(url);
			document.body.removeChild(a);
			return { success: true };
		}
		throw new Error('파일 다운로드에 실패했습니다.');
	} catch (err) {
		throw new Error('파일 다운로드에 실패했습니다: ' + (err.message || '알 수 없는 오류'));
	}
}

/**
 * 정산 목록 Excel 다운로드 (관리자용)
 * @param {Object} params - 다운로드 파라미터
 * @param {Number} params.partnerId - 파트너 ID (선택)
 * @param {string} params.status - 정산 상태 (선택)
 * @returns {Promise} Excel 파일 Blob
 */
AdminService.downloadSettlementExcel = async function (params = {}) {
	try {
		const queryParams = new URLSearchParams();
		if (params.partnerId) queryParams.append('partnerId', params.partnerId);
		if (params.status) queryParams.append('status', params.status);
		
		const queryString = queryParams.toString();
		const url = `/admin/settlements/excel${queryString ? `?${queryString}` : ''}`;
		
		// FetchInterceptor는 response.data를 반환하므로, blob 응답의 경우 data가 Blob 객체
		const blob = await fetch({
			url: url,
			method: 'get',
			responseType: 'blob'
		});
		
		if (blob instanceof Blob) {
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = 'settlements.xlsx';
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
 * 고객 목록 조회 (관리자용)
 * @param {Object} params - 조회 파라미터
 * @param {Number} params.page - 페이지 번호 (0부터 시작, 기본값: 0)
 * @param {Number} params.pageSize - 페이지 크기 (기본값: 20)
 * @param {String} params.searchKeyword - 검색 키워드 (이름 또는 이메일, 선택)
 * @param {Boolean} params.emailVerified - 이메일 인증 여부 필터 (true/false/null, 선택)
 * @param {String} params.grade - 고객 등급 필터 (BEGINNER/SWIMMER/PRO/MASTER/LEGEND, 선택)
 * @returns {Promise} 고객 목록 및 통계
 */
AdminService.getCustomerList = function (params = {}) {
	const queryParams = new URLSearchParams();
	if (params.page !== undefined) {
		const zeroBased = Math.max(0, Number(params.page) - 1);
		queryParams.append('page', zeroBased);
	}
	if (params.pageSize !== undefined) {
		queryParams.append('pageSize', params.pageSize); // 호환 유지
		queryParams.append('size', params.pageSize); // 표준 키
	}
	if (params.size !== undefined) {
		queryParams.append('size', params.size);
	}
	if (params.searchKeyword) queryParams.append('searchKeyword', params.searchKeyword);
	if (params.emailVerified !== undefined && params.emailVerified !== null) {
		queryParams.append('emailVerified', params.emailVerified);
	}
	if (params.grade) {
		queryParams.append('grade', params.grade);
	}
	
	const queryString = queryParams.toString();
	return fetch({
		url: `/admin/customers${queryString ? '?' + queryString : ''}`,
		method: 'get'
	})
}

/**
 * 고객 상세 조회 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @returns {Promise} 고객 상세 정보 (주문/리뷰/반품 내역 포함)
 */
AdminService.getCustomerDetail = function (customerId) {
	return fetch({
		url: `/admin/customers/${customerId}`,
		method: 'get'
	})
}

/**
 * 고객 통계 조회 (관리자용)
 * @returns {Promise} 고객 통계 정보
 */
AdminService.getCustomerStatistics = function () {
	return fetch({
		url: '/admin/customers/statistics',
		method: 'get'
	})
}

/**
 * 고객 정보 수정 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @param {Object} data - 수정할 정보
 * @param {String} data.customerName - 고객 이름
 * @param {String} data.customerEmail - 고객 이메일
 * @returns {Promise} 수정된 고객 상세 정보
 */
AdminService.updateCustomer = function (customerId, data) {
	return fetch({
		url: `/admin/customers/${customerId}`,
		method: 'patch',
		data: data
	})
}

/**
 * 고객 계정 활성화/비활성화 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @param {Boolean} active - true면 활성화, false면 비활성화
 * @returns {Promise} 수정된 고객 상세 정보
 */
AdminService.updateCustomerAccountStatus = function (customerId, active) {
	const url = `/admin/customers/${customerId}/status?active=${active}`;
	return fetch({
		url: url,
		method: 'patch'
	})
}

/**
 * 고객 비밀번호 초기화 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @returns {Promise} 임시 비밀번호
 */
AdminService.resetCustomerPassword = function (customerId) {
	return fetch({
		url: `/admin/customers/${customerId}/reset-password`,
		method: 'post'
	})
}

/**
 * 고객 이메일 인증 재발송 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @returns {Promise} 성공 메시지
 */
AdminService.resendEmailVerification = function (customerId) {
	return fetch({
		url: `/admin/customers/${customerId}/resend-email-verification`,
		method: 'post'
	})
}

/**
 * 고객 관리 작업 이력 조회 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @returns {Promise} 작업 이력 목록
 */
AdminService.getCustomerHistory = function (customerId) {
	return fetch({
		url: `/admin/customers/${customerId}/history`,
		method: 'get'
	})
}

/**
 * 고객 메모 목록 조회 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @returns {Promise} 메모 목록
 */
AdminService.getCustomerNotes = function (customerId) {
	return fetch({
		url: `/admin/customers/${customerId}/notes`,
		method: 'get'
	})
}

/**
 * 고객 메모 작성 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @param {Object} data - 메모 데이터
 * @param {String} data.noteContent - 메모 내용
 * @param {Boolean} data.isImportant - 중요 여부 (선택)
 * @returns {Promise} 작성된 메모 정보
 */
AdminService.createCustomerNote = function (customerId, data) {
	return fetch({
		url: `/admin/customers/${customerId}/notes`,
		method: 'post',
		data: data
	})
}

/**
 * 고객 메모 수정 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @param {Number} noteId - 메모 ID
 * @param {Object} data - 수정할 메모 데이터
 * @returns {Promise} 수정된 메모 정보
 */
AdminService.updateCustomerNote = function (customerId, noteId, data) {
	return fetch({
		url: `/admin/customers/${customerId}/notes/${noteId}`,
		method: 'patch',
		data: data
	})
}

/**
 * 고객 메모 삭제 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @param {Number} noteId - 메모 ID
 * @returns {Promise} 성공 메시지
 */
AdminService.deleteCustomerNote = function (customerId, noteId) {
	return fetch({
		url: `/admin/customers/${customerId}/notes/${noteId}`,
		method: 'delete'
	})
}

/**
 * 고객 태그 목록 조회 (관리자용)
 * @returns {Promise} 태그 목록
 */
AdminService.getAllCustomerTags = function () {
	return fetch({
		url: `/admin/customers/tags`,
		method: 'get'
	})
}

/**
 * 고객 태그 생성 (관리자용)
 * @param {Object} data - 태그 데이터
 * @param {String} data.tagName - 태그 이름
 * @param {String} data.tagColor - 태그 색상 (선택)
 * @param {String} data.description - 태그 설명 (선택)
 * @returns {Promise} 생성된 태그 정보
 */
AdminService.createCustomerTag = function (data) {
	return fetch({
		url: `/admin/customers/tags`,
		method: 'post',
		data: data
	})
}

/**
 * 고객 태그 수정 (관리자용)
 * @param {Number} tagId - 태그 ID
 * @param {Object} data - 수정할 태그 데이터
 * @returns {Promise} 수정된 태그 정보
 */
AdminService.updateCustomerTag = function (tagId, data) {
	return fetch({
		url: `/admin/customers/tags/${tagId}`,
		method: 'patch',
		data: data
	})
}

/**
 * 고객 태그 삭제 (관리자용)
 * @param {Number} tagId - 태그 ID
 * @returns {Promise} 성공 메시지
 */
AdminService.deleteCustomerTag = function (tagId) {
	return fetch({
		url: `/admin/customers/tags/${tagId}`,
		method: 'delete'
	})
}

/**
 * 고객의 태그 목록 조회 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @returns {Promise} 태그 목록
 */
AdminService.getCustomerTags = function (customerId) {
	return fetch({
		url: `/admin/customers/${customerId}/tags`,
		method: 'get'
	})
}

/**
 * 고객에 태그 추가 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @param {Number} tagId - 태그 ID
 * @returns {Promise} 성공 메시지
 */
AdminService.addTagToCustomer = function (customerId, tagId) {
	return fetch({
		url: `/admin/customers/${customerId}/tags/${tagId}`,
		method: 'post'
	})
}

/**
 * 고객에서 태그 제거 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @param {Number} tagId - 태그 ID
 * @returns {Promise} 성공 메시지
 */
AdminService.removeTagFromCustomer = function (customerId, tagId) {
	return fetch({
		url: `/admin/customers/${customerId}/tags/${tagId}`,
		method: 'delete'
	})
}

/**
 * 고객 활동 로그 조회 (관리자용)
 * @param {Number} customerId - 고객 ID
 * @param {Number} page - 페이지 번호 (선택, 기본값: 0)
 * @param {Number} pageSize - 페이지 크기 (선택, 기본값: 50)
 * @returns {Promise} 활동 로그 목록
 */
AdminService.getCustomerActivityLogs = function (customerId, page, pageSize) {
	const params = new URLSearchParams();
	if (page !== undefined && page !== null) params.append('page', page);
	if (pageSize !== undefined && pageSize !== null) params.append('pageSize', pageSize);
	const queryString = params.toString();
	return fetch({
		url: `/admin/customers/${customerId}/activity-logs${queryString ? '?' + queryString : ''}`,
		method: 'get'
	})
}

/**
 * 매출 현황 통계 조회 (관리자용)
 * @param {String} startDate - 시작 날짜 (YYYY-MM-DD 형식, 선택)
 * @param {String} endDate - 종료 날짜 (YYYY-MM-DD 형식, 선택)
 * @returns {Promise} 매출 통계 데이터
 */
AdminService.getSalesStatistics = function (startDate, endDate) {
	const params = new URLSearchParams();
	if (startDate) params.append('startDate', startDate);
	if (endDate) params.append('endDate', endDate);
	const queryString = params.toString();
	return fetch({
		url: `/admin/sales/statistics${queryString ? '?' + queryString : ''}`,
		method: 'get'
	})
}

/**
 * 고객 등급 목록 조회 (관리자용)
 * @returns {Promise} 등급 목록
 */
AdminService.getCustomerGrades = function () {
	return fetch({
		url: '/admin/customer-grades',
		method: 'get'
	})
}

/**
 * 고객 등급 상세 조회 (관리자용)
 * @param {Number} gradeId - 등급 ID
 * @returns {Promise} 등급 정보
 */
AdminService.getCustomerGrade = function (gradeId) {
	return fetch({
		url: `/admin/customer-grades/${gradeId}`,
		method: 'get'
	})
}

/**
 * 고객 등급 수정 (관리자용)
 * 등급명과 등급 레벨은 Enum으로 고정되어 있어 변경 불가능합니다.
 * 최소 구매액, 최소 주문 건수, 할인율, 포인트 적립률, 활성화 여부만 수정 가능합니다.
 * @param {Number} gradeId - 등급 ID
 * @param {Object} data - 수정할 등급 데이터 (gradeName, gradeLevel 제외)
 * @returns {Promise} 수정된 등급 정보
 */
AdminService.updateCustomerGrade = function (gradeId, data) {
	return fetch({
		url: `/admin/customer-grades/${gradeId}`,
		method: 'patch',
		data: data
	})
}

/**
 * FAQ 목록 조회 (공개 API)
 * @param {String} category - 카테고리 (선택)
 * @returns {Promise} FAQ 목록
 */
AdminService.getFaqList = function (category = null) {
	const params = category ? { category } : {};
	return fetch({
		url: '/faq',
		method: 'get',
		params: params
	})
}

/**
 * FAQ 상세 조회 (공개 API)
 * @param {Number} faqNo - FAQ 번호
 * @returns {Promise} FAQ 정보
 */
AdminService.getFaqDetail = function (faqNo) {
	return fetch({
		url: `/faq/${faqNo}`,
		method: 'get'
	})
}

/**
 * FAQ 생성 (관리자용)
 * @param {Object} data - FAQ 데이터
 * @returns {Promise} 생성된 FAQ 정보
 */
AdminService.createFaq = function (data) {
	return fetch({
		url: '/faq',
		method: 'post',
		data: data
	})
}

/**
 * FAQ 수정 (관리자용)
 * @param {Number} faqNo - FAQ 번호
 * @param {Object} data - 수정할 FAQ 데이터
 * @returns {Promise} 수정된 FAQ 정보
 */
AdminService.updateFaq = function (faqNo, data) {
	return fetch({
		url: `/faq/${faqNo}`,
		method: 'put',
		data: data
	})
}

/**
 * FAQ 삭제 (관리자용)
 * @param {Number} faqNo - FAQ 번호
 * @returns {Promise} 성공 메시지
 */
AdminService.deleteFaq = function (faqNo) {
	return fetch({
		url: `/faq/${faqNo}`,
		method: 'delete'
	})
}

/**
 * 관리자 이벤트 목록 조회
 * @param {Object} params
 * @param {String} params.status - 이벤트 상태 (선택)
 * @param {String} params.keyword - 이벤트명 검색어 (선택)
 * @param {String} params.startAt - ISO datetime (선택)
 * @param {String} params.endAt - ISO datetime (선택)
 * @returns {Promise} 이벤트 목록
 */
AdminService.getAdminEventList = function (params = {}) {
	const queryParams = {};
	if (params.status) queryParams.status = params.status;
	if (params.keyword) queryParams.keyword = params.keyword;
	if (params.startAt) queryParams.startAt = params.startAt;
	if (params.endAt) queryParams.endAt = params.endAt;
	if (params.page !== undefined && params.page !== null) {
		queryParams.page = Math.max(0, Number(params.page) - 1);
	}
	if (params.size !== undefined) queryParams.size = params.size;

	return fetch({
		url: '/events/admin',
		method: 'get',
		params: queryParams
	})
}

/**
 * 관리자 이벤트 상세 조회
 * @param {Number} eventNo
 * @returns {Promise} 이벤트 상세
 */
AdminService.getAdminEventDetail = function (eventNo) {
	return fetch({
		url: `/events/${eventNo}/admin`,
		method: 'get'
	})
}

/**
 * 관리자 이벤트 참여 파트너 목록 조회
 * @param {Number} eventNo
 * @returns {Promise} 참여 파트너 목록
 */
AdminService.getAdminEventParticipants = function (eventNo) {
	return fetch({
		url: `/events/${eventNo}/admin/participants`,
		method: 'get'
	})
}

/**
 * 관리자 이벤트 생성
 * @param {Object} data
 * @returns {Promise} 생성 결과
 */
AdminService.createAdminEvent = function (data) {
	return fetch({
		url: '/admin/events',
		method: 'post',
		data
	})
}

/**
 * 관리자 이벤트 수정
 * @param {Number} eventNo
 * @param {Object} data
 * @returns {Promise} 수정 결과
 */
AdminService.updateAdminEvent = function (eventNo, data) {
	return fetch({
		url: `/admin/events/${eventNo}`,
		method: 'put',
		data
	})
}

/**
 * 관리자 이벤트 상태 변경
 * @param {Number} eventNo
 * @param {String} eventStatus
 * @returns {Promise} 상태 변경 결과
 */
AdminService.updateAdminEventStatus = function (eventNo, eventStatus) {
	return fetch({
		url: `/admin/events/${eventNo}/status`,
		method: 'patch',
		data: { eventStatus }
	})
}

/**
 * 관리자 이벤트 실적 조회
 * @param {number} eventNo
 * @param {string|null} from ISO datetime (선택)
 * @param {string|null} to ISO datetime (선택)
 * @returns {Promise} 성과 요약
 */
AdminService.getEventPerformance = function (eventNo, from = null, to = null) {
	const params = {};
	if (from) params.from = from;
	if (to) params.to = to;
	return fetch({
		url: `/admin/events/${eventNo}/performance`,
		method: 'get',
		params
	})
}

/**
 * 종료된 이벤트 전체 실적 목록
 * @param {string|null} from ISO datetime
 * @param {string|null} to ISO datetime
 * @returns {Promise<Array>}
 */
AdminService.getEndedEventPerformanceList = function (from = null, to = null) {
	const params = {};
	if (from) params.from = from;
	if (to) params.to = to;
	return fetch({
		url: `/admin/events/performance/list`,
		method: 'get',
		params
	})
}

/**
 * 실적 일자별 추이
 * @param {Object} params
 * @param {number|null} params.eventNo
 * @param {string|null} params.from
 * @param {string|null} params.to
 */
AdminService.getEventTimeseries = function (params = {}) {
	const q = {};
	if (params.eventNo) q.eventNo = params.eventNo;
	if (params.from) q.from = params.from;
	if (params.to) q.to = params.to;
	return fetch({
		url: `/admin/events/performance/timeseries`,
		method: 'get',
		params: q
	})
}

/**
 * 실적 Top N
 * @param {'partner'|'product'} type
 * @param {Object} params
 * @param {number} params.limit
 * @param {number|null} params.eventNo
 * @param {string|null} params.from
 * @param {string|null} params.to
 */
AdminService.getEventTop = function (type, params = {}) {
	const q = { type };
	if (params.limit) q.limit = params.limit;
	if (params.eventNo) q.eventNo = params.eventNo;
	if (params.from) q.from = params.from;
	if (params.to) q.to = params.to;
	return fetch({
		url: `/admin/events/performance/top`,
		method: 'get',
		params: q
	})
}

/**
 * 관리자용 - 특정 고객 포인트 내역 조회 (서버 페이지네이션)
 * @param {Number} customerId - 고객 ID
 * @param {{page?: number, size?: number}} params
 * @returns {Promise<{items: any[], total: number, page: number, size: number}>}
 */
AdminService.getCustomerPointHistory = function (customerId, params = {}) {
	const qp = { ...params };
	if (qp.page !== undefined && qp.page !== null) {
		qp.page = Math.max(0, Number(qp.page) - 1);
	}
	return fetch({
		url: `/admin/customers/${customerId}/points/history`,
		method: 'get',
		params: qp
	})
}

/**
 * 관리자용 - 고객 포인트 수동 지급
 * @param {Number} customerId - 고객 ID
 * @param {{ pointAmount: number, description: string }} data
 */
AdminService.addCustomerPoint = function (customerId, data) {
	return fetch({
		url: `/admin/customers/${customerId}/points/add`,
		method: 'post',
		data: data,
	})
}

/**
 * 관리자용 - 고객 포인트 수동 차감
 * @param {Number} customerId - 고객 ID
 * @param {{ pointAmount: number, description: string }} data
 */
AdminService.deductCustomerPoint = function (customerId, data) {
	return fetch({
		url: `/admin/customers/${customerId}/points/deduct`,
		method: 'post',
		data: data,
	})
}

/**
 * 상품 상태별 개수 요약 (관리자용)
 * @returns {Promise<Record<string, number>>}
 */
AdminService.getAdminProductStatusCounts = function () {
	return fetch({
		url: `/admin/products/status-counts`,
		method: 'get'
	})
}

/**
 * 관리자 컬러 목록 조회
 */
AdminService.getAdminColors = function () {
	return fetch({
		url: '/admin/colors',
		method: 'get'
	})
}

/**
 * 활성 컬러 목록 조회 (공개/파트너/고객 공용)
 */
AdminService.getActiveColors = function () {
	return fetch({
		url: '/colors',
		method: 'get'
	})
}

/**
 * 관리자 컬러 생성/업서트
 * @param {{code:string,label:string,hex?:string|null,sortOrder?:number,isActive?:boolean}} data
 */
AdminService.createAdminColor = function (data) {
	return fetch({
		url: '/admin/colors',
		method: 'post',
		data
	})
}

/**
 * 관리자 컬러 활성화/비활성화
 * @param {string} code
 * @param {boolean} isActive
 */
AdminService.updateAdminColorStatus = function (code, isActive) {
	return fetch({
		url: `/admin/colors/${code}/status`,
		method: 'patch',
		data: { isActive }
	})
}

/**
 * 관리자 컬러 유사어 추가
 * @param {string} code
 * @param {string} synonym
 */
AdminService.addAdminColorSynonym = function (code, synonym) {
	return fetch({
		url: `/admin/colors/${code}/synonyms`,
		method: 'post',
		data: { synonym }
	})
}

/**
 * 관리자 컬러 유사어 삭제
 * @param {number} synonymId
 */
AdminService.deleteAdminColorSynonym = function (synonymId) {
	return fetch({
		url: `/admin/colors/synonyms/${synonymId}`,
		method: 'delete'
	})
}

export default AdminService;
