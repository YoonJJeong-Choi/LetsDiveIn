import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Contact2 from "@/components/otherPages/Contact2";
import { STORE_MAP_EMBED_URL } from "@/data/storeContact";
import Link from "next/link";
import React from "react";

export const metadata = {
  title: "찾아오시는 길 || Let’s Dive In",
  description: "Let’s Dive In 위치·연락처·문의",
};

export default function ContactPage() {
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
              <h3 className="heading text-center">찾아오시는 길</h3>
              <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                <li>
                  <Link className="link" href="/">
                    홈
                  </Link>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>찾아오시는 길</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <iframe
        title="지도"
        src={STORE_MAP_EMBED_URL}
        width={600}
        height={450}
        style={{ border: 0, width: "100%" }}
        allowFullScreen=""
        loading="lazy"
        referrerPolicy="no-referrer-when-downgrade"
      />
      <Contact2 />
      <Footer1 />
    </>
  );
}
