"use client";

import { confirmPayment } from "@/lib/api/payment";
import { Suspense, useCallback, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function PaymentSuccessInner() {
  const sp = useSearchParams();
  const router = useRouter();
  const paymentKey = sp.get("paymentKey");
  const orderId = sp.get("orderId");
  const amount = sp.get("amount");

  const orderNo = (() => {
    const m = orderId?.match(/^ORD-(\d+)-/);
    return m ? m[1] : null;
  })();

  const onConfirm = useCallback(async () => {
    try {
      await confirmPayment({ paymentKey, orderId, amount: Number(amount || 0) });
    } catch (e) {
      // 실패해도 사용자에게 노출하지 않고 주문 확인 페이지로 유도
    }
  }, [paymentKey, orderId, amount]);

  useEffect(() => {
    (async () => {
      await onConfirm().catch(() => {});
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
