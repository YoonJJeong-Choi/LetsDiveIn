"use client";

import { useEffect, useState } from "react";
import { getPublicEventDetail } from "@/lib/api/event";

function formatDateOnly(dateString) {
  if (!dateString) return "-";
  const d = new Date(dateString);
  if (Number.isNaN(d.getTime())) return dateString;
  return d.toLocaleDateString("ko-KR");
}

function statusLabel(status) {
  const labels = {
    DRAFT: "임시 저장",
    SCHEDULED: "오픈 예정",
    ACTIVE: "진행 중",
    ENDED: "종료",
    INACTIVE: "비활성화",
  };
  return labels[status] || status;
}

// 카테고리 코드 → 한글 라벨 매핑
function toKoreanCategory(code) {
  const map = {
    SWIMSUIT_MEN: "남성 수영복",
    SWIMSUIT_WOMEN: "여성 수영복",
    SWIMSUIT_KIDS: "아동 수영복",
    SWIM_CAP: "수영모자",
    SWIM_GOGGLES: "수영안경",
    FINS: "오리발",
    SWIM_TOY: "수영용품",
    ETC: "기타",
  };
  return map[code] || code;
}

export default function EventDetail({ eventNo }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [event, setEvent] = useState(null);

  useEffect(() => {
    const fetchEvent = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await getPublicEventDetail(eventNo);
        setEvent(response?.data || response || null);
      } catch (err) {
        console.error("이벤트 상세 조회 실패:", err);
        setError("이벤트 상세 정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchEvent();
  }, [eventNo]);

  return (
    <section className="flat-spacing">
      <div className="container">
        {loading ? (
          <div className="text-center py-5">이벤트 정보를 불러오는 중...</div>
        ) : error ? (
          <div className="text-center py-5 text-danger">{error}</div>
        ) : !event ? (
          <div className="text-center py-5">이벤트를 찾을 수 없습니다.</div>
        ) : (
          <div className="border rounded p-4">
            <h4 className="mb-2">{event.eventTitle}</h4>
            <div className="mb-3">
              <span className="badge bg-main me-2">{statusLabel(event.eventStatus)}</span>
            </div>
            <div className="mb-3 text-secondary">
              고객 이벤트 기간: {formatDateOnly(event.customerEventStartAt)} ~{" "}
              {formatDateOnly(event.customerEventEndAt)}
            </div>
            <div style={{ whiteSpace: "pre-wrap", lineHeight: "1.7" }}>
              {event.eventContent}
            </div>

            {/* 대상 상세 안내 */}
            {event.eventType === "POINT" && (
              <div className="mt-3 p-3 rounded" style={{ backgroundColor: "#f6f8fa", fontSize: "14px" }}>
                {event.pointEventTargetType === "PRODUCT" && Array.isArray(event.pointEventTargetValues) && event.pointEventTargetValues.length > 0 && (
                  <div>상품: #{event.pointEventTargetValues.slice(0, 5).join(", #")}{event.pointEventTargetValues.length > 5 ? " …" : ""}</div>
                )}
                {event.pointEventTargetType === "OPTION" && Array.isArray(event.pointEventTargetValues) && event.pointEventTargetValues.length > 0 && (
                  <div>옵션: #{event.pointEventTargetValues.slice(0, 5).join(", #")}{event.pointEventTargetValues.length > 5 ? " …" : ""}</div>
                )}
                {event.pointEventTargetType === "CATEGORY" && Array.isArray(event.pointEventTargetValues) && event.pointEventTargetValues.length > 0 && (
                  <div>카테고리: {event.pointEventTargetValues.slice(0, 5).map(toKoreanCategory).join(", ")}{event.pointEventTargetValues.length > 5 ? " …" : ""}</div>
                )}
                {event.pointEventTargetType === "MIN_ORDER_AMOUNT" && event.pointEventMinOrderAmount != null && (
                  <div>최소 주문금액: {Number(event.pointEventMinOrderAmount).toLocaleString()}원 이상</div>
                )}
              </div>
            )}

            {event.eventType === "POINT" && (
              <div className="mt-3 p-3 rounded" style={{ backgroundColor: "#f8f9fa", fontSize: "14px" }}>
                포인트는 고객 1인당 최대 3회까지 지급됩니다.
              </div>
            )}

            {event.eventType === "SALE" && (
              <div className="mt-3 p-3 rounded" style={{ backgroundColor: "#f8f9fa", fontSize: "14px" }}>
                할인율/기간은 이벤트 정책에 따라 자동 적용됩니다.
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
