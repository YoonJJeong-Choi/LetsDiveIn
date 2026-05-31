"use client";

import { useEffect, useState } from "react";
import { getActiveColors } from "@/lib/api/color";
import { PRODUCT_TYPES, PRODUCT_SUB_TYPES } from "@/data/productTaxonomy";

import RangeSlider from "react-range-slider-input";
import { formatKrw } from '@/lib/price/formatKrw';

export default function FilterSidebar({ allProps }) {
  const selectedProductType = allProps.selectedProductType || null;
  const selectedProductSubType = allProps.selectedProductSubType || null;
  const expandedProductType = allProps.expandedProductType || null;
  const subTypes = expandedProductType
    ? PRODUCT_SUB_TYPES[expandedProductType] || []
    : [];
  const [colorList, setColorList] = useState([]);

  useEffect(() => {
    let abort = false;
    (async () => {
      try {
        const resp = await getActiveColors();
        const arr = Array.isArray(resp?.data) ? resp.data : [];
        if (!abort) setColorList(arr);
      } catch (e) {
        if (!abort) setColorList([]);
      }
    })();
    return () => {
      abort = true;
    };
  }, []);

  const codeToHex = (code) => {
    const map = {
      BLACK: "#000000",
      WHITE: "#ffffff",
      RED: "#ff3b30",
      BLUE: "#007aff",
      NAVY: "#001f3f",
      GREEN: "#34c759",
      YELLOW: "#ffcc00",
      PINK: "#ff2d55",
      ORANGE: "#ff9500",
      GRAY: "#8e8e93",
    };
    return map[code?.toUpperCase?.()] || "#cccccc";
  };

  return (
    <div className="sidebar-filter canvas-filter left">
      <div className="canvas-wrapper">
        <div className="canvas-header d-flex d-xl-none">
          <h5>필터</h5>
          <span className="icon-close close-filter" />
        </div>
        <div className="canvas-body">
          <div className="widget-facet facet-categories">
            <h6 className="facet-title">카테고리</h6>
            <ul className="facet-content">
              <li>
                <a
                  href="#"
                  className={`categories-item ${!selectedProductType ? "active" : ""}`}
                  onClick={(e) => {
                    e.preventDefault();
                    if (allProps.setProductTypeFilter) {
                      allProps.setProductTypeFilter(null);
                    }
                  }}
                >
                  전체
                </a>
              </li>
              {PRODUCT_TYPES.map((type) => {
                const isExpanded = expandedProductType === type.value;
                const typeSubTypes = PRODUCT_SUB_TYPES[type.value] || [];
                const hasSubTypes = typeSubTypes.length > 0;
                const isFiltered =
                  selectedProductType === type.value && !selectedProductSubType;

                return (
                  <li key={type.value} className={hasSubTypes ? "has-subcategories" : ""}>
                    <a
                      href="#"
                      className={`categories-item ${isFiltered ? "active" : ""}`}
                      onClick={(e) => {
                        e.preventDefault();
                        if (hasSubTypes) {
                          if (allProps.setProductType) {
                            allProps.setProductType(type.value);
                          }
                        } else {
                          if (allProps.setProductTypeFilter) {
                            allProps.setProductTypeFilter(
                              isFiltered ? null : type.value
                            );
                          }
                        }
                      }}
                    >
                      {type.label}
                      {hasSubTypes && <span className="icon-arrow-down" />}
                    </a>
                    {isExpanded && hasSubTypes && (
                      <ul className="subcategories-list">
                        <li>
                          <a
                            href="#"
                            className={`categories-item subcategory-item ${selectedProductType === type.value && !selectedProductSubType ? "active" : ""}`}
                            onClick={(e) => {
                              e.preventDefault();
                              if (allProps.setProductTypeFilter) {
                                allProps.setProductTypeFilter(type.value);
                              }
                            }}
                          >
                            전체
                          </a>
                        </li>
                        {typeSubTypes.map((subType) => {
                          const isSubFiltered =
                            selectedProductType === type.value &&
                            selectedProductSubType === subType.value;
                          return (
                            <li key={subType.value}>
                              <a
                                href="#"
                                className={`categories-item subcategory-item ${isSubFiltered ? "active" : ""}`}
                                onClick={(e) => {
                                  e.preventDefault();
                                  if (allProps.setProductSubType) {
                                    allProps.setProductSubType(
                                      subType.value,
                                      type.value
                                    );
                                  }
                                }}
                              >
                                {subType.label}
                              </a>
                            </li>
                          );
                        })}
                      </ul>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>

          <div className="widget-facet facet-price">
            <h6 className="facet-title">가격</h6>
            <RangeSlider
              min={0}
              max={1000000}
              value={allProps.price}
              onInput={(value) => allProps.setPrice(value)}
            />
            <div className="box-price-product mt-3">
              <div className="box-price-item">
                <span className="title-price">최저</span>
                <div
                  className="price-val"
                  id="price-min-value"
                  data-currency="₩"
                >
                  {formatKrw(allProps.price?.[0] || 0)}
                </div>
              </div>
              <div className="box-price-item">
                <span className="title-price">최고</span>
                <div
                  className="price-val"
                  id="price-max-value"
                  data-currency="₩"
                >
                  {formatKrw(allProps.price?.[1] || 1000000)}
                </div>
              </div>
            </div>
          </div>

          <div className="widget-facet facet-size">
            <h6 className="facet-title">사이즈</h6>
            <div className="facet-size-box size-box">
              {(allProps.sizeOptions || []).map((size, index) => (
                <span
                  key={index}
                  onClick={() => allProps.setSize(size.code)}
                  className={`size-item size-check ${
                    allProps.size === size.code ? "active" : ""
                  }`}
                >
                  {size.label}
                </span>
              ))}
              <span
                className={`size-item size-check free-size ${
                  allProps.size == "Free Size" ? "active" : ""
                } `}
                onClick={() => allProps.setSize("Free Size")}
              >
                프리사이즈
              </span>
            </div>
          </div>

          <div className="widget-facet facet-color">
            <h6 className="facet-title">색상</h6>
            <div className="facet-color-box">
              {colorList.map((c) => {
                const selected = allProps.color?.name
                  ? allProps.color.name === c.label
                  : false;
                return (
                  <div
                    key={c.code}
                    onClick={() => allProps.setColor({ name: c.label })}
                    className={`color-item color-check ${selected ? "active" : ""}`}
                  >
                    <span
                      className="color"
                      style={{
                        backgroundColor: c.hex || codeToHex(c.code),
                        border: "1px solid #ddd",
                      }}
                    />
                    {c.label}
                  </div>
                );
              })}
            </div>
          </div>

          <div className="widget-facet facet-fieldset">
            <h6 className="facet-title">브랜드</h6>
            <div className="box-fieldset-item">
              {(allProps.brandOptions || []).map((brand, index) => (
                <fieldset
                  key={index}
                  className="fieldset-item"
                  onClick={() => allProps.setBrands(brand.code)}
                >
                  <input
                    type="checkbox"
                    name="brand"
                    className="tf-check"
                    readOnly
                    checked={allProps.brands.includes(brand.code)}
                  />
                  <label>{brand.displayName}</label>
                </fieldset>
              ))}
            </div>
          </div>
        </div>
        <div className="canvas-bottom d-block d-xl-none">
          <button
            id="reset-filter"
            onClick={allProps.clearFilter}
            className="tf-btn btn-reset"
          >
            필터 초기화
          </button>
        </div>
      </div>
    </div>
  );
}
