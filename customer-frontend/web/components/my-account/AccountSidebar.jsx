"use client";
import React, { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { getMe, logout as authLogout } from "@/lib/api/auth";
import { useContextElement } from "@/context/Context";

export default function AccountSidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { syncCartForLogout } = useContextElement();
  const [user, setUser] = useState(null);

  useEffect(() => {
    getMe()
      .then((userData) => {
        console.log("AccountSidebar - getMe 응답:", userData);
        let user = null;
        if (userData?.data) {
          user = userData.data;
        } else if (userData) {
          user = userData;
        }
        console.log("AccountSidebar - 설정할 user:", user);
        console.log("AccountSidebar - user.role:", user?.role);
        setUser(user);
      })
      .catch((err) => {
        console.error("AccountSidebar - getMe 에러:", err);
      });
  }, []);

  // role이 문자열 또는 enum 객체일 수 있으므로 둘 다 체크
  const userRole = user?.role;
  const isAdmin = userRole === "ADMIN" || userRole?.name === "ADMIN";
  const isPartner = userRole === "PARTNER" || userRole?.name === "PARTNER";
  const isAdminOrPartner = isAdmin || isPartner;
  
  // 디버깅용 로그
  useEffect(() => {
    if (user) {
      console.log("AccountSidebar - 현재 user:", user);
      console.log("AccountSidebar - user.role:", user.role);
      console.log("AccountSidebar - user.role 타입:", typeof user.role);
      console.log("AccountSidebar - isPartner:", isPartner);
    }
  }, [user, isPartner]);

  const handleLogout = async () => {
    try {
      await authLogout();
    } catch (e) {
      // 서버 로그아웃 실패여도 프론트 상태는 정리
    } finally {
      setUser(null);
      syncCartForLogout();
      if (typeof window !== "undefined") {
        window.location.href = "/login";
      } else {
        router.replace("/login");
        router.refresh();
      }
    }
  };
  
  return (
    <div className="wrap-sidebar-account">
      <div className="sidebar-account">
        <div className="account-avatar">
          <h6 className="mb_4">{user?.name || "사용자"}</h6>
          <div className="body-text-1">{user?.email || ""}</div>
          {isAdminOrPartner && (
            <div className="badge bg-primary mt-2">
              {isAdmin ? "관리자" : "파트너"}
            </div>
          )}
        </div>
        <ul className="my-account-nav">
          <li>
            <Link
              href={`/my-account-orders`}
              className={`my-account-nav-item ${
                pathname == "/my-account-orders" ? "active" : ""
              } `}
            >
              <svg
                width={24}
                height={24}
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M16.5078 10.8734V6.36686C16.5078 5.17166 16.033 4.02541 15.1879 3.18028C14.3428 2.33514 13.1965 1.86035 12.0013 1.86035C10.8061 1.86035 9.65985 2.33514 8.81472 3.18028C7.96958 4.02541 7.49479 5.17166 7.49479 6.36686V10.8734M4.11491 8.62012H19.8877L21.0143 22.1396H2.98828L4.11491 8.62012Z"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              주문 내역
            </Link>
          </li>
          <li>
            <Link
              href={`/my-account-returns`}
              className={`my-account-nav-item ${
                pathname == "/my-account-returns" ? "active" : ""
              } `}
            >
              <svg
                width={24}
                height={24}
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M3 7H21L20 21H4L3 7Z"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M8 7V4C8 3.46957 8.21071 2.96086 8.58579 2.58579C8.96086 2.21071 9.46957 2 10 2H14C14.5304 2 15.0391 2.21071 15.4142 2.58579C15.7893 2.96086 16 3.46957 16 4V7"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M9 12L12 15L15 12"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              반품 내역
            </Link>
          </li>
          <li>
            <Link
              href={`/my-account-reviews`}
              className={`my-account-nav-item ${
                pathname == "/my-account-reviews" ? "active" : ""
              } `}
            >
              <svg
                width={24}
                height={24}
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              내 리뷰
            </Link>
          </li>
          <li>
            <Link
              href={`/my-account-points`}
              className={`my-account-nav-item ${
                pathname == "/my-account-points" ? "active" : ""
              } `}
            >
              <svg
                width={24}
                height={24}
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M12 2C6.48 2 2 6.48 2 12C2 17.52 6.48 22 12 22C17.52 22 22 17.52 22 12C22 6.48 17.52 2 12 2ZM12 20C7.59 20 4 16.41 4 12C4 7.59 7.59 4 12 4C16.41 4 20 7.59 20 12C20 16.41 16.41 20 12 20Z"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M12 6V12L16 14"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              포인트
            </Link>
          </li>
          <li>
            <Link
              href={`/my-account`}
              className={`my-account-nav-item ${
                pathname == "/my-account" ? "active" : ""
              } `}
            >
              <svg
                width={24}
                height={24}
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M12 11C14.2091 11 16 9.20914 16 7C16 4.79086 14.2091 3 12 3C9.79086 3 8 4.79086 8 7C8 9.20914 9.79086 11 12 11Z"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              내 정보
            </Link>
          </li>
          <li>
            <button
              type="button"
              className={`my-account-nav-item ${
                pathname == "/login" ? "active" : ""
              } `}
              style={{ width: "100%", textAlign: "left", background: "none", border: 0 }}
              onClick={handleLogout}
            >
              <svg
                width={24}
                height={24}
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  d="M9 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H9"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M16 17L21 12L16 7"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <path
                  d="M21 12H9"
                  stroke="#181818"
                  strokeWidth={2}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
              로그아웃
            </button>
          </li>
        </ul>
      </div>
    </div>
  );
}
