"use client";

import { Swiper, SwiperSlide } from "swiper/react";
import ProductCard1 from "../productCards/ProductCard1";
import { Pagination } from "swiper/modules";
import { useProductRecommendations } from "@/hooks/useProductRecommendations";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

export default function RecentProducts() {
  const { items, loading } = useProductRecommendations();

  if (loading) {
    return (
      <section className="flat-spacing pt-0">
        <div className="container py-3 d-flex justify-content-center">
          <InlineTemplateLoader />
        </div>
      </section>
    );
  }

  if (items.length === 0) {
    return null;
  }

  return (
    <section className="flat-spacing pt-0">
      <div className="container">
        <div className="heading-section text-center wow fadeInUp">
          <h4 className="heading">이런 상품은 어떠세요?</h4>
        </div>
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
            el: ".spd79",
          }}
        >
          {items.map((product) => (
            <SwiperSlide key={product.id} className="swiper-slide">
              <ProductCard1 product={product} />
            </SwiperSlide>
          ))}
          <div className="sw-pagination-latest sw-dots type-circle justify-content-center spd79" />
        </Swiper>
      </div>
    </section>
  );
}
