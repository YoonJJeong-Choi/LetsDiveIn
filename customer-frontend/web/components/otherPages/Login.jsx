"use client";
import React, { Suspense, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { login as authLogin } from "@/lib/api/auth";
import { getCart } from "@/lib/api/cart";
import { useContextElement } from "@/context/Context";

function safeNextPath(next) {
  if (!next || typeof next !== "string") return null;
  const t = next.trim();
  if (!t.startsWith("/") || t.startsWith("//")) return null;
  return t;
}

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { setCartProducts, setIsLoggedIn } = useContextElement();
  const [passwordType, setPasswordType] = useState("password");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const togglePassword = () => {
    setPasswordType((prevType) =>
      prevType === "password" ? "text" : "password"
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await authLogin(email, password);

      setIsLoggedIn(true);

      const { getMe } = await import("@/lib/api/auth");
      const userData = await getMe();
      const user = userData?.data || userData;
      const userRole = user?.role;

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

      if (userRole === "ADMIN" || userRole === "PARTNER") {
        router.push("/admin/dashboard");
      } else {
        router.push(next || "/");
      }
      router.refresh();
    } catch (err) {
      const msg = err.response?.data?.message || "로그인에 실패했습니다.";
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
              {error && (
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
                <div className="d-flex align-items-center justify-content-between">
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
                  <Link
                    href={`/forget-password`}
                    className="font-2 text-button forget-password link"
                  >
                    비밀번호를 잊으셨나요?
                  </Link>
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
