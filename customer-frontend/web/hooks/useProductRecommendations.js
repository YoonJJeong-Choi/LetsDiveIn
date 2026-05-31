"use client";

import { useContextElement } from "@/context/Context";
import { useEffect, useState, useMemo } from "react";
import { getActiveProductList } from "@/lib/api/product";
import { mapApiProductToCard } from "@/lib/product/mapApiProductToCard";

export const RECO_SIZE = 8;
const FETCH_SIZE = 48;

/** 판매 중(활성) 상품만, 등록일(신제품) 최신순, 장바구니에 담은 상품 번호는 제외, 상위 8개 */
function sortByNewestFirst(items) {
  return [...items].sort((a, b) => {
    const da = a.productCreatedAt ? new Date(a.productCreatedAt).getTime() : 0;
    const db = b.productCreatedAt ? new Date(b.productCreatedAt).getTime() : 0;
    return db - da;
  });
}

export function useProductRecommendations() {
  const { cartProducts } = useContextElement();
  const inCartNos = useMemo(
    () =>
      new Set(
        (cartProducts || [])
          .map((c) => Number(c.id))
          .filter((n) => !Number.isNaN(n))
      ),
    [cartProducts]
  );
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      try {
        const resp = await getActiveProductList({ page: 1, size: FETCH_SIZE });
        const payload = resp?.data ?? resp;
        const list = Array.isArray(payload?.items) ? payload.items : Array.isArray(resp) ? resp : [];
        const sorted = sortByNewestFirst(list);
        const picked = sorted
          .filter((p) => p?.productNo != null && !inCartNos.has(Number(p.productNo)))
          .slice(0, RECO_SIZE);
        const mapped = await Promise.all(picked.map((p) => mapApiProductToCard(p)));
        if (!cancelled) setItems(mapped);
      } catch {
        if (!cancelled) setItems([]);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => {
      cancelled = true;
    };
  }, [inCartNos]);

  return { items, loading };
}
