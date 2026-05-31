"use client";
import React, { useState, useEffect } from "react";
import Link from "next/link";
import { requestDeactivation, requestReactivation, getMyHistory, getMyPartnerInfo } from "@/lib/api/partner";
import { getMe, isPortalAccessError } from "@/lib/api/auth";
import { getPartnerAdminLoginUrl } from "@/lib/adminUrls";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

export default function PartnerManagement() {
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [partnerInfo, setPartnerInfo] = useState(null);
  const [history, setHistory] = useState([]);
  const [activeTab, setActiveTab] = useState("request"); // 'request' or 'history'
  const [isPartner, setIsPartner] = useState(false);
  const [accessState, setAccessState] = useState("loading"); // loading | guest | customer | portal | partner
  
  // 휴업 신청 폼
  const [deactivationForm, setDeactivationForm] = useState({
    deactivationReason: "",
  });

  // 재활성화 신청 폼
  const [reactivationForm, setReactivationForm] = useState({
    reactivationReason: "",
  });

  // 파트너 정보 및 이력 로드
  useEffect(() => {
    const fetchData = async () => {
      try {
        setInitialLoading(true);
        // 파트너 정보 조회 (getMe로 role 확인)
        const userData = await getMe({ throwOnForbidden: true });
        const user = userData?.data || userData;

        if (!user) {
          setIsPartner(false);
          setAccessState("guest");
          return;
        }
        
        if (user?.role === "PARTNER") {
          setIsPartner(true);
          setAccessState("partner");
          console.log("파트너로 확인됨. user:", user);
          // 파트너 상세 정보 조회
          try {
            const partnerData = await getMyPartnerInfo();
            const partner = partnerData?.data || partnerData;
            console.log("파트너 상세 정보 조회 성공:", partner);
            setPartnerInfo({
              role: user.role,
              email: user.email,
              partnerStatus: partner?.partnerStatus,
              deactivationRequestedAt: partner?.deactivationRequestedAt,
              reactivationRequestedAt: partner?.reactivationRequestedAt,
            });
          } catch (err) {
            console.error("파트너 상세 정보 조회 실패:", err);
            console.error("에러 상세:", err.response?.data || err.message);
            console.error("에러 상태:", err.response?.status);
            console.error("현재 사용자 role:", user?.role);
            
            // 403 에러인 경우 권한 문제이므로 경고 표시
            if (err.response?.status === 403) {
              console.warn("파트너 권한이 확인되지 않습니다. 세션을 확인해주세요.");
              console.warn("getMe()로 확인한 role:", user?.role);
              alert("파트너 권한이 확인되지 않습니다. 다시 로그인해주세요.");
            }
            
            // 기본 정보만 설정
            setPartnerInfo({
              role: user.role,
              email: user.email,
            });
          }
          
          // 이력 조회
          try {
            const historyData = await getMyHistory();
            const historyList = historyData?.data || historyData;
            setHistory(Array.isArray(historyList) ? historyList : []);
          } catch (err) {
            console.error("이력 조회 실패:", err);
          }
        } else {
          setIsPartner(false);
          setAccessState("customer");
        }
      } catch (error) {
        console.error("파트너 정보 조회 실패:", error);
        setIsPartner(false);
        setAccessState(isPortalAccessError(error) ? "portal" : "guest");
      } finally {
        setInitialLoading(false);
      }
    };
    fetchData();
  }, []);

  // 휴업 신청
  const handleDeactivationRequest = async (e) => {
    e.preventDefault();
    
    if (!deactivationForm.deactivationReason.trim()) {
      alert("휴업 신청 사유를 입력해주세요.");
      return;
    }

    if (deactivationForm.deactivationReason.trim().length < 10) {
      alert("휴업 신청 사유는 최소 10자 이상 입력해주세요.");
      return;
    }

    if (!confirm("휴업 신청을 제출하시겠습니까? 관리자 승인 후 휴업이 처리됩니다.")) {
      return;
    }

    try {
      setLoading(true);
      await requestDeactivation({
        deactivationReason: deactivationForm.deactivationReason.trim(),
      });
      alert("휴업 신청이 완료되었습니다. 관리자 승인을 기다려주세요.");
      setDeactivationForm({ deactivationReason: "" });
      // 이력 다시 로드
      const historyData = await getMyHistory();
      const historyList = historyData?.data || historyData;
      setHistory(Array.isArray(historyList) ? historyList : []);
    } catch (error) {
      alert(error.message || "휴업 신청에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 재활성화 신청
  const handleReactivationRequest = async (e) => {
    e.preventDefault();
    
    if (!reactivationForm.reactivationReason.trim()) {
      alert("재활성화 신청 사유를 입력해주세요.");
      return;
    }

    if (reactivationForm.reactivationReason.trim().length < 10) {
      alert("재활성화 신청 사유는 최소 10자 이상 입력해주세요.");
      return;
    }

    if (!confirm("재활성화 신청을 제출하시겠습니까? 관리자 승인 후 재활성화가 처리됩니다.")) {
      return;
    }

    try {
      setLoading(true);
      await requestReactivation({
        reactivationReason: reactivationForm.reactivationReason.trim(),
      });
      alert("재활성화 신청이 완료되었습니다. 관리자 승인을 기다려주세요.");
      setReactivationForm({ reactivationReason: "" });
      // 이력 다시 로드
      const historyData = await getMyHistory();
      const historyList = historyData?.data || historyData;
      setHistory(Array.isArray(historyList) ? historyList : []);
    } catch (error) {
      alert(error.message || "재활성화 신청에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };


  // 이력 액션 타입 한글 변환
  const getActionLabel = (actionType) => {
    const labels = {
      APPLICATION: "입점 신청",
      APPROVAL: "입점 승인",
      REJECTION: "입점 거절",
      DEACTIVATION_REQUEST: "휴업 신청",
      DEACTIVATION_APPROVED: "휴업 승인",
      DEACTIVATION_REJECTION: "휴업 신청 거절",
      REACTIVATION_REQUEST: "재활성화 신청",
      REACTIVATION_APPROVED: "재활성화 승인",
      REACTIVATION_REJECTION: "재활성화 신청 거절",
      DEACTIVATED: "비활성화",
      ACTIVATED: "재활성화",
    };
    return labels[actionType] || actionType;
  };

  // 초기 로딩 중
  if (initialLoading) {
    return (
      <div className="my-account-content">
        <div className="account-details d-flex justify-content-center" style={{ padding: "40px" }}>
          <InlineTemplateLoader />
        </div>
      </div>
    );
  }

  if (accessState === "portal") {
    return (
      <div className="my-account-content">
        <div className="account-details">
          <div className="alert alert-warning">
            <strong>현재 고객몰에서 사용할 수 없는 계정입니다.</strong>
            <p className="mb-3">
              파트너 관리 기능은 관리자 페이지에서만 이용할 수 있습니다.
              관리자 또는 파트너 계정으로 관리자 로그인 페이지를 이용해주세요.
            </p>
            <Link href={getPartnerAdminLoginUrl()} className="tf-btn btn-fill">
              <span className="text text-button">관리자 로그인으로 이동</span>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  if (accessState === "guest") {
    return (
      <div className="my-account-content">
        <div className="account-details">
          <div className="alert alert-info">
            <strong>로그인이 필요합니다.</strong>
            <p className="mb-3">
              파트너 관련 신청 상태와 관리 기능은 로그인 후 확인할 수 있습니다.
            </p>
            <div className="d-flex flex-wrap gap-2">
              <Link href="/login" className="tf-btn btn-fill">
                <span className="text text-button">고객 로그인</span>
              </Link>
              <Link href={getPartnerAdminLoginUrl()} className="tf-btn btn-outline">
                <span className="text text-button">관리자 로그인</span>
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (accessState === "customer" || !isPartner) {
    return (
      <div className="my-account-content">
        <div className="account-details">
          <div className="alert alert-warning">
            <strong>파트너 전용 관리 기능입니다.</strong>
            <p className="mb-3">
              휴업 신청과 재활성화 신청은 고객몰이 아니라 관리자 페이지에서
              파트너 계정으로 진행해 주세요.
            </p>
            <div className="d-flex flex-wrap gap-2">
              <Link href="/partner-application" className="tf-btn btn-outline">
                <span className="text text-button">입점 신청하기</span>
              </Link>
              <Link href={getPartnerAdminLoginUrl()} className="tf-btn btn-fill">
                <span className="text text-button">관리자 로그인으로 이동</span>
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // 파트너 상태 확인
  const partnerStatus = partnerInfo?.partnerStatus;
  const canRequestDeactivation = partnerStatus === "APPROVED" && !partnerInfo?.deactivationRequestedAt;
  const canRequestReactivation = partnerStatus === "INACTIVE" && !partnerInfo?.reactivationRequestedAt;

  return (
    <div className="my-account-content">
      <div className="account-details">
        {/* 탭 메뉴 */}
        <div className="tabs-container" style={{ marginBottom: "30px" }}>
          <ul className="nav nav-tabs" role="tablist">
            <li className="nav-item" role="presentation">
              <button
                className={`nav-link ${activeTab === "request" ? "active" : ""}`}
                onClick={() => setActiveTab("request")}
                type="button"
              >
                신청
              </button>
            </li>
            <li className="nav-item" role="presentation">
              <button
                className={`nav-link ${activeTab === "history" ? "active" : ""}`}
                onClick={() => setActiveTab("history")}
                type="button"
              >
                이력 ({history.length})
              </button>
            </li>
          </ul>
        </div>

        {/* 신청 탭 */}
        {activeTab === "request" && (
          <div className="tab-content">
            {/* 현재 상태 표시 */}
            <div className="alert alert-info" style={{ marginBottom: "30px" }}>
              <strong>현재 상태:</strong>{" "}
              {partnerStatus === "APPROVED" && "승인 · 운영 중"}
              {partnerStatus === "INACTIVE" && "승인 · 비활성"}
              {partnerStatus === "PENDING" && "입점 신청"}
              {partnerStatus === "REJECTED" && "거절"}
              {partnerInfo?.deactivationRequestedAt && (
                <span className="badge bg-warning ms-2">
                  휴업 신청 대기 중
                </span>
              )}
              {partnerInfo?.reactivationRequestedAt && (
                <span className="badge bg-info ms-2">
                  재활성화 신청 대기 중
                </span>
              )}
            </div>

            {/* 휴업 신청 폼 */}
            {canRequestDeactivation && (
              <form
                onSubmit={handleDeactivationRequest}
                className="form-account-details"
                style={{ marginBottom: "40px" }}
              >
                <div className="account-info">
                  <h5 className="title">휴업 신청</h5>
                  <p style={{ color: "#666", marginBottom: "20px" }}>
                    운영 중인 파트너만 휴업 신청이 가능합니다. 관리자 승인 후 휴업이 처리됩니다.
                  </p>
                  <fieldset className="mb_20">
                    <textarea
                      className="form-control"
                      rows={5}
                      placeholder="휴업 신청 사유를 입력해주세요. (최소 10자 이상)"
                      value={deactivationForm.deactivationReason}
                      onChange={(e) =>
                        setDeactivationForm({
                          ...deactivationForm,
                          deactivationReason: e.target.value,
                        })
                      }
                      required
                      minLength={10}
                      maxLength={500}
                      style={{ resize: "vertical" }}
                    />
                    <small style={{ color: "#999", fontSize: "12px", display: "block", marginTop: "4px" }}>
                      {deactivationForm.deactivationReason.length}/500자
                    </small>
                  </fieldset>
                </div>
                <div className="button-submit">
                  <button
                    className="tf-btn btn-fill"
                    type="submit"
                    disabled={loading}
                  >
                    <span className="text text-button">
                      {loading ? "신청 중..." : "휴업 신청"}
                    </span>
                  </button>
                </div>
              </form>
            )}

            {/* 재활성화 신청 폼 */}
            {canRequestReactivation && (
              <form
                onSubmit={handleReactivationRequest}
                className="form-account-details"
              >
                <div className="account-info">
                  <h5 className="title">재활성화 신청</h5>
                  <p style={{ color: "#666", marginBottom: "20px" }}>
                    비활성 상태인 파트너만 재활성화 신청이 가능합니다. 관리자 승인 후 재활성화가 처리됩니다.
                  </p>
                  <fieldset className="mb_20">
                    <textarea
                      className="form-control"
                      rows={5}
                      placeholder="재활성화 신청 사유를 입력해주세요. (최소 10자 이상)"
                      value={reactivationForm.reactivationReason}
                      onChange={(e) =>
                        setReactivationForm({
                          ...reactivationForm,
                          reactivationReason: e.target.value,
                        })
                      }
                      required
                      minLength={10}
                      maxLength={500}
                      style={{ resize: "vertical" }}
                    />
                    <small style={{ color: "#999", fontSize: "12px", display: "block", marginTop: "4px" }}>
                      {reactivationForm.reactivationReason.length}/500자
                    </small>
                  </fieldset>
                </div>
                <div className="button-submit">
                  <button
                    className="tf-btn btn-fill"
                    type="submit"
                    disabled={loading}
                  >
                    <span className="text text-button">
                      {loading ? "신청 중..." : "재활성화 신청"}
                    </span>
                  </button>
                </div>
              </form>
            )}

            {/* 신청 불가 안내 */}
            {!canRequestDeactivation && !canRequestReactivation && (
              <div className="alert alert-warning">
                <strong>신청 불가</strong>
                <p>
                  {partnerStatus === "PENDING" && "입점 신청이 승인되면 휴업 신청이 가능합니다."}
                  {partnerStatus === "REJECTED" && "입점이 거절된 상태입니다. 재신청이 필요합니다."}
                  {partnerInfo?.deactivationRequestedAt && "휴업 신청이 이미 접수되었습니다. 관리자 승인을 기다려주세요."}
                  {partnerInfo?.reactivationRequestedAt && "재활성화 신청이 이미 접수되었습니다. 관리자 승인을 기다려주세요."}
                </p>
              </div>
            )}

          </div>
        )}

        {/* 이력 탭 */}
        {activeTab === "history" && (
          <div className="tab-content">
            <div className="account-info">
              <h5 className="title">파트너 이력</h5>
              {history.length === 0 ? (
                <div className="alert alert-info">
                  <p>이력이 없습니다.</p>
                </div>
              ) : (
                <div className="history-list" style={{ marginTop: "20px" }}>
                  {history.map((item) => (
                    <div
                      key={item.historyId}
                      className="history-item"
                      style={{
                        border: "1px solid #e0e0e0",
                        borderRadius: "8px",
                        padding: "20px",
                        marginBottom: "15px",
                        backgroundColor: "#fff",
                      }}
                    >
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "10px" }}>
                        <div>
                          <strong style={{ fontSize: "16px" }}>
                            {getActionLabel(item.actionType)}
                          </strong>
                          <span
                            className="badge"
                            style={{
                              marginLeft: "10px",
                              backgroundColor:
                                item.actionType === "APPLICATION" ||
                                item.actionType === "DEACTIVATION_REQUEST" ||
                                item.actionType === "REACTIVATION_REQUEST"
                                  ? "#1890ff"
                                  : item.actionType === "APPROVAL" ||
                                    item.actionType === "REACTIVATION_APPROVED" ||
                                    item.actionType === "ACTIVATED"
                                  ? "#52c41a"
                                  : item.actionType === "REJECTION" ||
                                    item.actionType === "DEACTIVATION_REJECTION" ||
                                    item.actionType === "REACTIVATION_REJECTION"
                                  ? "#ff4d4f"
                                  : "#8c8c8c",
                            }}
                          >
                            {item.actionType === "APPLICATION" ||
                            item.actionType === "DEACTIVATION_REQUEST" ||
                            item.actionType === "REACTIVATION_REQUEST"
                              ? "파트너"
                              : "관리자"}
                          </span>
                        </div>
                        <span style={{ color: "#999", fontSize: "14px" }}>
                          {new Date(item.createdAt).toLocaleString("ko-KR")}
                        </span>
                      </div>
                      {item.reason && (
                        <div style={{ color: "#666", marginTop: "8px", marginBottom: "8px" }}>
                          <strong>사유:</strong> {item.reason}
                        </div>
                      )}
                      {item.adminName && (
                        <div style={{ color: "#999", fontSize: "14px" }}>
                          <strong>처리자:</strong> {item.adminName}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
