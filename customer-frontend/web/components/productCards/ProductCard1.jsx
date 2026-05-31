"use client";
import React, { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import CountdownTimer from "../common/Countdown";
import { useContextElement } from "@/context/Context";
import { formatKrw } from "@/lib/price/formatKrw";
import { DEFAULT_PRODUCT_PLACEHOLDER } from "@/lib/media/productImage";

function safeCardImage(url) {
  if (typeof url !== "string") return DEFAULT_PRODUCT_PLACEHOLDER;
  const t = url.trim();
  return t || DEFAULT_PRODUCT_PLACEHOLDER;
}

export default function ProductCard1({
  product,
  gridClass = "",
  parentClass = "card-product wow fadeInUp",
  isNotImageRatio = false,
  radiusClass = "",
}) {
  const [currentImage, setCurrentImage] = useState(
    safeCardImage(product.imgSrc),
  );
  const router = useRouter();

  const {
    setQuickAddItem,
    setQuickViewItem,
    addProductToCart,
    isAddedToCartProducts,
  } = useContextElement();

  useEffect(() => {
    setCurrentImage(safeCardImage(product.imgSrc));
  }, [product]);

  const goToDetail = () => {
    router.push(`/product-detail/${product.id}`);
  };

  return (
    <div
      className={`${parentClass} ${gridClass} ${
        product.isOnSale ? "on-sale" : ""
      } ${product.sizes ? "card-product-size" : ""}`}
    >
      <div
        className={`card-product-wrapper ${
          isNotImageRatio ? "aspect-ratio-0" : ""
        } ${radiusClass} `}
      >
        <Link
          href={`/product-detail/${product.id}`}
          className="product-img"
          onClick={(e) => {
            // 링크 기본 동작은 유지하되, 일부 오버레이 상황에서도 상세 이동을 보장
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
            src={safeCardImage(product.imgHover)}
            alt={product.title}
            width={600}
            height={800}
            onClick={goToDetail}
          />
        </Link>
        {product.hotSale && (
          <div className="marquee-product bg-main">
            <div className="marquee-wrapper">
              <div className="initial-child-container">
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
              </div>
            </div>
            <div className="marquee-wrapper">
              <div className="initial-child-container">
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
                <div className="marquee-child-item">
                  <p className="font-2 text-btn-uppercase fw-6 text-white">
                    Hot Sale 25% OFF
                  </p>
                </div>
                <div className="marquee-child-item">
                  <span className="icon icon-lightning text-critical" />
                </div>
              </div>
            </div>
          </div>
        )}
        {product.isOnSale && (
          <div className="on-sale-wrap">
            <span className="on-sale-item">-{product.salePercentage}</span>
          </div>
        )}
        {product.sizes && (
          <div className="variant-wrap size-list">
            <ul className="variant-box">
              {product.sizes.map((size, idx) => (
                <li key={`sz-${idx}-${String(size)}`} className="size-item">
                  {size}
                </li>
              ))}
            </ul>
          </div>
        )}
        {product.countdown && (
          <div className="variant-wrap countdown-wrap">
            <div className="variant-box">
              <div
                className="js-countdown"
                data-timer={product.countdown}
                data-labels="D :,H :,M :,S"
              >
                <CountdownTimer />
              </div>
            </div>
          </div>
        )}
        {product.oldPrice ? (
          <div className="on-sale-wrap">
            <span className="on-sale-item">-25%</span>
          </div>
        ) : (
          ""
        )}
        <div className="list-product-btn">
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
        <div className="list-btn-main">
          {(() => {
            // 재고 상태 확인
            let isOutOfStock = false;
            let maxAvailableStock = null;
            
            // 옵션이 없는 상품의 경우
            if (product.options && product.options.length === 1 && product.options[0].optionNo === null) {
              const stockQty = product.options[0].stockQuantity;
              const inStockValue = product.options[0].inStock;
              isOutOfStock = (stockQty === null || stockQty === undefined || stockQty === 0 || inStockValue === false);
              maxAvailableStock = stockQty;
            }
            // 옵션이 있는 상품의 경우: 최소 재고 확인
            else if (product.options && product.options.length > 0) {
              // optionNo가 null이 아닌 실제 옵션들만 필터링 (가상 옵션 제외)
              const realOptions = product.options.filter(opt => opt.optionNo !== null);
              
              if (realOptions.length > 0) {
                const stockQuantities = realOptions
                  .map(opt => {
                    const qty = opt.stockQuantity;
                    const inStock = opt.inStock;
                    
                    // 재고 정보가 없으면 제외 (null 또는 undefined)
                    if (qty === null || qty === undefined) {
                      return null;
                    }
                    
                    // 재고가 0이면 제외
                    if (qty === 0) {
                      return null;
                    }
                    
                    // inStock이 명시적으로 false면 제외
                    if (inStock === false) {
                      return null;
                    }
                    
                    // 재고 정보가 있고, 재고가 0보다 크고, inStock이 false가 아니면 포함
                    return qty;
                  })
                  .filter(qty => qty !== null);
                
                if (stockQuantities.length === 0) {
                  isOutOfStock = true; // 모든 옵션이 품절
                } else {
                  maxAvailableStock = Math.max(...stockQuantities);
                }
              }
            }
            
            // 품절 상태일 때만 버튼 비활성화 (재고가 5개 이하는 버튼은 활성화하되, 수량 제한은 QuickAdd 모달에서 처리)
            const isDisabled = isOutOfStock;
            
            // 옵션이 있는 상품이면 QuickAdd 모달 띄우기, 없으면 바로 추가
            if (product.options && product.options.length > 0) {
              return (
                <a
                  className={`btn-main-product ${isDisabled ? 'disabled' : ''}`}
                  href={isDisabled ? undefined : "#quickAdd"}
                  onClick={(e) => {
                    if (isDisabled) {
                      e.preventDefault();
                      return;
                    }
                    setQuickAddItem(product);
                  }}
                  data-bs-toggle={isDisabled ? undefined : "modal"}
                  style={{
                    opacity: isDisabled ? 0.5 : 1,
                    cursor: isDisabled ? 'not-allowed' : 'pointer',
                    pointerEvents: isDisabled ? 'none' : 'auto'
                  }}
                >
                  {isOutOfStock ? "품절" : "장바구니 담기"}
                </a>
              );
            } else if (product.addToCart == "Quick Add") {
              return (
                <a
                  className={`btn-main-product ${isDisabled ? 'disabled' : ''}`}
                  href={isDisabled ? undefined : "#quickAdd"}
                  onClick={(e) => {
                    if (isDisabled) {
                      e.preventDefault();
                      return;
                    }
                    setQuickAddItem(product);
                  }}
                  data-bs-toggle={isDisabled ? undefined : "modal"}
                  style={{
                    opacity: isDisabled ? 0.5 : 1,
                    cursor: isDisabled ? 'not-allowed' : 'pointer',
                    pointerEvents: isDisabled ? 'none' : 'auto'
                  }}
                >
                  {isOutOfStock ? "품절" : "Quick Add"}
                </a>
              );
            } else {
              return (
                <a
                  className={`btn-main-product ${isDisabled ? 'disabled' : ''}`}
                  onClick={(e) => {
                    if (isDisabled) {
                      e.preventDefault();
                      return;
                    }
                    addProductToCart(product.id, 1, null, product);
                  }}
                  style={{
                    opacity: isDisabled ? 0.5 : 1,
                    cursor: isDisabled ? 'not-allowed' : 'pointer',
                    pointerEvents: isDisabled ? 'none' : 'auto'
                  }}
                >
                  {isOutOfStock 
                    ? "품절" 
                    : isAddedToCartProducts(product.id)
                      ? "장바구니에 추가됨"
                      : "장바구니 담기"}
                </a>
              );
            }
          })()}
        </div>
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
        <span className="price">
          {product.oldPrice && (
            <span className="old-price">{formatKrw(product.oldPrice)}</span>
          )}{" "}
          {formatKrw(product.price)}
        </span>
        {/* 재고 정보 표시 (5개 이하일 때만) */}
        {(() => {
          // 옵션이 없는 상품의 경우 (가상 옵션 optionNo: null)
          if (product.options && product.options.length === 1 && product.options[0].optionNo === null) {
            const stockQty = product.options[0].stockQuantity;
            if (stockQty !== null && stockQty !== undefined) {
              if (stockQty === 0) {
                return (
                  <span style={{ 
                    display: 'block',
                    fontSize: '12px', 
                    color: '#ff4d4f',
                    fontWeight: '500',
                    marginTop: '4px'
                  }}>
                    품절
                  </span>
                );
              } else if (stockQty <= 5) {
                return (
                  <span style={{ 
                    display: 'block',
                    fontSize: '12px', 
                    color: '#fa8c16',
                    fontWeight: '500',
                    marginTop: '4px'
                  }}>
                    재고: {stockQty}개
                  </span>
                );
              }
            }
          }
          // 옵션이 있는 상품의 경우: 최소 재고 표시
          else if (product.options && product.options.length > 0) {
            // optionNo가 null이 아닌 실제 옵션들만 필터링 (가상 옵션 제외)
            const realOptions = product.options.filter(opt => opt.optionNo !== null);
            
            if (realOptions.length > 0) {
              // 재고가 있는 옵션들만 필터링
              // 재고 정보가 있고(stockQuantity !== null), 재고가 0보다 크고, inStock이 false가 아닌 경우
              const availableOptions = realOptions.filter(opt => {
                const qty = opt.stockQuantity;
                const inStock = opt.inStock;
                
                // 재고 정보가 없으면 제외 (null 또는 undefined)
                if (qty === null || qty === undefined) {
                  return false;
                }
                
                // 재고가 0이면 제외
                if (qty === 0) {
                  return false;
                }
                
                // inStock이 명시적으로 false면 제외
                if (inStock === false) {
                  return false;
                }
                
                // 재고 정보가 있고, 재고가 0보다 크고, inStock이 false가 아니면 포함
                return true;
              });
              
              // 재고가 있는 옵션이 하나도 없으면 품절
              if (availableOptions.length === 0) {
                return (
                  <span style={{ 
                    display: 'block',
                    fontSize: '12px', 
                    color: '#ff4d4f',
                    fontWeight: '500',
                    marginTop: '4px'
                  }}>
                    품절
                  </span>
                );
              }
              
              // 재고가 있는 옵션들의 재고 수량 추출
              const stockQuantities = availableOptions.map(opt => opt.stockQuantity);
              const minStock = Math.min(...stockQuantities);
              
              // 최소 재고가 5개 이하인 경우
              if (minStock <= 5) {
                return (
                  <span style={{ 
                    display: 'block',
                    fontSize: '12px', 
                    color: '#fa8c16',
                    fontWeight: '500',
                    marginTop: '4px'
                  }}>
                    재고: {minStock}개 이하
                  </span>
                );
              }
            }
          }
          return null;
        })()}
        {product.colors && (
          <ul className="list-color-product">
            {product.colors.map((color, index) => (
              <li
                key={index}
                className={`list-color-item color-swatch ${
                  currentImage == safeCardImage(color.imgSrc) ? "active" : ""
                } ${color.bgColor == "bg-white" ? "line" : ""}`}
                onMouseOver={() => setCurrentImage(safeCardImage(color.imgSrc))}
              >
                <span className={`swatch-value ${color.bgColor}`} />
                <Image
                  className="lazyload"
                  src={safeCardImage(color.imgSrc)}
                  alt="color variant"
                  width={600}
                  height={800}
                />
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
