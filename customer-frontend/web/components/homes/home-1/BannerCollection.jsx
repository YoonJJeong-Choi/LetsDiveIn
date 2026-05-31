"use client";

import React, { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { getPublicEventList } from "@/lib/api/event";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

const STATUS_PRIORITY = {
  ACTIVE: 0,
  SCHEDULED: 1,
  ENDED: 2,
};

function safeSrc(url, fallback) {
  if (typeof url !== "string") return fallback;
  const t = url.trim();
  return t || fallback;
}

function isNoticeEvent(event) {
  const t = String(event?.eventType || "").toUpperCase();
  return t === "NOTICE" || t.includes("NOTICE") || t.includes("ANNOUNCE");
}

function pickHomeEvents(list) {
  const arr = Array.isArray(list) ? list : [];
  return arr
    .filter((e) => e?.eventStatus !== "DRAFT" && e?.eventStatus !== "INACTIVE")
    .filter((e) => !isNoticeEvent(e))
    .sort((a, b) => {
      const ap = STATUS_PRIORITY[a?.eventStatus] ?? 50;
      const bp = STATUS_PRIORITY[b?.eventStatus] ?? 50;
      if (ap !== bp) return ap - bp;
      const aStart = a?.customerEventStartAt ? new Date(a.customerEventStartAt).getTime() : 0;
      const bStart = b?.customerEventStartAt ? new Date(b.customerEventStartAt).getTime() : 0;
      return aStart - bStart;
    })
    .slice(0, 2);
}

export default function BannerCollection() {
  const [loading, setLoading] = useState(true);
  const [events, setEvents] = useState([]);

  useEffect(() => {
    let alive = true;
    const run = async () => {
      try {
        const resp = await getPublicEventList();
        const list = resp?.data || resp || [];
        if (!alive) return;
        setEvents(pickHomeEvents(list));
      } catch {
        if (!alive) return;
        setEvents([]);
      } finally {
        if (!alive) return;
        setLoading(false);
      }
    };
    run();
    return () => {
      alive = false;
    };
  }, []);

  const primary = useMemo(() => events[0] ?? null, [events]);
  const secondary = useMemo(() => events[1] ?? null, [events]);

  if (!loading && events.length === 0) return null;

  return (
    <section className="flat-spacing pt-0">
      <div className="container">
        {loading ? (
          <div className="py-5 d-flex justify-content-center">
            <InlineTemplateLoader />
          </div>
        ) : (
        <div className="tf-grid-layout md-col-2">
          <div className="collection-default hover-img">
            <Link className="img-style" href={`/events/${primary?.eventNo ?? ""}`}>
              <Image
                className="lazyload"
                data-src={safeSrc(primary?.thumbnailUrl, "/images/section/page-title.jpg")}
                alt={primary?.eventTitle || "이벤트 배너"}
                src={safeSrc(primary?.thumbnailUrl, "/images/section/page-title.jpg")}
                width={945}
                height={709}
              />
            </Link>
            <div className="content">
              <h3 className="title wow fadeInUp">
                <Link href={`/events/${primary?.eventNo ?? ""}`} className="link">
                  {primary?.eventTitle || "진행 중인 이벤트"}
                </Link>
              </h3>
              <p className="desc wow fadeInUp">
                지금 참여 가능한 혜택과 기획전을 한눈에 확인해보세요.
              </p>
              <div className="wow fadeInUp">
                <Link href={`/events/${primary?.eventNo ?? ""}`} className="btn-line">
                  자세히 보기
                </Link>
              </div>
            </div>
          </div>
          <div className="collection-position hover-img">
            <Link className="img-style" href={`/events/${secondary?.eventNo ?? primary?.eventNo ?? ""}`}>
              <Image
                className="lazyload"
                data-src={safeSrc(
                  secondary?.thumbnailUrl,
                  safeSrc(primary?.thumbnailUrl, "/images/section/page-title.jpg"),
                )}
                alt={secondary?.eventTitle || "추천 이벤트 배너"}
                src={safeSrc(
                  secondary?.thumbnailUrl,
                  safeSrc(primary?.thumbnailUrl, "/images/section/page-title.jpg"),
                )}
                width={945}
                height={945}
              />
            </Link>
            <div className="content">
              <h3 className="title">
                <Link
                  href={`/events/${secondary?.eventNo ?? primary?.eventNo ?? ""}`}
                  className="link text-white wow fadeInUp"
                >
                  {secondary?.eventTitle || "추천 이벤트"}
                </Link>
              </h3>
              <p className="desc text-white wow fadeInUp">
                시즌 추천 이벤트를 확인하고 놓치기 쉬운 혜택까지 챙겨보세요.
              </p>
              <div className="wow fadeInUp">
                <Link
                  href={`/events/${secondary?.eventNo ?? primary?.eventNo ?? ""}`}
                  className="btn-line style-white"
                >
                  자세히 보기
                </Link>
              </div>
            </div>
          </div>
        </div>
        )}
      </div>
    </section>
  );
}
