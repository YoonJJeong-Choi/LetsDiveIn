import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Topbar6 from "@/components/headers/Topbar6";
import AccountSidebar from "@/components/my-account/AccountSidebar";
import Link from "next/link";
import ReturnsList from "@/components/my-account/ReturnsList";
import React, { Suspense } from "react";

export const metadata = {
  title:
    "My Account Returns || Modave - Multipurpose React Nextjs eCommerce Template",
  description: "Modave - Multipurpose React Nextjs eCommerce Template",
};

export default function MyAccountReturnsPage() {
  return (
    <>
      <Topbar6 bgColor="bg-main" />
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
                <h3 className="heading text-center">My Account</h3>
                <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                  <li>
                    <Link className="link" href={`/`}>
                      Homepage
                    </Link>
                  </li>
                  <li>
                    <i className="icon-arrRight" />
                  </li>
                  <li>
                    <a className="link" href="#">
                      Pages
                    </a>
                  </li>
                  <li>
                    <i className="icon-arrRight" />
                  </li>
                  <li>My Account</li>
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
            <Suspense fallback={<div className="text-center p-4">로딩 중...</div>}>
              <ReturnsList />
            </Suspense>
          </div>
        </div>
      </section>
      <Footer1 />
    </>
  );
}
