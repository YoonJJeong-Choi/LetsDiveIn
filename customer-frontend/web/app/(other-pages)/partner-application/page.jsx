import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Topbar6 from "@/components/headers/Topbar6";
import PartnerApplication from "@/components/otherPages/PartnerApplication";
import Link from "next/link";
import React from "react";

export const metadata = {
  title: "파트너 입점 신청 || Swim Mall",
  description: "Swim Mall 파트너 입점 신청 페이지",
};

export default function PartnerApplicationPage() {
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
              <h3 className="heading text-center">파트너 입점 신청</h3>
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
                <li>파트너 입점 신청</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <PartnerApplication />
      <Footer1 />
    </>
  );
}
