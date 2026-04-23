"use client";
import React, { useState, useMemo } from "react";

/**
 * 템플릿 스타일의 페이지네이션 컴포넌트
 * - 외부 제어(currentPage, onChange) 지원
 * - 미제공 시 내부 상태로 동작(레거시 호환)
 */
export default function Pagination({
  totalPages = 1,
  currentPage: controlledPage,
  onChange,
  maxButtons = 5,
}) {
  const [innerPage, setInnerPage] = useState(1);
  const activePage = controlledPage ?? innerPage;

  const handlePageClick = (page) => {
    if (page < 1 || page > totalPages) return;
    if (onChange) onChange(page);
    else setInnerPage(page);
  };

  // 가운데 정렬된 페이지 버튼 계산
  const pages = useMemo(() => {
    const btns = Math.max(1, Math.min(maxButtons, totalPages));
    const half = Math.floor(btns / 2);
    let start = Math.max(1, activePage - half);
    let end = start + btns - 1;
    if (end > totalPages) {
      end = totalPages;
      start = Math.max(1, end - btns + 1);
    }
    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }, [activePage, totalPages, maxButtons]);

  return (
    <>
      <li onClick={() => handlePageClick(activePage - 1)}>
        <a
          className={`pagination-item text-button ${
            activePage === 1 ? "disabled" : ""
          }`}
        >
          <i className="icon-arrLeft" />
        </a>
      </li>
      {pages.map((page) => (
        <li
          key={page}
          className={page === activePage ? "active" : ""}
          onClick={() => handlePageClick(page)}
        >
          <div className="pagination-item text-button">{page}</div>
        </li>
      ))}
      <li onClick={() => handlePageClick(activePage + 1)}>
        <a
          className={`pagination-item text-button ${
            activePage === totalPages ? "disabled" : ""
          }`}
        >
          <i className="icon-arrRight" />
        </a>
      </li>
    </>
  );
}
