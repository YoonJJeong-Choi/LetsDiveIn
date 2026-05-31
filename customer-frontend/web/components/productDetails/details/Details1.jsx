"use client";
import React, { useEffect, useState } from "react";
import Slider1 from "../sliders/Slider1";
import ColorSizeSelect from "../ColorSizeSelect";
import QuantitySelect from "../QuantitySelect";
import Link from "next/link";
import { useContextElement } from "@/context/Context";
import { getApplicableSale } from "@/lib/api/sale";
import { formatKrw } from "@/lib/price/formatKrw";
import { getReviewsByProduct } from "@/lib/api/review";
import { incrementProductView } from "@/lib/api/product";

const PRODUCT_TYPE_LABELS = {
  SWIMSUIT_MEN: "남성 수영복",
  SWIMSUIT_WOMEN: "여성 수영복",
  SWIMSUIT_KIDS: "아동 수영복",
  SWIM_CAP: "수모",
  SWIM_GOGGLES: "수경",
  FINS: "오리발",
  SWIM_TOY: "수영용품",
  ETC: "기타",
};

const PRODUCT_SUB_TYPE_LABELS = {
  NONE: "",
  ONE_PIECE: "원피스",
  BIKINI: "비키니",
  MONOKINI: "모노키니",
  RASH_GUARD: "래쉬가드",
  TRUNKS: "트렁크",
  JAMMER: "잠머",
  BRIEF: "브리프",
  CAP_SILICONE: "실리콘 수모",
  CAP_FABRIC: "천 수모",
  FINS_SHORT: "숏핀",
  FINS_LONG: "롱핀",
};
export default function Details1({ product }) {
  const [selectedOptionNo, setSelectedOptionNo] = useState(null);
  const [selectedColor, setSelectedColor] = useState(null);
  const [selectedSize, setSelectedSize] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [currentPrice, setCurrentPrice] = useState(product?.price || 0);
  const [displayPrice, setDisplayPrice] = useState(product?.price || 0);
  const [displayOldPrice, setDisplayOldPrice] = useState(null);
  const [activeColor, setActiveColor] = useState("gray"); // Slider1에서 사용
  const [stockQuantity, setStockQuantity] = useState(null); // 재고 수량
  const [inStock, setInStock] = useState(null); // 재고 있음 여부
  const [reviewCount, setReviewCount] = useState(0);
  const [liveViewCount, setLiveViewCount] = useState(0);
  const {
    addProductToCart,
    isAddedToCartProducts,
    cartProducts,
    updateQuantity,
  } = useContextElement();

  // 상품 전환 시 이전 상품의 옵션/수량 선택 상태를 초기화합니다.
  useEffect(() => {
    setSelectedOptionNo(null);
    setSelectedColor(null);
    setSelectedSize(null);
    setQuantity(1);
    setCurrentPrice(product?.price || 0);
    setDisplayPrice(product?.price || 0);
    setDisplayOldPrice(null);
    setStockQuantity(null);
    setInStock(null);
  }, [product?.id, product?.productNo]);

  // 옵션 선택 시 가격 및 재고 정보 업데이트
  useEffect(() => {
    if (selectedOptionNo && product?.options) {
      const selectedOption = product.options.find(opt => opt.optionNo === selectedOptionNo);
      if (selectedOption) {
        setCurrentPrice(selectedOption.totalPrice);
        // 재고 정보 업데이트
        // stockQuantity가 undefined가 아니면 그 값을 사용 (0 포함)
        // undefined이면 null로 설정 (재고 정보 없음)
        const stockQty = selectedOption.stockQuantity !== undefined ? selectedOption.stockQuantity : null;
        const stockStatus = selectedOption.inStock !== undefined ? selectedOption.inStock : null;
        setStockQuantity(stockQty);
        setInStock(stockStatus);
        // 재고가 부족하면 수량 조정
        if (stockQty !== null && stockQty !== undefined && stockQty > 0) {
          if (quantity > stockQty) {
            setQuantity(stockQty);
          }
        } else if (stockQty === 0) {
          // 재고가 0개이면 수량을 0으로 설정
          setQuantity(0);
        }
      }
    } else {
      // 옵션이 선택되지 않았거나 옵션이 없으면 기본 가격
      setCurrentPrice(product?.price || 0);
      // 옵션이 없는 상품의 경우: 백엔드에서 가상 옵션(optionNo: null)으로 재고 정보를 전달함
      if (product?.options && product.options.length === 1 && product.options[0].optionNo === null) {
        const virtualOption = product.options[0];
        const stockQty = virtualOption.stockQuantity !== undefined ? virtualOption.stockQuantity : null;
        const stockStatus = virtualOption.inStock !== undefined ? virtualOption.inStock : null;
        setStockQuantity(stockQty);
        setInStock(stockStatus);
        // 재고가 부족하면 수량 조정
        if (stockQty !== null && stockQty !== undefined && stockQty > 0) {
          if (quantity > stockQty) {
            setQuantity(stockQty);
          }
        } else if (stockQty === 0) {
          // 재고가 0개이면 수량을 0으로 설정
          setQuantity(0);
        }
      } else {
        // 옵션이 없는 상품의 경우 상품 레벨 재고 확인 (현재는 옵션 단위만 지원)
        setStockQuantity(null);
        setInStock(null);
      }
    }
  }, [selectedOptionNo, product]);

  const calculateDiscountAmount = (salePolicy, basePrice) => {
    if (!salePolicy || !salePolicy.discountType || salePolicy.discountValue == null) return 0;
    if (!basePrice || basePrice <= 0) return 0;

    const type = salePolicy.discountType;
    const value = Number(salePolicy.discountValue || 0);
    const maxDiscountAmount =
      salePolicy.maxDiscountAmount != null ? Number(salePolicy.maxDiscountAmount) : null;

    let discount = 0;
    if (type === "PERCENT") {
      discount = (basePrice * value) / 100;
    } else {
      discount = value;
    }
    if (discount <= 0) return 0;
    if (maxDiscountAmount != null && maxDiscountAmount > 0) {
      discount = Math.min(discount, maxDiscountAmount);
    }
    return Math.min(basePrice, Math.max(0, discount));
  };

  // 선택 옵션(또는 가상 옵션) 기준으로 세일가/원가 표시를 계산
  useEffect(() => {
    const run = async () => {
      const productNo = product?.productNo ?? product?.id;
      if (!productNo) return;

      const base = Number(currentPrice ?? 0);
      if (!base || base <= 0) {
        setDisplayPrice(base);
        setDisplayOldPrice(null);
        return;
      }

      try {
        const saleResult = await getApplicableSale({
          productNo,
          optionNo: selectedOptionNo ?? null,
        });
        const salePolicy = saleResult?.data ?? saleResult;

        const discountAmount = calculateDiscountAmount(salePolicy, base);
        const salePrice = Math.max(0, base - discountAmount);
        const hasSale = discountAmount > 0 && salePrice < base;

        setDisplayOldPrice(hasSale ? base : null);
        setDisplayPrice(hasSale ? salePrice : base);
      } catch (e) {
        setDisplayOldPrice(null);
        setDisplayPrice(base);
      }
    };

    run();
  }, [product, selectedOptionNo, currentPrice]);

  useEffect(() => {
    const fetchReviewCount = async () => {
      const productNo = product?.productNo ?? product?.id;
      if (!productNo) {
        setReviewCount(0);
        return;
      }
      try {
        const paged = await getReviewsByProduct(productNo, { page: 1, size: 1, sort: "latest" });
        const payload = paged?.data || paged;
        const total = Number(payload?.meta?.total || 0);
        setReviewCount(Number.isFinite(total) ? total : 0);
      } catch (e) {
        setReviewCount(0);
      }
    };
    fetchReviewCount();
  }, [product?.productNo, product?.id]);

  useEffect(() => {
    const trackProductView = async () => {
      const productNo = product?.productNo ?? product?.id;
      if (!productNo) {
        setLiveViewCount(0);
        return;
      }
      const count = await incrementProductView(productNo);
      setLiveViewCount(Number.isFinite(count) ? count : 0);
    };
    trackProductView();
  }, [product?.productNo, product?.id]);

  // 옵션이 하나만 있으면 자동 선택
  useEffect(() => {
    if (product?.options && product.options.length === 1 && !selectedOptionNo) {
      const singleOption = product.options[0];
      setSelectedOptionNo(singleOption.optionNo);
      // 재고 정보도 함께 업데이트
      if (singleOption.stockQuantity !== undefined) {
        setStockQuantity(singleOption.stockQuantity);
      }
      if (singleOption.inStock !== undefined) {
        setInStock(singleOption.inStock);
      }
    }
  }, [product, selectedOptionNo]);

  const handleSelectOption = (optionNo) => {
    setSelectedOptionNo(optionNo);
  };

  const handleColorSizeChange = (color, size) => {
    setSelectedColor(color);
    setSelectedSize(size);

    // 색상 변경으로 사이즈가 해제된 경우, 이전 옵션 재고가 남지 않도록 즉시 초기화
    if (color && !size) {
      setSelectedOptionNo(null);
      setStockQuantity(null);
      setInStock(null);
      return;
    }
    
    // 색상과 사이즈가 모두 있으면 즉시 옵션 찾기
    if (color && size && product?.options) {
      const matchingOption = product.options.find(
        opt => opt.color === color && opt.size === size
      );
      if (matchingOption) {
        setSelectedOptionNo(matchingOption.optionNo);
      }
    }
  };

  const basePrice = product ? parseInt(product.price || product.minPrice || 0) : 0;
  const realOptions = product?.options?.filter((opt) => opt.optionNo !== null) || [];
  const optionDisplayPrices = realOptions
    .map((opt) => Number(opt.salePrice ?? opt.totalPrice ?? 0))
    .filter((price) => Number.isFinite(price) && price > 0);
  const rangeMinPrice = optionDisplayPrices.length > 0 ? Math.min(...optionDisplayPrices) : Number(displayPrice || 0);
  const rangeMaxPrice = optionDisplayPrices.length > 0 ? Math.max(...optionDisplayPrices) : Number(displayPrice || 0);
  const showRangePrice =
    selectedOptionNo == null && realOptions.length > 0 && rangeMinPrice < rangeMaxPrice;

  return (
    <section className="flat-spacing">
      <div className="tf-main-product section-image-zoom">
        <div className="container">
          <div className="mb-3">
            <Link
              href="/shop-default-grid"
              className="tf-btn btn-reset"
              style={{
                border: "none",
                color: "#111111",
                background: "transparent",
              }}
            >
              ← 상품 목록으로
            </Link>
          </div>
          <div className="row">
            {/* Product default */}
            <div className="col-md-6">
              <div className="tf-product-media-wrap sticky-top">
                <Slider1
                  setActiveColor={setActiveColor}
                  activeColor={activeColor}
                  firstItem={product.imgSrc}
                  slideItems={(product?.images && product.images.length > 0)
                    ? [...product.images].sort((a,b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
                    : undefined}
                />
              </div>
            </div>
            {/* /Product default */}
            {/* tf-product-info-list */}
            <div className="col-md-6">
              <div className="tf-product-info-wrap position-relative mw-100p-hidden ">
                <div className="tf-zoom-main" />
                <div className="tf-product-info-list other-image-zoom">
                  <div className="tf-product-info-heading">
                    <div className="tf-product-info-name">
                      {product.brandName ? (
                        <Link
                          href={`/search-result?brand=${encodeURIComponent(product.brandName)}`}
                          className="text text-btn-uppercase"
                          style={{ textDecoration: "none", color: "#666" }}
                        >
                          {product.brandName}
                        </Link>
                      ) : (
                        <div className="text text-btn-uppercase">상품</div>
                      )}
                      <h3 className="name">{product.title}</h3>
                      <div className="sub">
                        <div className="tf-product-info-rate">
                          <div className="list-star">
                            <i className="icon icon-star" />
                            <i className="icon icon-star" />
                            <i className="icon icon-star" />
                            <i className="icon icon-star" />
                            <i className="icon icon-star" />
                          </div>
                          <div className="text text-caption-1">
                            (리뷰 {reviewCount}개)
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="tf-product-info-desc">
                      <div className="tf-product-info-price">
                        {showRangePrice ? (
                          <h5 className="price-on-sale font-2">
                            {formatKrw(rangeMinPrice)} ~ {formatKrw(rangeMaxPrice)}
                          </h5>
                        ) : (
                          <>
                            <h5 className="price-on-sale font-2">
                              {" "}
                              {formatKrw(displayPrice || 0)}
                            </h5>
                            {displayOldPrice && displayOldPrice !== currentPrice ? (
                              <>
                                <div className="compare-at-price font-2">
                                  {" "}
                                  {formatKrw(displayOldPrice || 0)}
                                </div>
                                <div className="badges-on-sale text-btn-uppercase">
                                  -{Math.round(((displayOldPrice - displayPrice) / displayOldPrice) * 100)}%
                                </div>
                              </>
                            ) : (
                              ""
                            )}
                          </>
                        )}
                      </div>
                      <div className="tf-product-info-liveview">
                        <i className="icon icon-eye" />
                        <p className="text-caption-1">
                          현재 <span className="liveview-count">{liveViewCount}</span>명이 이 상품을 보고 있어요
                        </p>
                      </div>
                    </div>
                  </div>
                  <div className="tf-product-info-choose-option">
                    {product?.options && product.options.length > 0 && (
                      <ColorSizeSelect
                        key={`detail-option-${product?.productNo ?? product?.id}`}
                        options={product.options}
                        selectedOptionNo={selectedOptionNo}
                        onSelectOption={handleSelectOption}
                        basePrice={basePrice}
                        onColorSizeChange={handleColorSizeChange}
                      />
                    )}
                    <div className="tf-product-info-quantity">
                      <div className="title mb_12">
                        수량:
                        {stockQuantity !== null && stockQuantity !== undefined && (
                          stockQuantity === 0 ? (
                            <span style={{ 
                              marginLeft: '8px', 
                              fontSize: '14px', 
                              color: '#ff4d4f',
                              fontWeight: '500'
                            }}>
                              (품절)
                            </span>
                          ) : stockQuantity <= 5 ? (
                            <span style={{ 
                              marginLeft: '8px', 
                              fontSize: '14px', 
                              color: '#fa8c16',
                              fontWeight: '500'
                            }}>
                              (재고: {stockQuantity}개)
                            </span>
                          ) : null
                        )}
                      </div>
                      <QuantitySelect
                        quantity={
                          isAddedToCartProducts(product.id, selectedOptionNo)
                            ? cartProducts.find(
                                (elm) => elm.id == product.id && elm.selectedOptionNo === selectedOptionNo
                              )?.quantity || quantity
                            : quantity
                        }
                        setQuantity={(qty) => {
                          if (isAddedToCartProducts(product.id, selectedOptionNo)) {
                            updateQuantity(product.id, qty, selectedOptionNo);
                          } else {
                            setQuantity(qty);
                          }
                        }}
                        maxQuantity={stockQuantity !== null && stockQuantity !== undefined ? stockQuantity : null}
                      />
                    </div>
                    <div>
                      <div className="tf-product-info-by-btn mb_10">
                        <a
                          onClick={() => {
                            // 재고 정보가 없거나 재고가 0개이면 클릭 무시
                            // 재고가 5개 이하일 때는 재고 수량 이상을 담지 못하게 막음 (이미 QuantitySelect에서 처리됨)
                            if (product?.options && product.options.length > 0) {
                              const selectedOption = product.options.find(opt => opt.optionNo === selectedOptionNo);
                              if (selectedOption) {
                                const stockQty = selectedOption.stockQuantity;
                                const inStockValue = selectedOption.inStock;
                                // 품절 상태이면 클릭 무시
                                if (stockQty === null || stockQty === undefined || stockQty === 0 || inStockValue === false) {
                                  return;
                                }
                                // 재고가 5개 이하일 때는 재고 수량 이상을 담지 못하게 막음 (QuantitySelect에서 이미 처리됨)
                                // 여기서는 품절 상태만 체크
                              }
                            } else {
                              const virtualOption = product.options && product.options.length === 1 && 
                                                   product.options[0].optionNo === null 
                                                   ? product.options[0] : null;
                              if (virtualOption) {
                                const stockQty = virtualOption.stockQuantity;
                                const inStockValue = virtualOption.inStock;
                                if (stockQty === null || stockQty === undefined || stockQty === 0 || inStockValue === false) {
                                  return;
                                }
                              } else {
                                return; // 재고 정보가 없으면 클릭 무시
                              }
                            }
                            
                            // 기존 로직 실행
                            console.log("Add to cart 클릭:", {
                              selectedOptionNo,
                              selectedColor,
                              selectedSize,
                              options: product?.options,
                            });
                            
                            if (product?.options && product.options.length > 0) {
                              // selectedOptionNo가 없으면 색상과 사이즈로 옵션 찾기
                              let finalOptionNo = selectedOptionNo;
                              if (!finalOptionNo && selectedColor && selectedSize) {
                                console.log("색상/사이즈로 옵션 찾기:", { selectedColor, selectedSize });
                                const matchingOption = product.options.find(
                                  opt => opt.color === selectedColor && opt.size === selectedSize
                                );
                                console.log("찾은 옵션:", matchingOption);
                                if (matchingOption) {
                                  finalOptionNo = matchingOption.optionNo;
                                  console.log("옵션 번호 설정:", finalOptionNo);
                                  setSelectedOptionNo(finalOptionNo);
                                }
                              }
                              
                              if (!finalOptionNo) {
                                console.warn("옵션을 선택하지 않음:", {
                                  selectedOptionNo,
                                  selectedColor,
                                  selectedSize,
                                });
                                alert("옵션을 선택해주세요.");
                                return;
                              }
                              
                              // 재고 확인
                              const selectedOption = product.options.find(opt => opt.optionNo === finalOptionNo);
                              if (selectedOption) {
                                const stockQty = selectedOption.stockQuantity;
                                const inStockValue = selectedOption.inStock;
                                
                                // 재고 정보가 없으면 장바구니에 담을 수 없음
                                if (stockQty === null || stockQty === undefined) {
                                  alert("재고 정보가 없어 장바구니에 담을 수 없습니다.");
                                  return;
                                }
                                
                                // 재고가 0개이거나 inStock이 false이면 품절
                                if (stockQty === 0 || inStockValue === false) {
                                  alert("품절된 상품입니다.");
                                  return;
                                }
                                
                                // 재고 수량 확인
                                if (quantity > stockQty) {
                                  alert(`재고가 부족합니다. (현재 재고: ${stockQty}개)`);
                                  return;
                                }
                              } else {
                                // 옵션을 찾을 수 없으면 장바구니에 담을 수 없음
                                alert("옵션 정보를 찾을 수 없습니다.");
                                return;
                              }
                              
                              console.log("최종 옵션 번호:", finalOptionNo);
                              
                              // 선택한 옵션의 가격으로 상품 정보 업데이트
                              const productWithPrice = {
                                ...product,
                                price: currentPrice,
                              };
                              addProductToCart(product.id, quantity, finalOptionNo, productWithPrice);
                            } else {
                              // 옵션이 없는 상품의 경우
                              // 백엔드에서 가상 옵션(optionNo: null)으로 재고 정보를 전달함
                              const virtualOption = product.options && product.options.length === 1 && 
                                                   product.options[0].optionNo === null 
                                                   ? product.options[0] : null;
                              
                              if (virtualOption) {
                                const stockQty = virtualOption.stockQuantity;
                                const inStockValue = virtualOption.inStock;
                                
                                // 재고 정보가 없으면 장바구니에 담을 수 없음
                                if (stockQty === null || stockQty === undefined) {
                                  alert("재고 정보가 없어 장바구니에 담을 수 없습니다.");
                                  return;
                                }
                                
                                // 재고가 0개이거나 inStock이 false이면 품절
                                if (stockQty === 0 || inStockValue === false) {
                                  alert("품절된 상품입니다.");
                                  return;
                                }
                                
                                // 재고 수량 확인
                                if (quantity > stockQty) {
                                  alert(`재고가 부족합니다. (현재 재고: ${stockQty}개)`);
                                  return;
                                }
                              } else {
                                // 재고 정보가 없는 경우
                                alert("재고 정보가 없어 장바구니에 담을 수 없습니다.");
                                return;
                              }
                              
                              const productWithPrice = {
                                ...product,
                                price: currentPrice,
                              };
                              addProductToCart(product.id, quantity, null, productWithPrice);
                            }
                          }}
                          className={`btn-style-2 flex-grow-1 text-btn-uppercase fw-6 btn-add-to-cart ${
                            // 재고 정보가 없거나 재고가 0개이면 비활성화 (재고가 5개 이하는 QuantitySelect에서 처리)
                            (stockQuantity === null || stockQuantity === undefined || stockQuantity === 0 || inStock === false) 
                              ? 'disabled' : ''
                          }`}
                          style={{
                            opacity: (stockQuantity === null || stockQuantity === undefined || stockQuantity === 0 || inStock === false) ? 0.5 : 1,
                            cursor: (stockQuantity === null || stockQuantity === undefined || stockQuantity === 0 || inStock === false) ? 'not-allowed' : 'pointer',
                            pointerEvents: (stockQuantity === null || stockQuantity === undefined || stockQuantity === 0 || inStock === false) ? 'none' : 'auto'
                          }}
                        >
                          <span>
                            장바구니 담기 -
                          </span>
                          <span className="tf-qty-price total-price">
                            {isAddedToCartProducts(product.id, selectedOptionNo)
                              ? formatKrw(
                                  displayPrice *
                                    (cartProducts.find(
                                      (elm) =>
                                        elm.id == product.id &&
                                        elm.selectedOptionNo === selectedOptionNo
                                    )?.quantity || quantity)
                                )
                              : formatKrw(displayPrice * quantity)}{" "}
                          </span>
                        </a>
                      </div>
                      <a href="#" className="btn-style-3 text-btn-uppercase">
                        바로 구매
                      </a>
                    </div>
                    <ul className="tf-product-info-sku">
                      <li>
                        <p className="text-caption-1">SKU:</p>
                        <p className="text-caption-1 text-1">{product?.sku || "-"}</p>
                      </li>
                      <li>
                        <p className="text-caption-1">브랜드:</p>
                        <p className="text-caption-1 text-1">{product?.brandName || "-"}</p>
                      </li>
                      <li>
                        <p className="text-caption-1">카테고리:</p>
                        <p className="text-caption-1 text-1">
                          {[
                            PRODUCT_TYPE_LABELS[product?.category] || product?.category,
                            PRODUCT_SUB_TYPE_LABELS[product?.subCategory] || product?.subCategory,
                          ]
                            .filter((label) => Boolean(label))
                            .join(" / ") || "-"}
                        </p>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
            {/* /tf-product-info-list */}
          </div>
        </div>
      </div>
    </section>
  );
}
