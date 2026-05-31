"use client";
import React from "react";
import Link from "next/link";
import { SHOP_LIST_PATH } from "@/data/navMain";

export default function Breadcumb({ product }) {
  return (
    <div className="tf-breadcrumb">
      <div className="container">
        <div className="tf-breadcrumb-wrap">
          <div className="tf-breadcrumb-list">
            <Link href="/" className="text text-caption-1">
              홈
            </Link>
            <i className="icon icon-arrRight" />
            <Link href={SHOP_LIST_PATH} className="text text-caption-1">
              상품
            </Link>
            <i className="icon icon-arrRight" />
            <span className="text text-caption-1">{product?.title}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
