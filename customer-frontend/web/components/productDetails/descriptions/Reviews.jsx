"use client";
import React, { useState, useEffect } from "react";
import Image from "next/image";
import ReviewSorting from "./ReviewSorting";
import { getReviewsByProduct } from "@/lib/api/review";
import Pagination from "@/components/common/Pagination";

export default function Reviews({ productNo }) {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sortBy, setSortBy] = useState("latest"); // latest, rating_high, rating_low
  const [page, setPage] = useState(1);
  const pageSize = 10;

  useEffect(() => {
    const fetchReviews = async () => {
      if (!productNo) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        console.log('[Product Reviews] request params =>', { productNo, page, size: pageSize, sort: sortBy });
        const paged = await getReviewsByProduct(productNo, { page, size: pageSize, sort: sortBy });
        const payload = paged?.data || paged;
        const items = payload?.reviews || [];
        const meta = payload?.meta;
        console.log('[Product Reviews] response meta =>', { page: meta?.page, size: meta?.size, total: meta?.total, itemsCount: Array.isArray(items) ? items.length : 0 });
        setReviews(Array.isArray(items) ? items : []);
        setTotal(Number(meta?.total || 0) || 0);
      } catch (err) {
        console.error("리뷰 목록 조회 실패:", err);
        setError("리뷰 목록을 불러오는데 실패했습니다.");
        setReviews([]);
      } finally {
        setLoading(false);
      }
    };

    fetchReviews();
  }, [productNo, page, pageSize, sortBy]);

  // 평점 통계 계산
  const calculateRatingStats = () => {
    if (!reviews || reviews.length === 0) {
      return {
        average: 0,
        total: 0,
        ratingCounts: { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 },
      };
    }

    const ratingCounts = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    let totalRating = 0;

    reviews.forEach((review) => {
      const rating = review.reviewRating || 0;
      if (rating >= 1 && rating <= 5) {
        ratingCounts[rating]++;
        totalRating += rating;
      }
    });

    const average = reviews.length > 0 ? (totalRating / reviews.length).toFixed(1) : 0;
    const total = reviews.length;

    return { average, total, ratingCounts };
  };

  // 정렬된 리뷰 목록
  const getSortedReviews = () => {
    if (!reviews || reviews.length === 0) return [];

    const sorted = [...reviews];
    switch (sortBy) {
      case "rating_high":
        return sorted.sort((a, b) => (b.reviewRating || 0) - (a.reviewRating || 0));
      case "rating_low":
        return sorted.sort((a, b) => (a.reviewRating || 0) - (b.reviewRating || 0));
      case "latest":
      default:
        return sorted.sort((a, b) => {
          const dateA = new Date(a.reviewCreatedAt || 0);
          const dateB = new Date(b.reviewCreatedAt || 0);
          return dateB - dateA;
        });
    }
  };

  // 날짜 포맷팅
  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffTime = Math.abs(now - date);
      const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

      if (diffDays === 0) return "오늘";
      if (diffDays === 1) return "1일 전";
      if (diffDays < 7) return `${diffDays}일 전`;
      if (diffDays < 30) return `${Math.floor(diffDays / 7)}주 전`;
      if (diffDays < 365) return `${Math.floor(diffDays / 30)}개월 전`;
      return `${Math.floor(diffDays / 365)}년 전`;
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

  const stats = calculateRatingStats();
  const [total, setTotal] = useState(0);
  const sortedReviews = getSortedReviews(); // 서버 정렬과 맞추지만 안전용으로 유지
  const totalPages = Math.max(1, Math.ceil((total || 0) / pageSize));
  const pagedReviews = reviews; // 서버 페이지네이션 결과 사용

  if (loading) {
    return (
      <div className="text-center p-4">
        <p>리뷰를 불러오는 중...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center p-4 text-danger">
        <p>{error}</p>
      </div>
    );
  }

  return (
    <>
      <div className="tab-reviews-heading">
        <div className="top">
          <div className="text-center">
            <div className="number title-display">{stats.average || "0"}</div>
            <div className="list-star">
              {renderStars(Math.round(stats.average))}
            </div>
            <p>({stats.total}개 리뷰)</p>
          </div>
          <div className="rating-score">
            {[5, 4, 3, 2, 1].map((rating) => {
              const count = stats.ratingCounts[rating] || 0;
              const percentage = stats.total > 0 ? (count / stats.total) * 100 : 0;
              return (
                <div key={rating} className="item">
                  <div className="number-1 text-caption-1">{rating}</div>
              <i className="icon icon-star" />
              <div className="line-bg">
                    <div style={{ width: `${percentage}%` }} />
              </div>
                  <div className="number-2 text-caption-1">{count}</div>
            </div>
              );
            })}
          </div>
        </div>
      </div>
      <div className="reply-comment style-1 cancel-review-wrap">
        <div className="d-flex mb_24 gap-20 align-items-center justify-content-between flex-wrap">
          <h4 className="">{stats.total}개 리뷰</h4>
          <div className="d-flex align-items-center gap-12">
            <div className="text-caption-1">정렬:</div>
            <ReviewSorting value={sortBy} onChange={setSortBy} />
          </div>
        </div>
        <div className="reply-comment-wrap">
          {sortedReviews.length === 0 ? (
            <div className="text-center p-4">
              <p className="text-secondary">아직 작성된 리뷰가 없습니다.</p>
            </div>
          ) : (
            pagedReviews.map((review) => (
              <div key={review.reviewNo} className="reply-comment-item">
            <div className="user">
              <div className="image">
                <Image
                      alt={review.customerName || "고객"}
                  src="/images/avatar/user-default.jpg"
                  width={120}
                  height={120}
                />
              </div>
              <div>
                <h6>
                      <span className="link">{review.customerName || "고객"}</span>
                </h6>
                    <div className="d-flex align-items-center gap-2 mb-2">
                      <div className="list-star" style={{ fontSize: "12px" }}>
                        {renderStars(review.reviewRating || 0)}
                      </div>
                    </div>
                <div className="day text-secondary-2 text-caption-1">
                      {formatDate(review.reviewCreatedAt)}
                </div>
              </div>
            </div>
                <p className="text-secondary">{review.reviewContent || ""}</p>
                {Array.isArray(review.imageUrls) && review.imageUrls.length > 0 && (
                  <div className="d-flex gap-8 flex-wrap mt-2">
                    {review.imageUrls.map((url, idx) => (
                      <div key={idx} style={{ width: 96, height: 96, borderRadius: 6, overflow: "hidden", border: "1px solid #eee" }}>
                        {/* next/image 도메인 제약을 피하기 위해 여기서는 img 사용 */}
                        <img
                          src={url}
                          alt={`review-${review.reviewNo}-${idx+1}`}
                          style={{ width: "100%", height: "100%", objectFit: "cover" }}
                          onError={(e) => { e.currentTarget.style.display = 'none'; }}
                        />
                      </div>
                    ))}
                  </div>
                )}
                {review.reviewReply && (
                  <div className="reply-comment-item type-reply mt-3">
            <div className="user">
              <div className="image">
                <Image
                          alt="파트너"
                  src="/images/avatar/user-modave.jpg"
                  width={104}
                  height={104}
                />
              </div>
              <div>
                <h6>
                          <span className="link">판매자 답변</span>
                </h6>
                <div className="day text-secondary-2 text-caption-1">
                          {formatDate(review.reviewReplyCreatedAt)}
                </div>
              </div>
            </div>
                    <p className="text-secondary">{review.reviewReply}</p>
          </div>
                )}
              </div>
            ))
          )}
        </div>
        {sortedReviews.length > 0 && (
          <ul className="wg-pagination md-center mt-4">
            <Pagination
              totalPages={totalPages}
              currentPage={page}
              onChange={(p) => setPage(p)}
            />
          </ul>
        )}
      </div>
      
    </>
  );
}







