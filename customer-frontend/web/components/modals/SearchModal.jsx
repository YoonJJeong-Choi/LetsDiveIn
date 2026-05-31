"use client";
import React, { useEffect, useMemo, useState } from "react";
import ProductCard1 from "../productCards/ProductCard1";
import { getActiveProductList, searchProducts } from "@/lib/api/product";
import { mapApiProductToCard } from "@/lib/product/mapApiProductToCard";

export default function SearchModal() {
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [products, setProducts] = useState([]);
  const [searched, setSearched] = useState(false);

  const hasKeyword = useMemo(() => keyword.trim().length > 0, [keyword]);

  useEffect(() => {
    let aborted = false;
    const fetchInitial = async () => {
      try {
        setLoading(true);
        const resp = await getActiveProductList({ page: 1, size: 8 });
        const payload = resp?.data || resp;
        const items = Array.isArray(payload?.items) ? payload.items : [];
        const mapped = await Promise.all(items.map(mapApiProductToCard));
        if (!aborted) {
          setProducts(mapped);
          setSearched(false);
        }
      } catch (e) {
        if (!aborted) setProducts([]);
      } finally {
        if (!aborted) setLoading(false);
      }
    };
    fetchInitial();
    return () => {
      aborted = true;
    };
  }, []);

  const handleSearch = async () => {
    const q = keyword.trim();
    if (!q) return;
    try {
      setLoading(true);
      const resp = await searchProducts({ keyword: q, page: 1, size: 12 });
      const payload = resp?.data || resp;
      const items = Array.isArray(payload?.items) ? payload.items : [];
      const mapped = await Promise.all(items.map(mapApiProductToCard));
      setProducts(mapped);
      setSearched(true);
    } catch (e) {
      setProducts([]);
      setSearched(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal fade modal-search" id="search">
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="d-flex justify-content-between align-items-center">
            <h5>검색</h5>
            <span
              className="icon-close icon-close-popup"
              data-bs-dismiss="modal"
            />
          </div>
          <form
            className="form-search"
            onSubmit={(e) => {
              e.preventDefault();
              handleSearch();
            }}
          >
            <fieldset className="text">
              <input
                type="text"
                placeholder="검색어를 입력하세요"
                className=""
                name="text"
                tabIndex={0}
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                aria-required="true"
              />
            </fieldset>
            <button className="" type="submit">
              <svg
                className="icon"
                width={20}
                height={20}
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M11 19C15.4183 19 19 15.4183 19 11C19 6.58172 15.4183 3 11 3C6.58172 3 3 6.58172 3 11C3 15.4183 6.58172 19 11 19Z"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M21.35 21.0004L17 16.6504"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </button>
          </form>
          <div>
            <h5 className="mb_16">오늘의 추천 키워드</h5>
            <ul className="list-tags">
              <li>
                <a href="#" className="radius-60 link">
                  원피스
                </a>
              </li>
              <li>
                <a href="#" className="radius-60 link">
                  여성 수영복
                </a>
              </li>
              <li>
                <a href="#" className="radius-60 link">
                  수영모
                </a>
              </li>
              <li>
                <a href="#" className="radius-60 link">
                  오리발
                </a>
              </li>
            </ul>
          </div>
          <div>
            <h6 className="mb_16">
              {searched ? "검색 결과" : "추천 상품"}
            </h6>
            <div className="tf-grid-layout tf-col-2 lg-col-3 xl-col-4">
              {products.map((product, i) => (
                <ProductCard1 product={product} key={i} />
              ))}
            </div>
            {loading ? <p className="mt-3">검색 중...</p> : null}
            {!loading && searched && hasKeyword && products.length === 0 ? (
              <p className="mt-3">검색 결과가 없습니다.</p>
            ) : null}
          </div>
        </div>
      </div>
    </div>
  );
}
