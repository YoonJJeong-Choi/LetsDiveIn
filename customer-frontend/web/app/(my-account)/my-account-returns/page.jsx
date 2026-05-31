import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import AccountSidebar from "@/components/my-account/AccountSidebar";
import Link from "next/link";
import ReturnsList from "@/components/my-account/ReturnsList";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";
import React, { Suspense } from "react";

export const metadata = {
  title: "반품 내역 || Let’s Dive In",
  description: "내 반품 내역 조회",
};

export default function MyAccountReturnsPage() {
  return (
    <>
      <Header1 />
      <>
        {/* page-title */}
        <div
          className="page-title"
          style={{ backgroundImage: "url(/images/section/page-title.jpg)" }}
        >
          <div className="container-full">
            <div className="row">
              <div className="col-12">
                <h3 className="heading text-center">마이페이지</h3>
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
                    <a className="link" href="#">
                      마이페이지
                    </a>
                  </li>
                  <li>
                    <i className="icon-arrRight" />
                  </li>
                  <li>반품 내역</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
        {/* /page-title */}
      </>

      <section className="flat-spacing">
        <div className="container">
          <div className="my-account-wrap">
            <AccountSidebar />
            <Suspense
              fallback={
                <div className="p-4 d-flex justify-content-center">
                  <InlineTemplateLoader />
                </div>
              }
            >
              <ReturnsList />
            </Suspense>
          </div>
        </div>
      </section>
      <Footer1 />
    </>
  );
}
