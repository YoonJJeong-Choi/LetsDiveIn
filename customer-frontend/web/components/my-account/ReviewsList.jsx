"use client";
import React, { useState, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { createReview, getMyReviews, getWritableReviews } from "@/lib/api/review";
import Pagination from "@/components/common/Pagination";

export default function ReviewsList() {
  const [reviews, setReviews] = useState([]);
  const [reviewsTotal, setReviewsTotal] = useState(0);
  const [reviewsPage, setReviewsPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [writableItems, setWritableItems] = useState([]);
  const [writablePage, setWritablePage] = useState(1);
  const [writablePageSize, setWritablePageSize] = useState(10);
  const [writableTotal, setWritableTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState("writable");
  const [selectedItem, setSelectedItem] = useState(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewContent, setReviewContent] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        // 작성한 리뷰: 서버 페이지네이션
        const reviewsData = await getMyReviews({ page: reviewsPage, size: pageSize });
        const payload = reviewsData?.data || reviewsData;
        const myReviews = payload?.reviews || [];
        const meta = payload?.meta;
        console.log('[Customer MyReviews] response meta =>', { page: meta?.page, size: meta?.size, total: meta?.total, itemsCount: Array.isArray(myReviews) ? myReviews.length : 0 });
        const total = meta?.total || 0;
        setReviews(myReviews);
        setReviewsTotal(Number(total) || 0);

        // 작성 가능 목록: 서버 페이지네이션
        const writableResp = await getWritableReviews({ page: writablePage, size: writablePageSize });
        const wPayload = writableResp?.data || writableResp;
        const writableItemsServer = wPayload?.writable || [];
        const wMeta = wPayload?.meta;
        console.log('[Customer WritableReviews] response meta =>', { page: wMeta?.page, size: wMeta?.size, total: wMeta?.total, itemsCount: Array.isArray(writableItemsServer) ? writableItemsServer.length : 0 });
        const wTotal = wMeta?.total || 0;
        setWritableItems(writableItemsServer);
        setWritableTotal(Number(wTotal) || 0);
      } catch (err) {
        console.error("리뷰 데이터 조회 실패:", err);
        setError("리뷰 데이터를 불러오는데 실패했습니다.");
        setReviews([]);
        setReviewsTotal(0);
        setWritableItems([]);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [reviewsPage, pageSize, writablePage, writablePageSize]);

  // 날짜 포맷팅
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

  // 별점 표시
  const renderStars = (rating) => {
    const stars = [];
    for (let i = 1; i <= 5; i++) {
      stars.push(
        <i
          key={i}
          className={`icon ${i <= rating ? "icon-star" : "icon-star-empty"}`}
          style={{ color: i <= rating ? "#ffc107" : "#ccc" }}
        />
      );
    }
    return stars;
  };

  const openWriteModal = (item) => {
    setSelectedItem(item);
    setReviewRating(5);
    setReviewContent("");
    setSubmitError("");
  };

  const closeWriteModal = () => {
    if (submitting) return;
    setSelectedItem(null);
    setSubmitError("");
  };

  const handleSubmitReview = async () => {
    if (!selectedItem) return;
    if (!reviewContent.trim()) {
      setSubmitError("리뷰 내용을 입력해주세요.");
      return;
    }
    if (!reviewRating || reviewRating < 1 || reviewRating > 5) {
      setSubmitError("평점을 선택해주세요.");
      return;
    }

    try {
      setSubmitting(true);
      setSubmitError("");

      await createReview({
        orderItemNo: selectedItem.orderItemNo,
        reviewContent: reviewContent.trim(),
        reviewRating,
      });

      const refreshed = await getMyReviews({ page: reviewsPage, size: pageSize });
      const p = refreshed?.data || refreshed;
      const refreshedReviews = p?.reviews || [];
      const m = p?.meta;
      console.log('[Customer MyReviews][afterCreate] response meta =>', { page: m?.page, size: m?.size, total: m?.total, itemsCount: Array.isArray(refreshedReviews) ? refreshedReviews.length : 0 });
      const total = m?.total || 0;
      setReviews(refreshedReviews);
      setReviewsTotal(Number(total) || 0);
      setWritableItems((prev) =>
        prev.filter((item) => item.orderItemNo !== selectedItem.orderItemNo)
      );
      setActiveTab("written");
      setSelectedItem(null);
    } catch (err) {
      setSubmitError(err?.message || "리뷰 작성에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="my-account-content">
        <div className="text-center p-4">리뷰 목록을 불러오는 중...</div>
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
      <div className="account-reviews">
        <div className="d-flex gap-2 mb-4">
          <button
            className={`tf-btn ${activeTab === "writable" ? "btn-fill" : "btn-outline"}`}
            onClick={() => setActiveTab("writable")}
            type="button"
          >
            리뷰 작성 가능 ({writableTotal})
          </button>
          <button
            className={`tf-btn ${activeTab === "written" ? "btn-fill" : "btn-outline"}`}
            onClick={() => setActiveTab("written")}
            type="button"
          >
            작성한 리뷰 ({reviewsTotal})
          </button>
        </div>

        {activeTab === "writable" ? (
          writableItems.length === 0 ? (
            <div className="text-center py-4">
              <p>리뷰 작성 가능한 주문이 없습니다.</p>
            </div>
          ) : (
            <div className="review-list">
              {writableItems.map((item) => (
                <div key={item.orderItemNo} className="review-item mb-4 p-4 border rounded">
                  <div className="d-flex gap-3">
                    <div className="review-product-image">
                      <Image
                        alt={item.productName || "상품"}
                        src={item.productImageUrl || "/images/products/womens/women-1.jpg"}
                        width={120}
                        height={120}
                        style={{ objectFit: "cover", borderRadius: "8px" }}
                      />
                    </div>
                    <div className="flex-grow-1">
                      <h6 className="mb-1">
                        <Link href={`/product-detail/${item.productNo}`} className="text-decoration-none">
                          {item.productName || "-"}
                        </Link>
                      </h6>
                      {(item.color || item.size) && (
                        <p className="text-muted mb-2" style={{ fontSize: "14px" }}>
                          옵션: {[item.color, item.size].filter(Boolean).join(" / ")}
                        </p>
                      )}
                      <div className="d-flex align-items-center gap-2 mb-2">
                        <span className="badge bg-info" style={{ fontSize: "11px" }}>
                          작성 가능
                        </span>
                        <span className="text-muted" style={{ fontSize: "12px" }}>
                          주문번호 #{item.orderNo}
                        </span>
                      </div>
                      <div className="d-flex gap-2">
                        <button
                          type="button"
                          className="tf-btn btn-fill"
                          onClick={() => openWriteModal(item)}
                        >
                          리뷰 작성
                        </button>
                        <Link
                          href={`/my-account-orders-details?orderNo=${item.orderNo}`}
                          className="tf-btn btn-outline"
                        >
                          주문 상세 보기
                        </Link>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
              {/* 페이지네이션 (작성 가능) */}
              {writableTotal > 0 && (
                <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mt-3">
                  <div className="text-muted small">
                    총 {writableTotal}건 • 페이지 {writablePage}/{Math.max(1, Math.ceil((writableTotal || 0) / writablePageSize))}
                  </div>
                  <div className="d-flex align-items-center gap-3">
                    <ul className="wg-pagination mb-0">
                      <Pagination
                        totalPages={Math.max(1, Math.ceil((writableTotal || 0) / writablePageSize))}
                        currentPage={writablePage}
                        onChange={(p) => setWritablePage(p)}
                        maxButtons={5}
                      />
                    </ul>
                    <select
                      className="form-select form-select-sm"
                      style={{ width: 100 }}
                      value={writablePageSize}
                      onChange={(e) => {
                        setWritablePageSize(Number(e.target.value));
                        setWritablePage(1);
                      }}
                    >
                      <option value={10}>10개씩</option>
                      <option value={20}>20개씩</option>
                      <option value={50}>50개씩</option>
                    </select>
                  </div>
                </div>
              )}
            </div>
          )
        ) : reviews.length === 0 ? (
          <div className="text-center py-4">
            <p>작성한 리뷰가 없습니다.</p>
          </div>
        ) : (
          <div className="review-list">
            {reviews.map((review) => (
              <div key={review.reviewNo} className="review-item mb-4 p-4 border rounded">
                <div className="d-flex gap-3">
                  <div className="review-product-image">
                    <Image
                      alt={review.productName || "상품"}
                      src={review.productImageUrl || "/images/products/womens/women-1.jpg"}
                      width={120}
                      height={120}
                      style={{ objectFit: "cover", borderRadius: "8px" }}
                      onError={(e) => {
                        e.target.src = "/images/products/womens/women-1.jpg";
                      }}
                    />
                  </div>
                  <div className="flex-grow-1">
                    <div className="mb-2">
                      <h6 className="mb-1">
                        <Link
                          href={`/product-detail/${review.productNo}`}
                          className="text-decoration-none"
                        >
                          {review.productName || "-"}
                        </Link>
                      </h6>
                      {(review.color || review.size) && (
                        <p className="text-muted mb-1" style={{ fontSize: "14px" }}>
                          옵션: {[review.color, review.size].filter(Boolean).join(" / ")}
                        </p>
                      )}
                      <div className="d-flex align-items-center gap-2 mb-2">
                        <div className="list-star" style={{ fontSize: "14px" }}>
                          {renderStars(review.reviewRating || 0)}
                        </div>
                        <span className="text-muted" style={{ fontSize: "12px" }}>
                          {formatDate(review.reviewCreatedAt)}
                        </span>
                        <span className="badge bg-secondary" style={{ fontSize: "11px" }}>
                          작성 완료
                        </span>
                      </div>
                    </div>
                    <p className="text-secondary mb-2">{review.reviewContent || ""}</p>
                    {review.reviewReply && (
                      <div className="mt-3 p-3 bg-light rounded">
                        <div className="d-flex align-items-center gap-2 mb-2">
                          <strong style={{ fontSize: "14px" }}>판매자 답변</strong>
                          {review.reviewReplyCreatedAt && (
                            <span className="text-muted" style={{ fontSize: "12px" }}>
                              {formatDate(review.reviewReplyCreatedAt)}
                            </span>
                          )}
                        </div>
                        <p className="text-secondary mb-0" style={{ fontSize: "14px" }}>
                          {review.reviewReply}
                        </p>
                      </div>
                    )}
                    <div className="mt-2">
                      <Link
                        href={`/my-account-orders-details?orderNo=${review.orderNo}`}
                        className="text-decoration-none"
                        style={{ fontSize: "12px" }}
                      >
                        주문 상세 보기 →
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            ))}
            {/* 페이지네이션 (작성한 리뷰) */}
            {reviewsTotal > 0 && (
              <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mt-3">
                <div className="text-muted small">
                  총 {reviewsTotal}건 • 페이지 {reviewsPage}/{Math.max(1, Math.ceil(reviewsTotal / pageSize))}
                </div>
                <div className="d-flex align-items-center gap-3">
                  <ul className="wg-pagination mb-0">
                    <Pagination
                      totalPages={Math.max(1, Math.ceil(reviewsTotal / pageSize))}
                      currentPage={reviewsPage}
                      onChange={(p) => setReviewsPage(p)}
                      maxButtons={5}
                    />
                  </ul>
                  <select
                    className="form-select form-select-sm"
                    style={{ width: 100 }}
                    value={pageSize}
                    onChange={(e) => {
                      setPageSize(Number(e.target.value));
                      setReviewsPage(1);
                    }}
                  >
                    <option value={10}>10개씩</option>
                    <option value={20}>20개씩</option>
                    <option value={50}>50개씩</option>
                  </select>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {selectedItem && (
        <>
          <div
            className="offcanvas-backdrop fade show"
            onClick={closeWriteModal}
            style={{ zIndex: 1040 }}
          />
          <div
            className="position-fixed top-50 start-50 translate-middle bg-white p-4 rounded shadow"
            style={{ width: "min(560px, 92vw)", zIndex: 1050 }}
          >
            <h5 className="mb-2">리뷰 작성</h5>
            <p className="text-muted mb-3" style={{ fontSize: "14px" }}>
              {selectedItem.productName}
            </p>

            <div className="d-flex gap-2 mb-3">
              {[1, 2, 3, 4, 5].map((rating) => (
                <button
                  key={rating}
                  className={`btn ${reviewRating === rating ? "btn-warning" : "btn-outline-warning"}`}
                  onClick={() => setReviewRating(rating)}
                  type="button"
                  disabled={submitting}
                >
                  {rating}
                </button>
              ))}
            </div>

            <textarea
              className="form-control mb-3"
              rows={5}
              placeholder="리뷰 내용을 입력해주세요."
              value={reviewContent}
              onChange={(e) => setReviewContent(e.target.value)}
              disabled={submitting}
            />

            {submitError && (
              <div className="text-danger mb-3" style={{ fontSize: "14px" }}>
                {submitError}
              </div>
            )}

            <div className="d-flex justify-content-end gap-2">
              <button className="btn btn-outline-secondary" onClick={closeWriteModal} type="button" disabled={submitting}>
                취소
              </button>
              <button className="btn btn-primary" onClick={handleSubmitReview} type="button" disabled={submitting}>
                {submitting ? "작성 중..." : "리뷰 등록"}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
