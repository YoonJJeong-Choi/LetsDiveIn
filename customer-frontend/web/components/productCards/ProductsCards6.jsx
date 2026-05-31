"use client";
import React, { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { formatKrw } from "@/lib/price/formatKrw";
import { DEFAULT_PRODUCT_PLACEHOLDER } from "@/lib/media/productImage";
import CountdownTimer from "../common/Countdown";

import { useContextElement } from "@/context/Context";
export default function ProductsCards6({ product }) {
  const [currentImage, setCurrentImage] = useState(product.imgSrc || DEFAULT_PRODUCT_PLACEHOLDER);
  const router = useRouter();

  const {
    setQuickAddItem,
    setQuickViewItem,
    addProductToCart,
    isAddedToCartProducts,
  } = useContextElement();

  useEffect(() => {
    setCurrentImage(product.imgSrc || DEFAULT_PRODUCT_PLACEHOLDER);
  }, [product]);

  const goToDetail = () => {
    router.push(`/product-detail/${product.id}`);
  };
  return (
    <div
      className="card-product style-list"
      data-availability="In stock"
      data-brand="gucci"
    >
      <div className="card-product-wrapper">
        <Link
          href={`/product-detail/${product.id}`}
          className="product-img"
          onClick={(e) => {
            e.preventDefault();
            goToDetail();
          }}
        >
          <Image
            className="lazyload img-product"
            src={currentImage}
            alt={product.title}
            width={600}
            height={800}
            onClick={goToDetail}
          />
          <Image
            className="lazyload img-hover"
            src={product.imgHover || DEFAULT_PRODUCT_PLACEHOLDER}
            alt={product.title}
            width={600}
            height={800}
            onClick={goToDetail}
          />
        </Link>
        {product.isOnSale && (
          <div className="on-sale-wrap">
            <span className="on-sale-item">-25%</span>
          </div>
        )}
      </div>
      <div className="card-product-info">
        {product.brandName && (
          <Link
            href={`/search-result?brand=${encodeURIComponent(product.brandName)}`}
            className="text-secondary"
            style={{ fontSize: "12px", marginBottom: "2px", display: "block", textDecoration: "none" }}
          >
            {product.brandName}
          </Link>
        )}
        <Link href={`/product-detail/${product.id}`} className="title link">
          {product.title}
        </Link>
        <span className="price current-price">
          {product.oldPrice && (
            <span className="old-price">{formatKrw(product.oldPrice)}</span>
          )}{" "}
          {formatKrw(product.price)}
        </span>
        <p className="description text-secondary text-line-clamp-2">
          The garments labelled as Committed are products that have been
          produced using sustainable fibres or processes, reducing their
          environmental impact.
        </p>
        <div className="variant-wrap-list">
          {product.colors && (
            <ul className="list-color-product">
              {product.colors.map((color, index) => (
                <li
                  key={index}
                  className={`list-color-item color-swatch ${
                    currentImage == color.imgSrc ? "active" : ""
                  } `}
                  onMouseOver={() => setCurrentImage(color.imgSrc)}
                >
                  <span className={`swatch-value ${color.bgColor}`} />
                  <Image
                    className="lazyload"
                    src={color.imgSrc || DEFAULT_PRODUCT_PLACEHOLDER}
                    alt="color variant"
                    width={600}
                    height={800}
                  />
                </li>
              ))}
            </ul>
          )}
          {product.sizes && (
            <div className="size-box list-product-btn">
              <span className="size-item box-icon">S</span>
              <span className="size-item box-icon">M</span>
              <span className="size-item box-icon">L</span>
              <span className="size-item box-icon">XL</span>
              <span className="size-item box-icon disable">XXL</span>
            </div>
          )}
          <div className="list-product-btn">
            <a
              onClick={() => addProductToCart(product.id, 1, null, product)}
              className="btn-main-product"
            >
              {isAddedToCartProducts(product.id)
                ? "Already Added"
                : "Add To cart"}
            </a>
            <a
              href="#quickView"
              onClick={() => setQuickViewItem(product)}
              data-bs-toggle="modal"
              className="box-icon quickview tf-btn-loading"
            >
              <span className="icon icon-eye" />
              <span className="tooltip">Quick View</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
