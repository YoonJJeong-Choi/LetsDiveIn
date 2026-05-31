"use client";

import { useEffect } from "react";

const SELECTOR = '[data-bs-target="#shoppingCart"], a[href="#shoppingCart"]';

/**
 * 전체 장바구니(ShopCart)·체크아웃 등에서 헤더/플로팅/툴바
 * 퀵 장바구니 모달 열기를 막는다. (Checkout과 동일 방식)
 */
export function useBlockQuickCartModal() {
  useEffect(() => {
    const quickCartLinks = Array.from(document.querySelectorAll(SELECTOR));
    quickCartLinks.forEach((el) => {
      try {
        el.setAttribute("aria-disabled", "true");
        el.style.pointerEvents = "none";
        el.style.opacity = "0.5";
        const onClick = (e) => {
          e.preventDefault();
          e.stopPropagation();
        };
        el.__quickCartBlocker__ = onClick;
        el.addEventListener("click", onClick, true);
      } catch {
        /* noop */
      }
    });

    const disableQuickCart = (e) => {
      const target = e.target && e.target.closest ? e.target.closest(SELECTOR) : null;
      if (!target) return;
      e.preventDefault();
      e.stopPropagation();
    };
    document.addEventListener("click", disableQuickCart, true);

    return () => {
      document.removeEventListener("click", disableQuickCart, true);
      quickCartLinks.forEach((el) => {
        try {
          el.removeAttribute("aria-disabled");
          el.style.pointerEvents = "";
          el.style.opacity = "";
          if (el.__quickCartBlocker__) {
            el.removeEventListener("click", el.__quickCartBlocker__, true);
            delete el.__quickCartBlocker__;
          }
        } catch {
          /* noop */
        }
      });
    };
  }, []);
}
