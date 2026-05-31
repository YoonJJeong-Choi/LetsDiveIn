export const INQUIRY_CATEGORIES = [
	{ code: 'ORDER_PAYMENT', label: '주문/결제' },
	{ code: 'DELIVERY', label: '배송' },
	{ code: 'RETURN_EXCHANGE', label: '취소/반품/교환' },
	{ code: 'MEMBER', label: '회원정보' },
	{ code: 'PRODUCT', label: '상품' },
	{ code: 'POINT', label: '포인트' },
	{ code: 'ETC', label: '기타' },
];

export const getCategoryLabel = (code) => {
	const found = INQUIRY_CATEGORIES.find((c) => c.code === code);
	return found ? found.label : code;
};
