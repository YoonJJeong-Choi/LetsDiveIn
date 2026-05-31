"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { applyForPartnership } from "@/lib/api/partner";
import { uploadFile } from "@/lib/api/file";
import { getPartnerAdminLoginUrl } from "@/lib/adminUrls";

export default function PartnerApplication() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    email: "",
    partnerName: "",
    partnerContact: "",
    partnerBankAccount: "",
    businessRegistrationNumber: "",
    representativeBrandCode: "",
  });
  const [message, setMessage] = useState({ type: "", text: "" });
  const [loading, setLoading] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [bizRegFile, setBizRegFile] = useState(null);
  const [bankbookFile, setBankbookFile] = useState(null);
  const [uploading, setUploading] = useState({ biz: false, bank: false });
  const [uploaded, setUploaded] = useState({ biz: null, bank: null });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // 연락처 자동 포맷팅 (010-XXXX-XXXX)
  const handleContactChange = (e) => {
    let value = e.target.value.replace(/[^0-9]/g, "");
    if (value.length > 11) value = value.slice(0, 11);
    
    if (value.length > 7) {
      value = value.slice(0, 3) + "-" + value.slice(3, 7) + "-" + value.slice(7);
    } else if (value.length > 3) {
      value = value.slice(0, 3) + "-" + value.slice(3);
    }
    
    setFormData((prev) => ({
      ...prev,
      partnerContact: value,
    }));
  };

  // 정산계좌 자동 포맷팅 (XXX-XXX-XXXXXX)
  const handleBankAccountChange = (e) => {
    let value = e.target.value.replace(/[^0-9]/g, "");
    
    if (value.length > 6) {
      value = value.slice(0, 3) + "-" + value.slice(3, 6) + "-" + value.slice(6);
    } else if (value.length > 3) {
      value = value.slice(0, 3) + "-" + value.slice(3);
    }
    
    setFormData((prev) => ({
      ...prev,
      partnerBankAccount: value,
    }));
  };

  // 사업자등록번호 자동 포맷팅 (XXX-XX-XXXXX)
  const handleBusinessNumberChange = (e) => {
    let value = e.target.value.replace(/[^0-9]/g, "");
    if (value.length > 10) value = value.slice(0, 10);
    
    if (value.length > 5) {
      value = value.slice(0, 3) + "-" + value.slice(3, 5) + "-" + value.slice(5);
    } else if (value.length > 3) {
      value = value.slice(0, 3) + "-" + value.slice(3);
    }
    
    setFormData((prev) => ({
      ...prev,
      businessRegistrationNumber: value,
    }));
  };

  async function handleSubmit(e) {
    e.preventDefault();
    setMessage({ type: "", text: "" });

    // 유효성 검사
    if (!formData.email.trim()) {
      setMessage({ type: "error", text: "이메일을 입력해주세요." });
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      setMessage({ type: "error", text: "올바른 이메일 형식이 아닙니다." });
      return;
    }

    if (!formData.partnerName.trim()) {
      setMessage({ type: "error", text: "파트너명을 입력해주세요." });
      return;
    }

    if (!formData.partnerContact) {
      setMessage({ type: "error", text: "연락처를 입력해주세요." });
      return;
    }

    if (!/^010-\d{4}-\d{4}$/.test(formData.partnerContact)) {
      setMessage({ type: "error", text: "연락처 형식은 010-XXXX-XXXX입니다." });
      return;
    }

    if (!formData.partnerBankAccount) {
      setMessage({ type: "error", text: "정산계좌를 입력해주세요." });
      return;
    }

    if (!/^\d{3}-\d{3}-\d{6,}$/.test(formData.partnerBankAccount)) {
      setMessage({ type: "error", text: "정산계좌 형식은 XXX-XXX-XXXXXX입니다." });
      return;
    }

    if (!formData.businessRegistrationNumber) {
      setMessage({ type: "error", text: "사업자등록번호를 입력해주세요." });
      return;
    }

    if (!/^\d{3}-\d{2}-\d{5}$/.test(formData.businessRegistrationNumber)) {
      setMessage({ type: "error", text: "사업자등록번호 형식은 XXX-XX-XXXXX입니다." });
      return;
    }

    if (!formData.representativeBrandCode.trim()) {
      setMessage({ type: "error", text: "대표 브랜드 코드를 입력해주세요." });
      return;
    }

    if (!agreeTerms) {
      setMessage({ type: "error", text: "이용약관에 동의해주세요." });
      return;
    }

    // 파일 업로드 확인 (필수 2종)
    if (!uploaded.biz?.fileId) {
      setMessage({ type: "error", text: "사업자등록증 사본을 업로드해주세요." });
      return;
    }
    if (!uploaded.bank?.fileId) {
      setMessage({ type: "error", text: "통장 사본을 업로드해주세요." });
      return;
    }

    setLoading(true);
    try {
      const res = await applyForPartnership({
        email: formData.email.trim(),
        partnerName: formData.partnerName.trim(),
        partnerContact: formData.partnerContact,
        partnerBankAccount: formData.partnerBankAccount,
        businessRegistrationNumber: formData.businessRegistrationNumber,
        representativeBrandCode: formData.representativeBrandCode.trim().toUpperCase(),
        businessRegistrationFileId: uploaded.biz.fileId,
        bankAccountFileId: uploaded.bank.fileId,
      });

      setMessage({
        type: "success",
        text: "입점 신청이 완료되었습니다. 결과는 이메일로 안내됩니다.",
      });

      // 성공 시 폼 초기화
      setFormData({
        email: "",
        partnerName: "",
        partnerContact: "",
        partnerBankAccount: "",
        businessRegistrationNumber: "",
        representativeBrandCode: "",
      });
      setAgreeTerms(false);
      setBizRegFile(null);
      setBankbookFile(null);
      setUploaded({ biz: null, bank: null });

      // 메인 페이지로 이동
      try {
        router.replace("/");
      } catch {}
    } catch (err) {
      // 에러 메시지 추출
      let errorMessage = "입점 신청에 실패했습니다.";

      if (err.response?.data) {
        const errorData = err.response.data;
        if (typeof errorData === "object" && errorData.message) {
          errorMessage = errorData.message;
        } else if (typeof errorData === "string") {
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

  async function uploadBizReg() {
    if (!bizRegFile) {
      setMessage({ type: "error", text: "사업자등록증 사본 파일을 선택해주세요." });
      return;
    }
    try {
      setUploading((s) => ({ ...s, biz: true }));
      const result = await uploadFile(bizRegFile, "partner-application-doc");
      const data = result?.data || result;
      setUploaded((s) => ({ ...s, biz: data }));
      setMessage({ type: "success", text: "사업자등록증 사본 업로드가 완료되었습니다." });
    } catch (err) {
      let errorMessage = "사업자등록증 업로드에 실패했습니다.";
      if (err?.response?.data?.message) errorMessage = err.response.data.message;
      else if (err?.message) errorMessage = err.message;
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setUploading((s) => ({ ...s, biz: false }));
    }
  }

  async function uploadBankbook() {
    if (!bankbookFile) {
      setMessage({ type: "error", text: "통장 사본 파일을 선택해주세요." });
      return;
    }
    try {
      setUploading((s) => ({ ...s, bank: true }));
      const result = await uploadFile(bankbookFile, "partner-application-doc");
      const data = result?.data || result;
      setUploaded((s) => ({ ...s, bank: data }));
      setMessage({ type: "success", text: "통장 사본 업로드가 완료되었습니다." });
    } catch (err) {
      let errorMessage = "통장 사본 업로드에 실패했습니다.";
      if (err?.response?.data?.message) errorMessage = err.response.data.message;
      else if (err?.message) errorMessage = err.message;
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setUploading((s) => ({ ...s, bank: false }));
    }
  }

  return (
    <section className="flat-spacing">
      <div className="container">
        <div className="login-wrap">
          <div className="left">
            <div className="heading">
              <h4>파트너 입점 신청</h4>
            </div>
            <form onSubmit={handleSubmit} className="form-login">
              <div className="wrap">
                <fieldset className="">
                  <input
                    className=""
                    type="email"
                    placeholder="이메일 주소*"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    tabIndex={1}
                    aria-required="true"
                    required
                  />
                </fieldset>
                <fieldset className="">
                  <input
                    className=""
                    type="text"
                    placeholder="파트너명 (회사명 또는 브랜드명)*"
                    name="partnerName"
                    value={formData.partnerName}
                    onChange={handleChange}
                    tabIndex={2}
                    aria-required="true"
                    required
                  />
                </fieldset>
                <fieldset className="">
                  <input
                    className=""
                    type="tel"
                    placeholder="연락처 (010-XXXX-XXXX)*"
                    name="partnerContact"
                    value={formData.partnerContact}
                    onChange={handleContactChange}
                    tabIndex={3}
                    aria-required="true"
                    required
                    maxLength={13}
                  />
                </fieldset>
                <fieldset className="">
                  <input
                    className=""
                    type="text"
                    placeholder="정산계좌 (XXX-XXX-XXXXXX)*"
                    name="partnerBankAccount"
                    value={formData.partnerBankAccount}
                    onChange={handleBankAccountChange}
                    tabIndex={4}
                    aria-required="true"
                    required
                  />
                </fieldset>
                <fieldset className="">
                  <input
                    className=""
                    type="text"
                    placeholder="사업자등록번호 (XXX-XX-XXXXX)*"
                    name="businessRegistrationNumber"
                    value={formData.businessRegistrationNumber}
                    onChange={handleBusinessNumberChange}
                    tabIndex={5}
                    aria-required="true"
                    required
                    maxLength={12}
                  />
                </fieldset>
                <fieldset className="">
                  <input
                    className=""
                    type="text"
                    placeholder="대표 브랜드 코드 (예: SPEEDO)*"
                    name="representativeBrandCode"
                    value={formData.representativeBrandCode}
                    onChange={handleChange}
                    tabIndex={6}
                    aria-required="true"
                    required
                  />
                </fieldset>
                <fieldset className="">
                  <label style={{ display: "block", marginBottom: "8px", fontWeight: 600 }}>
                    사업자등록증 사본 (PDF/JPG) — 필수
                  </label>
                  <input
                    className=""
                    type="file"
                    accept=".jpg,.jpeg,.png,.gif,.webp,.pdf"
                    onChange={(e) => setBizRegFile(e.target.files?.[0] || null)}
                  />
                  <div style={{ marginTop: "8px" }}>
                    <button
                      type="button"
                      className="tf-btn btn-fill"
                      onClick={uploadBizReg}
                      disabled={uploading.biz}
                    >
                      <span className="text text-button">
                        {uploading.biz ? "업로드 중..." : "사업자등록증 업로드"}
                      </span>
                    </button>
                  </div>
                  {uploaded.biz?.fileUrl && (
                    <p style={{ marginTop: "8px", fontSize: "14px" }}>
                      업로드 완료:&nbsp;
                      <a href={uploaded.biz.fileUrl} target="_blank" rel="noreferrer">
                        {uploaded.biz.originalName || "파일 보기"}
                      </a>
                    </p>
                  )}
                </fieldset>

                <fieldset className="">
                  <label style={{ display: "block", marginBottom: "8px", fontWeight: 600 }}>
                    통장 사본 (정산 계좌 확인용, PDF/JPG) — 필수
                  </label>
                  <input
                    className=""
                    type="file"
                    accept=".jpg,.jpeg,.png,.gif,.webp,.pdf"
                    onChange={(e) => setBankbookFile(e.target.files?.[0] || null)}
                  />
                  <div style={{ marginTop: "8px" }}>
                    <button
                      type="button"
                      className="tf-btn btn-fill"
                      onClick={uploadBankbook}
                      disabled={uploading.bank}
                    >
                      <span className="text text-button">
                        {uploading.bank ? "업로드 중..." : "통장 사본 업로드"}
                      </span>
                    </button>
                  </div>
                  {uploaded.bank?.fileUrl && (
                    <p style={{ marginTop: "8px", fontSize: "14px" }}>
                      업로드 완료:&nbsp;
                      <a href={uploaded.bank.fileUrl} target="_blank" rel="noreferrer">
                        {uploaded.bank.originalName || "파일 보기"}
                      </a>
                    </p>
                  )}
                </fieldset>

                {message.text && (
                  <div
                    className={`alert ${
                      message.type === "error"
                        ? "alert-danger"
                        : "alert-success"
                    }`}
                    style={{
                      marginTop: "1rem",
                      padding: "0.75rem",
                      borderRadius: "4px",
                      color: message.type === "error" ? "#dc3545" : "#28a745",
                      backgroundColor:
                        message.type === "error" ? "#f8d7da" : "#d4edda",
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
                        id="partner-form_agree"
                        name="agree_checkbox"
                      />
                      <div>
                        <i className="icon-check" />
                      </div>
                    </div>
                    <label
                      className="text-secondary-2"
                      htmlFor="partner-form_agree"
                    >
                      이용약관에 동의합니다&nbsp;
                    </label>
                  </div>
                  <Link
                    href={`/term-of-use`}
                    title="Terms of Service"
                    target="_blank"
                    rel="noreferrer"
                  >
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
                    {loading ? "처리 중..." : "입점 신청하기"}
                  </span>
                </button>
              </div>
            </form>
          </div>
          <div className="right">
            <h4 className="mb_8">입점 안내</h4>
            <p className="text-secondary">
              파트너 입점 신청을 해주시면 관리자 검토 후 승인 여부를 이메일로
              안내드립니다. 승인되면 임시 비밀번호가 발급되어 로그인하실 수
              있습니다.
            </p>
            <p className="text-secondary mt-3">
              입점 신청 시 입력하신 정보는 사업자 정보 확인 용도로만 사용됩니다.
            </p>
            <Link
              href={getPartnerAdminLoginUrl()}
              className="tf-btn btn-fill mt-4"
            >
              <span className="text text-button">로그인</span>
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
