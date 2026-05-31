import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import AccountSidebar from "@/components/my-account/AccountSidebar";
import ReviewsList from "@/components/my-account/ReviewsList";
import Link from "next/link";
import React from "react";

export const metadata = {
  title: "내 리뷰 || Let’s Dive In",
  description: "내 리뷰 관리",
};

export default function MyReviewsPage() {
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
                <h3 className="heading text-center">내 리뷰</h3>
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
                  <li>내 리뷰</li>
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
            <ReviewsList />
          </div>
        </div>
      </section>
      <Footer1 />
    </>
  );
}
