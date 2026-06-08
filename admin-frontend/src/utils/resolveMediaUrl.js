import { API_BASE_URL, CUSTOMER_WEB_URL } from 'configs/AppConfig';

function apiOrigin() {
	const base = API_BASE_URL || 'http://localhost:8080/api';
	if (/^https?:\/\//i.test(base)) {
		return base.replace(/\/api\/?$/, '');
	}
	if (typeof window !== 'undefined' && window.location?.origin) {
		return window.location.origin;
	}
	return 'http://localhost:8080';
}

/**
 * DB·API에 저장된 상대 경로를 admin SPA에서 표시 가능한 절대 URL로 변환한다.
 * - /images/... → 고객몰 static (시드 상품 이미지)
 * - /api/..., /uploads/... → API 서버
 * - /img/... → admin-frontend public (그대로)
 * - http(s):// → 그대로
 */
export function resolveMediaUrl(url) {
	if (url == null) {
		return '';
	}
	const trimmed = String(url).trim();
	if (!trimmed) {
		return '';
	}
	if (/^https?:\/\//i.test(trimmed)) {
		return trimmed;
	}
	if (!trimmed.startsWith('/')) {
		return trimmed;
	}
	if (trimmed.startsWith('/images/')) {
		const customerBase = (CUSTOMER_WEB_URL || 'http://localhost:3000').replace(/\/$/, '');
		return `${customerBase}${trimmed}`;
	}
	if (trimmed.startsWith('/api/') || trimmed.startsWith('/uploads/')) {
		return `${apiOrigin()}${trimmed}`;
	}
	return trimmed;
}

export default resolveMediaUrl;
