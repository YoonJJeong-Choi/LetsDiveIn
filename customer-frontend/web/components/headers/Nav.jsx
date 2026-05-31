"use client";
import Link from "next/link";
import React, { Suspense } from "react";
import { usePathname, useSearchParams } from "next/navigation";
import {
  SHOP_LIST_PATH,
  SHOP_PRODUCT_TYPES,
  shopHref,
  CUSTOMER_SERVICE_LINKS,
} from "@/data/navMain";

function NavInner() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const productTypeFromUrl = searchParams.get("productType");

  const isHome = pathname === "/";
  const isShop =
    pathname === SHOP_LIST_PATH || pathname.startsWith(`${SHOP_LIST_PATH}/`);
  const isEvents = pathname.startsWith("/events");
  const isCustomerService = CUSTOMER_SERVICE_LINKS.some(
    (l) => pathname === l.href || pathname.startsWith(l.href + "/")
  );
  const isMyAccount = pathname.startsWith("/my-account");

  return (
    <>
      <li className={`menu-item ${isHome ? "active" : ""}`}>
        <Link href="/" className="item-link">
          홈
        </Link>
      </li>

      <li className={`menu-item position-relative ${isShop ? "active" : ""}`}>
        <a href="#" className="item-link">
          쇼핑
          <i className="icon icon-arrow-down" />
        </a>
        <div className="sub-menu submenu-default">
          <ul className="menu-list">
            <li
              className={`menu-item-li ${
                isShop && !productTypeFromUrl ? "active" : ""
              }`}
            >
              <Link href={SHOP_LIST_PATH} className="menu-link-text">
                전체 상품
              </Link>
            </li>
            {SHOP_PRODUCT_TYPES.map((t) => (
              <li
                key={t.value}
                className={`menu-item-li ${
                  productTypeFromUrl === t.value ? "active" : ""
                }`}
              >
                <Link href={shopHref(t.value)} className="menu-link-text">
                  {t.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </li>

      <li className={`menu-item ${isEvents ? "active" : ""}`}>
        <Link href="/events" className="item-link">
          이벤트·공지
        </Link>
      </li>

      <li
        className={`menu-item position-relative ${
          isCustomerService ? "active" : ""
        }`}
      >
        <a href="#" className="item-link">
          고객센터
          <i className="icon icon-arrow-down" />
        </a>
        <div className="sub-menu submenu-default">
          <ul className="menu-list">
            {CUSTOMER_SERVICE_LINKS.map((l) => (
              <li
                key={l.href}
                className={`menu-item-li ${pathname === l.href ? "active" : ""}`}
              >
                <Link href={l.href} className="menu-link-text">
                  {l.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </li>

      <li className={`menu-item ${isMyAccount ? "active" : ""}`}>
        <Link href="/my-account" className="item-link">
          마이페이지
        </Link>
      </li>
    </>
  );
}

export default function Nav() {
  return (
    <Suspense fallback={null}>
      <NavInner />
    </Suspense>
  );
}
