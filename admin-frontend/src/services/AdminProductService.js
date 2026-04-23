import fetch from 'auth/FetchInterceptor'

const AdminProductService = {}

AdminProductService.getImages = function (productNo) {
	return fetch({
		url: `/admin/products/${productNo}/images`,
		method: 'get'
	})
}

AdminProductService.updateImages = function (productNo, images) {
	return fetch({
		url: `/admin/products/${productNo}/images`,
		method: 'patch',
		data: { images }
	})
}

export default AdminProductService

