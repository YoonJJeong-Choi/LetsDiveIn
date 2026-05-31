/** 고객 헤더/모바일 메뉴 — 쇼핑(전체 + 대분류) 공통 */

import { PRODUCT_TYPES } from "@/data/productTaxonomy";

export const SHOP_LIST_PATH = "/shop-default-grid";

export const SHOP_PRODUCT_TYPES = PRODUCT_TYPES;

/** @param {string} [productType] @param {string} [productSubType] */
export function shopHref(productType, productSubType) {
  if (!productType && !productSubType) return SHOP_LIST_PATH;
  const q = new URLSearchParams();
  if (productType) q.set("productType", productType);
  if (productSubType) q.set("productSubType", productSubType);
  const s = q.toString();
  return s ? `${SHOP_LIST_PATH}?${s}` : SHOP_LIST_PATH;
}

/** 고객센터 드롭다운 */
export const CUSTOMER_SERVICE_LINKS = [
  { href: "/FAQs", label: "자주 묻는 질문" },
  { href: "/qna", label: "1:1 QnA" },
];
