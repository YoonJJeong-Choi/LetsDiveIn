"use client";
import React, { useState } from "react";
import Image from "next/image";
export default function About() {
  const [activeTab, setActiveTab] = useState(1);
  return (
    <section className="flat-spacing about-us-main pb_0">
      <div className="container">
        <div className="row">
          <div className="col-md-6">
            <div className="about-us-features wow fadeInLeft">
              <Image
                className="lazyload"
                data-src="/images/banner/about-us.jpg"
                alt="image-team"
                src="/images/banner/about-us.jpg"
                width={930}
                height={618}
              />
            </div>
          </div>
          <div className="col-md-6">
            <div className="about-us-content">
              <h3 className="title wow fadeInUp">
                Let’s Dive In - 수영을 더 즐겁게 만드는 스토어
              </h3>
              <div className="widget-tabs style-3">
                <ul className="widget-menu-tab wow fadeInUp">
                  <li
                    className={`item-title ${activeTab == 1 ? "active" : ""} `}
                    onClick={() => setActiveTab(1)}
                  >
                    <span className="inner text-button">소개</span>
                  </li>
                  <li
                    className={`item-title ${activeTab == 2 ? "active" : ""} `}
                    onClick={() => setActiveTab(2)}
                  >
                    <span className="inner text-button">비전</span>
                  </li>
                  <li
                    className={`item-title ${activeTab == 3 ? "active" : ""} `}
                    onClick={() => setActiveTab(3)}
                  >
                    <span className="inner text-button">
                      차별화 포인트
                    </span>
                  </li>
                  <li
                    className={`item-title ${activeTab == 4 ? "active" : ""} `}
                    onClick={() => setActiveTab(4)}
                  >
                    <span className="inner text-button">약속</span>
                  </li>
                </ul>
                <div className="widget-content-tab wow fadeInUp">
                  <div
                    className={`widget-content-inner ${
                      activeTab == 1 ? "active" : ""
                    } `}
                  >
                    <p>
                      Let’s Dive In은 수영을 사랑하는 모두를 위한 전문 스토어입니다.
                      실전 훈련부터 레저 수영까지 필요한 제품을 엄선해 소개하며,
                      정확한 정보와 믿을 수 있는 품질로 고객의 선택을 돕습니다.
                    </p>
                  </div>
                  <div
                    className={`widget-content-inner ${
                      activeTab == 2 ? "active" : ""
                    } `}
                  >
                    <p>
                      누구나 자신의 목표와 스타일에 맞는 수영 장비를 쉽게 찾을 수
                      있도록, 카테고리와 옵션 정보를 체계적으로 제공하는 것이
                      Let’s Dive In의 비전입니다.
                    </p>
                  </div>
                  <div
                    className={`widget-content-inner ${
                      activeTab == 3 ? "active" : ""
                    } `}
                  >
                    <p>
                      파트너 브랜드와의 협업, 상세한 사이즈/옵션 정보, 빠른 검색과
                      추천 기능을 통해 고객이 필요한 제품을 더 정확하고 편리하게
                      찾을 수 있도록 설계했습니다.
                    </p>
                  </div>
                  <div
                    className={`widget-content-inner ${
                      activeTab == 4 ? "active" : ""
                    } `}
                  >
                    <p>
                      Let’s Dive In은 정확한 상품 정보, 안정적인 주문 경험, 신뢰할 수
                      있는 고객 지원을 바탕으로 오래 찾게 되는 수영 전문 쇼핑
                      플랫폼이 되겠습니다.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
