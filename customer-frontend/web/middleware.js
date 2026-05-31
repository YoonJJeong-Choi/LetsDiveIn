import { NextResponse } from "next/server";

/**
 * 예전 `/product-*-layout/:id` 데모 경로는 제거됨. 동일 상품은 `/product-detail/:id`로 리다이렉트.
 */
export function middleware(request) {
  const pathname = request.nextUrl.pathname;
  const match = pathname.match(/^\/product-([^/]+)\/([^/]+)\/?$/);
  if (!match) return NextResponse.next();
  const [, layoutSegment, productId] = match;
  if (layoutSegment === "detail") return NextResponse.next();
  const url = request.nextUrl.clone();
  url.pathname = `/product-detail/${productId}`;
  return NextResponse.redirect(url, 308);
}

export const config = {
  matcher: ["/product-:layout/:id"],
};
