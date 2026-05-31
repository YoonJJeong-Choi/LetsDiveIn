/** 시드·폴백용 대표 상품 이미지 (public/images/products/cap01.png) */
export const DEFAULT_PRODUCT_PLACEHOLDER = "/images/products/cap01.png";

/**
 * 상품 이미지: 레거시 URL 문자열 또는 업로드 fileId → 브라우저에서 요청할 경로
 * (동일 출처 `/api/files/:id/download`, Next rewrites로 백엔드 전달)
 */
export function resolveProductImageSrc(source) {
  if (source == null) return "";
  if (typeof source === "string") {
    return source.trim();
  }
  const url =
    source.imageUrl ??
    source.productImageUrl ??
    source.imgSrc ??
    "";
  const trimmed = typeof url === "string" ? url.trim() : "";
  if (trimmed) return trimmed;
  const fid = source.fileId ?? source.file_id;
  if (fid != null && fid !== "" && Number.isFinite(Number(fid))) {
    return `/api/files/${Number(fid)}/download`;
  }
  return "";
}

export function resolveProductImageSrcOrFallback(
  source,
  fallback = DEFAULT_PRODUCT_PLACEHOLDER,
) {
  const s = resolveProductImageSrc(source);
  return s || fallback;
}

/** 목록 DTO(productImageUrl + images[])에서 카드용 썸네일 한 개 */
/** 장바구니·모달 등 Next/Image용 — 빈 URL이면 폴백 (src="" 경고 방지) */
export function productDisplayImageSrc(
  source,
  fallback = DEFAULT_PRODUCT_PLACEHOLDER,
) {
  return resolveProductImageSrcOrFallback(source, fallback);
}

export function pickProductListThumbnail(product, fallback) {
  const main = resolveProductImageSrc({
    productImageUrl: product?.productImageUrl,
    imageUrl: product?.productImageUrl,
  });
  if (main) return resolveProductImageSrcOrFallback(main, fallback);
  const imgs = Array.isArray(product?.images) ? product.images : [];
  const sorted = [...imgs].sort((a, b) => {
    if (a?.isPrimary && !b?.isPrimary) return -1;
    if (!a?.isPrimary && b?.isPrimary) return 1;
    return (a?.sortOrder ?? 0) - (b?.sortOrder ?? 0);
  });
  for (const img of sorted) {
    const u = resolveProductImageSrc(img);
    if (u) return resolveProductImageSrcOrFallback(u, fallback);
  }
  return fallback;
}
