"use client";

import { confirmPayment } from "@/lib/api/payment";
import { getCart } from "@/lib/api/cart";
import { useContextElement } from "@/context/Context";
import { Suspense, useCallback, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function PaymentSuccessInner() {
  const sp = useSearchParams();
  const router = useRouter();
  const { setCartProducts } = useContextElement();
  const paymentKey = sp.get("paymentKey");
  const orderId = sp.get("orderId");
  const amount = sp.get("amount");

  const orderNo = (() => {
    const m = orderId?.match(/^ORD-(\d+)-/);
    return m ? m[1] : null;
  })();

  const onConfirm = useCallback(async () => {
    try {
      const result = await confirmPayment({ paymentKey, orderId, amount: Number(amount || 0) });
      if (result && result.approved === false) {
        return { ok: false, result };
      }
    } catch (e) {
      return { ok: false, error: e };
    }
    try {
      const cartData = await getCart();
      const transformedItems = (cartData?.items || []).map((cartItem) => {
        let optionDisplay = null;
        if (cartItem.color || cartItem.size) {
          if (cartItem.color && cartItem.size) {
            optionDisplay = `${cartItem.color} / ${cartItem.size}`;
          } else if (cartItem.color) {
            optionDisplay = cartItem.color;
          } else if (cartItem.size) {
            optionDisplay = cartItem.size;
          }
        }
        return {
          id: cartItem.productNo,
          title: cartItem.productName,
          imgSrc: cartItem.productImageUrl,
          price: cartItem.itemPrice,
          quantity: cartItem.quantity,
          selectedOptionNo: cartItem.optionNo,
          color: cartItem.color,
          size: cartItem.size,
          optionName: optionDisplay,
          cartItemNo: cartItem.cartItemNo,
        };
      });
      setCartProducts(transformedItems);
    } catch (e) {
      // 장바구니 동기화 실패 시 사용자 흐름은 유지
    }
    return { ok: true };
  }, [paymentKey, orderId, amount, setCartProducts]);

  useEffect(() => {
    (async () => {
      const outcome = await onConfirm().catch(() => ({ ok: false }));
      if (!outcome?.ok) {
        const r = outcome?.result;
        const q = new URLSearchParams();
        if (r?.code) q.set("code", String(r.code));
        if (r?.message) q.set("message", String(r.message));
        else q.set("message", "결제 승인에 실패했습니다.");
        if (orderId) q.set("orderId", orderId);
        router.replace(`/payment/fail?${q.toString()}`);
        return;
      }
      if (orderNo) {
        router.replace(`/order-confirmation?orderNo=${orderNo}`);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderNo]);

  return (
    <div className="container py-5" style={{ minHeight: "40vh", display: "flex", alignItems: "center", justifyContent: "center" }}>
      <div className="text-center">
        <div className="spinner-border text-primary" role="status" aria-hidden="true" />
        <p className="mt-3 mb-0">결제를 마무리하는 중입니다...</p>
      </div>
    </div>
  );
}

export default function PaymentSuccess() {
  return (
    <Suspense fallback={
      <div className="container py-5" style={{ minHeight: "40vh", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <div className="text-center">
          <div className="spinner-border text-primary" role="status" aria-hidden="true" />
          <p className="mt-3 mb-0">결제를 마무리하는 중입니다...</p>
        </div>
      </div>
    }>
      <PaymentSuccessInner />
    </Suspense>
  );
}
