import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Checkout from "@/components/otherPages/Checkout";
import Link from "next/link";
import React, { Suspense } from "react";

export const metadata = {
  title: "주문 / 결제 || Let’s Dive In",
  description: "주문 및 결제",
};

function CheckoutContent() {
  return <Checkout />;
}

export default function CheckoutPage() {
  return (
    <>
      <Header1 />
      <div
        className="page-title"
        style={{ backgroundImage: "url(/images/section/page-title.jpg)" }}
      >
        <div className="container">
          <h3 className="heading text-center">주문 / 결제</h3>
          <ul className="breadcrumbs d-flex align-items-center justify-content-center">
            <li>
              <Link className="link" href={`/`}>
                홈
              </Link>
            </li>
            <li>
              <i className="icon-arrRight" />
            </li>
            <li>
              <Link className="link" href={`/shop-default-grid`}>
                쇼핑
              </Link>
            </li>
            <li>
              <i className="icon-arrRight" />
            </li>
            <li>장바구니</li>
          </ul>
        </div>
      </div>
      <Suspense fallback={<div>불러오는 중…</div>}>
        <CheckoutContent />
      </Suspense>
      <Footer1 />
    </>
  );
}
