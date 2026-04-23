import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import AdminService from 'services/AdminService';

export const initialState = {
	loading: false,
	pendingPartners: [],
	allPartners: [], // 전체 파트너 목록
	selectedPartner: null, // 선택된 파트너 상세 정보
	partnerHistory: [], // 파트너 이력 목록
	allInventories: [], // 전체 재고 목록 (관리자용)
	partnerInventories: [], // 파트너별 재고 목록 (관리자용)
	error: null
}

/**
 * 대기 중인 파트너 신청 목록을 조회합니다.
 */
export const fetchPendingPartners = createAsyncThunk(
	'admin/fetchPendingPartners',
	async (_, { rejectWithValue }) => {
		try {
			const response = await AdminService.getPendingPartners();
			// ApiResponse 형태일 수 있으므로 data 필드 확인
			const data = response.data || response;
			return Array.isArray(data) ? data : [];
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 신청 목록을 불러오는데 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너를 승인합니다.
 */
export const approvePartner = createAsyncThunk(
	'admin/approvePartner',
	async (partnerId, { rejectWithValue }) => {
		try {
			const response = await AdminService.approvePartner(partnerId);
			return { partnerId, data: response.data || response };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 승인에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너를 거절합니다.
 */
export const rejectPartner = createAsyncThunk(
	'admin/rejectPartner',
	async ({ partnerId, rejectionReason }, { rejectWithValue }) => {
		try {
			const response = await AdminService.rejectPartner(partnerId, rejectionReason);
			return { partnerId, data: response.data || response };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 거절에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너 목록을 조회합니다 (상태별 필터링 및 휴업 신청 필터 가능).
 */
export const fetchAllPartners = createAsyncThunk(
	'admin/fetchAllPartners',
	async ({ status = null, hasDeactivationRequest = null, hasReactivationRequest = null } = {}, { rejectWithValue }) => {
		try {
			const response = await AdminService.getAllPartners(status, hasDeactivationRequest, hasReactivationRequest);
			const data = response.data || response;
			return Array.isArray(data) ? data : [];
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 목록을 불러오는데 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너 상세 정보를 조회합니다.
 */
export const fetchPartnerDetail = createAsyncThunk(
	'admin/fetchPartnerDetail',
	async (partnerId, { rejectWithValue }) => {
		try {
			const response = await AdminService.getPartnerDetail(partnerId);
			return response.data || response;
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 상세 정보를 불러오는데 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너를 비활성화합니다.
 */
export const deactivatePartner = createAsyncThunk(
	'admin/deactivatePartner',
	async (partnerId, { rejectWithValue }) => {
		try {
			const response = await AdminService.deactivatePartner(partnerId);
			return { partnerId, data: response.data || response };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 비활성화에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너를 재활성화합니다.
 */
export const activatePartner = createAsyncThunk(
	'admin/activatePartner',
	async (partnerId, { rejectWithValue }) => {
		try {
			const response = await AdminService.activatePartner(partnerId);
			return { partnerId, data: response.data || response };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 재활성화에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 휴업 신청 승인
 */
export const approveDeactivationRequest = createAsyncThunk(
	'admin/approveDeactivationRequest',
	async (partnerId, { rejectWithValue }) => {
		try {
			const response = await AdminService.approveDeactivationRequest(partnerId);
			return { partnerId, data: response.data || response };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '휴업 신청 승인에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 휴업 신청 거절
 */
export const rejectDeactivationRequest = createAsyncThunk(
	'admin/rejectDeactivationRequest',
	async ({ partnerId, rejectionReason }, { rejectWithValue }) => {
		try {
			const response = await AdminService.rejectDeactivationRequest(partnerId, { rejectionReason });
			return { partnerId, data: response.data || response };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '휴업 신청 거절에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 재활성화 신청 거절
 */
export const rejectReactivationRequest = createAsyncThunk(
	'admin/rejectReactivationRequest',
	async ({ partnerId, rejectionReason }, { rejectWithValue }) => {
		try {
			const response = await AdminService.rejectReactivationRequest(partnerId, { rejectionReason });
			return { partnerId, data: response.data || response };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재활성화 신청 거절에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너 이력 조회
 */
export const fetchPartnerHistory = createAsyncThunk(
	'admin/fetchPartnerHistory',
	async ({ partnerId, actionType = null }, { rejectWithValue }) => {
		try {
			const response = await AdminService.getPartnerHistory(partnerId, actionType);
			const data = response.data || response;
			return { partnerId, history: Array.isArray(data) ? data : [], actionType };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 이력 조회에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

export const adminSlice = createSlice({
	name: 'admin',
	initialState,
	reducers: {
		clearPendingPartners: (state) => {
			state.pendingPartners = [];
			state.error = null;
		},
		clearAllPartners: (state) => {
			state.allPartners = [];
			state.error = null;
		},
		clearSelectedPartner: (state) => {
			state.selectedPartner = null;
		}
	},
	extraReducers: (builder) => {
		builder
			.addCase(fetchPendingPartners.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(fetchPendingPartners.fulfilled, (state, action) => {
				state.loading = false;
				state.pendingPartners = action.payload || [];
				state.error = null;
			})
			.addCase(fetchPendingPartners.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
				state.pendingPartners = [];
			})
			.addCase(approvePartner.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(approvePartner.fulfilled, (state, action) => {
				state.loading = false;
				// 승인된 파트너를 목록에서 제거
				state.pendingPartners = state.pendingPartners.filter(
					partner => partner.partnerId !== action.payload.partnerId
				);
				state.error = null;
			})
			.addCase(approvePartner.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(rejectPartner.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(rejectPartner.fulfilled, (state, action) => {
				state.loading = false;
				// 거절된 파트너를 목록에서 제거
				state.pendingPartners = state.pendingPartners.filter(
					partner => partner.partnerId !== action.payload.partnerId
				);
				state.error = null;
			})
			.addCase(rejectPartner.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(fetchAllPartners.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(fetchAllPartners.fulfilled, (state, action) => {
				state.loading = false;
				state.allPartners = action.payload || [];
				state.error = null;
			})
			.addCase(fetchAllPartners.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
				state.allPartners = [];
			})
			.addCase(fetchPartnerDetail.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(fetchPartnerDetail.fulfilled, (state, action) => {
				state.loading = false;
				state.selectedPartner = action.payload;
				state.error = null;
			})
			.addCase(fetchPartnerDetail.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
				state.selectedPartner = null;
			})
			.addCase(deactivatePartner.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(deactivatePartner.fulfilled, (state, action) => {
				state.loading = false;
				// 목록에서 해당 파트너 상태 업데이트
				const partnerId = action.payload.partnerId;
				const index = state.allPartners.findIndex(p => p.partnerId === partnerId);
				if (index !== -1) {
					state.allPartners[index] = action.payload.data;
				}
				state.error = null;
			})
			.addCase(deactivatePartner.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(activatePartner.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(activatePartner.fulfilled, (state, action) => {
				state.loading = false;
				// 목록에서 해당 파트너 상태 업데이트
				const partnerId = action.payload.partnerId;
				const index = state.allPartners.findIndex(p => p.partnerId === partnerId);
				if (index !== -1) {
					state.allPartners[index] = action.payload.data;
				}
				state.error = null;
			})
			.addCase(activatePartner.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(approveDeactivationRequest.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(approveDeactivationRequest.fulfilled, (state, action) => {
				state.loading = false;
				// 목록에서 해당 파트너 상태 업데이트
				const partnerId = action.payload.partnerId;
				const index = state.allPartners.findIndex(p => p.partnerId === partnerId);
				if (index !== -1) {
					state.allPartners[index] = action.payload.data;
				}
				state.error = null;
			})
			.addCase(approveDeactivationRequest.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(rejectDeactivationRequest.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(rejectDeactivationRequest.fulfilled, (state, action) => {
				state.loading = false;
				// 목록에서 해당 파트너 상태 업데이트
				const partnerId = action.payload.partnerId;
				const index = state.allPartners.findIndex(p => p.partnerId === partnerId);
				if (index !== -1) {
					state.allPartners[index] = action.payload.data;
				}
				state.error = null;
			})
			.addCase(rejectDeactivationRequest.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(rejectReactivationRequest.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(rejectReactivationRequest.fulfilled, (state, action) => {
				state.loading = false;
				// 목록에서 해당 파트너 상태 업데이트
				const partnerId = action.payload.partnerId;
				const index = state.allPartners.findIndex(p => p.partnerId === partnerId);
				if (index !== -1) {
					state.allPartners[index] = action.payload.data;
				}
				state.error = null;
			})
			.addCase(rejectReactivationRequest.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(fetchPartnerHistory.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(fetchPartnerHistory.fulfilled, (state, action) => {
				state.loading = false;
				state.partnerHistory = action.payload.history || [];
				state.error = null;
			})
			.addCase(fetchPartnerHistory.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
				state.partnerHistory = [];
			})
			.addCase(fetchAllInventories.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(fetchAllInventories.fulfilled, (state, action) => {
				state.loading = false;
				state.allInventories = action.payload || [];
				state.error = null;
			})
			.addCase(fetchAllInventories.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
				state.allInventories = [];
			})
			.addCase(fetchInventoriesByPartner.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(fetchInventoriesByPartner.fulfilled, (state, action) => {
				state.loading = false;
				state.partnerInventories = action.payload.inventories || [];
				state.error = null;
			})
			.addCase(fetchInventoriesByPartner.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
				state.partnerInventories = [];
			});
	},
});

/**
 * 전체 재고 목록 조회 (관리자용)
 */
export const fetchAllInventories = createAsyncThunk(
	'admin/fetchAllInventories',
	async (_, { rejectWithValue }) => {
		try {
			const response = await AdminService.getAllInventories();
			const data = response.data || response;
			return Array.isArray(data) ? data : [];
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재고 목록을 불러오는데 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 특정 파트너의 재고 목록 조회 (관리자용)
 */
export const fetchInventoriesByPartner = createAsyncThunk(
	'admin/fetchInventoriesByPartner',
	async (partnerId, { rejectWithValue }) => {
		try {
			const response = await AdminService.getInventoriesByPartner(partnerId);
			const data = response.data || response;
			return { partnerId, inventories: Array.isArray(data) ? data : [] };
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '파트너 재고 목록을 불러오는데 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

export const { clearPendingPartners, clearAllPartners, clearSelectedPartner, clearAllInventories, clearPartnerInventories } = adminSlice.actions;
export default adminSlice.reducer;
