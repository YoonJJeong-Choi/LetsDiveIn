import React from "react";
import ProductCard1 from "../productCards/ProductCard1";

export default function GridView({ products }) {
  return (
    <>
      {products.map((product, index) => (
        <ProductCard1 key={index} product={product} gridClass="grid" />
      ))}
    </>
  );
}
