"use client";
import React, { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Pagination } from "swiper/modules";
import { Swiper, SwiperSlide } from "swiper/react";
import ProductCard1 from "../productCards/ProductCard1";
import Link from "next/link";
import { getActiveProductList, searchProducts } from "@/lib/api/product";
import { mapApiProductToCard } from "@/lib/product/mapApiProductToCard";

export default function SearchProducts() {
  const params = useSearchParams();
  const brandFromUrl = params.get("brand") || "";
  const [keyword, setKeyword] = useState("");
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [brandLabel, setBrandLabel] = useState("");

  useEffect(() => {
    let aborted = false;
    const fetchInitial = async () => {
      try {
        setLoading(true);
        if (brandFromUrl) {
          setBrandLabel(brandFromUrl);
          const resp = await searchProducts({ partnerBrandCode: brandFromUrl, page: 1, size: 40 });
          const payload = resp?.data || resp;
          const raw = Array.isArray(payload?.items) ? payload.items : [];
          const mapped = await Promise.all(raw.map(mapApiProductToCard));
          if (!aborted) { setItems(mapped); setSearched(true); }
        } else {
          const resp = await getActiveProductList({ page: 1, size: 12 });
          const payload = resp?.data || resp;
          const raw = Array.isArray(payload?.items) ? payload.items : [];
          const mapped = await Promise.all(raw.map(mapApiProductToCard));
          if (!aborted) setItems(mapped);
        }
      } catch (e) {
        if (!aborted) setItems([]);
      } finally {
        if (!aborted) setLoading(false);
      }
    };
    fetchInitial();
    return () => {
      aborted = true;
    };
  }, [brandFromUrl]);

  const onSearch = async () => {
    const q = keyword.trim();
    if (!q) return;
    try {
      setLoading(true);
      const resp = await searchProducts({ keyword: q, page: 1, size: 12 });
      const payload = resp?.data || resp;
      const raw = Array.isArray(payload?.items) ? payload.items : [];
      const mapped = await Promise.all(raw.map(mapApiProductToCard));
      setItems(mapped);
      setSearched(true);
    } catch (e) {
      setItems([]);
      setSearched(true);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* search */}
      <section className="flat-spacing page-search-inner">
        <div className="container">
          <div className="row justify-content-center">
            <div className="col-xl-6">
              <form
                className="form-search"
                onSubmit={(e) => {
                  e.preventDefault();
                  onSearch();
                }}
              >
                <fieldset className="text">
                  <input
                    type="text"
                    placeholder="상품명, 브랜드 등을 검색하세요"
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
              <div className="tf-col-quicklink">
                <span className="title">빠른 검색:</span>
                <Link className="link" href={`/search-result?brand=SPEEDO`}>
                  SPEEDO
                </Link>
                ,
                <Link className="link" href={`/search-result?brand=ARENA`}>
                  ARENA
                </Link>
                ,
                <Link className="link" href={`/shop-default-grid`}>
                  전체 상품
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>
      {/* /search */}
      {/* Top pick */}
      <section className="flat-spacing pt-0">
        <div className="container">
          <div className="heading-section text-center wow fadeInUp">
            <h3 className="heading">
              {brandLabel ? `${brandLabel}` : searched ? "검색 결과" : "최신 상품"}
            </h3>
          </div>
          {loading ? <p className="text-center">검색 중...</p> : null}
          {!loading && searched && items.length === 0 ? (
            <p className="text-center">검색 결과가 없습니다.</p>
          ) : null}
          <Swiper
            className="swiper tf-sw-latest"
            dir="ltr"
            spaceBetween={15}
            breakpoints={{
              0: { slidesPerView: 2, spaceBetween: 15 },

              768: { slidesPerView: 3, spaceBetween: 30 },
              1200: { slidesPerView: 4, spaceBetween: 30 },
            }}
            modules={[Pagination]}
            pagination={{
              clickable: true,
              el: ".spd4",
            }}
          >
            {items.slice(0, 12).map((product, i) => (
              <SwiperSlide key={i} className="swiper-slide">
                <ProductCard1 product={product} />
              </SwiperSlide>
            ))}

            <div className="sw-pagination-latest spd4  sw-dots type-circle justify-content-center" />
          </Swiper>
        </div>
      </section>
    </>
  );
}
