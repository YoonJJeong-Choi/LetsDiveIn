"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { getPublicEventList } from "@/lib/api/event";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

const STATUS_PRIORITY = {
  PUBLISHED: 0,
  ENDED: 1,
};

function formatDateOnly(dateString) {
  if (!dateString) return "-";
  const d = new Date(dateString);
  if (Number.isNaN(d.getTime())) return dateString;
  return d.toLocaleDateString("ko-KR");
}

/** 목록은 customerExposeAt 적용 후 서버에서 내려줌. 뱃지는 기간·visibleToCustomerNow 기준 */
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

export default function Events() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [events, setEvents] = useState([]);

  useEffect(() => {
    const fetchEvents = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await getPublicEventList();
        const list = response?.data || response || [];
        const sortedEvents = (Array.isArray(list) ? list : []).sort((a, b) => {
          const aPriority = STATUS_PRIORITY[a.eventStatus] ?? 50;
          const bPriority = STATUS_PRIORITY[b.eventStatus] ?? 50;
          if (aPriority !== bPriority) return aPriority - bPriority;

          // 종료 이벤트는 최근 종료순, 그 외는 시작일 기준 오름차순(가까운 일정 우선)
          if (a.eventStatus === "ENDED" && b.eventStatus === "ENDED") {
            const aEnd = a.customerEventEndAt ? new Date(a.customerEventEndAt).getTime() : 0;
            const bEnd = b.customerEventEndAt ? new Date(b.customerEventEndAt).getTime() : 0;
            return bEnd - aEnd;
          }

          const aStart = a.customerEventStartAt ? new Date(a.customerEventStartAt).getTime() : 0;
          const bStart = b.customerEventStartAt ? new Date(b.customerEventStartAt).getTime() : 0;
          return aStart - bStart;
        });
        setEvents(sortedEvents);
      } catch (err) {
        console.error("이벤트 목록 조회 실패:", err);
        setError("이벤트 목록을 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchEvents();
  }, []);

  return (
    <section className="flat-spacing">
      <div className="container">
        {loading ? (
          <div className="py-5 d-flex justify-content-center">
            <InlineTemplateLoader />
          </div>
        ) : error ? (
          <div className="text-center py-5 text-danger">{error}</div>
        ) : events.length === 0 ? (
          <div className="text-center py-5">진행 중인 이벤트가 없습니다.</div>
        ) : (
          <div className="row">
            {events.map((event) => (
              <div className="col-md-6 col-xl-4 mb-4" key={event.eventNo}>
                <Link
                  href={`/events/${event.eventNo}`}
                  className="border rounded overflow-hidden h-100 d-flex flex-column bg-white text-decoration-none text-dark"
                  style={{ cursor: "pointer" }}
                  aria-label={`${event.eventTitle || "이벤트"} 상세 보기`}
                >
                  <div className="position-relative" style={{ width: "100%", height: "210px" }}>
                    <Image
                      src={event.thumbnailUrl || "/images/section/page-title.jpg"}
                      alt={event.eventTitle || "이벤트 썸네일"}
                      fill
                      sizes="(max-width: 1200px) 50vw, 33vw"
                      style={{ objectFit: "cover" }}
                    />
                  </div>
                  <div className="p-3 d-flex flex-column h-100">
                    <div className="mb-2">
                      {(() => {
                        const ph = customerEventPhase(event);
                        return <span className={ph.className}>{ph.label}</span>;
                      })()}
                    </div>
                    <h6 className="mb-2">{event.eventTitle}</h6>
                    <p className="text-secondary mb-2" style={{ minHeight: "48px" }}>
                      {(event.eventContent || "").slice(0, 80)}
                      {(event.eventContent || "").length > 80 ? "..." : ""}
                    </p>
                    <div className="text-secondary mb-3" style={{ fontSize: "13px" }}>
                      이벤트 기간: {formatDateOnly(event.customerEventStartAt)} ~{" "}
                      {formatDateOnly(event.customerEventEndAt)}
                    </div>
                  </div>
                </Link>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
