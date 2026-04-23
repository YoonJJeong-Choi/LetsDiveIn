import axios from 'axios';
import { API_BASE_URL } from 'configs/AppConfig';
import { signOutSuccess } from 'store/slices/authSlice';
import store from '../store';
import { notification } from 'antd';

const unauthorizedCode = [401, 403]

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
		message: 'Error'
	})
	Promise.reject(error)
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
			message: 'Network Error',
			description: '서버에 연결할 수 없습니다.'
		})
		return Promise.reject(error);
	}

	const status = error.response.status;
	const url = error.config?.url || '';

	// /auth/me 호출 시 401은 정상 (로그인하지 않은 상태) - 에러 표시 안 함
	if (status === 401 && url.includes('/auth/me')) {
		return Promise.reject(error);
	}

	let notificationParam = {
		message: ''
	}
 
	// Remove token and redirect 
	if (unauthorizedCode.includes(status)) {
		notificationParam.message = 'Authentication Fail'
		notificationParam.description = 'Please login again'
		// 세션 기반이므로 localStorage 제거 불필요
		store.dispatch(signOutSuccess())
		notification.error(notificationParam)
		return Promise.reject(error);
	}

	if (status === 404) {
		notificationParam.message = 'Not Found'
		notification.error(notificationParam)
	}

	if (status === 500) {
		notificationParam.message = 'Internal Server Error'
		notification.error(notificationParam)
	}
	
	if (status === 508) {
		notificationParam.message = 'Time Out'
		notification.error(notificationParam)
	}

	return Promise.reject(error);
});

export default service