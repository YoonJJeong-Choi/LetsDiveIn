import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import AccountSidebar from "@/components/my-account/AccountSidebar";
import Points from "@/components/my-account/Points";
import Link from "next/link";
import React from "react";

export const metadata = {
  title: "포인트 || Let’s Dive In",
  description: "포인트 조회 및 내역",
};

export default function MyAccountPointsPage() {
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
                  <li>포인트</li>
                </ul>
              </div>
            </div>
          </div>
        </div>
        {/* /page-title */}
        <div className="btn-sidebar-account">
          <button data-bs-toggle="offcanvas" data-bs-target="#mbAccount">
            <i className="icon icon-squares-four" />
          </button>
        </div>
      </>

      <section className="flat-spacing">
        <div className="container">
          <div className="my-account-wrap">
            <AccountSidebar />
            <Points />
          </div>
        </div>
      </section>
      <Footer1 />
    </>
  );
}
