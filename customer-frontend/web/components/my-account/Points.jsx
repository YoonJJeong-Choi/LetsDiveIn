"use client";
import React, { useState, useEffect } from "react";
import { getPointBalance, getPointHistory } from "@/lib/api/point";
import Pagination from "@/components/common/Pagination";

export default function Points() {
  const [loading, setLoading] = useState(false);
  const [pointBalance, setPointBalance] = useState(null);
  const [pointHistory, setPointHistory] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalItems, setTotalItems] = useState(0);

  useEffect(() => {
    fetchPointData();
  }, [currentPage, pageSize]);

  const fetchPointData = async () => {
    try {
      setLoading(true);
      const [balanceResponse, historyResponse] = await Promise.all([
        getPointBalance(),
        getPointHistory({ page: currentPage, size: pageSize }),
      ]);

      const balanceData = balanceResponse?.data || balanceResponse;
      if (balanceData) {
        setPointBalance(balanceData.pointBalance || 0);
      }

      const payload = historyResponse?.data || historyResponse;
      const items = payload?.pointHistory || [];
      const meta = payload?.meta;
      console.log('[Customer Points] response meta =>', { page: meta?.page, size: meta?.size, total: meta?.total, itemsCount: Array.isArray(items) ? items.length : 0 });
      const total = meta?.total || 0;
      setPointHistory(items);
      setTotalItems(Number(total) || 0);
    } catch (error) {
      console.error("포인트 정보 조회 실패:", error);
      setPointBalance(0);
      setPointHistory([]);
    } finally {
      setLoading(false);
    }
  };

  const formatPoint = (amount) => {
    const value = Number(amount || 0);
    return `${value.toLocaleString("ko-KR")}점`;
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    return date.toLocaleString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const getPointTypeLabel = (pointType) => {
    const typeMap = {
      ACCUMULATE: "적립",
      USE: "사용",
      MANUAL_ADD: "관리자 지급",
      MANUAL_DEDUCT: "관리자 차감",
      EXPIRE: "만료",
    };
    return typeMap[pointType] || pointType;
  };

  const getPointTypeColor = (pointType, pointAmount) => {
    if (pointType === "ACCUMULATE" || pointType === "MANUAL_ADD") {
      return "text-success";
    } else if (pointType === "USE" || pointType === "MANUAL_DEDUCT" || pointType === "EXPIRE") {
      return "text-danger";
    }
    return "";
  };

  const totalPages = Math.max(1, Math.ceil((totalItems || 0) / pageSize));
  const pagedHistory = pointHistory; // 서버 페이지네이션 결과 사용

  const handleChangePage = (page) => {
    if (page < 1 || page > totalPages) return;
    setCurrentPage(page);
  };

  return (
    <div className="my-account-content">
      <div className="my-account-content-header">
        <h5 className="mb-4">포인트</h5>
      </div>

      {loading ? (
        <div className="text-center py-5">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">불러오는 중...</span>
          </div>
        </div>
      ) : (
        <>
          {/* 포인트 잔액 카드 */}
          <div className="card mb-4">
            <div className="card-body">
              <h5 className="card-title mb-2" style={{ fontSize: "18px" }}>현재 포인트 잔액</h5>
              <div className="fw-bold text-primary" style={{ fontSize: "28px", lineHeight: 1.2 }}>
                {formatPoint(pointBalance || 0)}
              </div>
            </div>
          </div>

          {/* 포인트 내역 */}
          <div className="card">
            <div className="card-header">
              <h5 className="mb-0">포인트 내역</h5>
            </div>
            <div className="card-body">
              {pointHistory.length === 0 ? (
                <div className="text-center py-5 text-muted">
                  포인트 내역이 없습니다.
                </div>
              ) : (
                <div className="table-responsive">
                  <table className="table table-hover">
                    <thead>
                      <tr>
                        <th>일시</th>
                        <th>유형</th>
                        <th className="text-end">변동 포인트</th>
                        <th className="text-end">변동 후 잔액</th>
                        <th>설명</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pagedHistory.map((history) => (
                        <tr key={history.historyId}>
                          <td>{formatDate(history.createdAt)}</td>
                          <td>
                            <span className="badge bg-secondary">
                              {getPointTypeLabel(history.pointType)}
                            </span>
                          </td>
                          <td className={`text-end fw-bold ${getPointTypeColor(history.pointType, history.pointAmount)}`}>
                            {history.pointAmount > 0 ? "+" : ""}
                            {formatPoint(history.pointAmount)}
                          </td>
                          <td className="text-end">
                            {formatPoint(history.pointBalanceAfter)}
                          </td>
                          <td>
                            <div>{history.description || "-"}</div>
                            {history.orderNo && (
                              <small className="text-muted">
                                주문번호: {history.orderNo}
                              </small>
                            )}
                            {history.orderItemNo && (
                              <small className="text-muted ms-2">
                                주문상품: {history.orderItemNo}
                              </small>
                            )}
                            {history.expireDate && (
                              <div className="text-muted small mt-1">
                                만료일: {formatDate(history.expireDate)}
                              </div>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {/* Pagination (템플릿 스타일) */}
                  <div className="d-flex flex-wrap align-items-center justify-content-between gap-2">
                    <div className="text-muted small">
                      총 {totalItems}건 • 페이지 {currentPage}/{totalPages}
                    </div>
                    <div className="d-flex align-items-center gap-3">
                      <ul className="wg-pagination mb-0">
                        <Pagination
                          totalPages={totalPages}
                          currentPage={currentPage}
                          onChange={(p) => handleChangePage(p)}
                          maxButtons={5}
                        />
                      </ul>
                      <select
                        className="form-select form-select-sm"
                        style={{ width: 100 }}
                        value={pageSize}
                        onChange={(e) => {
                          const size = Number(e.target.value);
                          setPageSize(size);
                          setCurrentPage(1);
                        }}
                      >
                        <option value={10}>10개씩</option>
                        <option value={20}>20개씩</option>
                        <option value={50}>50개씩</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
