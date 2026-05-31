"use client";
import React, { useEffect, useMemo, useState } from "react";
import { Swiper, SwiperSlide } from "swiper/react";
import { Pagination } from "swiper/modules";
import ProductCard1 from "../productCards/ProductCard1";
import { getRelatedProducts } from "@/lib/api/product";
import {
  pickProductListThumbnail,
  DEFAULT_PRODUCT_PLACEHOLDER,
} from "@/lib/media/productImage";
import { getRecentlyViewed } from "@/lib/recentlyViewed";

export default function RelatedProducts({ product }) {
  const [relatedProducts, setRelatedProducts] = useState([]);
  const [recentProducts, setRecentProducts] = useState([]);

  const preferredColor = useMemo(() => {
    const opts = Array.isArray(product?.options) ? product.options : [];
    const firstColor = opts.find((o) => o?.color)?.color;
    return firstColor || null;
  }, [product]);

  useEffect(() => {
    const productNo = product?.productNo;
    if (!productNo) return;
    let mounted = true;
    getRelatedProducts(productNo, { color: preferredColor, limit: 8 })
      .then((items) => {
        if (!mounted) return;
        const mapped = (items || []).map((item) => {
          const thumb = pickProductListThumbnail(item, DEFAULT_PRODUCT_PLACEHOLDER);
          return {
            ...item,
            id: item.productNo,
            title: item.productName,
            brandName: item.brandName || null,
            imgSrc: thumb,
            imgHover: thumb,
            price: item.minPrice || Number(item.productPrice || 0),
            oldPrice: item.maxPrice && item.maxPrice !== item.minPrice ? item.maxPrice : null,
            colors: Array.from(
              new Set((item.options || []).map((o) => o?.color).filter(Boolean))
            ),
            sizes: Array.from(
              new Set((item.options || []).map((o) => o?.size).filter(Boolean))
            ),
            label: null,
          };
        });
        setRelatedProducts(mapped);
      });
    return () => {
      mounted = false;
    };
  }, [product?.productNo, preferredColor]);

  useEffect(() => {
    const currentNo = product?.productNo ?? product?.id;
    const items = getRecentlyViewed()
      .filter((p) => p.id !== currentNo)
      .slice(0, 8);
    setRecentProducts(items);
  }, [product?.productNo, product?.id]);

  return (
    <section className="flat-spacing">
      <div className="container flat-animate-tab">
        <ul
          className="tab-product justify-content-sm-center wow fadeInUp"
          data-wow-delay="0s"
          role="tablist"
        >
          <li className="nav-tab-item" role="presentation">
            <a href="#ralatedProducts" className="active" data-bs-toggle="tab">
              연관 상품
            </a>
          </li>
          <li className="nav-tab-item" role="presentation">
            <a href="#recentlyViewed" data-bs-toggle="tab">
              최근 본 상품
            </a>
          </li>
        </ul>
        <div className="tab-content">
          <div
            className="tab-pane active show"
            id="ralatedProducts"
            role="tabpanel"
          >
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
              {(relatedProducts || []).map((product, i) => (
                <SwiperSlide key={i} className="swiper-slide">
                  <ProductCard1 product={product} />
                </SwiperSlide>
              ))}

              <div className="sw-pagination-latest spd4  sw-dots type-circle justify-content-center" />
            </Swiper>
          </div>
          <div className="tab-pane" id="recentlyViewed" role="tabpanel">
            {recentProducts.length === 0 ? (
              <p className="text-center text-secondary py-4">최근 본 상품이 없습니다.</p>
            ) : (
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
                  el: ".spd5",
                }}
              >
                {recentProducts.map((p) => (
                  <SwiperSlide key={p.id} className="swiper-slide">
                    <ProductCard1 product={p} />
                  </SwiperSlide>
                ))}
                <div className="sw-pagination-latest spd5 sw-dots type-circle justify-content-center" />
              </Swiper>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
