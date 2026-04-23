"use client";

import Link from "next/link";
import { Suspense } from "react";
import { usePathname, useSearchParams } from "next/navigation";
import {
  getProductSubTypeLabel,
  getProductTypeLabel,
} from "@/data/productTaxonomy";
import { SHOP_LIST_PATH } from "@/data/navMain";

function ShopPageHeadingInner() {
  const pathname = usePathname();
  const shopPath = pathname?.split("?")[0] || SHOP_LIST_PATH;
  const sp = useSearchParams();
  const pt = sp.get("productType");
  const pst = sp.get("productSubType");
  const typeLabel = getProductTypeLabel(pt);
  const subLabel = getProductSubTypeLabel(pt, pst);

  let heading = "전체 상품";
  if (typeLabel && subLabel) heading = `${typeLabel} · ${subLabel}`;
  else if (typeLabel) heading = typeLabel;

  return (
    <div className="container-full">
      <div className="row">
        <div className="col-12">
          <h3 className="heading text-center">{heading}</h3>
          <ul className="breadcrumbs d-flex align-items-center justify-content-center">
            <li>
              <Link className="link" href="/">
                홈
              </Link>
            </li>
            <li>
              <i className="icon-arrRight" />
            </li>
            <li>
              <Link className="link" href={shopPath}>
                쇼핑
              </Link>
            </li>
            <li>
              <i className="icon-arrRight" />
            </li>
            <li>{heading}</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

function Fallback() {
  return (
    <div className="container-full">
      <div className="row">
        <div className="col-12">
          <h3 className="heading text-center">전체 상품</h3>
          <ul className="breadcrumbs d-flex align-items-center justify-content-center">
            <li>
              <Link className="link" href="/">
                홈
              </Link>
            </li>
            <li>
              <i className="icon-arrRight" />
            </li>
            <li>
              <Link className="link" href={SHOP_LIST_PATH}>
                쇼핑
              </Link>
            </li>
            <li>
              <i className="icon-arrRight" />
            </li>
            <li>전체 상품</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default function ShopPageHeading() {
  return (
    <Suspense fallback={<Fallback />}>
      <ShopPageHeadingInner />
    </Suspense>
  );
}
