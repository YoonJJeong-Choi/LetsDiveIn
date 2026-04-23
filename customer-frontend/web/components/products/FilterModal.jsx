"use client";

import { colors } from "@/data/productFilterOptions";
import { PRODUCT_TYPES, PRODUCT_SUB_TYPES } from "@/data/productTaxonomy";

import RangeSlider from "react-range-slider-input";

export default function FilterModal({ allProps }) {
  const selectedProductType = allProps.selectedProductType || null;
  const selectedProductSubType = allProps.selectedProductSubType || null;
  const expandedProductType = allProps.expandedProductType || null;

  return (
    <div className="offcanvas offcanvas-start canvas-filter" id="filterShop">
      <div className="canvas-wrapper">
        <div className="canvas-header">
          <h5>필터</h5>
          <span
            className="icon-close icon-close-popup"
            data-bs-dismiss="offcanvas"
            aria-label="닫기"
          />
        </div>
        <div className="canvas-body">
          {/* 카테고리 필터 (대분류 + 소분류) */}
          <div className="widget-facet facet-categories">
            <h6 className="facet-title">카테고리</h6>
            <ul className="facet-content">
              <li>
                <a
                  href="#"
                  className={`categories-item ${!selectedProductType ? "active" : ""}`}
                  onClick={(e) => {
                    e.preventDefault();
                    allProps.setProductType(null);
                  }}
                >
                  전체
                </a>
              </li>
              {PRODUCT_TYPES.map((type) => {
                const isExpanded = expandedProductType === type.value; // 펼쳐짐 상태
                const typeSubTypes = PRODUCT_SUB_TYPES[type.value] || [];
                const hasSubTypes = typeSubTypes.length > 0;
                // 대분류가 필터링에 적용되었는지 확인 (소분류가 선택되지 않았을 때만)
                const isFiltered = selectedProductType === type.value && !selectedProductSubType;

                return (
                  <li key={type.value} className={hasSubTypes ? "has-subcategories" : ""}>
                    <a
                      href="#"
                      className={`categories-item ${isFiltered ? "active" : ""}`}
                      onClick={(e) => {
                        e.preventDefault();
                        // 대분류 클릭: 소분류만 펼치기/접기 (필터링은 하지 않음)
                        // 소분류가 있으면 펼치기만, 없으면 바로 필터링
                        if (hasSubTypes) {
                          // 소분류가 있으면 펼치기/접기만 (필터링 없음)
                          allProps.setProductType(type.value);
                        } else {
                          // 소분류가 없으면 바로 필터링
                          allProps.setProductTypeFilter(isFiltered ? null : type.value);
                        }
                      }}
                    >
                      {type.label}
                      {hasSubTypes && (
                        <span className="icon-arrow" style={{ 
                          float: 'right',
                          transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
                          transition: 'transform 0.2s'
                        }}>
                          ▶
                        </span>
                      )}
                    </a>
                    {/* 소분류 목록 (펼쳐진 대분류일 때만 표시) */}
                    {isExpanded && hasSubTypes && (
                      <ul className="subcategories-list" style={{
                        marginTop: '8px',
                        marginLeft: '16px',
                        paddingLeft: '0',
                        listStyle: 'none'
                      }}>
                        <li>
                          <a
                            href="#"
                            className={`categories-item subcategory-item ${selectedProductType === type.value && !selectedProductSubType ? "active" : ""}`}
                            onClick={(e) => {
                              e.preventDefault();
                              // "전체" 클릭: 대분류로 필터링 적용
                              allProps.setProductTypeFilter(type.value);
                            }}
                            style={{
                              fontSize: '0.9em',
                              color: selectedProductType === type.value && !selectedProductSubType ? '#1890ff' : 'inherit'
                            }}
                          >
                            전체
                          </a>
                        </li>
                        {typeSubTypes.map((subType) => (
                          <li key={subType.value}>
                            <a
                              href="#"
                              className={`categories-item subcategory-item ${selectedProductSubType === subType.value ? "active" : ""}`}
                              onClick={(e) => {
                                e.preventDefault();
                                // 소분류 클릭: 필터링 적용
                                allProps.setProductSubType(subType.value, type.value);
                              }}
                              style={{
                                fontSize: '0.9em',
                                color: selectedProductSubType === subType.value ? '#1890ff' : 'inherit'
                              }}
                            >
                              {subType.label}
                            </a>
                          </li>
                        ))}
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
              onInput={(value) => {
                // UI만 업데이트 (API 호출 없음)
                allProps.setPrice(value);
              }}
            />
            <div className="box-price-product mt-3">
              <div className="box-price-item">
                <span className="title-price">최저</span>
                <input
                  type="number"
                  className="form-control price-input"
                  value={allProps.price[0] || ''}
                  onChange={(e) => {
                    const minValue = parseInt(e.target.value) || 0;
                    const maxValue = allProps.price[1] || 0;
                    if (minValue <= maxValue) {
                      allProps.setPrice([minValue, maxValue]);
                    }
                  }}
                  placeholder="최소 가격"
                  style={{
                    width: '100%',
                    padding: '8px',
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    marginTop: '4px'
                  }}
                />
              </div>
              <div className="box-price-item">
                <span className="title-price">최고</span>
                <input
                  type="number"
                  className="form-control price-input"
                  value={allProps.price[1] || ''}
                  onChange={(e) => {
                    const maxValue = parseInt(e.target.value) || 0;
                    const minValue = allProps.price[0] || 0;
                    if (maxValue >= minValue) {
                      allProps.setPrice([minValue, maxValue]);
                    }
                  }}
                  placeholder="최대 가격"
                  style={{
                    width: '100%',
                    padding: '8px',
                    border: '1px solid #ddd',
                    borderRadius: '4px',
                    marginTop: '4px'
                  }}
                />
              </div>
            </div>
            <button
              type="button"
              onClick={allProps.applyPriceFilter}
              className="btn btn-primary mt-3"
              style={{
                width: '100%',
                padding: '10px',
                fontSize: '14px',
                fontWeight: '500'
              }}
            >
              가격 필터 적용
            </button>
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
              {colors.map((color, index) => (
                <div
                  onClick={() => allProps.setColor(color)}
                  key={index}
                  className={`color-item color-check ${
                    color == allProps.color ? "active" : ""
                  }`}
                >
                  <span className={`color ${color.className}`} />
                  {color.name}
                </div>
              ))}
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
                  <label>
                    {brand.displayName}
                  </label>
                </fieldset>
              ))}
            </div>
          </div>
        </div>
        <div className="canvas-bottom">
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
