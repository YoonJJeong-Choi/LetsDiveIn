"use client";

import { useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { getOrder } from "@/lib/api/order";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";
import { formatKrw } from "@/lib/price/formatKrw";
import { productDisplayImageSrc } from "@/lib/media/productImage";

export default function OrderConfirmation() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const orderNo = searchParams.get("orderNo");
  
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!orderNo) {
      setError("주문번호가 없습니다.");
      setLoading(false);
      return;
    }

    const fetchOrder = async () => {
      try {
        setLoading(true);
        const response = await getOrder(Number(orderNo));
        const orderData = response.data || response;
        setOrder(orderData);
      } catch (err) {
        console.error("주문 조회 실패:", err);
        setError("주문 정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchOrder();
  }, [orderNo]);

  // 주문 상태 한글 변환
  const getStatusLabel = (status) => {
    switch (status) {
      case "PENDING_PAYMENT":
        return "결제 대기중";
      case "PAID":
        return "결제 완료";
      case "PAYMENT_FAILED":
        return "결제 실패";
      case "ACTIVE":
        return "주문 진행중";
      case "CANCELLED":
        return "주문 취소";
      default:
        return status;
    }
  };

  const getPaymentMethodLabel = (method) => {
    switch (method) {
      case "CARD":
        return "카드";
      case "VIRTUAL_ACCOUNT":
        return "가상계좌";
      case "TRANSFER":
        return "계좌이체";
      case "MOBILE_PHONE":
        return "휴대폰 결제";
      case "EASY_PAY":
        return "간편결제";
      case "CASH":
        return "현금";
      default:
        return method || "-";
    }
  };

  // 날짜 포맷팅
  const formatDate = (dateString) => {
    if (!dateString) return "-";
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch (e) {
      return dateString;
    }
  };

  if (loading) {
    return (
      <section className="flat-spacing">
        <div className="container py-5 d-flex justify-content-center">
          <InlineTemplateLoader />
        </div>
      </section>
    );
  }

  if (error || !order) {
    return (
      <section className="flat-spacing">
        <div className="container">
          <div className="text-center py-5">
            <p className="text-danger mb-3">{error || "주문 정보를 찾을 수 없습니다."}</p>
            <Link href="/my-account-orders" className="tf-btn btn-fill">
              <span className="text">주문 목록으로</span>
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const isPaymentFailed = order.orderStatus === "PAYMENT_FAILED";

  return (
    <section className="flat-spacing">
      <div className="container">
        <div className="row justify-content-center">
          <div className="col-xl-8">
            {/* 주문 완료 / 결제 실패 헤더 */}
            <div className="text-center mb-5">
              <div className="mb-3">
                {isPaymentFailed ? (
                  <svg
                    width="80"
                    height="80"
                    viewBox="0 0 24 24"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                    style={{ color: "#dc3545" }}
                  >
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" fill="none" />
                    <path
                      d="M15 9l-6 6M9 9l6 6"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                    />
                  </svg>
                ) : (
                  <svg
                    width="80"
                    height="80"
                    viewBox="0 0 24 24"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                    style={{ color: "#28a745" }}
                  >
                    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" fill="none" />
                    <path
                      d="M8 12l2 2 4-4"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                )}
              </div>
              <h2 className="mb-2">
                {isPaymentFailed ? "결제에 실패했습니다" : "주문이 완료되었습니다!"}
              </h2>
              <p className="text-secondary">
                주문번호: <strong>#{order.orderNo}</strong>
              </p>
              <p className="text-secondary">
                주문일시: {formatDate(order.orderCreatedAt)}
              </p>
              {isPaymentFailed && order.paymentFailReason && (
                <p className="text-danger mt-2 mb-0 small">
                  {order.paymentFailReason}
                </p>
              )}
            </div>

            {isPaymentFailed ? (
              <>
                
                <div className="box-order-items bg-surface p-4 mb-4" style={{ borderRadius: "8px" }}>
                  <div className="list-product">
                    {order.orderItems?.map((item, index) => (
                      <div key={index} className="item-product d-flex align-items-center mb-3 pb-3 border-bottom">
                        <div className="img-product me-3">
                          <Image
                            alt={item.productName}
                            src={item.productImageUrl || "/images/banner/about-us.jpg"}
                            width={64}
                            height={64}
                            style={{ objectFit: "cover", borderRadius: "4px" }}
                          />
                        </div>
                        <div className="content-box flex-grow-1">
                          <div className="name-product text-title mb-1">{item.productName}</div>
                          {(item.color || item.size) && (
                            <div className="variant text-caption-1 text-secondary mb-1">
                              {item.size && <span>{item.size}</span>}
                              {item.color && item.size && <span> / </span>}
                              {item.color && <span>{item.color}</span>}
                            </div>
                          )}
                          <div className="total-price text-caption-1 text-secondary">
                            {Number(item.quantity || 0)}개 × {formatKrw(item.itemPrice || 0)}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </>
            ) : (
              <>
                {/* 주문 요약 */}
                <div className="box-order-summary bg-surface p-4 mb-4" style={{ borderRadius: "8px" }}>
                  <h5 className="mb-3">주문 요약</h5>
                  <div className="row">
                    <div className="col-md-6 mb-3">
                      <div className="text-caption-1 text-secondary">주문 상태</div>
                      <div className="text-title fw-6">{getStatusLabel(order.orderStatus)}</div>
                    </div>
                    <div className="col-md-6 mb-3">
                      <div className="text-caption-1 text-secondary">결제 방법</div>
                      <div className="text-title fw-6">
                        {getPaymentMethodLabel(order.paymentMethod)}
                      </div>
                    </div>
                    <div className="col-md-6 mb-3">
                      <div className="text-caption-1 text-secondary">총 주문 금액</div>
                      <div className="text-title fw-6" style={{ color: "#28a745" }}>
                        {formatKrw(order.orderTotalPrice || 0)}
                      </div>
                    </div>
                    <div className="col-md-6 mb-3">
                      <div className="text-caption-1 text-secondary">주문 아이템 수</div>
                      <div className="text-title fw-6">
                        {order.orderItems?.length || 0}개
                      </div>
                    </div>
                  </div>
                </div>

                {/* 배송 정보 */}
                <div className="box-delivery-info bg-surface p-4 mb-4" style={{ borderRadius: "8px" }}>
                  <h5 className="mb-3">배송 정보</h5>
                  <div className="row">
                    <div className="col-md-6 mb-3">
                      <div className="text-caption-1 text-secondary">수령인</div>
                      <div className="text-title">{order.recipientName}</div>
                    </div>
                    <div className="col-md-6 mb-3">
                      <div className="text-caption-1 text-secondary">연락처</div>
                      <div className="text-title">{order.recipientPhone}</div>
                    </div>
                    <div className="col-12">
                      <div className="text-caption-1 text-secondary">배송지</div>
                      <div className="text-title">
                        {order.deliveryAddress}
                        {order.deliveryAddressDetail && ` ${order.deliveryAddressDetail}`}
                        {order.deliveryZipCode && ` (${order.deliveryZipCode})`}
                      </div>
                    </div>
                  </div>
                </div>

                {/* 주문 상품 목록 */}
                <div className="box-order-items bg-surface p-4 mb-4" style={{ borderRadius: "8px" }}>
                  <h5 className="mb-3">주문 상품</h5>
                  <div className="list-product">
                    {order.orderItems?.map((item, index) => (
                      <div key={index} className="item-product d-flex align-items-center mb-3 pb-3 border-bottom">
                        <div className="img-product me-3">
                          <Image
                            alt={item.productName}
                            src={productDisplayImageSrc(item.productImageUrl)}
                            width={80}
                            height={80}
                            style={{ objectFit: "cover", borderRadius: "4px" }}
                          />
                        </div>
                        <div className="content-box flex-grow-1">
                          <div className="name-product text-title mb-1">{item.productName}</div>
                          {(item.color || item.size) && (
                            <div className="variant text-caption-1 text-secondary mb-1">
                              {item.size && <span>{item.size}</span>}
                              {item.color && item.size && <span> / </span>}
                              {item.color && <span>{item.color}</span>}
                            </div>
                          )}
                          <div className="total-price text-button">
                            {(() => {
                              const quantity = Number(item.quantity || 0);
                              const unitPrice = Number(item.itemPrice || 0);
                              const baseTotal = unitPrice * quantity;
                              const paidTotal = Number(item.itemTotalPrice || 0);
                              const hasSale = paidTotal > 0 && paidTotal < baseTotal;
                              return (
                                <div>
                                  <span className="count">{quantity}</span>개 × {formatKrw(unitPrice)}
                                  <span
                                    className="ms-2"
                                    style={hasSale ? { color: "#999", textDecoration: "line-through" } : undefined}
                                  >
                                    = {formatKrw(baseTotal)}
                                  </span>
                                  {hasSale && (
                                    <span className="ms-2 fw-6" style={{ color: "#dc3545" }}>
                                      = {formatKrw(paidTotal)}
                                    </span>
                                  )}
                                </div>
                              );
                            })()}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* 주문 메모 */}
                {order.orderMemo && (
                  <div className="box-order-memo bg-surface p-4 mb-4" style={{ borderRadius: "8px" }}>
                    <h5 className="mb-3">주문 메모</h5>
                    <p className="text-secondary">{order.orderMemo}</p>
                  </div>
                )}

                {/* 안내 메시지 */}
                {order.orderStatus === "PENDING_PAYMENT" && (
                  <div className="alert alert-info mb-4">
                    <strong>결제 대기중</strong>
                    <p className="mb-0 mt-2">
                      결제 승인 후 주문이 완료됩니다. 결제가 완료되면 주문 상태가 업데이트됩니다.
                    </p>
                  </div>
                )}
              </>
            )}

            {/* 액션 버튼 */}
            <div className="d-flex gap-3 justify-content-center flex-wrap">
              <Link href="/my-account-orders" className="tf-btn btn-fill">
                <span className="text">주문 내역 보기</span>
              </Link>
              <Link 
                href="/shop-default-grid" 
                className="tf-btn btn-out-line"
                style={{ 
                  WebkitTextFillColor: "#181818",
                  color: "#181818",
                }}
              >
                <span className="text" style={{ WebkitTextFillColor: "#181818", color: "#181818" }}>쇼핑 계속하기</span>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
