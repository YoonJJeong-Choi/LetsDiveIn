import axios from 'axios';
import { API_BASE_URL } from 'configs/AppConfig';
import { signOutSuccess } from 'store/slices/authSlice';
import store from '../store';
import { notification } from 'antd';

const LOGIN_PATH = '/auth/login'

const isAuthRequest = (url = '') => url.includes('/auth/login') || url.includes('/auth/me')

const buildAuthFailureMessage = (errorMessage) => ({
	message: '접근할 수 없는 계정입니다',
	description: errorMessage || '관리자 또는 파트너 계정으로 다시 로그인해주세요.'
})

const buildForbiddenMessage = (errorMessage) => ({
	message: '접근 권한이 없습니다',
	description: errorMessage || '이 작업을 수행할 권한이 없습니다.'
})

const buildUnauthorizedMessage = (errorMessage) => ({
	message: '로그인이 필요합니다',
	description: errorMessage || '로그인 후 다시 시도해주세요.'
})

const redirectToLoginWithReason = (reason) => {
	if (typeof window === 'undefined' || window.location.pathname === LOGIN_PATH) {
		return;
	}
	window.location.replace(`${LOGIN_PATH}?reason=${reason}`);
}

const service = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  withCredentials: true // 세션 쿠키를 위한 설정
})

// API Request interceptor
service.interceptors.request.use(config => {
	// 세션 기반 인증이므로 JWT 토큰 헤더 설정 불필요
	// 세션 쿠키는 withCredentials: true로 자동 전송됨
  	return config
}, error => {
	// Do something with request error here
	notification.error({
		message: '요청 처리 중 오류가 발생했습니다'
	})
	return Promise.reject(error)
})

// API respone interceptor
service.interceptors.response.use( (response) => {
	// blob 응답의 경우 response.data를 그대로 반환 (이미 Blob 객체)
	if (response.config.responseType === 'blob' || response.data instanceof Blob) {
		return response.data;
	}
	return response.data
}, (error) => {
	// error.response가 없으면 네트워크 에러 등
	if (!error.response) {
		notification.error({
			message: '네트워크 오류',
			description: '서버에 연결할 수 없습니다.'
		})
		return Promise.reject(error);
	}

	const status = error.response.status;
	const url = error.config?.url || '';
	const errorMessage = error.response?.data?.message;

	// /auth/me 호출 시 401은 정상 (로그인하지 않은 상태) - 에러 표시 안 함
	if (status === 401 && url.includes('/auth/me')) {
		return Promise.reject(error);
	}

	let notificationParam = {
		message: ''
	}
 
	if (status === 401) {
		store.dispatch(signOutSuccess())
		if (url.includes('/auth/login')) {
			return Promise.reject(error)
		}
		notification.error(buildUnauthorizedMessage(errorMessage))
		redirectToLoginWithReason('expired')
		return Promise.reject(error);
	}

	if (status === 403) {
		if (url.includes('/auth/login')) {
			return Promise.reject(error);
		}

		if (url.includes('/auth/me')) {
			store.dispatch(signOutSuccess())
			redirectToLoginWithReason('portal')
			return Promise.reject(error);
		}

		if (!isAuthRequest(url)) {
			notification.error(buildForbiddenMessage(errorMessage))
		}
		return Promise.reject(error);
	}

	if (status === 429) {
		notificationParam.message = errorMessage || '오늘 AI 사용 한도에 도달했습니다'
		notification.error(notificationParam)
		return Promise.reject(error);
	}

	if (status === 404) {
		notificationParam.message = '요청한 정보를 찾을 수 없습니다'
		notification.error(notificationParam)
	}

	if (status === 500) {
		notificationParam.message = '서버 오류가 발생했습니다'
		notification.error(notificationParam)
	}
	
	if (status === 508) {
		notificationParam.message = '응답 시간이 초과되었습니다'
		notification.error(notificationParam)
	}

	return Promise.reject(error);
});

export default service