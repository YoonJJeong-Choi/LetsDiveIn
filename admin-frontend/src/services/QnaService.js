import fetch from 'auth/FetchInterceptor'

const QnaService = {}

QnaService.getAdminQnaList = function (all = false) {
	return fetch({
		url: '/admin/qna',
		method: 'get',
		params: { all },
	})
}

QnaService.getAdminQnaDetail = function (qnaNo) {
	return fetch({
		url: `/admin/qna/${qnaNo}`,
		method: 'get',
	})
}

QnaService.replyAdminQna = function (qnaNo, body) {
	return fetch({
		url: `/admin/qna/${qnaNo}/reply`,
		method: 'post',
		data: { body },
	})
}

QnaService.getPartnerQnaList = function () {
	return fetch({
		url: '/partner/qna',
		method: 'get',
	})
}

QnaService.getPartnerQnaDetail = function (qnaNo) {
	return fetch({
		url: `/partner/qna/${qnaNo}`,
		method: 'get',
	})
}

QnaService.replyPartnerQna = function (qnaNo, body) {
	return fetch({
		url: `/partner/qna/${qnaNo}/reply`,
		method: 'post',
		data: { body },
	})
}

QnaService.draftAssistAdminQna = function (qnaNo) {
	return fetch({
		url: `/admin/qna/${qnaNo}/draft-assist`,
		method: 'post',
		data: {},
	})
}

QnaService.draftAssistPartnerQna = function (qnaNo) {
	return fetch({
		url: `/partner/qna/${qnaNo}/draft-assist`,
		method: 'post',
		data: {},
	})
}

export default QnaService
