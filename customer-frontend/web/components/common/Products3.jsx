"use client";

import ProductCard1 from "@/components/productCards/ProductCard1";
import { getActiveProductList } from "@/lib/api/product";
import { mapApiProductToCard } from "@/lib/product/mapApiProductToCard";
import Link from "next/link";
import React, { useCallback, useEffect, useState } from "react";

/** 홈 탭 ↔ 백엔드 /api/product/list/active 규칙 */
const HOME_PRODUCT_TABS = [
  {
    id: "new",
    label: "신상",
    fetchParams: {
      sortBy: "latest",
      sortDir: "desc",
      saleOnly: false,
    },
  },
  {
    id: "best",
    label: "베스트",
    fetchParams: {
      sortBy: "popularity",
      sortDir: "desc",
      saleOnly: false,
    },
  },
  {
    id: "sale",
    label: "세일",
    fetchParams: {
      sortBy: "latest",
      sortDir: "desc",
      saleOnly: true,
    },
  },
];

const PAGE_SIZE = 8;

export default function Products3({ parentClass = "flat-spacing-3" }) {
  const [activeId, setActiveId] = useState(HOME_PRODUCT_TABS[0].id);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async (tabId) => {
    const tab = HOME_PRODUCT_TABS.find((t) => t.id === tabId) ?? HOME_PRODUCT_TABS[0];
    setLoading(true);
    setError(null);
    try {
      const raw = await getActiveProductList({
        page: 1,
        size: PAGE_SIZE,
        ...tab.fetchParams,
      });
      const body = raw?.items != null ? raw : raw?.data ?? raw;
      const list = Array.isArray(body?.items)
        ? body.items
        : Array.isArray(raw)
          ? raw
          : [];
      const mapped = await Promise.all(list.map(mapApiProductToCard));
      setItems(mapped);
    } catch (e) {
      if (process.env.NODE_ENV === "development") {
        console.warn("[home products]", e?.message || e);
      }
      setError(e?.message || "상품을 불러오지 못했습니다.");
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(activeId);
  }, [activeId, load]);

  useEffect(() => {
    const el = document.getElementById("homeProductsTab");
    if (!el) return;
    el.classList.remove("filtered");
    const t = setTimeout(() => el.classList.add("filtered"), 50);
    return () => clearTimeout(t);
  }, [activeId, items]);

  return (
    <section className={parentClass}>
      <div className="container">
        <div className="flat-animate-tab">
          <ul className="tab-product justify-content-sm-center" role="tablist">
            {HOME_PRODUCT_TABS.map((tab) => (
              <li key={tab.id} className="nav-tab-item">
                <a
                  href="#"
                  className={activeId === tab.id ? "active" : ""}
                  onClick={(e) => {
                    e.preventDefault();
                    setActiveId(tab.id);
                  }}
                >
                  {tab.label}
                </a>
              </li>
            ))}
          </ul>
          <div className="tab-content">
            <div
              className="tab-pane active show tabFilter filtered"
              id="homeProductsTab"
              role="tabpanel"
            >
              {loading && (
                <div className="text-center py-5 d-flex justify-content-center align-items-center">
                  <div
                    className="spinner-border text-primary"
                    role="status"
                    aria-label="상품 불러오는 중"
                  >
                    <span className="visually-hidden">불러오는 중...</span>
                  </div>
                </div>
              )}
              {!loading && error && (
                <p className="text-center text-danger py-5 mb-0">{error}</p>
              )}
              {!loading && !error && items.length === 0 && (
                <p className="text-center text-secondary py-5 mb-0">
                  표시할 상품이 없습니다.
                </p>
              )}
              {!loading && items.length > 0 && (
                <div className="tf-grid-layout tf-col-2 lg-col-3 xl-col-4">
                  {items.map((product) => (
                    <ProductCard1
                      key={product.productNo ?? product.id}
                      product={product}
                    />
                  ))}
                </div>
              )}
              <div className="sec-btn text-center">
                <Link href="/shop-default-grid" className="btn-line">
                  전체 상품 보기
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
