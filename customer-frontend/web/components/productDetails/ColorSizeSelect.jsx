"use client";

import React, { useEffect, useMemo } from "react";
import { formatKrw } from "@/lib/price/formatKrw";

export default function ColorSizeSelect({ 
  options = [], 
  selectedOptionNo, 
  onSelectOption,
  basePrice,
  onColorSizeChange // 색상/사이즈 변경 시 부모에게 알림
}) {
  const LOW_STOCK_THRESHOLD = 5;

  if (!options || options.length === 0) {
    return null;
  }

  // 고유한 색상 목록 추출
  const uniqueColors = useMemo(() => {
    const colors = options
      .map(opt => opt.color)
      .filter(color => color != null && color !== "");
    return [...new Set(colors)];
  }, [options]);

  // 고유한 사이즈 목록 추출
  const uniqueSizes = useMemo(() => {
    const sizes = options
      .map(opt => opt.size)
      .filter(size => size != null && size !== "");
    return [...new Set(sizes)];
  }, [options]);

  const hasColorDimension = uniqueColors.length > 0;
  const hasSizeDimension = uniqueSizes.length > 0;

  const getAvailableStock = (opt) => {
    const qty = Number(opt?.stockQuantity ?? 0);
    if (opt?.inStock === false || qty <= 0) return 0;
    return qty;
  };

  const selectedOption = options.find(opt => opt.optionNo === selectedOptionNo);
  const [selectedColor, setSelectedColor] = React.useState(selectedOption?.color || null);
  const [selectedSize, setSelectedSize] = React.useState(selectedOption?.size || null);

  // selectedOptionNo가 유효할 때만 색상/사이즈를 동기화
  // (선택 중간 상태에서 onSelectOption(null)이 와도 사용자의 현재 선택 UI는 유지)
  useEffect(() => {
    if (selectedOption) {
      setSelectedColor(selectedOption.color || null);
      setSelectedSize(selectedOption.size || null);
    }
  }, [selectedOptionNo, selectedOption, options]);

  // 옵션 목록이 바뀌어 현재 선택이 더 이상 유효하지 않을 때만 정리
  useEffect(() => {
    if (!options || options.length === 0) {
      setSelectedColor(null);
      setSelectedSize(null);
      return;
    }

    if (selectedColor && !options.some((opt) => opt.color === selectedColor)) {
      setSelectedColor(null);
      setSelectedSize(null);
      return;
    }

    if (
      hasColorDimension &&
      hasSizeDimension &&
      selectedColor &&
      selectedSize &&
      !options.some((opt) => opt.color === selectedColor && opt.size === selectedSize)
    ) {
      setSelectedSize(null);
    }
  }, [options, selectedColor, selectedSize, hasColorDimension, hasSizeDimension]);

  const handleColorSelect = (color) => {
    const hasChangedColor = selectedColor !== color;
    const shouldResetSize = hasSizeDimension && hasChangedColor;

    setSelectedColor(color);
    if (shouldResetSize) {
      // 색상을 다시 고르면 기존 사이즈 선택은 해제
      setSelectedSize(null);
    }
    // 부모에게 색상/사이즈 변경 알림
    if (onColorSizeChange) {
      onColorSizeChange(color, shouldResetSize ? null : selectedSize);
    }
  };

  const handleSizeSelect = (size) => {
  
    setSelectedSize(size);
    // 부모에게 색상/사이즈 변경 알림
    if (onColorSizeChange) {
      onColorSizeChange(selectedColor, size);
    }
  };

  // 상태 변경을 기준으로 옵션 매칭(클릭 1회에 일관 반영)
  useEffect(() => {
    if (!onSelectOption) return;

    if (!hasColorDimension && hasSizeDimension) {
      // 사이즈만 있는 상품
      if (!selectedSize) {
        onSelectOption(null);
        return;
      }
      const matchingOption = options.find((opt) => opt.size === selectedSize);
      onSelectOption(matchingOption?.optionNo ?? null);
      return;
    }

    if (hasColorDimension && !hasSizeDimension) {
      // 컬러만 있는 상품
      if (!selectedColor) {
        onSelectOption(null);
        return;
      }
      const matchingOption = options.find((opt) => opt.color === selectedColor);
      onSelectOption(matchingOption?.optionNo ?? null);
      return;
    }

    // 컬러+사이즈 상품
    if (!selectedColor || !selectedSize) {
      onSelectOption(null);
      return;
    }
    const matchingOption = options.find(
      (opt) => opt.color === selectedColor && opt.size === selectedSize
    );
    onSelectOption(matchingOption?.optionNo ?? null);
  }, [
    selectedColor,
    selectedSize,
    options,
    hasColorDimension,
    hasSizeDimension,
    onSelectOption,
  ]);

  return (
    <>
      {/* 색상 선택 */}
      {uniqueColors.length > 0 && (
        <div className="variant-picker-item">
          <div className="d-flex justify-content-between mb_12">
            <div className="variant-picker-label">
              색상:
              {selectedColor && (
                <span className="text-title variant-picker-label-value">
                  {selectedColor}
                </span>
              )}
            </div>
          </div>
          <div className="variant-picker-values variant-other-size">
            {uniqueColors.map((color) => {
              const isSelected = selectedColor === color;
              
              // 해당 색상으로 선택 가능한 옵션들 찾기
              const colorOptions = options.filter(opt => opt.color === color);

              const availableColorOptions = colorOptions.filter(opt => getAvailableStock(opt) > 0);
              const totalAvailableStock = availableColorOptions.reduce(
                (sum, opt) => sum + getAvailableStock(opt),
                0
              );
              const isColorOutOfStock = totalAvailableStock <= 0;

              // 규칙:
              // 1) 컬러+사이즈 상품: 해당 컬러에서 "남은 사이즈가 1개뿐"이고 그 재고가 1~5개일 때만 컬러에 소량 표시
              // 2) 컬러만 상품: 해당 컬러 총 재고가 1~5개일 때 컬러에 소량 표시
              let lowStockText = null;
              if (!isColorOutOfStock) {
                if (hasSizeDimension) {
                  if (availableColorOptions.length === 1) {
                    const lastSizeStock = getAvailableStock(availableColorOptions[0]);
                    if (lastSizeStock > 0 && lastSizeStock <= LOW_STOCK_THRESHOLD) {
                      lowStockText = `재고 ${lastSizeStock}개 이하`;
                    }
                  }
                } else if (totalAvailableStock <= LOW_STOCK_THRESHOLD) {
                  lowStockText = `재고 ${totalAvailableStock}개 이하`;
                }
              }
              
              return (
                <div
                  key={color}
                  className={`btn-size other-variant-btn ${
                    isSelected ? "active" : ""
                  } ${isColorOutOfStock ? "out-of-stock" : ""}`}
                  onClick={() => {
                    if (!isColorOutOfStock) {
                      handleColorSelect(color);
                    }
                  }}
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    minWidth: "80px",
                    padding: "12px 16px",
                    opacity: isColorOutOfStock ? 0.5 : 1,
                    cursor: isColorOutOfStock ? 'not-allowed' : 'pointer',
                    position: 'relative'
                  }}
                  aria-disabled={isColorOutOfStock ? "true" : "false"}
                  title={isColorOutOfStock ? "이 색상은 모든 사이즈가 품절입니다." : undefined}
                >
                  <span style={{ 
                    fontSize: "16px", 
                    lineHeight: "24px", 
                    fontWeight: "500",
                    whiteSpace: "nowrap"
                  }}>
                    {color}
                    {isColorOutOfStock && (
                      <span style={{ 
                        fontSize: "10px", 
                        color: "#ff4d4f", 
                        marginLeft: "4px",
                        fontWeight: "600"
                      }}>
                        (품절)
                      </span>
                    )}
                  </span>
                  {lowStockText && (
                    <span 
                      style={{ 
                        fontSize: "11px",
                        color: "#fa8c16",
                        fontWeight: "500",
                        marginTop: "2px",
                        lineHeight: "14px"
                      }}
                    >
                      {lowStockText}
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* 사이즈 선택 */}
      {uniqueSizes.length > 0 && (
        <div className="variant-picker-item">
          <div className="d-flex justify-content-between mb_12">
            <div className="variant-picker-label">
              사이즈:
              {selectedSize && (
                <span className="text-title variant-picker-label-value">
                  {selectedSize}
                </span>
              )}
            </div>
          </div>
          <div className="variant-picker-values variant-other-size">
            {uniqueSizes.map((size, sizeIdx) => {
              const isSelected = selectedSize === size;
              
              // 선택된 색상과 사이즈 조합의 옵션 찾기
              const matchingOption = selectedColor 
                ? options.find(opt => opt.color === selectedColor && opt.size === size)
                : options.find(opt => opt.size === size);
              
              // 해당 사이즈로 선택 가능한 옵션들 찾기
              const sizeOptions = selectedColor
                ? options.filter(opt => opt.color === selectedColor && opt.size === size)
                : options.filter(opt => opt.size === size);
              
              // 선택 전(색상 미선택)에는 사이즈를 항상 활성화
              let isOutOfStock = false;
              let showStockInfo = false;
              let minStockForSize = null;
              if (hasColorDimension) {
                if (selectedColor) {
                  // 컬러 선택 후에만 해당 컬러 기준 재고 안내 노출
                  const stockQuantities = sizeOptions
                    .map(opt => getAvailableStock(opt))
                    .filter(qty => qty > 0);
                  minStockForSize = stockQuantities.length > 0 ? Math.min(...stockQuantities) : null;
                  isOutOfStock = stockQuantities.length === 0;
                  showStockInfo = minStockForSize !== null && minStockForSize <= LOW_STOCK_THRESHOLD;
                }
              } else {
                // 사이즈만 있는 상품은 사이즈 기준으로 항상 재고 표시
                const stockQuantities = sizeOptions
                  .map(opt => getAvailableStock(opt))
                  .filter(qty => qty > 0);
                minStockForSize = stockQuantities.length > 0 ? Math.min(...stockQuantities) : null;
                isOutOfStock = stockQuantities.length === 0;
                showStockInfo = minStockForSize !== null && minStockForSize <= LOW_STOCK_THRESHOLD;
              }
              
              const optionPrice = matchingOption?.optionAddPrice || 0;
              
              return (
                <div
                  key={`size-${sizeIdx}-${String(size)}`}
                  className={`btn-size other-variant-btn ${
                    isSelected ? "active" : ""
                  } ${isOutOfStock ? "out-of-stock" : ""}`}
                  onClick={() => {
                    if (!isOutOfStock) {
                      handleSizeSelect(size);
                    }
                  }}
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    minWidth: "80px",
                    padding: "12px 16px",
                    opacity: isOutOfStock ? 0.5 : 1,
                    cursor: isOutOfStock ? 'not-allowed' : 'pointer',
                    position: 'relative'
                  }}
                  aria-disabled={isOutOfStock ? "true" : "false"}
                  title={isOutOfStock ? "선택한 색상에서 이 사이즈는 품절입니다." : undefined}
                >
                  <span style={{ 
                    fontSize: "16px", 
                    lineHeight: "24px", 
                    fontWeight: "500",
                    whiteSpace: "nowrap"
                  }}>
                    {size}
                    {isOutOfStock && (
                      <span style={{ 
                        fontSize: "10px", 
                        color: "#ff4d4f", 
                        marginLeft: "4px",
                        fontWeight: "600"
                      }}>
                        (품절)
                      </span>
                    )}
                  </span>
                  {optionPrice > 0 && (
                    <span 
                      style={{ 
                        fontSize: "12px",
                        color: "#888",
                        fontWeight: "400",
                        marginTop: "4px",
                        lineHeight: "16px"
                      }}
                    >
                      +{formatKrw(optionPrice)}
                    </span>
                  )}
                  {showStockInfo && (
                    <span 
                      style={{ 
                        fontSize: "11px",
                        color: minStockForSize <= 3 ? "#fa8c16" : "#52c41a",
                        fontWeight: "500",
                        marginTop: "2px",
                        lineHeight: "14px"
                      }}
                    >
                      재고 {minStockForSize}개 이하
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* 선택된 옵션 정보 표시 */}
      {selectedOption && selectedColor && selectedSize && (
        <div className="variant-picker-item">
          <div className="d-flex justify-content-between mb_12">
            <div className="variant-picker-label">
              선택된 옵션:
              <span className="text-title variant-picker-label-value">
                {selectedColor} / {selectedSize}
              </span>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
