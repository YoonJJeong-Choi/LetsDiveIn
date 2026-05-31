"use client";

import { homeCategoryCollections } from "@/data/homeCollections";
import { SHOP_LIST_PATH } from "@/data/navMain";
import { Swiper, SwiperSlide } from "swiper/react";
import Image from "next/image";
import Link from "next/link";
import { Navigation, Pagination } from "swiper/modules";

export default function Collections() {
  return (
    <section className="flat-spacing-2 pb_0">
      <div className="container">
        <div className="heading-section-2 wow fadeInUp">
          <h3>카테고리</h3>
          <Link href={SHOP_LIST_PATH} className="btn-line">
            전체 상품 보기
          </Link>
        </div>
        <div
          className="flat-collection-circle wow fadeInUp"
          data-wow-delay="0.1s"
        >
          <Swiper
            dir="ltr"
            slidesPerView={5}
            spaceBetween={20}
            breakpoints={{
              1200: { slidesPerView: 5, spaceBetween: 20 },
              1000: { slidesPerView: 4, spaceBetween: 20 },
              768: { slidesPerView: 3, spaceBetween: 20 },
              480: { slidesPerView: 2, spaceBetween: 15 },
              0: { slidesPerView: 2, spaceBetween: 15 },
            }}
            modules={[Pagination, Navigation]}
            pagination={{
              clickable: true,
              el: ".spd54",
            }}
            navigation={{
              prevEl: ".snbp12",
              nextEl: ".snbn12",
            }}
          >
            {homeCategoryCollections.map((collection) => (
              <SwiperSlide key={collection.productType}>
                <div className="collection-circle hover-img">
                  <Link
                    href={collection.href}
                    className="img-style"
                    style={{
                      position: "relative",
                      display: "block",
                      width: "100%",
                      aspectRatio: "1 / 1",
                    }}
                  >
                    <Image
                      alt={collection.alt}
                      src={collection.imgSrc}
                      fill
                      sizes="(max-width: 480px) 45vw, (max-width: 768px) 30vw, (max-width: 1200px) 22vw, 18vw"
                      style={{ objectFit: "cover" }}
                    />
                  </Link>
                  <div className="collection-content text-center">
                    <div>
                      <Link href={collection.href} className="cls-title">
                        <h6 className="text">{collection.title}</h6>
                        <i className="icon icon-arrowUpRight" />
                      </Link>
                    </div>
                  </div>
                </div>
              </SwiperSlide>
            ))}
          </Swiper>
          <div className="d-flex d-lg-none sw-pagination-collection sw-dots type-circle justify-content-center spd54" />
          <div className="nav-prev-collection d-none d-lg-flex nav-sw style-line nav-sw-left snbp12">
            <i className="icon icon-arrLeft" />
          </div>
          <div className="nav-next-collection d-none d-lg-flex nav-sw style-line nav-sw-right snbn12">
            <i className="icon icon-arrRight" />
          </div>
        </div>
      </div>
    </section>
  );
}
