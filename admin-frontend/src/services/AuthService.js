import fetch from 'auth/FetchInterceptor'

const AuthService = {}

AuthService.login = function (data) {
	return fetch({
		url: '/auth/login',
		method: 'post',
		data: {
			...data,
			portal: 'ADMIN'
		}
	})
}

AuthService.logout = function () {
	return fetch({
		url: '/auth/logout',
		method: 'post'
	})
}

AuthService.getCurrentUser = function () {
	return fetch({
		url: '/auth/me',
		method: 'get'
	})
}

export default AuthService;