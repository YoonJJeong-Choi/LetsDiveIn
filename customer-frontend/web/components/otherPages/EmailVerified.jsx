"use client";

import Link from "next/link";
import { Suspense, useMemo } from "react";
import { useSearchParams } from "next/navigation";

function EmailVerifiedInner() {
  const searchParams = useSearchParams();
  const status = searchParams.get("status") || "invalid";

  const content = useMemo(() => {
    switch (status) {
      case "success":
        return {
          title: "이메일 인증이 완료되었습니다",
          desc: "이제 로그인 후 서비스를 이용하실 수 있습니다.",
          variant: "success",
        };
      case "already":
        return {
          title: "이미 인증된 이메일입니다",
          desc: "로그인 페이지에서 로그인해 주세요.",
          variant: "info",
        };
      case "expired":
        return {
          title: "인증 링크가 만료되었습니다",
          desc: "회원가입을 다시 진행하시거나, 고객센터로 문의해 주세요.",
          variant: "warning",
        };
      default:
        return {
          title: "인증을 처리할 수 없습니다",
          desc: "링크가 잘못되었거나 이미 사용되었을 수 있습니다. 회원가입을 다시 시도해 주세요.",
          variant: "danger",
        };
    }
  }, [status]);

  const alertClass =
    content.variant === "success"
      ? "alert-success"
      : content.variant === "info"
        ? "alert-info"
        : content.variant === "warning"
          ? "alert-warning"
          : "alert-danger";

  return (
    <section className="flat-spacing">
      <div className="container" style={{ maxWidth: 560 }}>
        <div className={`alert ${alertClass}`} role="alert">
          <h4 className="alert-heading mb-3">{content.title}</h4>
          <p className="mb-0">{content.desc}</p>
        </div>
        <div className="d-flex flex-wrap gap-3 justify-content-center mt-4">
          <Link href="/login" className="tf-btn btn-fill animate-hover-btn">
            로그인하기
          </Link>
          <Link href="/register" className="tf-btn btn-outline animate-hover-btn">
            회원가입
          </Link>
          <Link href="/" className="tf-btn btn-outline animate-hover-btn">
            홈으로
          </Link>
        </div>
      </div>
    </section>
  );
}

export default function EmailVerified() {
  return (
    <Suspense
      fallback={
        <section className="flat-spacing">
          <div className="container text-center py-5 text-muted">불러오는 중…</div>
        </section>
      }
    >
      <EmailVerifiedInner />
    </Suspense>
  );
}
