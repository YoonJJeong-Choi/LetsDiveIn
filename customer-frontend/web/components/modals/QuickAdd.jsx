"use client";
import { useContextElement } from "@/context/Context";
import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import ColorSizeSelect from "../productDetails/ColorSizeSelect";
import QuantitySelect from "../productDetails/QuantitySelect";
import { formatKrw } from "@/lib/price/formatKrw";
import { DEFAULT_PRODUCT_PLACEHOLDER } from "@/lib/media/productImage";
export default function QuickAdd() {
  const [quantity, setQuantity] = useState(1);
  const [selectedOptionNo, setSelectedOptionNo] = useState(null);
  const [selectedColor, setSelectedColor] = useState(null);
  const [selectedSize, setSelectedSize] = useState(null);
  const [currentPrice, setCurrentPrice] = useState(0);
  const [currentOldPrice, setCurrentOldPrice] = useState(null);
  const [currentDiscountPercent, setCurrentDiscountPercent] = useState(null);
  const [maxQuantity, setMaxQuantity] = useState(null); // 재고 수량 제한
  const {
    quickAddItem,
    addProductToCart,
    isAddedToCartProducts,
  } = useContextElement();

  const resetQuickAddSelection = (item) => {
    if (!item || typeof item !== "object") {
      setQuantity(1);
      setSelectedOptionNo(null);
      setSelectedColor(null);
      setSelectedSize(null);
      setCurrentPrice(0);
      setMaxQuantity(null);
      return;
    }

    setQuantity(1);
    setSelectedOptionNo(null);
    setSelectedColor(null);
    setSelectedSize(null);
    const baseSalePrice = Number(item.price || item.minPrice || 0);
    const baseOldPrice =
      Number(item?.oldPrice || 0) > baseSalePrice ? Number(item.oldPrice) : null;
    const baseDiscountPercent =
      baseOldPrice && baseOldPrice > 0
        ? Math.round(((baseOldPrice - baseSalePrice) / baseOldPrice) * 100)
        : null;
    setCurrentPrice(baseSalePrice);
    setCurrentOldPrice(baseOldPrice);
    setCurrentDiscountPercent(baseDiscountPercent);

    // 옵션이 하나만 있으면 자동 선택 (가상 옵션 제외)
    if (item.options && item.options.length === 1 && item.options[0].optionNo !== null) {
      setSelectedOptionNo(item.options[0].optionNo);
    }

    // 재고 수량 설정 (옵션이 없는 상품의 경우)
    if (item.options && item.options.length === 1 && item.options[0].optionNo === null) {
      const stockQty = item.options[0].stockQuantity;
      setMaxQuantity(stockQty !== null && stockQty !== undefined ? stockQty : null);
    } else {
      setMaxQuantity(null); // 옵션이 있는 상품은 옵션 선택 후 설정됨
    }
  };

  // quickAddItem이 변경되면 초기화
  useEffect(() => {
    resetQuickAddSelection(quickAddItem);
  }, [quickAddItem]);

  // 같은 상품을 다시 열어도 이전 옵션 선택값이 남지 않도록 모달 표시 시 초기화
  useEffect(() => {
    const modal = document.getElementById("quickAdd");
    if (!modal) return;

    const handleShow = () => {
      resetQuickAddSelection(quickAddItem);
    };

    modal.addEventListener("show.bs.modal", handleShow);
    return () => {
      modal.removeEventListener("show.bs.modal", handleShow);
    };
  }, [quickAddItem]);

  // 옵션 선택 시 가격 및 재고 수량 업데이트
  useEffect(() => {
    if (selectedOptionNo && quickAddItem?.options) {
      const selectedOption = quickAddItem.options.find(opt => opt.optionNo === selectedOptionNo);
      if (selectedOption) {
        const optionSalePrice = Number(
          selectedOption.salePrice ?? selectedOption.totalPrice ?? 0
        );
        const optionOldPrice =
          selectedOption.salePrice != null &&
          Number(selectedOption.totalPrice || 0) > optionSalePrice
            ? Number(selectedOption.totalPrice || 0)
            : null;
        const optionDiscountPercent =
          selectedOption.salePercent != null
            ? Number(selectedOption.salePercent)
            : optionOldPrice && optionOldPrice > 0
            ? Math.round(((optionOldPrice - optionSalePrice) / optionOldPrice) * 100)
            : null;

        setCurrentPrice(optionSalePrice);
        setCurrentOldPrice(optionOldPrice);
        setCurrentDiscountPercent(optionDiscountPercent);
        // 선택된 옵션의 재고 수량 설정
        const stockQty = selectedOption.stockQuantity;
        setMaxQuantity(stockQty !== null && stockQty !== undefined ? stockQty : null);
      }
    } else if (quickAddItem) {
      setCurrentPrice(quickAddItem.price || quickAddItem.minPrice || 0);
      const baseSalePrice = Number(quickAddItem.price || quickAddItem.minPrice || 0);
      const baseOldPrice =
        Number(quickAddItem?.oldPrice || 0) > baseSalePrice
          ? Number(quickAddItem.oldPrice)
          : null;
      const baseDiscountPercent =
        baseOldPrice && baseOldPrice > 0
          ? Math.round(((baseOldPrice - baseSalePrice) / baseOldPrice) * 100)
          : null;
      setCurrentOldPrice(baseOldPrice);
      setCurrentDiscountPercent(baseDiscountPercent);
      const realOptions = quickAddItem.options?.filter(opt => opt.optionNo !== null) || [];
      if (realOptions.length > 0) {
        // 옵션 상품에서 선택이 해제되면 이전 옵션 재고가 남지 않도록 초기화
        setMaxQuantity(null);
      }
      // 옵션이 없는 상품의 경우 재고 수량은 초기화(useEffect)에서 이미 설정됨
    }
  }, [selectedOptionNo, quickAddItem]);

  const handleSelectOption = (optionNo) => {
    setSelectedOptionNo(optionNo);
  };

  const handleColorSizeChange = (color, size) => {
    const hasChangedColor = selectedColor !== color;
    setSelectedColor(color);
    setSelectedSize(size);

    // 색상 변경 시에는 이전 옵션/수량/재고 표시를 즉시 초기화하여
    // 이전 선택의 잔존 데이터가 남지 않도록 처리
    if (hasChangedColor) {
      setSelectedOptionNo(null);
      setQuantity(1);
      setMaxQuantity(null);
    }

    // 색상 변경으로 사이즈가 해제된 경우, 이전 옵션 재고 표시를 즉시 초기화
    if (color && !size) {
      setSelectedOptionNo(null);
      setMaxQuantity(null);
      return;
    }
    
    // 색상과 사이즈가 모두 있으면 즉시 옵션 찾기
    if (color && size && quickAddItem?.options) {
      const matchingOption = quickAddItem.options.find(
        opt => opt.color === color && opt.size === size
      );
      if (matchingOption) {
        setSelectedOptionNo(matchingOption.optionNo);
      }
    }
  };

  if (!quickAddItem || typeof quickAddItem !== "object") {
    return (
      <div className="modal fade modal-quick-add" id="quickAdd" style={{ display: 'none' }}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
          </div>
        </div>
      </div>
    );
  }

  const item = quickAddItem;
  const realOptions = item.options?.filter((opt) => opt.optionNo !== null) || [];
  const optionSalePrices = realOptions
    .map((opt) => Number(opt.salePrice ?? opt.totalPrice ?? 0))
    .filter((price) => Number.isFinite(price) && price > 0);
  const rangeMinPrice =
    optionSalePrices.length > 0 ? Math.min(...optionSalePrices) : Number(item.price || 0);
  const rangeMaxPrice =
    optionSalePrices.length > 0 ? Math.max(...optionSalePrices) : Number(item.price || 0);
  const shouldShowRangePrice = selectedOptionNo == null && realOptions.length > 0;

  return (
    <div className="modal fade modal-quick-add" id="quickAdd">
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="header">
            <span
              className="icon-close icon-close-popup"
              data-bs-dismiss="modal"
            />
          </div>
          <div>
            <div className="tf-product-info-list">
              <div className="tf-product-info-item">
                <div className="image">
                  <Image alt="" src={item.imgSrc || DEFAULT_PRODUCT_PLACEHOLDER} width={600} height={800} />
                </div>
                <div className="content">
                  <Link href={`/product-detail/${item.id}`}>{item.title}</Link>
                  <div className="tf-product-info-price">
                    {shouldShowRangePrice ? (
                      <h5 className="price-on-sale font-2">
                        {formatKrw(rangeMinPrice)} ~ {formatKrw(rangeMaxPrice)}
                      </h5>
                    ) : (
                      <>
                        <h5 className="price-on-sale font-2">
                          {formatKrw(currentPrice || 0)}
                        </h5>
                        {currentOldPrice ? (
                          <>
                            <div className="compare-at-price font-2">
                              {formatKrw(currentOldPrice)}
                            </div>
                            <div className="badges-on-sale text-btn-uppercase">
                              -{currentDiscountPercent}%
                            </div>
                          </>
                        ) : null}
                      </>
                    )}
                  </div>
                </div>
              </div>
              <div className="tf-product-info-choose-option">
                {/* 옵션 선택 (실제 옵션이 있는 경우만) */}
                {(() => {
                  const realOptions = item.options?.filter(opt => opt.optionNo !== null) || [];
                  if (realOptions.length > 0) {
                    return (
                      <ColorSizeSelect
                        key={`quickadd-option-${item.productNo ?? item.id}`}
                        options={item.options}
                        selectedOptionNo={selectedOptionNo}
                        onSelectOption={handleSelectOption}
                        basePrice={item.price || item.minPrice || 0}
                        onColorSizeChange={handleColorSizeChange}
                      />
                    );
                  }
                  return null;
                })()}
                <div className="tf-product-info-quantity">
                  <div className="title mb_12">
                    수량:
                    {maxQuantity !== null && maxQuantity !== undefined && (
                      maxQuantity === 0 ? (
                        <span style={{ 
                          marginLeft: '8px', 
                          fontSize: '14px', 
                          color: '#ff4d4f',
                          fontWeight: '500'
                        }}>
                          (품절)
                        </span>
                      ) : maxQuantity <= 5 ? (
                        <span style={{ 
                          marginLeft: '8px', 
                          fontSize: '14px', 
                          color: '#fa8c16',
                          fontWeight: '500'
                        }}>
                          (재고: {maxQuantity}개)
                        </span>
                      ) : null
                    )}
                  </div>
                  <QuantitySelect
                    quantity={quantity}
                    setQuantity={setQuantity}
                    maxQuantity={maxQuantity}
                  />
                </div>
                <div>
                  <div className="tf-product-info-by-btn mb_10">
                    <a
                      className="btn-style-2 flex-grow-1 text-btn-uppercase fw-6 show-shopping-cart"
                      onClick={() => {
                        
                        // 실제 옵션이 있는지 확인 (가상 옵션 제외)
                        const realOptions = item.options?.filter(opt => opt.optionNo !== null) || [];
                        
                        if (realOptions.length > 0) {
                          // selectedOptionNo가 없으면 색상과 사이즈로 옵션 찾기
                          let finalOptionNo = selectedOptionNo;
                          if (!finalOptionNo && selectedColor && selectedSize) {
                            const matchingOption = item.options.find(
                              opt => opt.color === selectedColor && opt.size === selectedSize
                            );
                            if (matchingOption) {
                              finalOptionNo = matchingOption.optionNo;
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
                          
                          addProductToCart(item.id, quantity, finalOptionNo, item);
                        } else {
                          // 옵션이 없으면 그냥 추가 (optionNo는 null)
                          addProductToCart(item.id, quantity, null, item);
                        }
                      }}
                    >
                      <span>
                        {isAddedToCartProducts(item.id, selectedOptionNo)
                          ? "장바구니에 추가됨"
                          : "장바구니 담기 -"}
                        &nbsp;
                      </span>
                      <span className="tf-qty-price total-price">
                        {formatKrw(Number(currentPrice || 0) * quantity)}
                      </span>
                    </a>
                  </div>
                  <a href="#" className="btn-style-3 text-btn-uppercase">
                    바로 구매
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
