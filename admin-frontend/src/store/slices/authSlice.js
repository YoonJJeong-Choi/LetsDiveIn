import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import AuthService from 'services/AuthService';

export const initialState = {
	loading: false,
	message: '',
	showMessage: false,
	redirect: '',
	token: null, // 세션 기반 인증: null = 미로그인, 'session' = 로그인됨 (세션 쿠키로 관리)
	user: null // 현재 사용자 정보 (role, name, email 등)
}

export const signIn = createAsyncThunk('auth/signIn',async (data, { rejectWithValue }) => {
	const { email, password } = data
	try {
		const response = await AuthService.login({ email, password })
		// 세션 기반 인증: 세션 쿠키는 자동으로 저장됨
		// token은 Redux 상태 관리용 (null = 미로그인, 'session' = 로그인됨)
		return { user: response, token: 'session' };
	} catch (err) {
		const errorMessage = err.response?.data?.message || err.message || '로그인에 실패했습니다.'
		return rejectWithValue(errorMessage)
	}
})

export const signOut = createAsyncThunk('auth/signOut',async (_, { rejectWithValue }) => {
	try {
		await AuthService.logout()
		// 세션 기반이므로 localStorage 제거 불필요
		return null
	} catch (err) {
		// 로그아웃 실패해도 클라이언트에서는 로그아웃 처리
		return null
	}
})

export const checkAuth = createAsyncThunk('auth/checkAuth', async (_, { rejectWithValue }) => {
	try {
		const response = await AuthService.getCurrentUser()
		// 세션 확인 성공: 로그인된 상태
		return { user: response, token: 'session' }
	} catch (err) {
		return rejectWithValue(null) // 인증되지 않음
	}
})

export const authSlice = createSlice({
	name: 'auth',
	initialState,
	reducers: {
		authenticated: (state, action) => {
			state.loading = false
			state.redirect = '/'
			state.token = action.payload
		},
		showAuthMessage: (state, action) => {
			state.message = action.payload
			state.showMessage = true
			state.loading = false
		},
		hideAuthMessage: (state) => {
			state.message = ''
			state.showMessage = false
		},
		signOutSuccess: (state) => {
			state.loading = false
			state.token = null
			state.redirect = '/'
		},
		showLoading: (state) => {
			state.loading = true
		},
		signInSuccess: (state, action) => {
			state.loading = false
			state.token = action.payload
		}
	},
	extraReducers: (builder) => {
		builder
			.addCase(signIn.pending, (state) => {
				state.loading = true
			})
			.addCase(signIn.fulfilled, (state, action) => {
				state.loading = false
				// role에 따라 다른 페이지로 리다이렉트 (선택사항)
				const role = action.payload.user?.role
				if (role === 'ADMIN') {
					state.redirect = '/app/dashboards/default'
				} else if (role === 'PARTNER') {
					state.redirect = '/app/dashboards/default' // 파트너 대시보드 (나중에 변경 가능)
				} else {
					state.redirect = '/app/dashboards/default'
				}
				state.token = action.payload.token
				state.user = action.payload.user
			})
			.addCase(signIn.rejected, (state, action) => {
				state.message = action.payload
				state.showMessage = true
				state.loading = false
			})
			.addCase(signOut.fulfilled, (state) => {
				state.loading = false
				state.token = null
				state.redirect = '/'
			})
			.addCase(signOut.rejected, (state) => {
				state.loading = false
				state.token = null
				state.redirect = '/'
			})
			.addCase(checkAuth.fulfilled, (state, action) => {
				state.token = action.payload.token
				state.user = action.payload.user
			})
			.addCase(checkAuth.rejected, (state) => {
				state.token = null
				state.user = null
			})
	},
})

export const { 
	authenticated,
	showAuthMessage,
	hideAuthMessage,
	signOutSuccess,
	showLoading,
	signInSuccess
} = authSlice.actions

export default authSlice.reducer