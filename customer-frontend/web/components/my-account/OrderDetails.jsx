"use client";
import React, { useState, useEffect } from "react";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { getOrder, completeOrder, completeOrderItem, cancelOrder } from "@/lib/api/order";
import { getDeliveriesByOrderNo } from "@/lib/api/delivery";
import { requestReturn } from "@/lib/api/return";
import { createReview, getMyReviews } from "@/lib/api/review";
import { uploadFile } from "@/lib/api/file";

export default function OrderDetails() {
  const [activeTab, setActiveTab] = useState(1);
  const [order, setOrder] = useState(null);
  const [deliveries, setDeliveries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [completing, setCompleting] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [returning, setReturning] = useState(false);
  const [returnModalVisible, setReturnModalVisible] = useState(false);
  const [selectedOrderItem, setSelectedOrderItem] = useState(null);
  const [returnReasonType, setReturnReasonType] = useState("CHANGE_OF_MIND");
  const [returnReason, setReturnReason] = useState("");
  const [returnImages, setReturnImages] = useState([]); // {fileId, fileUrl, name}[]
  const [reviewModalVisible, setReviewModalVisible] = useState(false);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewContent, setReviewContent] = useState("");
  const [reviewImages, setReviewImages] = useState([]); // {fileId, fileUrl, name}[]
  const [reviewing, setReviewing] = useState(false);
  const [myReviews, setMyReviews] = useState([]);
  const searchParams = useSearchParams();
  const orderNo = searchParams?.get("orderNo");

  useEffect(() => {
    const fetchOrderAndDeliveries = async () => {
      if (!orderNo) {
        setError("주문 번호가 없습니다.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        
        // 주문 정보 조회
        const orderData = await getOrder(parseInt(orderNo));
        setOrder(orderData.data || orderData);
        
        // 배송 정보 조회
        try {
          const deliveryData = await getDeliveriesByOrderNo(parseInt(orderNo));
          setDeliveries(deliveryData.data || deliveryData || []);
        } catch (deliveryError) {
          // 배송이 없을 수도 있음 (결제 전 등)
          console.log("배송 정보 없음:", deliveryError);
          setDeliveries([]);
        }
        
        // 내 리뷰 목록 조회 (리뷰 작성 여부 확인용)
        try {
          const reviewsData = await getMyReviews({ page: 1, size: 9999 });
          const list = reviewsData?.items || reviewsData?.data?.items || [];
          setMyReviews(Array.isArray(list) ? list : []);
        } catch (reviewError) {
          console.log("리뷰 목록 조회 실패:", reviewError);
          setMyReviews([]);
        }
      } catch (err) {
        console.error("주문 정보 조회 실패:", err);
        setError(err.message || "주문 정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchOrderAndDeliveries();
  }, [orderNo]);

  // 배송 상태 한글 변환
  const getDeliveryStatusLabel = (status) => {
    switch (status) {
      case "READY":
        return "배송 준비";
      case "SHIPPED":
        return "배송 중";
      case "DELIVERED":
        return "배송 완료";
      default:
        return status;
    }
  };

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

  // 주문에 반품 상태 확인 (status 필드 기반)
  const getReturnStatus = () => {
    if (!order || !order.orderItems || !Array.isArray(order.orderItems)) return null;
    
    // status 필드를 기반으로 반품 상태 확인
    const itemsWithReturn = order.orderItems.filter(item => 
      item.status === "REFUNDED" || 
      item.status === "RETURN_REJECTED" || 
      item.status === "RETURN_IN_PROGRESS"
    );
    if (itemsWithReturn.length === 0) return null;
    
    // 우선순위: REFUNDED > RETURN_REJECTED > RETURN_IN_PROGRESS
    const hasRefunded = itemsWithReturn.some(item => item.status === "REFUNDED");
    const hasRejected = itemsWithReturn.some(item => item.status === "RETURN_REJECTED");
    const hasInProgress = itemsWithReturn.some(item => item.status === "RETURN_IN_PROGRESS");
    
    if (hasRefunded) return "REFUNDED";
    if (hasRejected) return "REJECTED";
    if (hasInProgress) return "IN_PROGRESS";
    
    return null;
  };

  // 모든 주문 상품이 구매 확정되었는지 확인 (status 필드 기반)
  const isAllItemsCompleted = () => {
    if (!order || !order.orderItems || !Array.isArray(order.orderItems)) return false;
    if (order.orderItems.length === 0) return false;
    
    // 모든 주문 상품이 구매 확정되었는지 확인 (status 필드 사용)
    return order.orderItems.every(item => item.status === "COMPLETED");
  };

  // 주문 상태 한글 변환 (혼합 상태 처리)
  const getOrderStatusLabel = (status) => {
    // 취소된 주문은 항상 "주문 취소"
    if (status === "CANCELLED") {
      return "주문 취소";
    }
    
    if (!order.orderItems || !Array.isArray(order.orderItems) || order.orderItems.length === 0) {
      // 주문 상품이 없으면 기본 주문 상태 반환
      switch (status) {
        case "PENDING_PAYMENT":
          return "결제 대기";
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
    
    // 각 상품의 상태 집계
    const statusCounts = {
      COMPLETED: 0,      // 구매 확정
      REFUNDED: 0,       // 환불 완료
      RETURN_REJECTED: 0, // 반품 거절
      RETURN_IN_PROGRESS: 0, // 반품 진행 중
      DELIVERED: 0,      // 배송 완료
      SHIPPED: 0,        // 배송 중
      READY: 0,          // 배송 준비
      CONFIRMED: 0,      // 발주 확인됨
      PENDING_CONFIRMATION: 0, // 발주 확인 대기
      CANCELLED: 0       // 취소됨
    };
    
    order.orderItems.forEach(item => {
      if (item.status) {
        statusCounts[item.status] = (statusCounts[item.status] || 0) + 1;
      }
    });
    
    const totalItems = order.orderItems.length;
    
    // 혼합 상태 처리 우선순위 (간단하게)
    // 1. 반품 진행 중인 상품이 있는 경우 (최우선 - 진행 중인 작업이 있으면 표시)
    if (statusCounts.RETURN_IN_PROGRESS > 0) {
      return "반품 진행 중";
    }
    
    // 2. 모든 상품이 완료된 경우 (구매 확정 또는 환불 완료)
    const completedCount = statusCounts.COMPLETED + statusCounts.REFUNDED;
    if (completedCount === totalItems) {
      // 모두 구매 확정이면 "구매 확정", 모두 환불 완료면 "환불 완료", 혼합이면 "완료"
      if (statusCounts.COMPLETED === totalItems) {
        return "구매 확정";
      } else if (statusCounts.REFUNDED === totalItems) {
        return "환불 완료";
      } else {
        return "완료"; // 구매 확정 + 환불 완료 혼합
      }
    }
    
    // 3. 일부 완료된 경우 (진행 중인 작업이 없고 일부만 완료)
    if (completedCount > 0) {
      return "주문 진행중"; // 상세는 각 상품별로 표시됨
    }
    
    // 7. 모든 상품이 구매 확정 또는 배송 완료인 경우
    if (statusCounts.COMPLETED + statusCounts.DELIVERED === totalItems) {
      if (statusCounts.COMPLETED > 0) {
        return "일부 구매 확정";
      }
      return "배송 완료";
    }
    
    // 8. 기본 주문 상태 반환
    switch (status) {
      case "PENDING_PAYMENT":
        return "결제 대기";
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

  // 모든 배송이 완료되었는지 확인
  const isAllDeliveriesCompleted = () => {
    if (!deliveries || deliveries.length === 0) return false;
    return deliveries.every(
      (delivery) => delivery.deliveryStatus === "DELIVERED"
    );
  };

  // 주문 상품별 구매 확정 가능 여부 확인 (status 필드 기반)
  const canCompleteOrderItem = (orderItem) => {
    if (!order || !orderItem) return false;
    
    // status 필드를 기반으로 구매 확정 가능 여부 확인
    // DELIVERED 상태이고, 반품 진행 중이 아니고, 구매 확정되지 않은 경우만 가능
    return orderItem.status === "DELIVERED";
  };

  // 주문 취소 가능 여부 확인 (status 필드 기반)
  const canCancelOrder = () => {
    if (!order || !order.orderItems) return false;
    const orderStatus = order.orderStatus;
    
    // 취소된 주문은 취소 불가
    if (orderStatus === "CANCELLED") {
      return false;
    }
    
    // 발주 확인된 주문 상품이 있으면 취소 불가 (status 필드 사용)
    const hasConfirmedItem = order.orderItems.some(item => 
      item.status === "CONFIRMED" || 
      item.status === "READY" || 
      item.status === "SHIPPED" || 
      item.status === "DELIVERED" || 
      item.status === "COMPLETED"
    );
    if (hasConfirmedItem) {
      return false;
    }
    
    // 구매 확정된 주문 상품이 있으면 취소 불가
    const hasCompletedItem = order.orderItems.some(item => item.status === "COMPLETED");
    if (hasCompletedItem) {
      return false;
    }
    
    // PENDING_PAYMENT, PAID 상태에서 취소 가능
    if (orderStatus === "PENDING_PAYMENT" || orderStatus === "PAID") {
      return true;
    }
    
    // ACTIVE 상태는 배송이 시작되지 않은 경우만 취소 가능
    if (orderStatus === "ACTIVE") {
      // 모든 주문 상품이 READY 상태이거나 배송 정보가 없을 때만 취소 가능
      return order.orderItems.every(
        (item) => item.status === "READY" || item.status === "CONFIRMED" || item.status === "PENDING_CONFIRMATION"
      );
    }
    
    return false;
  };

  // 주문 상품별 구매 확정 처리
  const handleCompleteOrderItem = async (orderItem) => {
    if (!confirm("구매 확정하시겠습니까? 구매 확정 후에는 반품이 불가능합니다.")) {
      return;
    }

    try {
      setCompleting(true);
      const response = await completeOrderItem(orderItem.orderItemNo);
      const updatedOrderItem = response.data || response;
      
      // 주문 정보 새로고침
      const orderData = await getOrder(parseInt(orderNo));
      setOrder(orderData.data || orderData);
      
      alert("구매 확정이 완료되었습니다.");
    } catch (err) {
      console.error("구매 확정 실패:", err);
      alert(err.message || "구매 확정에 실패했습니다.");
    } finally {
      setCompleting(false);
    }
  };

  // 주문 취소 처리
  const handleCancelOrder = async () => {
    if (!confirm("주문을 취소하시겠습니까? 취소 후에는 복구할 수 없습니다.")) {
      return;
    }

    try {
      setCancelling(true);
      const response = await cancelOrder(parseInt(orderNo));
      const updatedOrder = response.data || response;
      setOrder(updatedOrder);
      alert("주문이 취소되었습니다.");
    } catch (err) {
      console.error("주문 취소 실패:", err);
      alert(err.message || "주문 취소에 실패했습니다.");
    } finally {
      setCancelling(false);
    }
  };

  // 반품 상태 한글 변환
  const getReturnStatusLabel = (status) => {
    if (!status) return null;
    switch (status) {
      case "REQUESTED":
        return "반품신청";
      case "APPROVED":
        return "반품승인";
      case "REJECTED":
        return "반품거절";
      case "PICKUP_COMPLETED":
        return "수거완료";
      case "REFUNDED":
        return "환불완료";
      default:
        return status;
    }
  };

  // 반품 신청 가능 여부 확인 (status 필드 기반)
  const canRequestReturn = (orderItem) => {
    if (!order || !orderItem) return false;
    
    // 주문 상태 확인: ACTIVE 상태여야 함
    if (order.orderStatus !== "ACTIVE") {
      return false;
    }
    
    // status 필드를 기반으로 반품 신청 가능 여부 확인
    // DELIVERED 상태이고, 반품 진행 중이 아니고, 구매 확정되지 않은 경우만 가능
    return orderItem.status === "DELIVERED";
  };

  // 리뷰 작성 가능 여부 확인
  const canWriteReview = (orderItem) => {
    if (!orderItem) return false;
    
    // 구매 확정된 상품만 리뷰 작성 가능
    if (orderItem.status !== "COMPLETED") {
      return false;
    }
    
    // 이미 리뷰를 작성한 상품인지 확인
    const hasReview = myReviews.some(review => review.orderItemNo === orderItem.orderItemNo);
    if (hasReview) {
      return false;
    }
    
    return true;
  };

  // 리뷰 작성 모달 열기
  const handleOpenReviewModal = (orderItem) => {
    setSelectedOrderItem(orderItem);
    setReviewRating(5);
    setReviewContent("");
    setReviewImages([]);
    setReviewModalVisible(true);
  };

  // 리뷰 작성 처리
  const handleCreateReview = async () => {
    if (!selectedOrderItem || !reviewContent.trim()) {
      alert("리뷰 내용을 입력해주세요.");
      return;
    }

    if (!reviewRating || reviewRating < 1 || reviewRating > 5) {
      alert("평점을 선택해주세요.");
      return;
    }

    // 리뷰 작성 후 수정/삭제 불가 안내 및 확인
    const confirmed = window.confirm(
      "리뷰를 작성하면 수정 및 삭제가 불가능합니다.\n등록하시겠습니까?"
    );

    if (!confirmed) {
      return;
    }

    try {
      setReviewing(true);
      await createReview({
        orderItemNo: selectedOrderItem.orderItemNo,
        reviewContent: reviewContent.trim(),
        reviewRating: reviewRating,
        imageFileIds: reviewImages.map(img => img.fileId),
      });
      alert("리뷰가 작성되었습니다.");
      setReviewModalVisible(false);
      setReviewContent("");
      setReviewRating(5);
      setSelectedOrderItem(null);
      
      // 리뷰 목록 새로고침
      const reviewsData = await getMyReviews();
      setMyReviews(reviewsData.data || reviewsData || []);
    } catch (err) {
      console.error("리뷰 작성 실패:", err);
      alert(err.message || "리뷰 작성에 실패했습니다.");
    } finally {
      setReviewing(false);
    }
  };

  const beforeSelectReviewImage = (file) => {
    const isAllowed = ["image/jpeg", "image/png", "image/gif", "image/webp"].includes(file.type);
    if (!isAllowed) {
      alert("이미지 파일만 업로드할 수 있습니다. (jpg, png, gif, webp)");
      return false;
    }
    const isLt5M = file.size / 1024 / 1024 < 5;
    if (!isLt5M) {
      alert("이미지 파일은 5MB 이하만 업로드할 수 있습니다.");
      return false;
    }
    return true;
  };

  const handleUploadReviewImages = async (files) => {
    if (!files || files.length === 0) return;
    const maxCount = 5;
    const remain = Math.max(0, maxCount - reviewImages.length);
    const slice = Array.from(files).slice(0, remain);
    const uploaded = [];
    for (const f of slice) {
      if (!beforeSelectReviewImage(f)) continue;
      try {
        const res = await uploadFile(f, "review-image");
        const payload = res?.data || res;
        if (payload?.data) {
          uploaded.push({
            fileId: payload.data.fileId,
            fileUrl: payload.data.fileUrl,
            name: f.name,
          });
        } else if (payload?.fileId) {
          uploaded.push({
            fileId: payload.fileId,
            fileUrl: payload.fileUrl,
            name: f.name,
          });
        }
      } catch (e) {
        console.error("리뷰 이미지 업로드 실패:", e);
        alert(e?.response?.data?.message || e?.message || "이미지 업로드에 실패했습니다.");
      }
    }
    if (uploaded.length > 0) {
      setReviewImages(prev => [...prev, ...uploaded]);
    }
  };

  // 반품 신청 모달 열기
  const handleOpenReturnModal = (orderItem) => {
    setSelectedOrderItem(orderItem);
    setReturnReasonType("CHANGE_OF_MIND");
    setReturnReason("");
    setReturnImages([]);
    setReturnModalVisible(true);
  };

  const needsReturnDetailText = () => {
    return ["DEFECT", "WRONG_ITEM", "OTHER"].includes(returnReasonType);
  };

  /** 불량·쇼핑몰 측 오배송·기타는 증빙 이미지 필수 */
  const needsReturnImages = () => {
    return ["DEFECT", "WRONG_ITEM", "OTHER"].includes(returnReasonType);
  };

  const beforeSelectReturnImage = (file) => {
    const isAllowed = ["image/jpeg", "image/png", "image/gif", "image/webp"].includes(file.type);
    if (!isAllowed) {
      alert("이미지 파일만 업로드할 수 있습니다. (jpg, png, gif, webp)");
      return false;
    }
    const isLt5M = file.size / 1024 / 1024 < 5;
    if (!isLt5M) {
      alert("이미지 파일은 5MB 이하만 업로드할 수 있습니다.");
      return false;
    }
    return true;
  };

  const handleUploadReturnImages = async (files) => {
    if (!files || files.length === 0) return;
    const maxCount = 5;
    const remain = Math.max(0, maxCount - returnImages.length);
    const slice = Array.from(files).slice(0, remain);
    const uploaded = [];
    for (const f of slice) {
      if (!beforeSelectReturnImage(f)) continue;
      try {
        const res = await uploadFile(f, "return-image");
        const payload = res?.data || res;
        if (payload?.data) {
          uploaded.push({
            fileId: payload.data.fileId,
            fileUrl: payload.data.fileUrl,
            name: f.name,
          });
        } else if (payload?.fileId) {
          uploaded.push({
            fileId: payload.fileId,
            fileUrl: payload.fileUrl,
            name: f.name,
          });
        }
      } catch (e) {
        console.error("반품 이미지 업로드 실패:", e);
        alert(e?.response?.data?.message || e?.message || "이미지 업로드에 실패했습니다.");
      }
    }
    if (uploaded.length > 0) {
      setReturnImages(prev => [...prev, ...uploaded]);
    }
  };

  // 반품 신청 처리
  const handleRequestReturn = async () => {
    if (needsReturnImages() && returnImages.length === 0) {
      alert("선택하신 반품 사유는 증빙 이미지를 최소 1장 업로드해주세요.");
      return;
    }
    const detail = returnReason.trim();
    if (needsReturnDetailText()) {
      if (!detail) {
        alert("선택하신 반품 유형은 상세 사유 입력이 필요합니다.");
        return;
      }
      if (returnReasonType === "OTHER" && detail.length < 10) {
        alert("기타 사유는 10자 이상 입력해주세요.");
        return;
      }
      if ((returnReasonType === "DEFECT" || returnReasonType === "WRONG_ITEM") && detail.length < 5) {
        alert("불량·쇼핑몰 측 오배송 사유는 5자 이상 입력해주세요.");
        return;
      }
    }
    try {
      setReturning(true);
      const response = await requestReturn({
        orderItemNo: selectedOrderItem.orderItemNo,
        returnReasonType,
        returnReason: detail,
        imageFileIds: returnImages.map(img => img.fileId),
      });
      alert("반품 신청이 완료되었습니다.");
      setReturnModalVisible(false);
      setReturnReasonType("CHANGE_OF_MIND");
      setReturnReason("");
      setReturnImages([]);
      setSelectedOrderItem(null);
      // 주문 정보 새로고침
      const orderData = await getOrder(parseInt(orderNo));
      setOrder(orderData.data || orderData);
    } catch (err) {
      console.error("반품 신청 실패:", err);
      alert(err.message || "반품 신청에 실패했습니다.");
    } finally {
      setReturning(false);
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

  if (!order) {
    return (
      <div className="my-account-content">
        <div className="text-center p-4">주문 정보를 찾을 수 없습니다.</div>
      </div>
    );
  }
  return (
    <div className="my-account-content">
      <div className="account-order-details">
        <div className="wd-form-order">
          <div className="order-head">
            <figure className="img-product">
              <Image
                alt="product"
                src="/images/products/womens/women-1.jpg"
                width={600}
                height={800}
              />
            </figure>
            <div className="content">
              <div className={`badge ${
                getReturnStatus() === "REFUNDED" ? "bg-success" :
                getReturnStatus() === "REJECTED" ? "bg-danger" :
                getReturnStatus() === "IN_PROGRESS" ? "bg-warning" :
                getOrderStatusLabel(order.orderStatus) === "구매 확정" ? "bg-info" :
                ""
              }`}>
                {getOrderStatusLabel(order.orderStatus)}
              </div>
              <h6 className="mt-8 fw-5">Order #{order.orderNo}</h6>
              <div className="mt-3 d-flex gap-2 flex-wrap">
                {canCancelOrder() && (
                  <button
                    className="btn btn-danger"
                    onClick={handleCancelOrder}
                    disabled={cancelling}
                    style={{
                      padding: "8px 16px",
                      borderRadius: "4px",
                      border: "none",
                      cursor: cancelling ? "not-allowed" : "pointer",
                    }}
                  >
                    {cancelling ? "처리 중..." : "주문 취소"}
                  </button>
                )}
              </div>
              {/* 구매 확정은 OrderItem.completedAt으로 관리하므로 주문 레벨에서는 표시하지 않음 */}
              {order.orderStatus === "PAID" && (
                <div className="mt-3 text-info">
                  <small>파트너가 발주 확인 중입니다. 발주 확인 후 배송이 시작됩니다.</small>
                </div>
              )}
            </div>
          </div>
          <div className="tf-grid-layout md-col-2 gap-15">
            <div className="item">
              <div className="text-2 text_black-2">Item</div>
              <div className="text-2 mt_4 fw-6">
                {order.orderItems && order.orderItems.length > 0
                  ? order.orderItems.length === 1
                    ? order.orderItems[0].productName
                    : `${order.orderItems[0].productName} 외 ${order.orderItems.length - 1}개`
                  : "-"}
              </div>
            </div>
            <div className="item">
              <div className="text-2 text_black-2">Courier</div>
              <div className="text-2 mt_4 fw-6">
                {deliveries.length > 0 && deliveries[0].deliveryCourier
                  ? deliveries[0].deliveryCourier
                  : "-"}
              </div>
            </div>
            <div className="item">
              <div className="text-2 text_black-2">Start Time</div>
              <div className="text-2 mt_4 fw-6">
                {deliveries.length > 0 && deliveries[0].deliveryStartDate
                  ? formatDate(deliveries[0].deliveryStartDate)
                  : "-"}
              </div>
            </div>
            <div className="item">
              <div className="text-2 text_black-2">Address</div>
              <div className="text-2 mt_4 fw-6">
                {order.deliveryAddress || "-"}
                {order.deliveryAddressDetail && ` ${order.deliveryAddressDetail}`}
              </div>
            </div>
          </div>
          <div className="widget-tabs style-3 widget-order-tab">
            <ul className="widget-menu-tab">
              <li
                className={`item-title ${activeTab == 1 ? "active" : ""} `}
                onClick={() => setActiveTab(1)}
              >
                <span className="inner">Order History</span>
              </li>
              <li
                className={`item-title ${activeTab == 2 ? "active" : ""} `}
                onClick={() => setActiveTab(2)}
              >
                <span className="inner">Item Details</span>
              </li>
              <li
                className={`item-title ${activeTab == 3 ? "active" : ""} `}
                onClick={() => setActiveTab(3)}
              >
                <span className="inner">Courier</span>
              </li>
              <li
                className={`item-title ${activeTab == 4 ? "active" : ""} `}
                onClick={() => setActiveTab(4)}
              >
                <span className="inner">Receiver</span>
              </li>
            </ul>
            <div className="widget-content-tab">
              <div
                className={`widget-content-inner ${
                  activeTab == 1 ? "active" : ""
                } `}
              >
                {order.orderItems && order.orderItems.length > 0 ? (
                  <>
                    {order.orderItems.map((item, itemIndex) => {
                      const hasDelivery = item.deliveryNo != null || item.deliveryStatus != null;
                      
                      return (
                        <div key={item.orderItemNo || itemIndex} className={itemIndex > 0 ? "mt-4 pt-4 border-top" : ""}>
                          <h6 className="mb-3 fw-6">
                            {item.productName || `상품 #${itemIndex + 1}`}
                            {item.color || item.size ? ` (${[item.color, item.size].filter(Boolean).join(" / ")})` : ""}
                          </h6>
                          
                          {!hasDelivery ? (
                            <div className="text-center p-4">
                              <p className="text-2 text-muted">
                                {order.orderStatus === "PENDING_PAYMENT" 
                                  ? "결제 완료 후 배송 정보가 생성됩니다."
                                  : order.orderStatus === "PAID"
                                  ? "발주 확인 후 배송 정보가 생성됩니다."
                                  : "배송 정보가 아직 생성되지 않았습니다."}
                              </p>
                            </div>
                          ) : (
                            <div className="widget-timeline">
                              <ul className="timeline">
                                <li>
                                  <div className={`timeline-badge ${
                                    item.deliveryStatus === "DELIVERED" ? "success" :
                                    item.deliveryStatus === "SHIPPED" ? "success" : ""
                                  }`} />
                                  <div className="timeline-box">
                                    <a className="timeline-panel" href="#">
                                      <div className="text-2 fw-6">
                                        {getDeliveryStatusLabel(item.deliveryStatus)}
                                      </div>
                                      {item.deliveryStartDate && (
                                        <span>{formatDate(item.deliveryStartDate)}</span>
                                      )}
                                    </a>
                                    {item.deliveryCourier && (
                                      <p>
                                        <strong>택배사 : </strong>
                                        {item.deliveryCourier}
                                      </p>
                                    )}
                                    {item.deliveryTrackingNumber && (
                                      <p>
                                        <strong>송장번호 : </strong>
                                        {item.deliveryTrackingNumber}
                                      </p>
                                    )}
                                    {item.deliveryEndDate && (
                                      <p>
                                        <strong>배송 완료일 : </strong>
                                        {formatDate(item.deliveryEndDate)}
                                      </p>
                                    )}
                                  </div>
                                </li>
                                {/* 주문 생성 시점 표시 (모든 상품에 표시) */}
                                <li>
                                  <div className="timeline-badge" />
                                  <div className="timeline-box">
                                    <a className="timeline-panel" href="#">
                                      <div className="text-2 fw-6">주문 생성</div>
                                      <span>{formatDate(order.orderCreatedAt)}</span>
                                    </a>
                                  </div>
                                </li>
                              </ul>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </>
                ) : (
                  <div className="text-center p-4">
                    <p className="text-2">배송 정보가 없습니다.</p>
                  </div>
                )}
              </div>
              <div
                className={`widget-content-inner ${
                  activeTab == 2 ? "active" : ""
                } `}
              >
                {order.orderItems && order.orderItems.length > 0 ? (
                  <>
                    {order.orderItems.map((item, index) => (
                      <div key={item.orderItemNo || index} className={index > 0 ? "mt-4 pt-4 border-top" : ""}>
                        <div className="order-head">
                          <figure className="img-product">
                            <Image
                              alt={item.productName || "상품"}
                              src={item.productImageUrl || "/images/products/womens/women-1.jpg"}
                              width={600}
                              height={800}
                              onError={(e) => {
                                e.target.src = "/images/products/womens/women-1.jpg";
                              }}
                            />
                          </figure>
                          <div className="content">
                            <div className="d-flex align-items-center gap-2 mb-2">
                              <div className="text-2 fw-6">{item.productName || "-"}</div>
                              {/* 주문 상품별 상태 배지 표시 (status 필드 기반) */}
                              {(() => {
                                if (!item.status) return null;
                                
                                // status 필드를 기반으로 배지 표시
                                switch (item.status) {
                                  case "CANCELLED":
                                    return <span className="badge bg-secondary" style={{ fontSize: "11px" }}>취소됨</span>;
                                  case "REFUNDED":
                                    return <span className="badge bg-success" style={{ fontSize: "11px" }}>환불 완료</span>;
                                  case "RETURN_REJECTED":
                                    return <span className="badge bg-danger" style={{ fontSize: "11px" }}>반품 거절</span>;
                                  case "RETURN_IN_PROGRESS":
                                    return <span className="badge bg-warning" style={{ fontSize: "11px" }}>반품 진행 중</span>;
                                  case "COMPLETED":
                                    return <span className="badge bg-info" style={{ fontSize: "11px" }}>구매 확정</span>;
                                  case "DELIVERED":
                                    return <span className="badge bg-primary" style={{ fontSize: "11px" }}>배송 완료</span>;
                                  case "SHIPPED":
                                    return <span className="badge bg-primary" style={{ fontSize: "11px" }}>배송 중</span>;
                                  case "READY":
                                    return <span className="badge bg-primary" style={{ fontSize: "11px" }}>배송 준비</span>;
                                  case "CONFIRMED":
                                    return <span className="badge bg-secondary" style={{ fontSize: "11px" }}>발주 확인됨</span>;
                                  case "PENDING_CONFIRMATION":
                                    return <span className="badge bg-secondary" style={{ fontSize: "11px" }}>발주 확인 대기</span>;
                                  default:
                                    return null;
                                }
                              })()}
                            </div>
                            <div className="mt_4">
                              <span className="fw-6">단가 :</span> ₩{item.itemPrice?.toLocaleString() || 0}
                            </div>
                            <div className="mt_4">
                              <span className="fw-6">수량 :</span> {item.quantity || 0}개
                            </div>
                            {(item.color || item.size) && (
                              <div className="mt_4">
                                <span className="fw-6">옵션 :</span>{" "}
                                {[item.color, item.size].filter(Boolean).join(" / ") || "-"}
                              </div>
                            )}
                            {item.itemDiscountAmount > 0 && (
                              <div className="mt_4">
                                <span className="fw-6">할인 :</span> -₩{item.itemDiscountAmount?.toLocaleString() || 0}
                              </div>
                            )}
                            {/* 반품 상태 표시 */}
                            {item.returnStatus && (
                              <div className="mt_4 p-3" style={{ 
                                backgroundColor: "#f8f9fa", 
                                borderRadius: "4px",
                                border: "1px solid #e0e0e0"
                              }}>
                                <div className="mb-2">
                                  <span className="fw-6">반품 상태 :</span>{" "}
                                  <span className={`badge ${
                                    item.returnStatus === "REFUNDED" ? "bg-success" :
                                    item.returnStatus === "REJECTED" ? "bg-danger" :
                                    item.returnStatus === "APPROVED" || item.returnStatus === "PICKUP_COMPLETED" ? "bg-info" :
                                    "bg-warning"
                                  }`}>
                                    {getReturnStatusLabel(item.returnStatus)}
                                  </span>
                                </div>
                                {item.returnRequestedAt && (
                                  <div className="text-muted" style={{ fontSize: "12px", marginBottom: "4px" }}>
                                    신청일: {formatDate(item.returnRequestedAt)}
                                  </div>
                                )}
                                {item.returnStatus === "REJECTED" && item.rejectionReason && (
                                  <div className="text-danger mt-2" style={{ fontSize: "12px" }}>
                                    <strong>거절 사유:</strong> {item.rejectionReason}
                                  </div>
                                )}
                                {item.returnTrackingNumber && (
                                  <div className="text-muted mt-2" style={{ fontSize: "12px" }}>
                                    반품 송장번호: {item.returnTrackingNumber}
                                    {item.returnCourier && ` (${item.returnCourier})`}
                                  </div>
                                )}
                                {item.returnReason && (
                                  <div className="text-muted mt-2" style={{ fontSize: "12px" }}>
                                    <strong>반품 사유:</strong> {item.returnReason}
                                  </div>
                                )}
                              </div>
                            )}
                            <div className="mt_4 d-flex gap-2">
                              {canCompleteOrderItem(item) && (
                                <button
                                  className="btn btn-primary"
                                  onClick={() => handleCompleteOrderItem(item)}
                                  disabled={completing}
                                  style={{
                                    padding: "6px 12px",
                                    borderRadius: "4px",
                                    border: "none",
                                    cursor: completing ? "not-allowed" : "pointer",
                                    fontSize: "14px",
                                  }}
                                >
                                  {completing ? "처리 중..." : "구매 확정"}
                                </button>
                              )}
                              {item.completedAt && (
                                <span className="badge bg-success" style={{ fontSize: "12px", padding: "6px 12px" }}>
                                  구매 확정 완료 ({formatDate(item.completedAt)})
                                </span>
                              )}
                              {canRequestReturn(item) && (
                                <button
                                  className="btn btn-warning"
                                  onClick={() => handleOpenReturnModal(item)}
                                  style={{
                                    padding: "6px 12px",
                                    borderRadius: "4px",
                                    border: "none",
                                    cursor: "pointer",
                                    fontSize: "14px",
                                  }}
                                >
                                  반품 신청
                                </button>
                              )}
                              {canWriteReview(item) && (
                                <button
                                  className="btn btn-success"
                                  onClick={() => handleOpenReviewModal(item)}
                                  style={{
                                    padding: "6px 12px",
                                    borderRadius: "4px",
                                    border: "none",
                                    cursor: "pointer",
                                    fontSize: "14px",
                                  }}
                                >
                                  리뷰 작성
                                </button>
                              )}
                              {!canWriteReview(item) && item.status === "COMPLETED" && myReviews.some(r => r.orderItemNo === item.orderItemNo) && (
                                <span className="badge bg-info" style={{ fontSize: "12px", padding: "6px 12px" }}>
                                  리뷰 작성 완료
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                        <ul>
                          <li className="d-flex justify-content-between text-2">
                            <span>아이템 총액</span>
                            <span className="fw-6">₩{item.itemTotalPrice?.toLocaleString() || 0}</span>
                          </li>
                        </ul>
                      </div>
                    ))}
                    <div className="mt-4 pt-4 border-top">
                      <ul>
                        <li className="d-flex justify-content-between text-2">
                          <span>주문 총액</span>
                          <span className="fw-6">₩{order.orderTotalPrice?.toLocaleString() || 0}</span>
                        </li>
                        {order.paymentAmount && (
                          <li className="d-flex justify-content-between text-2 mt_4">
                            <span>결제 금액</span>
                            <span className="fw-6">₩{order.paymentAmount?.toLocaleString() || 0}</span>
                          </li>
                        )}
                      </ul>
                    </div>
                  </>
                ) : (
                  <div className="text-center p-4">
                    <p className="text-2">주문 아이템 정보가 없습니다.</p>
                  </div>
                )}
              </div>
              <div
                className={`widget-content-inner ${
                  activeTab == 3 ? "active" : ""
                } `}
              >
                {order.orderItems && order.orderItems.length > 0 ? (
                  <>
                    {order.orderItems.map((item, itemIndex) => {
                      const hasDelivery = item.deliveryNo != null || item.deliveryStatus != null;
                      
                      return (
                        <div key={item.orderItemNo || itemIndex} className={itemIndex > 0 ? "mb-4 p-3 border rounded" : "mb-4 p-3 border rounded"}>
                          <h6 className="fw-6 mb-3">
                            {item.productName || `상품 #${itemIndex + 1}`}
                            {item.color || item.size ? ` (${[item.color, item.size].filter(Boolean).join(" / ")})` : ""}
                          </h6>
                          
                          {!hasDelivery ? (
                            <p className="text-2 text-muted">
                              {order.orderStatus === "PENDING_PAYMENT"
                                ? "배송 정보가 없습니다. 결제 완료 후 배송 정보가 생성됩니다."
                                : order.orderStatus === "PAID"
                                ? "배송 정보가 없습니다. 발주 확인 후 배송 정보가 생성됩니다."
                                : "배송 정보가 없습니다."}
                            </p>
                          ) : (
                            <ul className="list-unstyled">
                              <li className="mb-2">
                                <strong>배송 상태 : </strong>
                                {getDeliveryStatusLabel(item.deliveryStatus)}
                              </li>
                              {item.deliveryCourier && (
                                <li className="mb-2">
                                  <strong>택배사 : </strong>
                                  {item.deliveryCourier}
                                </li>
                              )}
                              {item.deliveryTrackingNumber && (
                                <li className="mb-2">
                                  <strong>송장번호 : </strong>
                                  {item.deliveryTrackingNumber}
                                </li>
                              )}
                              {item.deliveryStartDate && (
                                <li className="mb-2">
                                  <strong>배송 시작일 : </strong>
                                  {formatDate(item.deliveryStartDate)}
                                </li>
                              )}
                              {item.deliveryEndDate && (
                                <li className="mb-2">
                                  <strong>배송 완료일 : </strong>
                                  {formatDate(item.deliveryEndDate)}
                                </li>
                              )}
                            </ul>
                          )}
                        </div>
                      );
                    })}
                  </>
                ) : (
                  <p className="text-2">
                    {order.orderStatus === "PENDING_PAYMENT"
                      ? "배송 정보가 없습니다. 결제 완료 후 배송 정보가 생성됩니다."
                      : order.orderStatus === "PAID"
                      ? "배송 정보가 없습니다. 발주 확인 후 배송 정보가 생성됩니다."
                      : "배송 정보가 없습니다."}
                  </p>
                )}
              </div>
              <div
                className={`widget-content-inner ${
                  activeTab == 4 ? "active" : ""
                } `}
              >
                <p className="text-2 text-success">
                  Thank you Your order has been received
                </p>
                <ul className="mt_20">
                  <li>
                    Order Number : <span className="fw-7">#{order.orderNo}</span>
                  </li>
                  <li>
                    Date : <span className="fw-7">{formatDate(order.orderCreatedAt)}</span>
                  </li>
                  <li>
                    Total : <span className="fw-7">{order.orderTotalPrice?.toLocaleString()}원</span>
                  </li>
                  <li>
                    Payment Methods :
                    <span className="fw-7">{order.paymentMethod || "-"}</span>
                  </li>
                  {order.paidAt && (
                    <li>
                      Payment Date : <span className="fw-7">{formatDate(order.paidAt)}</span>
                    </li>
                  )}
                  {order.recipientName && (
                    <li>
                      Recipient : <span className="fw-7">{order.recipientName}</span>
                    </li>
                  )}
                  {order.recipientPhone && (
                    <li>
                      Recipient Phone : <span className="fw-7">{order.recipientPhone}</span>
                    </li>
                  )}
                  {order.deliveryAddress && (
                    <li>
                      Delivery Address : <span className="fw-7">{order.deliveryAddress}{order.deliveryAddressDetail ? ` ${order.deliveryAddressDetail}` : ""}</span>
                    </li>
                  )}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 반품 신청 모달 */}
      {returnModalVisible && (
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
          onClick={() => {
            if (!returning) {
              setReturnModalVisible(false);
              setReturnReasonType("CHANGE_OF_MIND");
              setReturnReason("");
              setReturnImages([]);
              setSelectedOrderItem(null);
            }
          }}
        >
          <div
            style={{
              backgroundColor: "white",
              padding: "24px",
              borderRadius: "8px",
              maxWidth: "500px",
              width: "90%",
              maxHeight: "80vh",
              overflow: "auto",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h5 className="mb-3">반품 신청</h5>
            {selectedOrderItem && (
              <div className="mb-3">
                <p className="mb-2">
                  <strong>상품명:</strong> {selectedOrderItem.productName}
                </p>
                {(selectedOrderItem.color || selectedOrderItem.size) && (
                  <p className="mb-2">
                    <strong>옵션:</strong>{" "}
                    {[selectedOrderItem.color, selectedOrderItem.size]
                      .filter(Boolean)
                      .join(" / ")}
                  </p>
                )}
                <p className="mb-2">
                  <strong>수량:</strong> {selectedOrderItem.quantity}개
                </p>
                <p className="mb-3">
                  <strong>금액:</strong>{" "}
                  {(selectedOrderItem.itemTotalPrice ?? 0).toLocaleString()}원
                </p>
              </div>
            )}
            <div className="mb-3">
              <label className="form-label">
                <strong>반품 사유 유형 *</strong>
              </label>
              <select
                className="form-select"
                value={returnReasonType}
                onChange={(e) => setReturnReasonType(e.target.value)}
                disabled={returning}
              >
                <option value="CHANGE_OF_MIND">단순 변심</option>
                <option value="ORDER_MISTAKE">주문 실수 (색/사이즈 등)</option>
                <option value="DEFECT">상품 불량·하자</option>
                <option value="WRONG_ITEM">쇼핑몰 측 오배송</option>
                <option value="OTHER">기타</option>
              </select>
            </div>
            <div className="mb-3">
              <label className="form-label">
                <strong>
                  반품 증빙 이미지
                  {needsReturnImages() ? " * (최대 5장)" : " (선택, 최대 5장)"}
                </strong>
              </label>
              <div
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  if (returning) return;
                  handleUploadReturnImages(e.dataTransfer.files);
                }}
                style={{
                  border: "1px dashed #d9d9d9",
                  padding: "12px",
                  borderRadius: "6px",
                  background: "#fafafa",
                  cursor: "pointer",
                  marginBottom: "8px",
                }}
                onClick={() => {
                  if (returning) return;
                  const input = document.createElement("input");
                  input.type = "file";
                  input.accept = "image/*";
                  input.multiple = true;
                  input.onchange = (e) => handleUploadReturnImages(e.target.files);
                  input.click();
                }}
              >
                여기에 이미지를 끌어다 놓거나 클릭해서 선택하세요.
              </div>
              {returnImages.length > 0 && (
                <div className="d-flex gap-2 flex-wrap">
                  {returnImages.map((img, idx) => (
                    <div key={img.fileId || idx} style={{ position: "relative" }}>
                      <Image
                        src={img.fileUrl}
                        alt={img.name || `return-${idx + 1}`}
                        width={96}
                        height={96}
                        style={{ objectFit: "cover", borderRadius: 6, border: "1px solid #eee" }}
                      />
                      <button
                        type="button"
                        onClick={() =>
                          setReturnImages(prev => prev.filter((_, i) => i !== idx))
                        }
                        style={{
                          position: "absolute",
                          top: -6,
                          right: -6,
                          background: "#ff4d4f",
                          color: "#fff",
                          border: "none",
                          borderRadius: "50%",
                          width: 20,
                          height: 20,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          padding: 0,
                          lineHeight: 1,
                          fontSize: 14,
                          fontWeight: 700,
                          cursor: "pointer",
                        }}
                        disabled={returning}
                        aria-label="remove image"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="mb-3">
              <label className="form-label">
                <strong>
                  상세 사유{needsReturnDetailText() ? " *" : " (선택)"}
                </strong>
              </label>
              <textarea
                className="form-control"
                rows="4"
                value={returnReason}
                onChange={(e) => setReturnReason(e.target.value)}
                placeholder={
                  returnReasonType === "OTHER"
                    ? "기타 사유를 10자 이상 구체적으로 입력해주세요."
                    : needsReturnDetailText()
                    ? "불량·쇼핑몰 측 오배송 내용을 5자 이상 입력해주세요."
                    : "필요 시 추가로 남길 메모를 입력해주세요."
                }
                disabled={returning}
              />
            </div>
            <div className="d-flex gap-2 justify-content-end">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setReturnModalVisible(false);
                  setReturnReasonType("CHANGE_OF_MIND");
                  setReturnReason("");
                  setReturnImages([]);
                  setSelectedOrderItem(null);
                }}
                disabled={returning}
              >
                취소
              </button>
              <button
                className="btn btn-warning"
                onClick={handleRequestReturn}
                disabled={
                  returning ||
                  (needsReturnImages() && returnImages.length === 0) ||
                  (needsReturnDetailText() && !returnReason.trim()) ||
                  (returnReasonType === "OTHER" && returnReason.trim().length < 10)
                }
              >
                {returning ? "처리 중..." : "반품 신청"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 리뷰 작성 모달 */}
      {reviewModalVisible && (
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
          onClick={() => {
            if (!reviewing) {
              setReviewModalVisible(false);
              setReviewContent("");
              setReviewRating(5);
              setSelectedOrderItem(null);
            }
          }}
        >
          <div
            style={{
              backgroundColor: "white",
              padding: "24px",
              borderRadius: "8px",
              maxWidth: "600px",
              width: "90%",
              maxHeight: "80vh",
              overflow: "auto",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h5 className="mb-3">리뷰 작성</h5>
            {selectedOrderItem && (
              <div className="mb-3">
                <p className="mb-2">
                  <strong>상품명:</strong> {selectedOrderItem.productName}
                </p>
                {(selectedOrderItem.color || selectedOrderItem.size) && (
                  <p className="mb-2">
                    <strong>옵션:</strong>{" "}
                    {[selectedOrderItem.color, selectedOrderItem.size]
                      .filter(Boolean)
                      .join(" / ")}
                  </p>
                )}
                <p className="mb-3">
                  <strong>수량:</strong> {selectedOrderItem.quantity}개
                </p>
              </div>
            )}
            <div className="mb-3">
              <label className="form-label">
                <strong>사진 첨부 (최대 5장)</strong>
              </label>
              <div
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  if (reviewing) return;
                  handleUploadReviewImages(e.dataTransfer.files);
                }}
                style={{
                  border: "1px dashed #d9d9d9",
                  padding: "12px",
                  borderRadius: "6px",
                  background: "#fafafa",
                  cursor: "pointer",
                  marginBottom: "8px",
                }}
                onClick={() => {
                  if (reviewing) return;
                  const input = document.createElement("input");
                  input.type = "file";
                  input.accept = "image/*";
                  input.multiple = true;
                  input.onchange = (e) => handleUploadReviewImages(e.target.files);
                  input.click();
                }}
              >
                여기에 이미지를 끌어다 놓거나 클릭해서 선택하세요.
              </div>
              {reviewImages.length > 0 && (
                <div className="d-flex gap-2 flex-wrap">
                  {reviewImages.map((img, idx) => (
                    <div key={img.fileId || idx} style={{ position: "relative" }}>
                      <Image
                        src={img.fileUrl}
                        alt={img.name || `review-${idx+1}`}
                        width={96}
                        height={96}
                        style={{ objectFit: "cover", borderRadius: 6, border: "1px solid #eee" }}
                      />
                      <button
                        type="button"
                        onClick={() =>
                          setReviewImages(prev => prev.filter((_, i) => i !== idx))
                        }
                        style={{
                          position: "absolute",
                          top: -6,
                          right: -6,
                          background: "#ff4d4f",
                          color: "#fff",
                          border: "none",
                          borderRadius: "50%",
                          width: 20,
                          height: 20,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          padding: 0,
                          lineHeight: 1,
                          fontSize: 14,
                          fontWeight: 700,
                          cursor: "pointer",
                        }}
                        disabled={reviewing}
                        aria-label="remove image"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="mb-3">
              <label className="form-label">
                <strong>평점 *</strong>
              </label>
              <div className="d-flex gap-2 align-items-center">
                {[5, 4, 3, 2, 1].map((rating) => (
                  <button
                    key={rating}
                    type="button"
                    className={`btn ${reviewRating === rating ? "btn-warning" : "btn-outline-warning"}`}
                    onClick={() => setReviewRating(rating)}
                    disabled={reviewing}
                    style={{
                      minWidth: "40px",
                      padding: "4px 8px",
                    }}
                  >
                    {rating}⭐
                  </button>
                ))}
              </div>
            </div>
            <div className="mb-3">
              <label className="form-label">
                <strong>리뷰 내용 *</strong>
              </label>
              <textarea
                className="form-control"
                rows="6"
                value={reviewContent}
                onChange={(e) => setReviewContent(e.target.value)}
                placeholder="리뷰 내용을 입력해주세요."
                disabled={reviewing}
              />
            </div>
            <div className="d-flex gap-2 justify-content-end">
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setReviewModalVisible(false);
                  setReviewContent("");
                  setReviewRating(5);
                  setSelectedOrderItem(null);
                }}
                disabled={reviewing}
              >
                취소
              </button>
              <button
                className="btn btn-success"
                onClick={handleCreateReview}
                disabled={reviewing || !reviewContent.trim() || !reviewRating}
              >
                {reviewing ? "작성 중..." : "리뷰 작성"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
