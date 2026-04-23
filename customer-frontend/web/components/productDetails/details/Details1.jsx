"use client";
import React, { useEffect, useState } from "react";
import Slider1 from "../sliders/Slider1";
import ColorSizeSelect from "../ColorSizeSelect";
import QuantitySelect from "../QuantitySelect";
import Image from "next/image";
import Link from "next/link";
import { useContextElement } from "@/context/Context";
import ProductStikyBottom from "../ProductStikyBottom";
import { getApplicableSale } from "@/lib/api/sale";
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
  const {
    addProductToCart,
    isAddedToCartProducts,
    addToWishlist,
    isAddedtoWishlist,
    isAddedtoCompareItem,
    addToCompareItem,
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

  return (
    <section className="flat-spacing">
      <div className="tf-main-product section-image-zoom">
        <div className="container">
          <div className="mb-3">
            <Link href="/shop-default-grid" className="tf-btn btn-line">
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
                      <div className="text text-btn-uppercase">Clothing</div>
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
                            (134 reviews)
                          </div>
                        </div>
                        <div className="tf-product-info-sold">
                          <i className="icon icon-lightning" />
                          <div className="text text-caption-1">
                            18&nbsp;sold in last&nbsp;32&nbsp;hours
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="tf-product-info-desc">
                      <div className="tf-product-info-price">
                        <h5 className="price-on-sale font-2">
                          {" "}
                          ₩{displayPrice?.toLocaleString() || 0}
                        </h5>
                        {displayOldPrice && displayOldPrice !== currentPrice ? (
                          <>
                            <div className="compare-at-price font-2">
                              {" "}
                              ₩{displayOldPrice?.toLocaleString() || 0}
                            </div>
                            <div className="badges-on-sale text-btn-uppercase">
                              -{Math.round(((displayOldPrice - displayPrice) / displayOldPrice) * 100)}%
                            </div>
                          </>
                        ) : (
                          ""
                        )}
                      </div>
                      <p>
                        The garments labelled as Committed are products that
                        have been produced using sustainable fibres or
                        processes, reducing their environmental impact.
                      </p>
                      <div className="tf-product-info-liveview">
                        <i className="icon icon-eye" />
                        <p className="text-caption-1">
                          <span className="liveview-count">28</span> people are
                          viewing this right now
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
                        Quantity:
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
                          isAddedToCartProducts(product.id)
                            ? cartProducts.filter(
                                (elm) => elm.id == product.id
                              )[0].quantity
                            : quantity
                        }
                        setQuantity={(qty) => {
                          if (isAddedToCartProducts(product.id)) {
                            updateQuantity(product.id, qty);
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
                            Add to cart -
                          </span>
                          <span className="tf-qty-price total-price">
                            ₩
                            {isAddedToCartProducts(product.id, selectedOptionNo)
                              ? (
                                  displayPrice *
                                  cartProducts.find(
                                    (elm) => elm.id == product.id && elm.selectedOptionNo === selectedOptionNo
                                  )?.quantity || quantity
                                ).toLocaleString()
                              : (displayPrice * quantity).toLocaleString()}{" "}
                          </span>
                        </a>
                        <a
                          href="#compare"
                          data-bs-toggle="offcanvas"
                          aria-controls="compare"
                          onClick={() => addToCompareItem(product.id)}
                          className="box-icon hover-tooltip compare btn-icon-action"
                        >
                          <span className="icon icon-gitDiff" />
                          <span className="tooltip text-caption-2">
                            {isAddedtoCompareItem(product.id)
                              ? "Already compared"
                              : "Compare"}
                          </span>
                        </a>
                        <a
                          onClick={() => addToWishlist(product.id)}
                          className="box-icon hover-tooltip text-caption-2 wishlist btn-icon-action"
                        >
                          <span className="icon icon-heart" />
                          <span className="tooltip text-caption-2">
                            {isAddedtoWishlist(product.id)
                              ? "Already Wishlished"
                              : "Wishlist"}
                          </span>
                        </a>
                      </div>
                      <a href="#" className="btn-style-3 text-btn-uppercase">
                        Buy it now
                      </a>
                    </div>
                    <div className="tf-product-info-help">
                      <div className="tf-product-info-extra-link">
                        <a
                          href="#delivery_return"
                          data-bs-toggle="modal"
                          className="tf-product-extra-icon"
                        >
                          <div className="icon">
                            <i className="icon-shipping" />
                          </div>
                          <p className="text-caption-1">
                            Delivery &amp; Return
                          </p>
                        </a>
                        <a
                          href="#ask_question"
                          data-bs-toggle="modal"
                          className="tf-product-extra-icon"
                        >
                          <div className="icon">
                            <i className="icon-question" />
                          </div>
                          <p className="text-caption-1">Ask A Question</p>
                        </a>
                        <a
                          href="#share_social"
                          data-bs-toggle="modal"
                          className="tf-product-extra-icon"
                        >
                          <div className="icon">
                            <i className="icon-share" />
                          </div>
                          <p className="text-caption-1">Share</p>
                        </a>
                      </div>
                      <div className="tf-product-info-time">
                        <div className="icon">
                          <i className="icon-timer" />
                        </div>
                        <p className="text-caption-1">
                          Estimated Delivery:&nbsp;&nbsp;<span>12-26 days</span>
                          (International), <span>3-6 days</span> (United States)
                        </p>
                      </div>
                      <div className="tf-product-info-return">
                        <div className="icon">
                          <i className="icon-arrowClockwise" />
                        </div>
                        <p className="text-caption-1">
                          Return within <span>45 days</span> of purchase. Duties
                          &amp; taxes are non-refundable.
                        </p>
                      </div>
                      <div className="dropdown dropdown-store-location">
                        <div
                          className="dropdown-title dropdown-backdrop"
                          data-bs-toggle="dropdown"
                          aria-haspopup="true"
                        >
                          <div className="tf-product-info-view link">
                            <div className="icon">
                              <i className="icon-map-pin" />
                            </div>
                            <span>View Store Information</span>
                          </div>
                        </div>
                        <div className="dropdown-menu dropdown-menu-end">
                          <div className="dropdown-content">
                            <div className="dropdown-content-heading">
                              <h5>Store Location</h5>
                              <i className="icon icon-close" />
                            </div>
                            <div className="line-bt" />
                            <div>
                              <h6>Fashion Modave</h6>
                              <p>Pickup available. Usually ready in 24 hours</p>
                            </div>
                            <div>
                              <p>766 Rosalinda Forges Suite 044,</p>
                              <p>Gracielahaven, Oregon</p>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                    <ul className="tf-product-info-sku">
                      <li>
                        <p className="text-caption-1">SKU:</p>
                        <p className="text-caption-1 text-1">53453412</p>
                      </li>
                      <li>
                        <p className="text-caption-1">Vendor:</p>
                        <p className="text-caption-1 text-1">Modave</p>
                      </li>
                      <li>
                        <p className="text-caption-1">Available:</p>
                        <p className="text-caption-1 text-1">Instock</p>
                      </li>
                      <li>
                        <p className="text-caption-1">Categories:</p>
                        <p className="text-caption-1">
                          <a href="#" className="text-1 link">
                            Clothes
                          </a>
                          ,
                          <a href="#" className="text-1 link">
                            women
                          </a>
                          ,
                          <a href="#" className="text-1 link">
                            T-shirt
                          </a>
                        </p>
                      </li>
                    </ul>
                    <div className="tf-product-info-guranteed">
                      <div className="text-title">Guranteed safe checkout:</div>
                      <div className="tf-payment">
                        <a href="#">
                          <Image
                            alt=""
                            src="/images/payment/img-1.png"
                            width={100}
                            height={64}
                          />
                        </a>
                        <a href="#">
                          <Image
                            alt=""
                            src="/images/payment/img-2.png"
                            width={100}
                            height={64}
                          />
                        </a>
                        <a href="#">
                          <Image
                            alt=""
                            src="/images/payment/img-3.png"
                            width={100}
                            height={64}
                          />
                        </a>
                        <a href="#">
                          <Image
                            alt=""
                            src="/images/payment/img-4.png"
                            width={98}
                            height={64}
                          />
                        </a>
                        <a href="#">
                          <Image
                            alt=""
                            src="/images/payment/img-5.png"
                            width={102}
                            height={64}
                          />
                        </a>
                        <a href="#">
                          <Image
                            alt=""
                            src="/images/payment/img-6.png"
                            width={98}
                            height={64}
                          />
                        </a>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            {/* /tf-product-info-list */}
          </div>
        </div>
      </div>
      <ProductStikyBottom />
    </section>
  );
}
