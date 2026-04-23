/** 고객 헤더/모바일 메뉴 — 쇼핑(전체 + 대분류) 공통 */

import { PRODUCT_TYPES } from "@/data/productTaxonomy";

export const SHOP_LIST_PATH = "/shop-default-grid";

export const SHOP_PRODUCT_TYPES = PRODUCT_TYPES;

export function shopHref(productType) {
  if (!productType) return SHOP_LIST_PATH;
  const q = new URLSearchParams({ productType });
  return `${SHOP_LIST_PATH}?${q.toString()}`;
}

/** 고객센터 드롭다운 — 문의(/contact)는 기능 준비 후 아래 한 줄 추가 */
export const CUSTOMER_SERVICE_LINKS = [
  { href: "/FAQs", label: "자주 묻는 질문" },
  // { href: "/contact", label: "문의하기" },
];
