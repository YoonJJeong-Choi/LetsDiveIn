import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Login from "@/components/otherPages/Login";
import Link from "next/link";
import React from "react";

export const metadata = {
  title: "로그인 || Swim Mall",
  description: "스윔몰 로그인",
};

export default function LoginPage() {
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
              <h3 className="heading text-center">로그인</h3>
              <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                <li>
                  <Link className="link" href={`/`}>
                    홈
                  </Link>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>로그인</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <Login />
      <Footer1 />
    </>
  );
}
