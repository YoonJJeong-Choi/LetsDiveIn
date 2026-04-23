import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import ShopPageHeading from "@/components/products/ShopPageHeading";
import Products12 from "@/components/products/Products12";
import React, { Suspense } from "react";

export const metadata = {
  title: "쇼핑 || Swim Mall",
  description: "상품 목록",
};

export default function ShopDefaultGridPage() {
  return (
    <>
      <Header1 />
      <div
        className="page-title"
        style={{ backgroundImage: "url(/images/section/page-title.jpg)" }}
      >
        <ShopPageHeading />
      </div>
      <Suspense fallback={null}>
        <Products12 />
      </Suspense>
      <Footer1 />
    </>
  );
}
