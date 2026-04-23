import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Topbar6 from "@/components/headers/Topbar6";
import OrderConfirmation from "@/components/otherPages/OrderConfirmation";
import Link from "next/link";
import React from "react";

// order-confirmation은 query(orderNo)에 의존하므로 정적 프리렌더를 막습니다.
export const dynamic = "force-dynamic";

export const metadata = {
  title: "주문 완료 || Modave - Multipurpose React Nextjs eCommerce Template",
  description: "Modave - Multipurpose React Nextjs eCommerce Template",
};

export default function OrderConfirmationPage() {
  return (
    <>
      <Topbar6 bgColor="bg-main" />
      <Header1 />
      <div
        className="page-title"
        style={{ backgroundImage: "url(/images/section/page-title.jpg)" }}
      >
        <div className="container-full">
          <div className="row">
            <div className="col-12">
              <h3 className="heading text-center">주문 완료</h3>
              <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                <li>
                  <Link className="link" href={`/`}>
                    Homepage
                  </Link>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>주문 완료</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <OrderConfirmation />
      <Footer1 />
    </>
  );
}
