import React from "react";
import Link from "next/link";
import { PRODUCT_TYPES, PRODUCT_SUB_TYPES } from "@/data/productTaxonomy";
import { shopHref } from "@/data/navMain";

function collapseId(value) {
  return `shopCat-${value.replace(/_/g, "-")}`;
}

export default function Categories() {
  return (
    <div
      className="offcanvas offcanvas-start canvas-filter canvas-categories"
      id="shopCategories"
    >
      <div className="canvas-wrapper">
        <div className="canvas-header">
          <span className="icon-left icon-filter" />
          <h5>카테고리</h5>
          <span
            className="icon-close icon-close-popup"
            data-bs-dismiss="offcanvas"
            aria-label="닫기"
          />
        </div>
        <div className="canvas-body">
          {PRODUCT_TYPES.map((type) => {
            const subs = PRODUCT_SUB_TYPES[type.value] || [];
            const hasSubs = subs.length > 0;
            const cid = collapseId(type.value);

            return (
              <div className="wd-facet-categories" key={type.value}>
                <div
                  role="button"
                  className="facet-title collapsed"
                  data-bs-target={`#${cid}`}
                  data-bs-toggle="collapse"
                  aria-expanded="false"
                  aria-controls={cid}
                >
                  <span className="title">{type.label}</span>
                  <span className="icon icon-arrow-down" />
                </div>
                <div id={cid} className="collapse">
                  <ul className="facet-body">
                    {hasSubs ? (
                      <>
                        <li>
                          <Link
                            href={shopHref(type.value)}
                            className="item link"
                            data-bs-dismiss="offcanvas"
                            data-bs-target="#shopCategories"
                          >
                            <span className="title-sub text-caption-1 text-secondary">
                              {type.label} 전체
                            </span>
                          </Link>
                        </li>
                        {subs.map((sub) => (
                          <li key={sub.value}>
                            <Link
                              href={shopHref(type.value, sub.value)}
                              className="item link"
                              data-bs-dismiss="offcanvas"
                              data-bs-target="#shopCategories"
                            >
                              <span className="title-sub text-caption-1 text-secondary">
                                {sub.label}
                              </span>
                            </Link>
                          </li>
                        ))}
                      </>
                    ) : (
                      <li>
                        <Link
                          href={shopHref(type.value)}
                          className="item link"
                          data-bs-dismiss="offcanvas"
                          data-bs-target="#shopCategories"
                        >
                          <span className="title-sub text-caption-1 text-secondary">
                            {type.label} 보기
                          </span>
                        </Link>
                      </li>
                    )}
                  </ul>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
