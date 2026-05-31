"use client";

/**
 * 템플릿과 동일한 원형 스피너(spin 키프레임은 전역 SCSS). 문구 없이 아이콘만 표시.
 * 결제·주문 제출 버튼 등은 별도 처리(Checkout 등).
 */
export default function InlineTemplateLoader({ className = "" }) {
  return (
    <div
      className={`d-flex justify-content-center align-items-center ${className}`}
      role="status"
      aria-label="로딩 중"
    >
      <span
        className="d-inline-block rounded-circle"
        style={{
          width: 28,
          height: 28,
          border: "2px solid var(--line, #e8e8e8)",
          borderTopColor: "var(--primary, #181818)",
          animation: "spin 1s linear infinite",
        }}
      />
    </div>
  );
}
