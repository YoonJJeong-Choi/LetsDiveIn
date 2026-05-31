import React from "react";

export default function Shipping() {
  return (
    <>
      <div className="w-100">
        <div className="text-btn-uppercase mb_12">배송 안내</div>
        <p className="mb_12">
          전 상품 무료배송으로 운영됩니다.
        </p>
        <p>
          도서/산간 일부 지역은 배송일이 추가로 소요될 수 있습니다.
        </p>
      </div>
      <div className="w-100">
        <div className="text-btn-uppercase mb_12">출고 일정</div>
        <p>
          결제 완료 후 영업일 기준 1~3일 내 순차 출고됩니다.
        </p>
      </div>
      <div className="w-100">
        <div className="text-btn-uppercase mb_12">고객센터</div>
        <p>배송 관련 상세 문의는 고객센터를 통해 빠르게 안내해 드립니다.</p>
      </div>
    </>
  );
}
