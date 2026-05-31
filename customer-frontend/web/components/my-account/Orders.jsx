"use client";
import React, { useState, useEffect } from "react";
import Link from "next/link";
import { getOrders } from "@/lib/api/order";
import Pagination from "@/components/common/Pagination";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";
import { formatKrw } from "@/lib/price/formatKrw";

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(1);
  const pageSize = 10;

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        setLoading(true);
        const res = await getOrders({ page, size: pageSize });
        const payload = res?.data || res;
        const items = payload?.orders || payload?.items || (payload?.data?.items || []);
        const meta = payload?.meta || {
          page: payload?.page,
          size: payload?.size,
          total: payload?.total,
        };
        console.log('[Customer Orders] response meta =>', { page: meta?.page, size: meta?.size, total: meta?.total, itemsCount: Array.isArray(items) ? items.length : 0 });
        setOrders(Array.isArray(items) ? items : []);
        setTotal(Number(meta?.total ?? (Array.isArray(items) ? items.length : 0)) || 0);
      } catch (err) {
        console.error("주문 목록 조회 실패:", err);
        setError("주문 목록을 불러오는데 실패했습니다.");
        setOrders([]);
        setTotal(0);
      } finally {
        setLoading(false);
      }
    };
    fetchOrders();
  }, [page]);

  const [total, setTotal] = useState(0);
  const totalPages = Math.max(1, Math.ceil((total || 0) / pageSize));
  const pagedOrders = orders;

  const getReturnStatus = (order) => {
    if (!order.orderItems || !Array.isArray(order.orderItems)) return null;
    
    const itemsWithReturn = order.orderItems.filter(item => 
      item.status === "REFUNDED" || 
      item.status === "RETURN_REJECTED" || 
      item.status === "RETURN_IN_PROGRESS"
    );
    if (itemsWithReturn.length === 0) return null;
    
    const hasRefunded = itemsWithReturn.some(item => item.status === "REFUNDED");
    const hasRejected = itemsWithReturn.some(item => item.status === "RETURN_REJECTED");
    const hasInProgress = itemsWithReturn.some(item => item.status === "RETURN_IN_PROGRESS");
    
    if (hasRefunded) return "REFUNDED";
    if (hasRejected) return "REJECTED";
    if (hasInProgress) return "IN_PROGRESS";
    
    return null;
  };

  const isAllItemsCompleted = (order) => {
    if (!order || !order.orderItems || !Array.isArray(order.orderItems)) return false;
    if (order.orderItems.length === 0) return false;
    
    return order.orderItems.every(item => item.status === "COMPLETED");
  };

  const getStatusLabel = (order) => {
    if (order?.orderDisplayStatusLabel) {
      return order.orderDisplayStatusLabel;
    }

    const status = order.orderStatus;
    
    if (status === "CANCELLED") {
      return "주문 취소";
    }
    
    if (!order.orderItems || !Array.isArray(order.orderItems) || order.orderItems.length === 0) {
      switch (status) {
        case "PENDING_PAYMENT":
          return "결제 대기중";
        case "PAID":
          return "발주 확인하는 중";
        case "PAYMENT_FAILED":
          return "결제 실패";
        case "ACTIVE":
          return "주문 진행중";
        default:
          return status;
      }
    }
    
    const statusCounts = {
      COMPLETED: 0,
      REFUNDED: 0,
      RETURN_REJECTED: 0,
      RETURN_IN_PROGRESS: 0,
      DELIVERED: 0,
      SHIPPED: 0,
      READY: 0,
      CONFIRMED: 0,
      PENDING_CONFIRMATION: 0,
      CANCELLED: 0
    };
    
    order.orderItems.forEach(item => {
      if (item.status) {
        statusCounts[item.status] = (statusCounts[item.status] || 0) + 1;
      }
    });
    
    const totalItems = order.orderItems.length;
    
    if (statusCounts.RETURN_IN_PROGRESS > 0) {
      return "반품 진행 중";
    }
    
    const completedCount = statusCounts.COMPLETED + statusCounts.REFUNDED;
    if (completedCount === totalItems) {
      if (statusCounts.COMPLETED === totalItems) {
        return "구매 확정";
      } else if (statusCounts.REFUNDED === totalItems) {
        return "환불 완료";
      } else {
        return "완료";
      }
    }
    
    if (completedCount > 0) {
      return "주문 진행중";
    }
    
    if (statusCounts.COMPLETED + statusCounts.DELIVERED === totalItems) {
      if (statusCounts.COMPLETED > 0) {
        return "일부 구매 확정";
      }
      return "배송 완료";
    }
    
    switch (status) {
      case "PENDING_PAYMENT":
        return "결제 대기중";
      case "PAID":
        return "발주 확인하는 중";
      case "PAYMENT_FAILED":
        return "결제 실패";
      case "ACTIVE":
        return "주문 진행중";
      default:
        return status;
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
      });
    } catch (e) {
      return dateString;
    }
  };

  const getItemCount = (order) => {
    if (!order.orderItems || !Array.isArray(order.orderItems)) return 0;
    return order.orderItems.reduce((sum, item) => sum + (item.quantity || 0), 0);
  };

  if (loading) {
    return (
      <div className="my-account-content">
        <div className="account-orders">
          <div className="wrap-account-order py-4 d-flex justify-content-center">
            <InlineTemplateLoader />
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="my-account-content">
        <div className="account-orders">
          <div className="wrap-account-order">
            <p className="text-center py-4 text-danger">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="my-account-content">
      <div className="account-orders">
        <div className="wrap-account-order">
          {orders.length === 0 ? (
            <div className="text-center py-4">
              <p>주문 내역이 없습니다.</p>
              <Link href="/shop-default-grid" className="tf-btn btn-fill radius-4 mt-3">
                <span className="text">쇼핑하러 가기</span>
              </Link>
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th className="fw-6">주문번호</th>
                  <th className="fw-6">주문일</th>
                  <th className="fw-6">상태</th>
                  <th className="fw-6">총액</th>
                  <th className="fw-6">상세보기</th>
                </tr>
              </thead>
              <tbody>
                {pagedOrders.map((order) => {
                  const returnStatus = getReturnStatus(order);
                  const statusLabel = getStatusLabel(order);
                  
                  let statusStyle = {};
                  if (returnStatus === "REFUNDED") {
                    statusStyle = { color: "#28a745", fontWeight: "600" };
                  } else if (returnStatus === "REJECTED") {
                    statusStyle = { color: "#dc3545", fontWeight: "600" };
                  } else if (returnStatus === "IN_PROGRESS") {
                    statusStyle = { color: "#ffc107", fontWeight: "600" };
                  } else if (statusLabel === "구매 확정") {
                    statusStyle = { color: "#6f42c1", fontWeight: "600" };
                  }
                  
                  return (
                    <tr key={order.orderNo} className="tf-order-item">
                      <td>#{order.orderNo}</td>
                      <td>{formatDate(order.orderCreatedAt)}</td>
                      <td>
                        <span style={statusStyle}>
                          {statusLabel}
                        </span>
                      </td>
                      <td>
                        {formatKrw(order.orderTotalPrice || 0)} ({getItemCount(order)}개)
                      </td>
                      <td>
                        <Link
                          href={`/my-account-orders-details?orderNo=${order.orderNo}`}
                          className="tf-btn btn-fill radius-4"
                        >
                          <span className="text">상세보기</span>
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
        {total > 0 && (
          <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 my-2">
            <div className="text-muted small">총 {total}건 • 페이지 {page}/{totalPages}</div>
          </div>
        )}
        {total > 0 && (
          <ul className="wg-pagination md-center">
            <Pagination
              totalPages={totalPages}
              currentPage={page}
              onChange={(p) => setPage(p)}
            />
          </ul>
        )}
      </div>
    </div>
  );
}
