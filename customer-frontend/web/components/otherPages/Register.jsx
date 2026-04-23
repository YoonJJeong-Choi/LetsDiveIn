"use client";

import Link from "next/link";
import { useState } from "react";
import { join } from "@/lib/api/customer";
import { useRouter } from "next/navigation";

export default function Register() {
  const router = useRouter();
  const [customerName, setCustomerName] = useState("");
  const [customerEmail, setCustomerEmail] = useState("");
  const [customerPassword, setCustomerPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [customerBirth, setCustomerBirth] = useState("");
  const [passwordType, setPasswordType] = useState("password");
  const [confirmPasswordType, setConfirmPasswordType] = useState("password");
  const [message, setMessage] = useState({ type: "", text: "" });
  const [loading, setLoading] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(true);

  const togglePassword = () => {
    setPasswordType((prevType) =>
      prevType === "password" ? "text" : "password"
    );
  };

  const toggleConfirmPassword = () => {
    setConfirmPasswordType((prevType) =>
      prevType === "password" ? "text" : "password"
    );
  };

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage({ type: "", text: "" });

    // 유효성 검사
    if (!customerName.trim()) {
      setMessage({ type: "error", text: "이름을 입력해주세요." });
      return;
    }

    if (!customerEmail.trim()) {
      setMessage({ type: "error", text: "이메일을 입력해주세요." });
      return;
    }

    if (!customerPassword) {
      setMessage({ type: "error", text: "비밀번호를 입력해주세요." });
      return;
    }

    if (customerPassword.length < 6) {
      setMessage({ type: "error", text: "비밀번호는 6자 이상이어야 합니다." });
      return;
    }

    if (customerPassword !== confirmPassword) {
      setMessage({ type: "error", text: "비밀번호가 일치하지 않습니다." });
      return;
    }

    if (!customerBirth) {
      setMessage({ type: "error", text: "생년월일을 입력해주세요." });
      return;
    }

    if (!agreeTerms) {
      setMessage({ type: "error", text: "이용약관에 동의해주세요." });
      return;
    }

    setLoading(true);
    try {
      const res = await join({
        customerName: customerName.trim(),
        customerEmail: customerEmail.trim(),
        customerPassword,
        customerBirth,
      });
      setMessage({ type: "success", text: res || "입력하신 이메일로 인증 요청하였습니다." });
      
      // 성공 시 폼 초기화
      setCustomerName("");
      setCustomerEmail("");
      setCustomerPassword("");
      setConfirmPassword("");
      setCustomerBirth("");
      
      // 3초 후 로그인 페이지로 이동
      setTimeout(() => {
        router.push("/login");
      }, 3000);
    } catch (err) {
      // 에러 메시지 추출
      let errorMessage = "회원가입 요청에 실패했습니다.";
      
      if (err.response?.data) {
        const errorData = err.response.data;
        if (typeof errorData === 'object' && errorData.message) {
          errorMessage = errorData.message;
        } else if (typeof errorData === 'string') {
          errorMessage = errorData;
        }
      } else if (err.message) {
        errorMessage = err.message;
      }
      
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="flat-spacing">
      <div className="container">
        <div className="login-wrap">
          <div className="left">
            <div className="heading">
              <h4>회원가입</h4>
            </div>
            <form
              onSubmit={handleSubmit}
              className="form-login form-has-password"
            >
              <div className="wrap">
                <fieldset className="">
                  <input
                    className=""
                    type="text"
                    placeholder="이름*"
                    name="customerName"
                    value={customerName}
                    onChange={(e) => setCustomerName(e.target.value)}
                    tabIndex={1}
                    aria-required="true"
                    required
                  />
                </fieldset>
                <fieldset className="">
                  <input
                    className=""
                    type="email"
                    placeholder="이메일 주소*"
                    name="customerEmail"
                    value={customerEmail}
                    onChange={(e) => setCustomerEmail(e.target.value)}
                    tabIndex={2}
                    aria-required="true"
                    required
                  />
                </fieldset>
                <fieldset className="position-relative password-item">
                  <input
                    className="input-password"
                    type={passwordType}
                    placeholder="비밀번호*"
                    name="customerPassword"
                    value={customerPassword}
                    onChange={(e) => setCustomerPassword(e.target.value)}
                    tabIndex={3}
                    aria-required="true"
                    required
                    minLength={6}
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

                <fieldset className="position-relative password-item">
                  <input
                    className="input-password"
                    type={confirmPasswordType}
                    placeholder="비밀번호 확인*"
                    name="confirmPassword"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    tabIndex={4}
                    aria-required="true"
                    required
                  />
                  <span
                    className={`toggle-password ${
                      !(confirmPasswordType === "text") ? "unshow" : ""
                    }`}
                    onClick={toggleConfirmPassword}
                  >
                    <i
                      className={`icon-eye-${
                        !(confirmPasswordType === "text") ? "hide" : "show"
                      }-line`}
                    />
                  </span>
                </fieldset>

                <fieldset className="">
                  <input
                    className=""
                    type="date"
                    placeholder="생년월일*"
                    name="customerBirth"
                    value={customerBirth}
                    onChange={(e) => setCustomerBirth(e.target.value)}
                    tabIndex={5}
                    aria-required="true"
                    required
                  />
                </fieldset>

                {message.text && (
                  <div
                    className={`alert ${
                      message.type === "error" ? "alert-danger" : "alert-success"
                    }`}
                    style={{
                      marginTop: "1rem",
                      padding: "0.75rem",
                      borderRadius: "4px",
                      color: message.type === "error" ? "#dc3545" : "#28a745",
                      backgroundColor: message.type === "error" ? "#f8d7da" : "#d4edda",
                    }}
                  >
                    {message.text}
                  </div>
                )}

                <div className="d-flex align-items-center">
                  <div className="tf-cart-checkbox">
                    <div className="tf-checkbox-wrapp">
                      <input
                        checked={agreeTerms}
                        onChange={(e) => setAgreeTerms(e.target.checked)}
                        className=""
                        type="checkbox"
                        id="login-form_agree"
                        name="agree_checkbox"
                      />
                      <div>
                        <i className="icon-check" />
                      </div>
                    </div>
                    <label
                      className="text-secondary-2"
                      htmlFor="login-form_agree"
                    >
                      이용약관에 동의합니다&nbsp;
                    </label>
                  </div>
                  <Link href={`/term-of-use`} title="Terms of Service">
                    이용약관
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
                    {loading ? "처리 중..." : "가입하기"}
                  </span>
                </button>
              </div>
            </form>
          </div>
          <div className="right">
            <h4 className="mb_8">이미 계정이 있으신가요?</h4>
            <p className="text-secondary">
              로그인하시면 주문 내역·위시리스트 등 회원 전용 서비스를 이용하실 수
              있습니다.
            </p>
            <Link href={`/login`} className="tf-btn btn-fill">
              <span className="text text-button">로그인</span>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
