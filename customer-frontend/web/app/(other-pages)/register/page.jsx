import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Register from "@/components/otherPages/Register";
import Link from "next/link";
import React from "react";

export const metadata = {
  title: "회원가입 || Swim Mall",
  description: "스윔몰 회원가입",
};

export default function RegisterPage() {
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
              <h3 className="heading text-center">회원가입</h3>
              <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                <li>
                  <Link className="link" href={`/`}>
                    홈
                  </Link>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>회원가입</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <Register />
      <Footer1 />
    </>
  );
}
