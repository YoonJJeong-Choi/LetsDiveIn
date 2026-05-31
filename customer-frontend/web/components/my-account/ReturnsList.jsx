"use client";
import React, { useState, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { getReturnsByCustomer, getReturnStatusLabel } from "@/lib/api/return";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";
import { formatKrw } from "@/lib/price/formatKrw";

export default function ReturnsList() {
  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedReturn, setSelectedReturn] = useState(null);

  useEffect(() => {
    const fetchReturns = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await getReturnsByCustomer();
        const returnsData = response.data || response || [];
        setReturns(Array.isArray(returnsData) ? returnsData : []);
      } catch (err) {
        console.error("반품 목록 조회 실패:", err);
        setError(err.message || "반품 목록을 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchReturns();
  }, []);

  // 날짜 포맷팅
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

  // 반품 상태에 따른 배지 색상
  const getStatusBadgeClass = (status) => {
    if (!status) return "bg-secondary";
    switch (status) {
      case "REFUNDED":
        return "bg-success";
      case "REJECTED":
        return "bg-danger";
      case "APPROVED":
      case "PICKUP_COMPLETED":
        return "bg-info";
      case "REQUESTED":
        return "bg-warning";
      default:
        return "bg-secondary";
    }
  };

  if (loading) {
    return (
      <div className="my-account-content">
        <div className="p-4 d-flex justify-content-center">
          <InlineTemplateLoader />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="my-account-content">
        <div className="text-center p-4 text-danger">{error}</div>
      </div>
    );
  }

  return (
    <div className="my-account-content">
      <div className="account-order">
        <h5 className="mb-4">반품 목록</h5>
        {returns.length === 0 ? (
          <div className="text-center p-4">
            <p className="text-2">반품 내역이 없습니다.</p>
          </div>
        ) : (
          <div className="row g-3">
            {returns.map((returnItem) => (
              <div key={returnItem.returnNo} className="col-12">
                <div className="card" style={{ border: "1px solid #e0e0e0", borderRadius: "8px" }}>
                  <div className="card-body p-4">
                    <div className="row align-items-center">
                      {/* 상품 이미지 및 정보 */}
                      <div className="col-md-4">
                        <div className="d-flex align-items-center gap-3">
                          <Image
                            alt={returnItem.productName || "상품"}
                            src={
                              returnItem.productImageUrl ||
                              "/images/products/cap01.png"
                            }
                            width={80}
                            height={80}
                            style={{ objectFit: "cover", borderRadius: "8px", border: "1px solid #e0e0e0" }}
                            onError={(e) => {
                              e.target.src = "/images/products/cap01.png";
                            }}
                          />
                          <div>
                            <div className="fw-6 mb-1">{returnItem.productName}</div>
                            {(returnItem.color || returnItem.size) && (
                              <div className="text-muted" style={{ fontSize: "13px", marginBottom: "4px" }}>
                                {[returnItem.color, returnItem.size]
                                  .filter(Boolean)
                                  .join(" / ")}
                              </div>
                            )}
                            <div className="text-muted" style={{ fontSize: "13px" }}>
                              수량: {returnItem.quantity}개
                            </div>
                          </div>
                        </div>
                      </div>
                      
                      {/* 반품 상태 */}
                      <div className="col-md-2 text-center">
                        <div className="mb-2">
                          <span
                            className={`badge ${getStatusBadgeClass(
                              returnItem.returnStatus
                            )}`}
                            style={{ fontSize: "13px", padding: "6px 12px" }}
                          >
                            {getReturnStatusLabel(returnItem.returnStatus)}
                          </span>
                        </div>
                        {returnItem.returnStatus === "REJECTED" &&
                          returnItem.rejectionReason && (
                            <div className="text-danger" style={{ fontSize: "12px", marginTop: "8px" }}>
                              <strong>거절 사유:</strong>
                              <div style={{ marginTop: "4px" }}>{returnItem.rejectionReason}</div>
                            </div>
                          )}
                        {returnItem.returnTrackingNumber && (
                          <div className="text-muted mt-2" style={{ fontSize: "12px" }}>
                            <div>반품 송장: {returnItem.returnTrackingNumber}</div>
                            {returnItem.returnCourier && (
                              <div>{returnItem.returnCourier}</div>
                            )}
                          </div>
                        )}
                      </div>
                      
                      {/* 반품 금액 */}
                      <div className="col-md-2 text-center">
                        <div className="text-muted" style={{ fontSize: "12px", marginBottom: "4px" }}>
                          반품 금액
                        </div>
                        <div className="fw-6" style={{ fontSize: "18px", color: "#333" }}>
                          {formatKrw(returnItem.returnAmount || 0)}
                        </div>
                      </div>
                      
                      {/* 신청일 */}
                      <div className="col-md-2 text-center">
                        <div className="text-muted" style={{ fontSize: "12px", marginBottom: "4px" }}>
                          신청일
                        </div>
                        <div style={{ fontSize: "13px" }}>
                          {formatDate(returnItem.returnRequestedAt)}
                        </div>
                      </div>
                      
                      {/* 주문 상세 링크 */}
                      <div className="col-md-2 text-center">
                        <div className="d-flex flex-column gap-2 align-items-center">
                        {returnItem.orderNo && (
                          <Link
                            href={`/my-account-orders-details?orderNo=${returnItem.orderNo}`}
                            className="btn btn-sm btn-outline-primary"
                            style={{ whiteSpace: "nowrap" }}
                          >
                            주문 상세
                          </Link>
                        )}
                          <button
                            type="button"
                            className="btn btn-sm btn-outline-secondary"
                            style={{ whiteSpace: "nowrap" }}
                            onClick={() => setSelectedReturn(returnItem)}
                          >
                            반품 상세
                          </button>
                        </div>
                      </div>
                    </div>
                    
                    {/* 반품 사유 (있는 경우) */}
                    {returnItem.returnReason && (
                      <div className="mt-3 pt-3 border-top">
                        <div className="text-muted" style={{ fontSize: "12px", marginBottom: "4px" }}>
                          반품 사유
                        </div>
                        <div style={{ fontSize: "13px" }}>{returnItem.returnReason}</div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {selectedReturn && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(0, 0, 0, 0.5)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
          onClick={() => setSelectedReturn(null)}
        >
          <div
            style={{
              backgroundColor: "#fff",
              borderRadius: "8px",
              width: "min(640px, 92vw)",
              maxHeight: "80vh",
              overflowY: "auto",
              padding: "24px",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="d-flex align-items-center justify-content-between mb-3">
              <h5 className="mb-0">반품 상세</h5>
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                onClick={() => setSelectedReturn(null)}
              >
                닫기
              </button>
            </div>

            <div className="row g-3">
              <div className="col-md-6">
                <div className="text-muted" style={{ fontSize: "12px" }}>반품번호</div>
                <div className="fw-6">#{selectedReturn.returnNo}</div>
              </div>
              <div className="col-md-6">
                <div className="text-muted" style={{ fontSize: "12px" }}>상태</div>
                <span
                  className={`badge ${getStatusBadgeClass(selectedReturn.returnStatus)}`}
                  style={{ fontSize: "13px", padding: "6px 12px" }}
                >
                  {getReturnStatusLabel(selectedReturn.returnStatus)}
                </span>
              </div>
              <div className="col-md-6">
                <div className="text-muted" style={{ fontSize: "12px" }}>상품명</div>
                <div>{selectedReturn.productName || "-"}</div>
              </div>
              <div className="col-md-6">
                <div className="text-muted" style={{ fontSize: "12px" }}>옵션</div>
                <div>{[selectedReturn.color, selectedReturn.size].filter(Boolean).join(" / ") || "-"}</div>
              </div>
              <div className="col-md-6">
                <div className="text-muted" style={{ fontSize: "12px" }}>수량</div>
                <div>{selectedReturn.quantity || 0}개</div>
              </div>
              <div className="col-md-6">
                <div className="text-muted" style={{ fontSize: "12px" }}>반품 금액</div>
                <div>{formatKrw(selectedReturn.returnAmount || 0)}</div>
              </div>
              <div className="col-md-6">
                <div className="text-muted" style={{ fontSize: "12px" }}>신청일</div>
                <div>{formatDate(selectedReturn.returnRequestedAt)}</div>
              </div>
              <div className="col-md-6">
                <div className="text-muted" style={{ fontSize: "12px" }}>주문번호</div>
                <div>{selectedReturn.orderNo ? `#${selectedReturn.orderNo}` : "-"}</div>
              </div>
              <div className="col-12">
                <div className="text-muted" style={{ fontSize: "12px" }}>반품 사유</div>
                <div>{selectedReturn.returnReason || "-"}</div>
              </div>
              {selectedReturn.rejectionReason && (
                <div className="col-12">
                  <div className="text-muted" style={{ fontSize: "12px" }}>거절 사유</div>
                  <div className="text-danger">{selectedReturn.rejectionReason}</div>
                </div>
              )}
              {selectedReturn.returnTrackingNumber && (
                <div className="col-12">
                  <div className="text-muted" style={{ fontSize: "12px" }}>반품 송장</div>
                  <div>
                    {selectedReturn.returnTrackingNumber}
                    {selectedReturn.returnCourier ? ` (${selectedReturn.returnCourier})` : ""}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
