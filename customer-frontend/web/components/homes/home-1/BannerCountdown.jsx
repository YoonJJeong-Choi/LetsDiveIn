"use client";

import React, { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import CountdownTimer from "@/components/common/Countdown";
import { getPublicEventList } from "@/lib/api/event";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

function safeSrc(url, fallback) {
  if (typeof url !== "string") return fallback;
  const t = url.trim();
  return t || fallback;
}

function isNoticeEvent(event) {
  const t = String(event?.eventType || "").toUpperCase();
  return t === "NOTICE" || t.includes("NOTICE") || t.includes("ANNOUNCE");
}

function pickCountdownEvent(list) {
  const arr = Array.isArray(list) ? list : [];
  const now = Date.now();
  return arr
    .filter((e) => e?.eventStatus === "ACTIVE" && e?.customerEventEndAt)
    .filter((e) => !isNoticeEvent(e))
    .sort((a, b) => {
      const aEnd = a?.customerEventEndAt ? new Date(a.customerEventEndAt).getTime() : 0;
      const bEnd = b?.customerEventEndAt ? new Date(b.customerEventEndAt).getTime() : 0;
      return aEnd - bEnd;
    })
    .find((e) => {
      const end = e?.customerEventEndAt ? new Date(e.customerEventEndAt).getTime() : 0;
      return end > now;
    });
}

export default function BannerCountdown() {
  const [loading, setLoading] = useState(true);
  const [events, setEvents] = useState([]);

  useEffect(() => {
    let alive = true;
    const run = async () => {
      try {
        const resp = await getPublicEventList();
        const list = resp?.data || resp || [];
        if (!alive) return;
        setEvents(Array.isArray(list) ? list : []);
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

  const countdownEvent = useMemo(() => pickCountdownEvent(events), [events]);

  if (!loading && !countdownEvent) return null;

  return (
    <section className="bg-surface flat-spacing flat-countdown-banner">
      <div className="container">
        {loading ? (
          <div className="py-5 d-flex justify-content-center">
            <InlineTemplateLoader />
          </div>
        ) : (
        <div className="row align-items-center">
          <div className="col-lg-5">
            <div className="banner-left">
              <div className="box-title">
                <h3 className="wow fadeInUp">
                  {countdownEvent?.eventTitle || "진행 중인 이벤트"}
                </h3>
                <p className="text-secondary wow fadeInUp">
                  종료까지 남은 시간을 확인하고 혜택을 놓치지 마세요.
                </p>
              </div>
              <div className="btn-banner wow fadeInUp">
                <Link
                  href={`/events/${countdownEvent?.eventNo ?? ""}`}
                  className="tf-btn btn-fill"
                >
                  <span className="text">이벤트 보기</span>
                  <i className="icon icon-arrowUpRight" />
                </Link>
              </div>
            </div>
          </div>
          <div className="col-lg-2">
            <div className="banner-img">
              <Image
                className="lazyload"
                data-src={safeSrc(countdownEvent?.thumbnailUrl, "/images/banner/img-countdown1.png")}
                alt={countdownEvent?.eventTitle || "이벤트 카운트다운 배너"}
                src={safeSrc(countdownEvent?.thumbnailUrl, "/images/banner/img-countdown1.png")}
                width={607}
                height={655}
              />
            </div>
          </div>
          <div className="col-lg-5">
            <div className="banner-right">
              <div className="tf-countdown-lg">
                <div
                  className="js-countdown"
                  data-timer={1007500}
                  data-labels="Days,Hours,Mins,Secs"
                >
                  <CountdownTimer
                    style={2}
                    targetDate={countdownEvent?.customerEventEndAt}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        )}
      </div>
    </section>
  );
}
