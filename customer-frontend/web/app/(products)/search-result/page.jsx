import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import SearchProducts from "@/components/products/SearchProducts";
import React from "react";

export const metadata = {
  title: "검색 결과 || Let’s Dive In",
  description: "상품 검색",
};

export default function SearchResultPage() {
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
              <h3 className="heading text-center">상품 검색</h3>
            </div>
          </div>
        </div>
      </div>
      <SearchProducts />

      <Footer1 />
    </>
  );
}
