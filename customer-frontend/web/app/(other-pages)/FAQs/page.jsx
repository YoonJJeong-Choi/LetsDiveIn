import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Faqs from "@/components/otherPages/Faqs";
import React from "react";
import Link from "next/link";
export const metadata = {
  title: "자주 묻는 질문 || Let’s Dive In",
  description: "자주 묻는 질문",
};

export default function FAQSPage() {
  return (
    <>
      <Header1 />
      <div
        className="page-title"
        style={{ backgroundImage: "url(/images/section/page-title.jpg)" }}
      >
        <div className="container-full">
          <div className="row">
            <div className="col-12">
              <h3 className="heading text-center">FAQs</h3>
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
                <li>FAQs</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <Faqs />
      <Footer1 />
    </>
  );
}
