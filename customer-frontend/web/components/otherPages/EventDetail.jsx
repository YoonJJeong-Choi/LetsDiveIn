"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { getPublicEventDetail } from "@/lib/api/event";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

function formatDateOnly(dateString) {
  if (!dateString) return "-";
  const d = new Date(dateString);
  if (Number.isNaN(d.getTime())) return dateString;
  return d.toLocaleDateString("ko-KR");
}

function customerEventPhase(event) {
  if (!event) return { label: "-", className: "badge bg-secondary" };
  if (event.eventStatus === "ENDED") {
    return { label: "종료", className: "badge bg-secondary" };
  }
  if (event.eventStatus === "PUBLISHED") {
    if (event.visibleToCustomerNow) {
      return { label: "진행 중", className: "badge bg-main" };
    }
    const now = Date.now();
    const start = event.customerEventStartAt ? new Date(event.customerEventStartAt).getTime() : 0;
    const end = event.customerEventEndAt ? new Date(event.customerEventEndAt).getTime() : 0;
    if (start > now) return { label: "오픈 예정", className: "badge bg-main" };
    if (end < now) return { label: "종료", className: "badge bg-secondary" };
    return { label: "공개", className: "badge bg-main" };
  }
  return { label: event.eventStatus || "-", className: "badge bg-main" };
}

// 카테고리 코드 → 한글 라벨 매핑
function toKoreanCategory(code) {
  const map = {
    SWIMSUIT_MEN: "남성 수영복",
    SWIMSUIT_WOMEN: "여성 수영복",
    SWIMSUIT_KIDS: "아동 수영복",
    SWIM_CAP: "수모",
    SWIM_GOGGLES: "수경",
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
          <div className="py-5 d-flex justify-content-center">
            <InlineTemplateLoader />
          </div>
        ) : error ? (
          <div className="text-center py-5 text-danger">{error}</div>
        ) : !event ? (
          <div className="text-center py-5">이벤트를 찾을 수 없습니다.</div>
        ) : (
          <div className="border rounded p-4">
            <div className="d-flex justify-content-between align-items-center gap-3 mb-4">
              <h4 className="mb-0">{event.eventTitle}</h4>
              <Link href="/events" className="btn-line event-detail-back-link">
                목록
              </Link>
            </div>
            <div className="mb-4 rounded overflow-hidden bg-light text-center">
              <Image
                src={event.thumbnailUrl || "/images/section/page-title.jpg"}
                alt={event.eventTitle || "이벤트 썸네일"}
                width={1200}
                height={800}
                sizes="100vw"
                style={{ width: "100%", height: "auto", objectFit: "contain" }}
                priority
              />
            </div>
            <div className="mb-3">
              {(() => {
                const ph = customerEventPhase(event);
                return <span className={`me-2 ${ph.className}`}>{ph.label}</span>;
              })()}
            </div>
            <div className="mb-3 text-secondary">
              이벤트 기간: {formatDateOnly(event.customerEventStartAt)} ~{" "}
              {formatDateOnly(event.customerEventEndAt)}
            </div>
            <div style={{ whiteSpace: "pre-wrap", lineHeight: "1.7" }}>
              {event.eventContent}
            </div>
            {event.eventType === "POINT" && (
              <div className="mt-3 p-3 rounded" style={{ backgroundColor: "#f8f9fa", fontSize: "14px" }}>
                포인트는 고객 1인당 최대 3회까지 지급됩니다.
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
