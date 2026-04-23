import fetch from 'auth/FetchInterceptor'

const FileService = {}

/**
 * 파일 업로드 (multipart/form-data)
 * @param {File} file
 * @param {string} category - 예: 'profile', 'product', 'event', 'partner-doc'
 */
FileService.uploadFile = function (file, category = 'profile') {
	const formData = new FormData()
	formData.append('file', file)
	formData.append('category', category)
	return fetch({
		url: '/files/upload',
		method: 'post',
		data: formData,
		headers: {
			'Content-Type': 'multipart/form-data'
		}
	})
}

export default FileService

