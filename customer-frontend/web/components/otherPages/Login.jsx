"use client";
import React, { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { login as authLogin } from "@/lib/api/auth";
import { getCart } from "@/lib/api/cart";
import { useContextElement } from "@/context/Context";
import { getPartnerAdminLoginUrl } from "@/lib/adminUrls";

function safeNextPath(next) {
  if (!next || typeof next !== "string") return null;
  const t = next.trim();
  if (!t.startsWith("/") || t.startsWith("//")) return null;
  return t;
}

function getPortalErrorMessage(message) {
  if (message) {
    return message;
  }
  return "고객몰은 고객 계정만 로그인할 수 있습니다. 관리자 또는 파트너 계정은 관리자 페이지를 이용해주세요.";
}

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { setCartProducts, setIsLoggedIn } = useContextElement();
  const [passwordType, setPasswordType] = useState("password");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [portalError, setPortalError] = useState(false);
  const [loading, setLoading] = useState(false);
  const showPortalGuide = searchParams.get("reason") === "portal" || portalError;

  const togglePassword = () => {
    setPasswordType((prevType) =>
      prevType === "password" ? "text" : "password"
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setPortalError(false);
    setLoading(true);
    try {
      await authLogin(email, password);

      const { getMe } = await import("@/lib/api/auth");
      const userData = await getMe();
      const user = userData?.data || userData;
      const userRole = user?.role;

      if (userRole && userRole !== "CUSTOMER") {
        setPortalError(true);
        setError(getPortalErrorMessage());
        return;
      }

      setIsLoggedIn(true);

      try {
        const cartData = await getCart();
        const transformedItems =
          cartData?.items?.map((item) => ({
            id: item.productNo,
            title: item.productName,
            imgSrc: item.productImageUrl,
            price: item.itemPrice,
            quantity: item.quantity,
            selectedOptionNo: item.optionNo,
            color: item.color,
            size: item.size,
            optionName:
              item.color && item.size
                ? `${item.color} / ${item.size}`
                : item.color || item.size || null,
            cartItemNo: item.cartItemNo,
          })) || [];
        setCartProducts(transformedItems);
      } catch (cartError) {
        console.error("로그인 후 장바구니 동기화 실패:", cartError);
      }

      const next = safeNextPath(searchParams.get("next"));
      router.push(next || "/");
      router.refresh();
    } catch (err) {
      const status = err.response?.status;
      const backendMessage = err.response?.data?.message;
      const isPortalMismatch = status === 403;
      const msg = isPortalMismatch
        ? getPortalErrorMessage(backendMessage)
        : backendMessage || "로그인에 실패했습니다.";
      setPortalError(isPortalMismatch);
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="flat-spacing">
      <div className="container">
        <div className="login-wrap">
          <div className="left">
            <div className="heading">
              <h4>로그인</h4>
            </div>
            <form
              onSubmit={handleSubmit}
              className="form-login form-has-password"
            >
              {showPortalGuide && (
                <div className="alert alert-warning mb-3" role="status">
                  <strong>현재 고객 페이지에서 사용할 수 없는 계정입니다.</strong>
                  <p className="mt-2 mb-2">
                    관리자 또는 파트너 계정은 고객몰에 로그인할 수 없습니다.
                    고객 계정으로 다시 로그인하거나 관리자 페이지를 이용해주세요.
                  </p>
                  <Link
                    href={getPartnerAdminLoginUrl()}
                    className="text-button link"
                  >
                    관리자 로그인으로 이동
                  </Link>
                </div>
              )}
              {error && !portalError && (
                <p className="text-danger mb-2" role="alert">
                  {error}
                </p>
              )}
              <div className="wrap">
                <fieldset className="">
                  <input
                    className=""
                    type="email"
                    placeholder="이메일 주소*"
                    name="email"
                    tabIndex={2}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    aria-required="true"
                    required
                  />
                </fieldset>
                <fieldset className="position-relative password-item">
                  <input
                    className="input-password"
                    type={passwordType}
                    placeholder="비밀번호*"
                    name="password"
                    tabIndex={2}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    aria-required="true"
                    required
                  />
                  <span
                    className={`toggle-password ${
                      !(passwordType === "text") ? "unshow" : ""
                    }`}
                    onClick={togglePassword}
                  >
                    <i
                      className={`icon-eye-${
                        !(passwordType === "text") ? "hide" : "show"
                      }-line`}
                    />
                  </span>
                </fieldset>
                <div className="d-flex align-items-center">
                  <div className="tf-cart-checkbox">
                    <div className="tf-checkbox-wrapp">
                      <input
                        defaultChecked
                        className=""
                        type="checkbox"
                        id="login-form_agree"
                        name="agree_checkbox"
                      />
                      <div>
                        <i className="icon-check" />
                      </div>
                    </div>
                    <label htmlFor="login-form_agree">로그인 상태 유지</label>
                  </div>
                </div>
              </div>
              <div className="button-submit">
                <button
                  className="tf-btn btn-fill"
                  type="submit"
                  disabled={loading}
                >
                  <span className="text text-button">
                    {loading ? "로그인 중..." : "로그인"}
                  </span>
                </button>
              </div>
            </form>
          </div>
          <div className="right">
            <h4 className="mb_8">처음이신가요?</h4>
            <p className="text-secondary">
              회원 가입 후 주문 조회, 맞춤 혜택 등 스윔몰의 서비스를 편리하게
              이용해 보세요.
            </p>
            <Link href={`/register`} className="tf-btn btn-fill">
              <span className="text text-button">회원가입</span>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

export default function Login() {
  return (
    <Suspense fallback={
      <section className="flat-spacing">
        <div className="container text-center py-5">
          <div className="spinner-border" role="status" />
        </div>
      </section>
    }>
      <LoginForm />
    </Suspense>
  );
}
