"use client";

import { useState } from "react";
import { join } from "../../lib/api/customer";

export default function SignupPage() {
  const [customerName, setCustomerName] = useState("");
  const [customerEmail, setCustomerEmail] = useState("");
  const [customerPassword, setCustomerPassword] = useState("");
  const [customerBirth, setCustomerBirth] = useState("");
  const [message, setMessage] = useState({ type: "", text: "" });
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage({ type: "", text: "" });
    setLoading(true);
    try {
      // 생년월일 필수 검증
      if (!customerBirth) {
        setMessage({ type: "error", text: "생년월일을 입력해주세요." });
        setLoading(false);
        return;
      }

      const res = await join({
        customerName: customerName.trim(),
        customerEmail: customerEmail.trim(),
        customerPassword,
        customerBirth, // 필수 필드
      });
      setMessage({ type: "success", text: res || "입력하신 이메일로 인증 요청하였습니다." });
      setCustomerName("");
      setCustomerEmail("");
      setCustomerPassword("");
      setCustomerBirth("");
    } catch (err) {
      // 에러 메시지 추출 (ApiResponse 형태 또는 일반 에러)
      let errorMessage = "회원가입 요청에 실패했습니다.";
      
      if (err.response?.data) {
        const errorData = err.response.data;
        // ApiResponse 형태: { success: false, message: "...", code: "..." }
        if (typeof errorData === 'object' && errorData.message) {
          errorMessage = errorData.message;
        } 
        // 단순 문자열
        else if (typeof errorData === 'string') {
          errorMessage = errorData;
        }
        // 기타 객체 형태
        else {
          errorMessage = JSON.stringify(errorData);
        }
      } 
      // 일반 Error 객체
      else if (err.message) {
        errorMessage = err.message;
      }
      
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ maxWidth: 400, margin: "2rem auto", padding: 16 }}>
      <h1>고객 회원가입</h1>
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: "block", marginBottom: 4 }}>이름</label>
          <input
            type="text"
            value={customerName}
            onChange={(e) => setCustomerName(e.target.value)}
            required
            placeholder="홍길동"
            style={{ width: "100%", padding: 8 }}
          />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: "block", marginBottom: 4 }}>이메일</label>
          <input
            type="email"
            value={customerEmail}
            onChange={(e) => setCustomerEmail(e.target.value)}
            required
            placeholder="you@example.com"
            style={{ width: "100%", padding: 8 }}
          />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: "block", marginBottom: 4 }}>비밀번호</label>
          <input
            type="password"
            value={customerPassword}
            onChange={(e) => setCustomerPassword(e.target.value)}
            required
            minLength={6}
            style={{ width: "100%", padding: 8 }}
          />
        </div>
        <div style={{ marginBottom: 12 }}>
          <label style={{ display: "block", marginBottom: 4 }}>생년월일</label>
          <input
            type="date"
            value={customerBirth}
            onChange={(e) => setCustomerBirth(e.target.value)}
            required
            style={{ width: "100%", padding: 8 }}
          />
        </div>
        {message.text && (
          <p style={{ color: message.type === "error" ? "red" : "green", marginBottom: 12 }}>
            {message.text}
          </p>
        )}
        <button type="submit" disabled={loading} style={{ padding: "8px 16px" }}>
          {loading ? "처리 중..." : "가입 요청"}
        </button>
      </form>
    </div>
  );
}
