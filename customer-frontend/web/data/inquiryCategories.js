export const INQUIRY_CATEGORIES = [
  { code: "ORDER_PAYMENT", label: "주문/결제" },
  { code: "DELIVERY", label: "배송" },
  { code: "RETURN_EXCHANGE", label: "취소/반품/교환" },
  { code: "MEMBER", label: "회원정보" },
  { code: "PRODUCT", label: "상품" },
  { code: "POINT", label: "포인트" },
  { code: "ETC", label: "기타" },
];

export const QNA_SCOPES = [
  { code: "POLICY", label: "이용 방법·기간·정책이 궁금해요" },
  { code: "ORDER_ITEM", label: "주문한 상품의 취소/반품/교환 건이에요" },
];

export function getCategoryLabel(code) {
  return INQUIRY_CATEGORIES.find((c) => c.code === code)?.label ?? code;
}
