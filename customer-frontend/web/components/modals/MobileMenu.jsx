"use client";
import React, { Suspense } from "react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import {
  SHOP_LIST_PATH,
  SHOP_PRODUCT_TYPES,
  shopHref,
  CUSTOMER_SERVICE_LINKS,
} from "@/data/navMain";
import {
  STORE_ADDRESS,
  STORE_EMAIL,
  STORE_PHONE_DISPLAY,
} from "@/data/storeContact";

function MobileMenuInner() {
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
    <div className="offcanvas offcanvas-start canvas-mb" id="mobileMenu">
      <span
        className="icon-close icon-close-popup"
        data-bs-dismiss="offcanvas"
        aria-label="Close"
      />
      <div className="mb-canvas-content">
        <div className="mb-body">
          <div className="mb-content-top">
            <form className="form-search" onSubmit={(e) => e.preventDefault()}>
              <fieldset className="text">
                <input
                  type="text"
                  placeholder="무엇을 찾고 계신가요?"
                  className=""
                  name="text"
                  tabIndex={0}
                  defaultValue=""
                  aria-required="true"
                  required
                />
              </fieldset>
              <button className="" type="submit">
                <svg
                  width={24}
                  height={24}
                  viewBox="0 0 24 24"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M11 19C15.4183 19 19 15.4183 19 11C19 6.58172 15.4183 3 11 3C6.58172 3 3 6.58172 3 11C3 15.4183 6.58172 19 11 19Z"
                    stroke="#181818"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  <path
                    d="M20.9984 20.9999L16.6484 16.6499"
                    stroke="#181818"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              </button>
            </form>
            <ul className="nav-ul-mb" id="wrapper-menu-navigation">
              <li className={`nav-mb-item ${isHome ? "active" : ""}`}>
                <Link href="/" className="mb-menu-link">
                  홈
                </Link>
              </li>

              <li className={`nav-mb-item ${isShop ? "active" : ""}`}>
                <a
                  href="#mb-nav-shop"
                  className={`collapsed mb-menu-link ${isShop ? "active" : ""}`}
                  data-bs-toggle="collapse"
                  aria-expanded="false"
                  aria-controls="mb-nav-shop"
                >
                  <span>쇼핑</span>
                  <span className="btn-open-sub" />
                </a>
                <div id="mb-nav-shop" className="collapse">
                  <ul className="sub-nav-menu">
                    <li>
                      <Link
                        href={SHOP_LIST_PATH}
                        className={`sub-nav-link ${
                          isShop && !productTypeFromUrl ? "active" : ""
                        }`}
                      >
                        전체 상품
                      </Link>
                    </li>
                    {SHOP_PRODUCT_TYPES.map((t) => (
                      <li key={t.value}>
                        <Link
                          href={shopHref(t.value)}
                          className={`sub-nav-link ${
                            productTypeFromUrl === t.value ? "active" : ""
                          }`}
                        >
                          {t.label}
                        </Link>
                      </li>
                    ))}
                  </ul>
                </div>
              </li>

              <li className={`nav-mb-item ${isEvents ? "active" : ""}`}>
                <Link href="/events" className="mb-menu-link">
                  이벤트
                </Link>
              </li>

              <li className={`nav-mb-item ${isCustomerService ? "active" : ""}`}>
                <a
                  href="#mb-nav-support"
                  className={`collapsed mb-menu-link ${
                    isCustomerService ? "active" : ""
                  }`}
                  data-bs-toggle="collapse"
                  aria-expanded="false"
                  aria-controls="mb-nav-support"
                >
                  <span>고객센터</span>
                  <span className="btn-open-sub" />
                </a>
                <div id="mb-nav-support" className="collapse">
                  <ul className="sub-nav-menu">
                    {CUSTOMER_SERVICE_LINKS.map((l) => (
                      <li key={l.href}>
                        <Link
                          href={l.href}
                          className={`sub-nav-link ${
                            pathname === l.href ? "active" : ""
                          }`}
                        >
                          {l.label}
                        </Link>
                      </li>
                    ))}
                  </ul>
                </div>
              </li>

              <li className={`nav-mb-item ${isMyAccount ? "active" : ""}`}>
                <Link href="/my-account" className="mb-menu-link">
                  마이페이지
                </Link>
              </li>
            </ul>
          </div>
          <div className="mb-other-content">
            <div className="group-icon">
              <Link href={`/login`} className="site-nav-icon">
                <svg
                  className="icon"
                  width={18}
                  height={18}
                  viewBox="0 0 24 24"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <path
                    d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21"
                    stroke="#181818"
                    strokeWidth={2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  <path
                    d="M12 11C14.2091 11 16 9.20914 16 7C16 4.79086 14.2091 3 12 3C9.79086 3 8 4.79086 8 7C8 9.20914 9.79086 11 12 11Z"
                    stroke="#181818"
                    strokeWidth={2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                로그인
              </Link>
            </div>
            <div className="mb-notice">
              <Link href={`/FAQs`} className="text-need">
                도움이 필요하신가요?
              </Link>
            </div>
            <div className="mb-contact">
              <p className="text-caption-1">
                {STORE_ADDRESS}
              </p>
              <Link
                href={`/contact`}
                className="tf-btn-default text-btn-uppercase"
              >
                오시는 길
                <i className="icon-arrowUpRight" />
              </Link>
            </div>
            <ul className="mb-info">
              <li>
                <i className="icon icon-mail" />
                <p>{STORE_EMAIL}</p>
              </li>
              <li>
                <i className="icon icon-phone" />
                <p>{STORE_PHONE_DISPLAY}</p>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function MobileMenu() {
  return (
    <Suspense fallback={null}>
      <MobileMenuInner />
    </Suspense>
  );
}
