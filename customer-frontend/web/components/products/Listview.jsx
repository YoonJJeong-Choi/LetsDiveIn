import React from "react";
import ProductsCards6 from "../productCards/ProductsCards6";

export default function Listview({ products }) {
  return (
    <>
      {products.map((product, i) => (
        <ProductsCards6 product={product} key={i} />
      ))}
    </>
  );
}
