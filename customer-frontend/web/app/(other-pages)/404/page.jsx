import Image from "next/image";
import React from "react";
import Link from "next/link";
export const metadata = {
  title: "페이지를 찾을 수 없습니다 · Let’s Dive In",
  description:
    "요청하신 주소의 페이지를 찾을 수 없습니다. 주소가 맞는지 확인해 주세요.",
};

export default function PageNotFoundPage() {
  return (
    <section className="flat-spacing page-404">
      <div className="container">
        <div className="page-404-inner">
          <div className="image">
            <Image
              className="lazyload"
              data-src="/images/logo/LetsDiveIn01.png"
              alt="Let's Dive In 로고"
              src="/images/logo/LetsDiveIn01.png"
              width={400}
              height={400}
              style={{ width: "auto", height: "auto", maxWidth: "100%", objectFit: "contain" }}
            />
          </div>
          <div className="content">
            <div className="heading">404</div>
            <div>
              <h2 className="title mb_4">페이지를 찾을 수 없습니다</h2>
              <div className="text body-text-1 text-secondary">
                주소가 잘못 입력되었거나, 삭제·이동된 페이지일 수 있습니다.
                입력하신 주소를 다시 확인해 주세요.
              </div>
            </div>
            <Link href={`/`} className="tf-btn btn-fill">
              <span className="text text-button">홈으로 이동</span>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
