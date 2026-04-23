"use client";
import React, { useState, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { getReturnsByCustomer, getReturnStatusLabel } from "@/lib/api/return";

export default function ReturnsList() {
  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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
        <div className="text-center p-4">로딩 중...</div>
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
                              "/images/products/womens/women-1.jpg"
                            }
                            width={80}
                            height={80}
                            style={{ objectFit: "cover", borderRadius: "8px", border: "1px solid #e0e0e0" }}
                            onError={(e) => {
                              e.target.src = "/images/products/womens/women-1.jpg";
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
                          ₩{returnItem.returnAmount?.toLocaleString() || 0}
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
                        {returnItem.orderNo && (
                          <Link
                            href={`/my-account-orders-details?orderNo=${returnItem.orderNo}`}
                            className="btn btn-sm btn-outline-primary"
                            style={{ whiteSpace: "nowrap" }}
                          >
                            주문 상세
                          </Link>
                        )}
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
    </div>
  );
}
