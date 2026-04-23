import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import PartnerService from 'services/PartnerService';

export const initialState = {
	loading: false,
	products: [],
	error: null
}

/**
 * 파트너의 상품 목록을 조회합니다.
 */
export const fetchMyProducts = createAsyncThunk(
	'partner/fetchMyProducts',
	async (_, { rejectWithValue }) => {
		try {
			const response = await PartnerService.getMyProducts();
			return response; // 백엔드에서 배열로 반환
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '상품 목록을 불러오는데 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너 상품을 등록합니다.
 */
export const createProduct = createAsyncThunk(
	'partner/createProduct',
	async (productData, { rejectWithValue }) => {
		try {
			const response = await PartnerService.createProduct(productData);
			return response; // 등록된 상품 정보
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '상품 등록에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너 상품을 수정합니다.
 */
export const updateProduct = createAsyncThunk(
	'partner/updateProduct',
	async ({ productNo, productData }, { rejectWithValue }) => {
		try {
			const response = await PartnerService.updateProduct(productNo, productData);
			return response; // 수정된 상품 정보
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '상품 수정에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너 상품을 삭제합니다.
 */
export const deleteProduct = createAsyncThunk(
	'partner/deleteProduct',
	async (productNo, { rejectWithValue }) => {
		try {
			await PartnerService.deleteProduct(productNo);
			return productNo; // 삭제된 상품 번호 반환
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '상품 삭제에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

/**
 * 파트너 상품 수정 신청 취소 (PENDING_UPDATE → ACTIVE)
 */
export const cancelProductUpdate = createAsyncThunk(
	'partner/cancelProductUpdate',
	async (productNo, { rejectWithValue }) => {
		try {
			const response = await PartnerService.cancelProductUpdate(productNo);
			return response; // 취소된 상품 정보
		} catch (err) {
			const errorMessage = err.response?.data?.message || err.message || '수정 신청 취소에 실패했습니다.';
			return rejectWithValue(errorMessage);
		}
	}
);

export const partnerSlice = createSlice({
	name: 'partner',
	initialState,
	reducers: {
		clearProducts: (state) => {
			state.products = [];
			state.error = null;
		}
	},
	extraReducers: (builder) => {
		builder
			.addCase(fetchMyProducts.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(fetchMyProducts.fulfilled, (state, action) => {
				state.loading = false;
				state.products = action.payload || [];
				state.error = null;
			})
			.addCase(fetchMyProducts.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
				state.products = [];
			})
			.addCase(createProduct.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(createProduct.fulfilled, (state, action) => {
				state.loading = false;
				// 등록된 상품을 목록에 추가 (fetchMyProducts가 호출되므로 임시로만 추가)
				// 실제 목록은 fetchMyProducts에서 서버에서 가져온 데이터로 교체됨
				if (action.payload) {
					state.products = [action.payload, ...state.products];
				}
				state.error = null;
			})
			.addCase(createProduct.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(updateProduct.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(updateProduct.fulfilled, (state, action) => {
				state.loading = false;
				// 수정된 상품을 목록에서 업데이트
				const updatedProduct = action.payload;
				state.products = state.products.map(product => 
					product.productNo === updatedProduct.productNo ? updatedProduct : product
				);
				state.error = null;
			})
			.addCase(updateProduct.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			})
			.addCase(deleteProduct.pending, (state) => {
				state.loading = true;
				state.error = null;
			})
			.addCase(deleteProduct.fulfilled, (state, action) => {
				state.loading = false;
				// 삭제된 상품을 목록에서 제거
				state.products = state.products.filter(
					product => product.productNo !== action.payload
				);
				state.error = null;
			})
			.addCase(deleteProduct.rejected, (state, action) => {
				state.loading = false;
				state.error = action.payload;
			});
	},
});

export const { clearProducts } = partnerSlice.actions;
export default partnerSlice.reducer;
