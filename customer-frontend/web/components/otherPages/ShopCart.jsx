"use client";
import React, { useEffect, useRef, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useContextElement } from "@/context/Context";
import { getCart, updateCartItem, removeCartItem } from "@/lib/api/cart";
import { getProductDetail } from "@/lib/api/product";
import { getMe, isPortalAccessError } from "@/lib/api/auth";
import { getApplicableSale } from "@/lib/api/sale";
import { startPriceLock } from "@/lib/api/order";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";
import { useBlockQuickCartModal } from "@/hooks/useBlockQuickCartModal";
import { formatKrw } from "@/lib/price/formatKrw";
import { productDisplayImageSrc } from "@/lib/media/productImage";

export default function ShopCart() {
  useBlockQuickCartModal();
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [editingOptionItemNo, setEditingOptionItemNo] = useState(null); // 옵션 편집 중인 아이템
  const [productOptionsMap, setProductOptionsMap] = useState({}); // 상품별 옵션 목록 캐시
  const { cartProducts, setCartProducts, totalPrice } = useContextElement();
  const [selectedCartItemNos, setSelectedCartItemNos] = useState(new Set()); // 선택된 장바구니 아이템 번호들 (cartItemNo 또는 비로그인용 키)
  const [hasInitialized, setHasInitialized] = useState(false); // 초기화 완료 여부
  const [actuallyLoggedIn, setActuallyLoggedIn] = useState(false);
  const [saleAdjustedTotalByCartItemNo, setSaleAdjustedTotalByCartItemNo] = useState({});
  const priceLockStartedRef = useRef(false);

  const redirectToCartLogin = (reason = "login") => {
    const query = reason === "portal"
      ? "/login?reason=portal&next=/shopping-cart"
      : "/login?next=/shopping-cart";
    router.replace(query);
  };

  // 아이템의 고유 키 생성 (로그인: cartItemNo, 비로그인: id + selectedOptionNo)
  const getItemKey = (item) => {
    if (item.cartItemNo) {
      return `cart_${item.cartItemNo}`;
    }
    // 게스트 항목에 guestKey가 있으면 그 키를 우선 사용 (중복 라인 개별 식별)
    if (item.guestKey) {
      return `guestkey_${item.guestKey}`;
    }
    // 비로그인 상태: id + 옵션으로 고유 키 생성 (인덱스 제거로 안정성 향상)
    const optionKey = item.selectedOptionNo || item.optionName || 'no_option';
    return `guest_${item.id}_${optionKey}`;
  };

  // 장바구니 로드 시 모든 아이템 자동 선택 (최초 1회만)
  useEffect(() => {
    if (!hasInitialized && cartProducts.length > 0) {
      const allKeys = cartProducts.map((item) => getItemKey(item));
      if (allKeys.length > 0 && selectedCartItemNos.size === 0) {
        // 선택된 아이템이 없을 때만 자동 선택
        setSelectedCartItemNos(new Set(allKeys));
      }
      setHasInitialized(true);
    }
  }, [cartProducts, hasInitialized, selectedCartItemNos.size]);

  // 장바구니 조회 및 동기화 (로그인 상태에서만)
  useEffect(() => {
    const fetchCart = async () => {
      try {
        setLoading(true);
        
        // 로그인 상태 확인
        const user = await getMe({ throwOnForbidden: true });
        if (!user) {
          setActuallyLoggedIn(false);
          setLoading(false);
          return;
        }
        setActuallyLoggedIn(true);
        
        const cartData = await getCart();
        
        // 백엔드 응답을 프론트 형식으로 변환
        const transformedItems = cartData.items.map((item) => {
          // color와 size를 조합해서 optionName 생성
          let optionDisplay = null;
          if (item.color || item.size) {
            if (item.color && item.size) {
              optionDisplay = `${item.color} / ${item.size}`;
            } else if (item.color) {
              optionDisplay = item.color;
            } else if (item.size) {
              optionDisplay = item.size;
            }
          }
          
          
          return {
            id: item.productNo,
            title: item.productName,
            imgSrc: productDisplayImageSrc(item.productImageUrl),
            price: item.itemPrice,
            quantity: item.quantity,
            selectedOptionNo: item.optionNo,
            color: item.color,
            size: item.size,
            optionName: optionDisplay, // color/size 조합으로 생성
            cartItemNo: item.cartItemNo, // 백엔드 아이템 번호 (수정/삭제용)
          };
        });
        
        setCartProducts(transformedItems);
        
        // 상품별 옵션 목록 미리 로드 (옵션 변경을 위해)
        const productNos = [...new Set(transformedItems.map(item => item.id))];
        const optionsMap = {};
        for (const productNo of productNos) {
          try {
            const productDetail = await getProductDetail(productNo);
            if (productDetail.options && productDetail.options.length > 0) {
              optionsMap[productNo] = productDetail.options;
            }
          } catch (error) {
            console.error(`상품 ${productNo} 옵션 로드 실패:`, error);
          }
        }
        setProductOptionsMap(optionsMap);
      } catch (error) {
        if (isPortalAccessError(error)) {
          setActuallyLoggedIn(false);
          redirectToCartLogin("portal");
          return;
        }
        console.error("장바구니 조회 실패:", error);
        // 에러 발생 시 로컬 장바구니 유지
      } finally {
        setLoading(false);
      }
    };

    fetchCart();
  }, []);

  const setQuantity = async (cartItemNo, productId, currentQuantity, newQuantity) => {
    if (newQuantity < 1) return;
    
    try {
      // 프론트엔드 먼저 업데이트 (Optimistic Update)
      const updatedItems = cartProducts.map((item) => {
        if (item.cartItemNo === cartItemNo) {
          return { ...item, quantity: newQuantity };
        }
        return item;
      });
      setCartProducts(updatedItems);

      // 백엔드 업데이트
      await updateCartItem(cartItemNo, { quantity: newQuantity });
    } catch (error) {
      if (isPortalAccessError(error)) {
        redirectToCartLogin("portal");
        return;
      }
      if (error.response?.status === 401) {
        redirectToCartLogin();
        return;
      }
      console.error("수량 수정 실패:", error);
      // 실패 시 롤백
      const rollbackItems = cartProducts.map((item) => {
        if (item.cartItemNo === cartItemNo) {
          return { ...item, quantity: currentQuantity };
        }
        return item;
      });
      setCartProducts(rollbackItems);
      alert("수량 수정에 실패했습니다.");
    }
  };

  const removeItem = async (cartItemNo, productId, selectedOptionNo, optionName, guestKey) => {
    
    
    // cartItemNo가 null이거나 undefined이면 프론트엔드에서만 삭제
    if (!cartItemNo || cartItemNo === null || cartItemNo === undefined) {
      
      setCartProducts((pre) => pre.filter(item => {
        // guestKey가 전달되면 해당 키로만 정확히 제거
        if (guestKey && item.guestKey) {
          return item.guestKey !== guestKey;
        }
        // guestKey가 없다면 기존 폴백(상품+옵션) 기준
        const sameProduct = item.id === productId;
        const sameOption = (item.selectedOptionNo === selectedOptionNo) || (item.optionName === optionName);
        return !(sameProduct && sameOption);
      }));
      return;
    }
    
    try {
      // 백엔드 삭제 먼저 시도
      await removeCartItem(cartItemNo);
      
      // 삭제 성공 후 장바구니 다시 조회하여 동기화
      const cartData = await getCart();
      
      const transformedItems = cartData.items.map((item) => {
        let optionDisplay = null;
        if (item.color || item.size) {
          if (item.color && item.size) {
            optionDisplay = `${item.color} / ${item.size}`;
          } else if (item.color) {
            optionDisplay = item.color;
          } else if (item.size) {
            optionDisplay = item.size;
          }
        }
        return {
          id: item.productNo,
          title: item.productName,
          imgSrc: productDisplayImageSrc(item.productImageUrl),
          price: item.itemPrice,
          quantity: item.quantity,
          selectedOptionNo: item.optionNo,
          color: item.color,
          size: item.size,
          optionName: optionDisplay,
          cartItemNo: item.cartItemNo,
        };
      });
      setCartProducts(transformedItems);
    } catch (error) {
      if (isPortalAccessError(error)) {
        redirectToCartLogin("portal");
        return;
      }
      if (error.response?.status === 401) {
        redirectToCartLogin();
        return;
      }
      console.error("장바구니 삭제 실패:", error);
      console.error("에러 상세:", error.response?.data || error.message);
      // 실패 시 장바구니 다시 조회하여 원래 상태로 복구
      try {
        const cartData = await getCart();
        const transformedItems = cartData.items.map((item) => {
          let optionDisplay = null;
          if (item.color || item.size) {
            if (item.color && item.size) {
              optionDisplay = `${item.color} / ${item.size}`;
            } else if (item.color) {
              optionDisplay = item.color;
            } else if (item.size) {
              optionDisplay = item.size;
            }
          }
          return {
            id: item.productNo,
            title: item.productName,
            imgSrc: productDisplayImageSrc(item.productImageUrl),
            price: item.itemPrice,
            quantity: item.quantity,
            selectedOptionNo: item.optionNo,
            color: item.color,
            size: item.size,
            optionName: optionDisplay,
            cartItemNo: item.cartItemNo,
          };
        });
        setCartProducts(transformedItems);
        alert("장바구니에서 삭제하는데 실패했습니다: " + (error.response?.data?.message || error.message));
      } catch (fetchError) {
        console.error("장바구니 조회 실패:", fetchError);
        alert("장바구니 삭제 및 조회에 실패했습니다.");
      }
    }
  };

  // 옵션 변경
  const changeOption = async (cartItemNo, productId, newOptionNo) => {
    try {
      // 프론트엔드 먼저 업데이트 (Optimistic Update)
      const item = cartProducts.find(elm => elm.cartItemNo === cartItemNo);
      if (!item) return;

      // 새 옵션 정보 가져오기
      const options = productOptionsMap[productId] || [];
      const newOption = options.find(opt => opt.optionNo === newOptionNo);
      if (!newOption) return;

      // 옵션명 생성
      let optionDisplay = null;
      if (newOption.color || newOption.size) {
        if (newOption.color && newOption.size) {
          optionDisplay = `${newOption.color} / ${newOption.size}`;
        } else if (newOption.color) {
          optionDisplay = newOption.color;
        } else if (newOption.size) {
          optionDisplay = newOption.size;
        }
      }

      // 가격 계산 (기본 가격 + 옵션 추가 가격)
      const basePrice = parseInt(item.price) - (item.selectedOptionNo ? 
        (options.find(opt => opt.optionNo === item.selectedOptionNo)?.optionAddPrice || 0) : 0);
      const newPrice = basePrice + (newOption.optionAddPrice || 0);

      const updatedItems = cartProducts.map((elm) => {
        if (elm.cartItemNo === cartItemNo) {
          return {
            ...elm,
            selectedOptionNo: newOptionNo,
            color: newOption.color,
            size: newOption.size,
            optionName: optionDisplay,
            price: newPrice,
          };
        }
        return elm;
      });
      setCartProducts(updatedItems);
      setEditingOptionItemNo(null);

      // 백엔드 업데이트
      await updateCartItem(cartItemNo, { optionNo: newOptionNo });
    } catch (error) {
      if (isPortalAccessError(error)) {
        redirectToCartLogin("portal");
        return;
      }
      if (error.response?.status === 401) {
        redirectToCartLogin();
        return;
      }
      console.error("옵션 변경 실패:", error);
      // 실패 시 장바구니 다시 조회
      const cartData = await getCart();
      const transformedItems = cartData.items.map((item) => {
        let optionDisplay = null;
        if (item.color || item.size) {
          if (item.color && item.size) {
            optionDisplay = `${item.color} / ${item.size}`;
          } else if (item.color) {
            optionDisplay = item.color;
          } else if (item.size) {
            optionDisplay = item.size;
          }
        }
        return {
          id: item.productNo,
          title: item.productName,
          imgSrc: productDisplayImageSrc(item.productImageUrl),
          price: item.itemPrice,
          quantity: item.quantity,
          selectedOptionNo: item.optionNo,
          color: item.color,
          size: item.size,
          optionName: optionDisplay,
          cartItemNo: item.cartItemNo,
        };
      });
      setCartProducts(transformedItems);
      setEditingOptionItemNo(null);
      alert("옵션 변경에 실패했습니다.");
    }
  };

  const calculateDiscountAmount = (salePolicy, baseTotalPrice) => {
    if (!salePolicy || !salePolicy.discountType || salePolicy.discountValue == null) return 0;
    if (!baseTotalPrice || baseTotalPrice <= 0) return 0;

    const type = salePolicy.discountType;
    const value = Number(salePolicy.discountValue || 0);
    const maxDiscountAmount =
      salePolicy.maxDiscountAmount != null ? Number(salePolicy.maxDiscountAmount) : null;

    let rawDiscount = 0;
    if (type === "PERCENT") {
      rawDiscount = (baseTotalPrice * value) / 100;
    } else {
      rawDiscount = value;
    }
    if (rawDiscount <= 0) return 0;
    if (maxDiscountAmount != null && maxDiscountAmount > 0) {
      rawDiscount = Math.min(rawDiscount, maxDiscountAmount);
    }
    rawDiscount = Math.max(0, rawDiscount);
    return Math.min(rawDiscount, baseTotalPrice);
  };

  useEffect(() => {
    const adjustSales = async () => {
      if (!actuallyLoggedIn) {
        setSaleAdjustedTotalByCartItemNo({});
        return;
      }

      const targetItems = (cartProducts || []).filter((it) => it.cartItemNo != null);
      if (targetItems.length === 0) {
        setSaleAdjustedTotalByCartItemNo({});
        return;
      }

      try {
        if (!priceLockStartedRef.current) {
          await startPriceLock().catch(() => {});
          priceLockStartedRef.current = true;
        }

        const resultEntries = {};
        await Promise.all(
          targetItems.map(async (item) => {
            const productNo = item.id;
            const optionNo = item.selectedOptionNo ?? null;
            const unitPrice = Number(item.price ?? 0);
            const qty = Number(item.quantity ?? 0);
            const baseTotal = unitPrice * qty;

            const saleResult = await getApplicableSale({ productNo, optionNo });
            const salePolicy = saleResult?.data ?? saleResult;
            const discountAmount = calculateDiscountAmount(salePolicy, baseTotal);
            resultEntries[item.cartItemNo] = Math.max(0, baseTotal - discountAmount);
          })
        );
        setSaleAdjustedTotalByCartItemNo(resultEntries);
      } catch (error) {
        setSaleAdjustedTotalByCartItemNo({});
      }
    };

    adjustSales();
  }, [actuallyLoggedIn, cartProducts]);

  if (loading) {
    return (
      <div className="container py-5 d-flex justify-content-center">
        <InlineTemplateLoader />
      </div>
    );
  }

  const selectedItems = cartProducts.filter((item) =>
    selectedCartItemNos.has(getItemKey(item))
  );
  const selectedBaseTotal = selectedItems.reduce(
    (sum, item) => sum + Number(item.price ?? 0) * Number(item.quantity ?? 0),
    0
  );
  const selectedSaleAdjustedTotal = selectedItems.reduce((sum, item) => {
    const baseTotal = Number(item.price ?? 0) * Number(item.quantity ?? 0);
    const adjustedTotal = saleAdjustedTotalByCartItemNo[item.cartItemNo];
    return sum + (adjustedTotal != null ? adjustedTotal : baseTotal);
  }, 0);
  const selectedSaleDiscount = Math.max(0, selectedBaseTotal - selectedSaleAdjustedTotal);

  return (
    <>
      <section className="flat-spacing">
        <div className="container">
          <div className="row">
            <div className="col-xl-8">
              {cartProducts.length ? (
                <form onSubmit={(e) => e.preventDefault()}>
                  <table className="tf-table-page-cart">
                    <thead>
                      <tr>
                        <th>
                          <input
                            type="checkbox"
                            checked={(() => {
                              // 아이템이 없으면 체크 해제
                              if (cartProducts.length === 0) {
                                return false;
                              }
                              
                              // 모든 아이템이 선택되어 있는지 확인
                              return cartProducts.every((item) => 
                                selectedCartItemNos.has(getItemKey(item))
                              );
                            })()}
                            onChange={(e) => {
                              console.log("전체 선택 체크박스 클릭:", e.target.checked);
                              if (e.target.checked) {
                                // 전체 선택: 모든 아이템 선택
                                const allKeys = cartProducts.map((item) => getItemKey(item));
                                console.log("전체 선택할 아이템들:", allKeys);
                                setSelectedCartItemNos(new Set(allKeys));
                              } else {
                                // 전체 해제
                                console.log("전체 해제");
                                setSelectedCartItemNos(new Set());
                              }
                            }}
                            onClick={(e) => {
                              // 이벤트 전파 방지 (필요한 경우)
                              e.stopPropagation();
                            }}
                            style={{ 
                              marginRight: "8px", 
                              cursor: "pointer",
                              width: "18px",
                              height: "18px",
                              position: "relative",
                              zIndex: 10
                            }}
                            aria-label="전체 선택"
                          />
                          상품
                        </th>
                        <th>가격</th>
                        <th>수량</th>
                        <th>합계</th>
                        <th />
                      </tr>
                    </thead>
                    <tbody>
                      {cartProducts.map((elm, i) => {
                        const itemKey = getItemKey(elm);
                        const isSelected = selectedCartItemNos.has(itemKey);
                        return (
                        <tr key={i} className="tf-cart-item file-delete">
                          <td className="tf-cart-item_product" style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                            {/* 체크박스 추가 - 로그인/비로그인 모두 선택 가능 */}
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={(e) => {
                                console.log("개별 체크박스 클릭:", itemKey, e.target.checked, elm.cartItemNo ? "로그인" : "비로그인");
                                const newSelected = new Set(selectedCartItemNos);
                                if (e.target.checked) {
                                  newSelected.add(itemKey);
                                } else {
                                  newSelected.delete(itemKey);
                                }
                                console.log("새로운 선택 상태:", Array.from(newSelected));
                                setSelectedCartItemNos(newSelected);
                              }}
                              onClick={(e) => {
                                // 이벤트 전파 방지 (필요한 경우)
                                e.stopPropagation();
                              }}
                              style={{ 
                                cursor: "pointer",
                                width: "20px",
                                height: "20px",
                                flexShrink: 0,
                                margin: 0
                              }}
                              aria-label={`${elm.title} 선택`}
                            />
                            <Link
                              href={`/product-detail/${elm.id}`}
                              className="img-box"
                              style={{ flexShrink: 0 }}
                            >
                              <Image
                                alt="product"
                                src={productDisplayImageSrc(elm.imgSrc)}
                                width={600}
                                height={800}
                              />
                            </Link>
                            <div className="cart-info">
                              <Link
                                href={`/product-detail/${elm.id}`}
                                className="cart-title link"
                              >
                                {elm.title}
                              </Link>
                              {elm.optionName && (
                              <div className="variant-box">
                                  {editingOptionItemNo === elm.cartItemNo ? (
                                    <div className="option-selector">
                                      <select
                                        className="form-select form-select-sm"
                                        value={elm.selectedOptionNo || ""}
                                        onChange={(e) => {
                                          const newOptionNo = e.target.value ? parseInt(e.target.value) : null;
                                          if (newOptionNo && newOptionNo !== elm.selectedOptionNo) {
                                            changeOption(elm.cartItemNo, elm.id, newOptionNo);
                                          } else {
                                            setEditingOptionItemNo(null);
                                          }
                                        }}
                                        onBlur={() => setEditingOptionItemNo(null)}
                                        autoFocus
                                      >
                                        <option value="">옵션 선택</option>
                                        {(productOptionsMap[elm.id] || []).map((opt) => {
                                          let optDisplay = "";
                                          if (opt.color && opt.size) {
                                            optDisplay = `${opt.color} / ${opt.size}`;
                                          } else if (opt.color) {
                                            optDisplay = opt.color;
                                          } else if (opt.size) {
                                            optDisplay = opt.size;
                                          }
                                          const priceText = opt.optionAddPrice > 0 ? ` (+${formatKrw(opt.optionAddPrice)})` : "";
                                          return (
                                            <option key={opt.optionNo} value={opt.optionNo}>
                                              {optDisplay}{priceText}
                                            </option>
                                          );
                                        })}
                                  </select>
                                </div>
                                  ) : (
                                    <div 
                                      className="text-caption-1 text-secondary"
                                      style={{ cursor: "pointer", textDecoration: "underline" }}
                                      onClick={() => {
                                        // 옵션이 있는 상품만 편집 가능
                                        if (productOptionsMap[elm.id] && productOptionsMap[elm.id].length > 0) {
                                          setEditingOptionItemNo(elm.cartItemNo);
                                        }
                                      }}
                                    >
                                      옵션: {elm.optionName} {productOptionsMap[elm.id] && productOptionsMap[elm.id].length > 0 ? "✏️" : ""}
                                </div>
                                  )}
                              </div>
                              )}
                            </div>
                          </td>
                          <td
                            data-cart-title="가격"
                            className="tf-cart-item_price text-center"
                          >
                            <div className="cart-price text-button price-on-sale">
                              {(() => {
                                const baseUnitPrice = Number(elm.price ?? 0);
                                const adjustedUnitPrice =
                                  saleAdjustedTotalByCartItemNo[elm.cartItemNo] != null
                                    ? Math.floor(
                                        saleAdjustedTotalByCartItemNo[elm.cartItemNo] /
                                          Number(elm.quantity ?? 1)
                                      )
                                    : baseUnitPrice;
                                const hasSale = adjustedUnitPrice < baseUnitPrice;
                                return (
                                  <div className="d-flex flex-column align-items-center">
                                    {hasSale && (
                                      <span
                                        className="text-caption-1 text-secondary"
                                        style={{ textDecoration: "line-through" }}
                                      >
                                        {formatKrw(baseUnitPrice)}
                                      </span>
                                    )}
                                    <span>{formatKrw(adjustedUnitPrice)}</span>
                                  </div>
                                );
                              })()}
                            </div>
                          </td>
                          <td
                            data-cart-title="수량"
                            className="tf-cart-item_quantity"
                          >
                            <div className="wg-quantity mx-md-auto">
                              <span
                                className="btn-quantity btn-decrease"
                                onClick={() =>
                                  setQuantity(elm.cartItemNo, elm.id, elm.quantity, elm.quantity - 1)
                                }
                              >
                                -
                              </span>
                              <input
                                type="text"
                                className="quantity-product"
                                name="number"
                                value={elm.quantity}
                                readOnly
                              />
                              <span
                                className="btn-quantity btn-increase"
                                onClick={() =>
                                  setQuantity(elm.cartItemNo, elm.id, elm.quantity, elm.quantity + 1)
                                }
                              >
                                +
                              </span>
                            </div>
                          </td>
                          <td
                            data-cart-title="합계"
                            className="tf-cart-item_total text-center"
                          >
                            <div className="cart-total text-button total-price">
                              {(() => {
                                const baseTotal = Number(elm.price ?? 0) * Number(elm.quantity ?? 0);
                                const adjustedTotal =
                                  saleAdjustedTotalByCartItemNo[elm.cartItemNo] != null
                                    ? saleAdjustedTotalByCartItemNo[elm.cartItemNo]
                                    : baseTotal;
                                const hasSale = adjustedTotal < baseTotal;
                                return (
                                  <div className="d-flex flex-column align-items-center">
                                    {hasSale && (
                                      <span
                                        className="text-caption-1 text-secondary"
                                        style={{ textDecoration: "line-through" }}
                                      >
                                        {formatKrw(baseTotal)}
                                      </span>
                                    )}
                                    <span>{formatKrw(adjustedTotal)}</span>
                                  </div>
                                );
                              })()}
                            </div>
                          </td>
                          <td
                            data-cart-title="삭제"
                            className="remove-cart"
                            onClick={() => removeItem(elm.cartItemNo, elm.id, elm.selectedOptionNo, elm.optionName, elm.guestKey)}
                          >
                            <span className="remove icon icon-close" />
                          </td>
                        </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </form>
              ) : (
                <div>
                  장바구니가 비어 있습니다. 원하는 상품을 담아보세요.{" "}
                  <Link className="btn-line" href="/shop-default-grid">
                    상품 보러가기
                  </Link>
                </div>
              )}
            </div>
            <div className="col-xl-4">
              <div className="fl-sidebar-cart">
                <div className="box-order bg-surface">
                  <h5 className="title">주문 요약</h5>
                  {selectedSaleDiscount > 0 ? (
                    <>
                      <div className="subtotal text-button d-flex justify-content-between align-items-center">
                        <span>상품 총액</span>
                        <span
                          className="total"
                          style={{ color: "#999", textDecoration: "line-through" }}
                        >
                          {formatKrw(selectedBaseTotal)}
                        </span>
                      </div>
                      <div className="discount text-button d-flex justify-content-between align-items-center">
                        <span>세일 할인</span>
                        <span className="total">-{formatKrw(selectedSaleDiscount)}</span>
                      </div>
                      <div className="discount text-button d-flex justify-content-between align-items-center">
                        <span>세일 적용 금액</span>
                        <span className="total">{formatKrw(selectedSaleAdjustedTotal)}</span>
                      </div>
                    </>
                  ) : (
                    <div className="subtotal text-button d-flex justify-content-between align-items-center">
                      <span>상품 금액</span>
                      <span className="total">{formatKrw(selectedSaleAdjustedTotal)}</span>
                    </div>
                  )}
                  <p className="text-caption-1 text-secondary mt-2 mb-0">
                    전 상품 무료배송
                  </p>
                  <div className="subtotal text-button d-flex justify-content-between align-items-center mt-2">
                    <span>배송비</span>
                    <span className="total">
                      {selectedItems.length === 0 ? "—" : "무료"}
                    </span>
                  </div>
                  {selectedSaleDiscount > 0 && (
                    <h5 className="total-order d-flex justify-content-between align-items-center">
                      <span>최종 결제금액</span>
                      <span className="total">
                        {formatKrw(selectedSaleAdjustedTotal)}
                      </span>
                    </h5>
                  )}
                  <div className="box-progress-checkout">
                    <Link 
                      href={(() => {
                        if (!actuallyLoggedIn) {
                          return "#";
                        }
                        // 로그인 상태: cartItemNo만 필터링하여 전달
                        const cartItemNos = cartProducts
                          .map((item) => {
                            const key = getItemKey(item);
                            if (selectedCartItemNos.has(key) && item.cartItemNo) {
                              return item.cartItemNo;
                            }
                            return null;
                          })
                          .filter(no => no !== null);
                        
                        if (cartItemNos.length > 0) {
                          return `/checkout?cartItemNos=${cartItemNos.join(',')}`;
                        }
                        // 비로그인 상태 또는 선택된 아이템이 없을 때
                        return `/checkout`;
                      })()}
                      className={`tf-btn btn-reset ${selectedCartItemNos.size === 0 ? 'disabled' : ''}`}
                      onClick={(e) => {
                        if (selectedCartItemNos.size === 0) {
                          e.preventDefault();
                          alert("주문할 상품을 선택해주세요.");
                          return;
                        }
                        if (!actuallyLoggedIn) {
                          e.preventDefault();
                          const confirmMessage = "주문을 진행하려면 로그인이 필요합니다.\n\n로그인 페이지로 이동하시겠습니까?";
                          if (confirm(confirmMessage)) {
                            router.push("/login?next=/checkout");
                          }
                        }
                      }}
                    >
                      결제하기 {selectedCartItemNos.size > 0 && `(${selectedCartItemNos.size})`}
                    </Link>
                    <div className="text-center mt-3">
                      <p className="text-caption-1 text-secondary mb-2">또는</p>
                      <button
                        type="button"
                        className="shop-cart-continue-btn d-inline-block"
                        onClick={() => {
                          if (typeof window !== "undefined" && window.history.length > 1) {
                            router.back();
                          } else {
                            router.push("/shop-default-grid");
                          }
                        }}
                      >
                        쇼핑 계속하기
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
