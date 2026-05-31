"use client";
import React, { useEffect, useState, useMemo } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";
import { useContextElement } from "@/context/Context";
import { useProductRecommendations } from "@/hooks/useProductRecommendations";
import { removeCartItem, getCart } from "@/lib/api/cart";
import { getApplicableSale } from "@/lib/api/sale";
import { formatKrw } from "@/lib/price/formatKrw";
import { productDisplayImageSrc } from "@/lib/media/productImage";

export default function CartModal() {
  const router = useRouter();
  const { cartProducts, setCartProducts, isLoggedIn } = useContextElement();

  const { items: recoItems, loading: recoLoading } = useProductRecommendations();

  /** Bootstrap 모달과 Next Link가 충돌하지 않도록: 닫은 뒤 클라이언트 라우팅 */
  const goRecoProductDetail = (e, productId) => {
    e.preventDefault();
    try {
      const bootstrap = require("bootstrap");
      const el = document.getElementById("shoppingCart");
      if (el) {
        const inst = bootstrap.Modal.getInstance(el);
        inst?.hide();
      }
    } catch {
      /* noop */
    }
    router.push(`/product-detail/${productId}`);
  };

  /** 장바구니 페이지 결제하기와 동일: cartItemNo가 모두 있으면 쿼리로 전달(옵션·라인 구분), 아니면 /checkout에서 동기화 */
  const checkoutHref = useMemo(() => {
    const items = cartProducts || [];
    if (items.length === 0) return "/shopping-cart";
    const allHaveCartItemNo = items.every(
      (i) =>
        i.cartItemNo != null &&
        i.cartItemNo !== "" &&
        !Number.isNaN(Number(i.cartItemNo))
    );
    if (allHaveCartItemNo) {
      const nos = items.map((i) => Number(i.cartItemNo)).filter((n) => !Number.isNaN(n));
      if (nos.length > 0) {
        return `/checkout?cartItemNos=${nos.join(",")}`;
      }
    }
    return "/checkout";
  }, [cartProducts]);

  const goCheckoutFromModal = (e) => {
    e.preventDefault();
    try {
      const bootstrap = require("bootstrap");
      const el = document.getElementById("shoppingCart");
      if (el) {
        const inst = bootstrap.Modal.getInstance(el);
        inst?.hide();
      }
    } catch {
      /* noop */
    }
    router.push(checkoutHref);
  };

  const goContinueShoppingFromModal = (e) => {
    e.preventDefault();
    try {
      const bootstrap = require("bootstrap");
      const el = document.getElementById("shoppingCart");
      if (el) {
        const inst = bootstrap.Modal.getInstance(el);
        inst?.hide();
      }
    } catch {
      /* noop */
    }
    router.push("/shop-default-grid");
  };

  const removeItem = async (cartItemNo, productId) => {
    console.log("CartModal 삭제 시도:", { cartItemNo, productId, cartItemNoType: typeof cartItemNo });
    
    // 비로그인 상태거나 cartItemNo가 없으면 프론트엔드에서만 삭제
    if (!isLoggedIn || !cartItemNo) {
      console.warn("CartModal: cartItemNo가 없어서 프론트엔드에서만 삭제합니다.");
      setCartProducts((pre) =>
        pre.filter((elm) => {
          if (cartItemNo && elm.cartItemNo) {
            return elm.cartItemNo !== cartItemNo;
          }
          return elm.id != productId;
        })
      );
      return;
    }
    
    // cartItemNo를 숫자로 변환
    const cartItemNoNum = typeof cartItemNo === 'string' ? parseInt(cartItemNo) : cartItemNo;
    if (isNaN(cartItemNoNum)) {
      console.error("CartModal: cartItemNo가 유효한 숫자가 아닙니다:", cartItemNo);
      alert("장바구니 아이템 번호가 유효하지 않습니다.");
      return;
    }
    
    try {
      // 백엔드 삭제 먼저 시도
      console.log("CartModal 백엔드 삭제 API 호출 시작, cartItemNo:", cartItemNoNum);
      const response = await removeCartItem(cartItemNoNum);
      console.log("CartModal 백엔드 삭제 성공, 응답:", response);
      
      // 삭제 성공 후 장바구니 다시 조회하여 동기화
      console.log("CartModal 장바구니 재조회 시작");
      const cartData = await getCart();
      console.log("CartModal 장바구니 재조회 성공:", cartData);
      
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
      console.log("CartModal 변환된 장바구니 아이템:", transformedItems);
      setCartProducts(transformedItems);
    } catch (error) {
      console.error("CartModal 장바구니 삭제 실패:", error);
      console.error("CartModal 에러 상세:", error.response?.data || error.message);
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
        console.error("CartModal 장바구니 조회 실패:", fetchError);
        alert("장바구니 삭제 및 조회에 실패했습니다.");
      }
    }
  };

  const [saleAdjustedTotalByCartItemNo, setSaleAdjustedTotalByCartItemNo] = useState({});

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
    return Math.min(baseTotalPrice, Math.max(0, rawDiscount));
  };

  useEffect(() => {
    const adjustSales = async () => {
      const targetItems = (cartProducts || []).filter((it) => it.cartItemNo != null);
      if (targetItems.length === 0) {
        setSaleAdjustedTotalByCartItemNo({});
        return;
      }

      try {
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
      } catch (e) {
        setSaleAdjustedTotalByCartItemNo({});
      }
    };

    adjustSales();
  }, [cartProducts]);

  const baseSubtotal = (cartProducts || []).reduce(
    (sum, item) => sum + Number(item.price ?? 0) * Number(item.quantity ?? 0),
    0
  );
  const saleAdjustedSubtotal = (cartProducts || []).reduce((sum, item) => {
    const baseTotal = Number(item.price ?? 0) * Number(item.quantity ?? 0);
    const adjusted = saleAdjustedTotalByCartItemNo[item.cartItemNo];
    return sum + (adjusted != null ? adjusted : baseTotal);
  }, 0);
  const saleDiscountAmount = Math.max(0, baseSubtotal - saleAdjustedSubtotal);

  return (
    <div className="modal fullRight fade modal-shopping-cart" id="shoppingCart">
      <div className="modal-dialog">
        <div className="modal-content">
          {(recoLoading || recoItems.length > 0) && (
            <div className="tf-minicart-recommendations">
              <h6 className="title">이런 상품은 어떠세요?</h6>
              <div className="wrap-recommendations">
                {recoLoading ? (
                  <div className="py-2 d-flex justify-content-center">
                    <InlineTemplateLoader />
                  </div>
                ) : (
                  <div className="list-cart">
                    {recoItems.map((product) => (
                      <div className="list-cart-item" key={product.id}>
                        <div className="image">
                          <Image
                            className="lazyload"
                            data-src={product.imgSrc}
                            alt={product.title || ""}
                            src={productDisplayImageSrc(product.imgSrc)}
                            width={600}
                            height={800}
                          />
                        </div>
                        <div className="content">
                          <div className="name">
                            <Link
                              className="link text-line-clamp-1"
                              href={`/product-detail/${product.id}`}
                              onClick={(e) => goRecoProductDetail(e, product.id)}
                            >
                              {product.title}
                            </Link>
                          </div>
                          <div className="cart-item-bot">
                            <div className="text-button price">
                              {formatKrw(product.price)}
                            </div>
                            <Link
                              className="link text-button"
                              href={`/product-detail/${product.id}`}
                              onClick={(e) => goRecoProductDetail(e, product.id)}
                            >
                              상세 보기
                            </Link>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
          <div className="d-flex flex-column flex-grow-1 h-100">
            <div className="header">
              <h5 className="title">장바구니</h5>
              <span
                className="icon-close icon-close-popup"
                data-bs-dismiss="modal"
              />
            </div>
            <div className="wrap">
              <div className="tf-mini-cart-threshold">
                <div className="tf-progress-bar">
                  <div
                    className="value"
                    style={{ width: "0%" }}
                    data-progress={75}
                  >
                    <i className="icon icon-shipping" />
                  </div>
                </div>
                <div className="text-caption-1">
                  전 상품 무료배송
                </div>
              </div>
              <div className="tf-mini-cart-wrap">
                <div className="tf-mini-cart-main">
                  <div className="tf-mini-cart-sroll">
                    {cartProducts.length ? (
                      <div className="tf-mini-cart-items">
                        {cartProducts.map((product, i) => (
                          <div
                            key={i}
                            className="tf-mini-cart-item file-delete"
                          >
                            <div className="tf-mini-cart-image">
                              <Image
                                className="lazyload"
                                alt=""
                                src={productDisplayImageSrc(product.imgSrc)}
                                width={600}
                                height={800}
                              />
                            </div>
                            <div className="tf-mini-cart-info flex-grow-1">
                              <div className="mb_12 d-flex align-items-center justify-content-between flex-wrap gap-12">
                                <div className="text-title">
                                  <Link
                                    href={`/product-detail/${product.id}`}
                                    className="link text-line-clamp-1"
                                  >
                                    {product.title}
                                  </Link>
                                </div>
                                <div
                                  className="text-button tf-btn-remove remove"
                                  onClick={() => removeItem(product.cartItemNo, product.id)}
                                >
                                  삭제
                                </div>
                              </div>
                              <div className="d-flex align-items-center justify-content-between flex-wrap gap-12">
                                <div className="text-secondary-2">
                                  {product.optionName || (product.color && product.size ? `${product.color} / ${product.size}` : product.color || product.size || "")}
                                </div>
                                <div className="text-button">
                                  {product.quantity} X{" "}
                                  {formatKrw(
                                    (saleAdjustedTotalByCartItemNo[product.cartItemNo] != null
                                      ? saleAdjustedTotalByCartItemNo[product.cartItemNo]
                                      : Number(product.price ?? 0) * Number(product.quantity ?? 0)) /
                                    Math.max(1, Number(product.quantity ?? 1))
                                  )}
                                </div>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="p-4">
                        장바구니가 비어 있어요. 마음에 드는 상품을 담아보세요.{" "}
                        <Link className="btn-line" href="/shop-default-grid">
                          상품 둘러보기
                        </Link>
                      </div>
                    )}
                  </div>
                </div>
                <div className="tf-mini-cart-bottom">
                  <div className="tf-mini-cart-bottom-wrap">
                    <div className="tf-cart-totals-discounts">
                      <h5>상품 합계</h5>
                      <h5 className="tf-totals-total-value">
                        {formatKrw(saleAdjustedSubtotal)}
                      </h5>
                    </div>
                    {saleDiscountAmount > 0 && (
                      <div className="tf-cart-totals-discounts">
                        <h6 style={{ color: "#777" }}>세일 할인</h6>
                        <h6 style={{ color: "#d64545" }}>-{formatKrw(saleDiscountAmount)}</h6>
                      </div>
                    )}
                    <div className="tf-mini-cart-view-checkout">
                      <Link
                        href={`/shopping-cart`}
                        className="tf-btn w-100 btn-white radius-4 has-border"
                      >
                        <span className="text">장바구니 보기</span>
                      </Link>
                      <Link
                        href={checkoutHref}
                        className="tf-btn w-100 btn-fill radius-4"
                        onClick={goCheckoutFromModal}
                      >
                        <span className="text">주문·결제</span>
                      </Link>
                    </div>
                    <div className="text-center">
                      <Link
                        className="link text-btn-uppercase"
                        href="/shop-default-grid"
                        onClick={goContinueShoppingFromModal}
                      >
                        쇼핑 계속하기
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
