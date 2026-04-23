"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { getPublicEventList } from "@/lib/api/event";

const STATUS_PRIORITY = {
  ACTIVE: 0,
  SCHEDULED: 1,
  ENDED: 2,
};

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

function badgeClass(status) {
  return status === "ENDED" ? "badge bg-secondary" : "badge bg-main";
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
          <div className="text-center py-5">이벤트를 불러오는 중...</div>
        ) : error ? (
          <div className="text-center py-5 text-danger">{error}</div>
        ) : events.length === 0 ? (
          <div className="text-center py-5">진행 중인 이벤트가 없습니다.</div>
        ) : (
          <div className="row">
            {events.map((event) => (
              <div className="col-md-6 col-xl-4 mb-4" key={event.eventNo}>
                <div className="border rounded overflow-hidden h-100 d-flex flex-column bg-white">
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
                      <span className={badgeClass(event.eventStatus)}>
                        {statusLabel(event.eventStatus)}
                      </span>
                    </div>
                    <h6 className="mb-2">{event.eventTitle}</h6>
                    <p className="text-secondary mb-2" style={{ minHeight: "48px" }}>
                      {(event.eventContent || "").slice(0, 80)}
                      {(event.eventContent || "").length > 80 ? "..." : ""}
                    </p>
                    <div className="text-secondary mb-3" style={{ fontSize: "13px" }}>
                      고객 이벤트 기간: {formatDateOnly(event.customerEventStartAt)} ~{" "}
                      {formatDateOnly(event.customerEventEndAt)}
                    </div>
                    <div className="mt-auto">
                      <Link href={`/events/${event.eventNo}`} className="tf-btn btn-line">
                        상세 보기
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
