import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import PartnerService from 'services/PartnerService';

export const initialState = {
	loading: false,
	inventories: [],
	error: null
}

/**
 * 파트너의 전체 재고 목록을 조회합니다.
 */
export const fetchMyInventories = createAsyncThunk(
	'inventory/fetchMyInventories',
	async (_, { rejectWithValue }) => {
		try {
			const response = await PartnerService.getMyInventories();
			// ApiResponse 형태인 경우 data 필드 추출, 아니면 response 자체가 배열
			const data = Array.isArray(response) ? response : (response?.data || []);
			return Array.isArray(data) ? data : [];
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재고 목록을 불러오는데 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 특정 옵션의 재고를 조회합니다.
 */
export const fetchInventoryByOptionNo = createAsyncThunk(
	'inventory/fetchInventoryByOptionNo',
	async (optionNo, { rejectWithValue }) => {
		try {
			const response = await PartnerService.getInventoryByOptionNo(optionNo);
			return response;
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재고 조회에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 특정 상품의 재고를 조회합니다 (옵션이 없는 상품의 경우).
 */
export const fetchInventoryByProductNo = createAsyncThunk(
	'inventory/fetchInventoryByProductNo',
	async (productNo, { rejectWithValue }) => {
		try {
			const response = await PartnerService.getInventoryByProductNo(productNo);
			return response;
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재고 조회에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 재고를 생성합니다 (옵션이 있는 상품의 경우).
 */
export const createInventory = createAsyncThunk(
	'inventory/createInventory',
	async ({ optionNo, inventoryData }, { rejectWithValue }) => {
		try {
			const response = await PartnerService.createInventory(optionNo, inventoryData);
			return response;
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재고 생성에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 재고를 생성합니다 (옵션이 없는 상품의 경우).
 */
export const createInventoryForProduct = createAsyncThunk(
	'inventory/createInventoryForProduct',
	async ({ productNo, inventoryData }, { rejectWithValue }) => {
		try {
			const response = await PartnerService.createInventoryForProduct(productNo, inventoryData);
			return response;
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재고 생성에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 재고를 수정합니다 (옵션이 있는 상품의 경우).
 */
export const updateInventory = createAsyncThunk(
	'inventory/updateInventory',
	async ({ optionNo, inventoryData }, { rejectWithValue }) => {
		try {
			const response = await PartnerService.updateInventory(optionNo, inventoryData);
			return response;
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재고 수정에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 재고를 수정합니다 (옵션이 없는 상품의 경우).
 */
export const updateInventoryForProduct = createAsyncThunk(
	'inventory/updateInventoryForProduct',
	async ({ productNo, inventoryData }, { rejectWithValue }) => {
		try {
			const response = await PartnerService.updateInventoryForProduct(productNo, inventoryData);
			return response;
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '재고 수정에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

export const inventorySlice = createSlice({
	name: 'inventory',
	initialState,
	reducers: {
		clearInventories: (state) => {
			state.inventories = [];
			state.error = null;
		}
	},
	extraReducers: (builder) => {
		builder
			.addCase(fetchMyInventories.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(fetchMyInventories.fulfilled, (state, action) => {
				state.loading = false;
				// payload가 배열인지 확인
				const payload = action.payload;
				state.inventories = Array.isArray(payload) ? payload : (payload?.data || []);
				state.error = null;
			})
			.addCase(fetchMyInventories.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
				state.inventories = [];
			})
			.addCase(createInventory.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(createInventory.fulfilled, (state, action) => {
				state.loading = false;
				if (action.payload) {
					state.inventories = [action.payload, ...state.inventories];
				}
				state.error = null;
			})
			.addCase(createInventory.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(createInventoryForProduct.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(createInventoryForProduct.fulfilled, (state, action) => {
				state.loading = false;
				if (action.payload) {
					state.inventories = [action.payload, ...state.inventories];
				}
				state.error = null;
			})
			.addCase(createInventoryForProduct.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(updateInventory.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(updateInventory.fulfilled, (state, action) => {
				state.loading = false;
				const updatedInventory = action.payload;
				state.inventories = state.inventories.map(inventory => 
					inventory.inventoryNo === updatedInventory.inventoryNo ? updatedInventory : inventory
				);
				state.error = null;
			})
			.addCase(updateInventory.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(updateInventoryForProduct.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(updateInventoryForProduct.fulfilled, (state, action) => {
				state.loading = false;
				const updatedInventory = action.payload;
				state.inventories = state.inventories.map(inventory => 
					inventory.inventoryNo === updatedInventory.inventoryNo ? updatedInventory : inventory
				);
				state.error = null;
			})
			.addCase(updateInventoryForProduct.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			});
	},
});

export const { clearInventories } = inventorySlice.actions;
export default inventorySlice.reducer;
