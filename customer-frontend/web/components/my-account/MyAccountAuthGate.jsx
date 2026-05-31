"use client";

import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { getMe, isPortalAccessError } from "@/lib/api/auth";

export default function MyAccountAuthGate({ children }) {
  const router = useRouter();
  const pathname = usePathname();
  const [allowed, setAllowed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const user = await getMe({ throwOnForbidden: true });
        if (cancelled) return;
        if (!user) {
          const next = encodeURIComponent(pathname || "/my-account");
          router.replace(`/login?next=${next}`);
          return;
        }
        setAllowed(true);
      } catch (err) {
        if (cancelled) return;
        const next = encodeURIComponent(pathname || "/my-account");
        if (isPortalAccessError(err)) {
          router.replace(`/login?reason=portal&next=${next}`);
          return;
        }
        alert("로그인이 필요합니다.");
        router.replace(`/login?next=${next}`);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [router, pathname]);

  if (!allowed) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status" aria-label="로딩" />
        <p className="mt-3 text-caption-1">확인 중...</p>
      </div>
    );
  }

  return <>{children}</>;
}
