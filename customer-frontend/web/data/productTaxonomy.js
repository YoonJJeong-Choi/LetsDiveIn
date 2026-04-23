/**
 * 백엔드 ProductType / ProductSubType — 헤더·필터·상품목록 제목 공통
 */

export const PRODUCT_TYPES = [
  { value: "SWIMSUIT_MEN", label: "남성 수영복" },
  { value: "SWIMSUIT_WOMEN", label: "여성 수영복" },
  { value: "SWIMSUIT_KIDS", label: "아동 수영복" },
  { value: "SWIM_CAP", label: "수영모자" },
  { value: "SWIM_GOGGLES", label: "수영안경" },
  { value: "FINS", label: "오리발" },
  { value: "SWIM_TOY", label: "수영용품" },
  { value: "ETC", label: "기타" },
];

export const PRODUCT_SUB_TYPES = {
  SWIMSUIT_WOMEN: [
    { value: "ONE_PIECE", label: "원피스" },
    { value: "BIKINI", label: "비키니" },
    { value: "MONOKINI", label: "모노키니" },
    { value: "RASH_GUARD", label: "래쉬가드" },
  ],
  SWIMSUIT_MEN: [
    { value: "TRUNKS", label: "트렁크" },
    { value: "JAMMER", label: "잠머" },
    { value: "BRIEF", label: "브리프" },
  ],
  SWIM_CAP: [
    { value: "CAP_SILICONE", label: "실리콘 수모" },
    { value: "CAP_FABRIC", label: "천 수모" },
  ],
  FINS: [
    { value: "FINS_SHORT", label: "숏핀" },
    { value: "FINS_LONG", label: "롱핀" },
  ],
};

export function getProductTypeLabel(value) {
  if (!value) return null;
  return PRODUCT_TYPES.find((t) => t.value === value)?.label ?? null;
}

export function getProductSubTypeLabel(productType, subValue) {
  if (!productType || !subValue) return null;
  const list = PRODUCT_SUB_TYPES[productType] || [];
  return list.find((s) => s.value === subValue)?.label ?? null;
}
