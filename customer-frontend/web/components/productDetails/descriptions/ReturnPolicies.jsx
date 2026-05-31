import React from "react";

export default function ReturnPolicies() {
  return (
    <>
      {" "}
      <div className="text-btn-uppercase mb_12">반품 정책</div>
      <p className="mb_12 text-secondary">
        스윔몰은 상품 품질을 최우선으로 생각합니다. 구매하신 상품에 만족하지
        못하신 경우, 배송 완료일 기준 30일 이내 반품을 지원합니다.
      </p>
      <div className="text-btn-uppercase mb_12">교환/환불 안내</div>
      <ul className="list-text type-disc mb_12 gap-6">
        <li className="text-secondary font-2">
          다른 사이즈, 색상, 스타일로 교환하거나 전액 환불을 받을 수 있습니다.
        </li>
        <li className="text-secondary font-2">
          반품 상품은 미착용 상태여야 하며, 원포장 및 택이 유지되어야 합니다.
        </li>
      </ul>
      <div className="text-btn-uppercase mb_12">반품 절차</div>
      <ul className="list-text type-number">
        <li className="text-secondary font-2">
          마이페이지에서 반품을 접수하거나 고객센터로 문의해 주세요.
        </li>
        <li className="text-secondary font-2">
          상품을 안전하게 포장하고 주문 정보를 함께 동봉해 주세요.
        </li>
        <li className="text-secondary font-2">
          안내된 반품 방법에 따라 상품을 보내 주세요.
        </li>
        <li className="text-secondary font-2">
          물류센터 입고 확인 후 환불이 순차적으로 처리됩니다.
        </li>
      </ul>
      <p className="text-secondary font-2">
        반품 관련 문의사항은 고객센터로 연락해 주세요. 빠르게 도와드리겠습니다.
      </p>
    </>
  );
}
