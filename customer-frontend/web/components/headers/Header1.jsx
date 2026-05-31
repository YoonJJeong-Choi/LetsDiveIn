"use client";
import React, { useState, useEffect } from "react";
import Nav from "./Nav";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import CartLength from "../common/CartLength";
import { getMe, logout as authLogout } from "@/lib/api/auth";
import { useContextElement } from "@/context/Context";
import { STORE_EMAIL, STORE_PHONE_DISPLAY } from "@/data/storeContact";

export default function Header1({ fullWidth = false }) {
  const router = useRouter();
  const [user, setUser] = useState(null);
  const { syncCartForLogout } = useContextElement();

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => setUser(null));
  }, []);

  const handleLogout = async () => {
    try {
      await authLogout();
    } catch (e) {
      // 서버 로그아웃 실패여도 프론트 상태는 정리
    } finally {
      setUser(null);
      syncCartForLogout();
      if (typeof window !== "undefined") {
        window.location.href = "/";
      } else {
        router.push("/");
        router.refresh();
      }
    }
  };
  return (
    <header
      id="header"
      className={`header-default ${fullWidth ? "header-fullwidth" : ""} `}
    >
      <div className={fullWidth ? "" : "container"}>
        <div className="row wrapper-header align-items-center">
          <div className="col-md-4 col-3 d-xl-none">
            <a
              href="#mobileMenu"
              className="mobile-menu"
              data-bs-toggle="offcanvas"
              aria-controls="mobileMenu"
            >
              <i className="icon icon-categories" />
            </a>
          </div>
          <div className="col-xl-3 col-md-4 col-6">
            <Link href={`/`} className="logo-header">
              <Image
                alt="logo"
                className="logo"
                src="/images/logo/LetsDiveIn03.png"
                width={144}
                height={25}
              />
            </Link>
          </div>
          <div className="col-xl-6 d-none d-xl-block">
            <nav className="box-navigation text-center">
              <ul className="box-nav-ul d-flex align-items-center justify-content-center">
                <Nav />
              </ul>
            </nav>
          </div>
          <div className="col-xl-3 col-md-4 col-3">
            <ul className="nav-icon d-flex justify-content-end align-items-center">
              <li className="nav-search">
                <a
                  href="#search"
                  data-bs-toggle="modal"
                  className="nav-icon-item"
                >
                  <svg
                    className="icon"
                    width={24}
                    height={24}
                    viewBox="0 0 24 24"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                  >
                    <path
                      d="M11 19C15.4183 19 19 15.4183 19 11C19 6.58172 15.4183 3 11 3C6.58172 3 3 6.58172 3 11C3 15.4183 6.58172 19 11 19Z"
                      stroke="#181818"
                      strokeWidth={2}
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                    <path
                      d="M21.35 21.0004L17 16.6504"
                      stroke="#181818"
                      strokeWidth={2}
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                </a>
              </li>
              <li className="nav-account">
                <a href="#" className="nav-icon-item">
                  <svg
                    className="icon"
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
                </a>
                <div className="dropdown-account dropdown-login">
                  <div className="sub-top">
                    {user ? (
                      <>
                        <p className="text-center mb-2">안녕하세요, <strong>{user.name}</strong>님</p>
                        <Link href={`/my-account`} className="tf-btn btn-reset d-block mb-2 text-center">마이페이지</Link>
                        <button type="button" className="tf-btn btn-reset w-100" onClick={handleLogout}>로그아웃</button>
                      </>
                    ) : (
                      <>
                        <Link href={`/login`} className="tf-btn btn-reset">
                          로그인
                        </Link>
                    <p className="text-center text-secondary-2 mb-0">
                      계정이 없으신가요?{" "}
                      <Link href={`/register`}>회원가입</Link>
                    </p>
                      </>
                    )}
                  </div>
                  <div className="sub-bot">
                    <span className="body-text-">고객센터</span>
                    <p className="text-secondary-2 mb-0" style={{ fontSize: "12px", marginTop: "6px" }}>
                      전화: {STORE_PHONE_DISPLAY}
                    </p>
                    <p className="text-secondary-2 mb-0" style={{ fontSize: "12px" }}>
                      이메일: {STORE_EMAIL}
                    </p>
                  </div>
                </div>
              </li>
              <li className="nav-cart">
                <a
                  href="#shoppingCart"
                  data-bs-toggle="modal"
                  className="nav-icon-item"
                >
                  <svg
                    className="icon"
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
                  <span className="count-box">
                    <CartLength />
                  </span>
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </header>
  );
}
