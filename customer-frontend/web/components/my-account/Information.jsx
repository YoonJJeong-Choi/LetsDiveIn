"use client";
import React, { useState, useEffect } from "react";
import { getMe, updateProfile, changePassword } from "@/lib/api/customer";
import Address from "@/components/my-account/Address";

export default function Information() {
  const [loading, setLoading] = useState(false);
  const [passwordType, setPasswordType] = useState("password");
  const [confirmPasswordType, setConfirmPasswordType] = useState("password");
  const [newPasswordType, setNewPasswordType] = useState("password");
  
  // 프로필 정보
  const [profileData, setProfileData] = useState({
    customerName: "",
    customerEmail: "",
    customerBirth: "",
  });

  // 비밀번호 변경
  const [passwordData, setPasswordData] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });

  // 프로필 정보 로드
  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await getMe();
        const data = response.data || response;
        if (data) {
          setProfileData({
            customerName: data.customerName || "",
            customerEmail: data.customerEmail || "",
            customerBirth: data.customerBirth ? data.customerBirth.split('T')[0] : "",
          });
        }
      } catch (error) {
        console.error("프로필 정보 조회 실패:", error);
      }
    };
    fetchProfile();
  }, []);

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

  const toggleNewPassword = () => {
    setNewPasswordType((prevType) =>
      prevType === "password" ? "text" : "password"
    );
  };

  // 프로필 정보 수정
  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    
    // 이름이 변경되었는지 확인
    const response = await getMe();
    const currentData = response.data || response;
    const currentName = currentData?.customerName || "";
    const newName = profileData.customerName.trim();
    
    if (currentName !== newName) {
      if (!confirm(`이름을 "${currentName}"에서 "${newName}"(으)로 변경하시겠습니까?`)) {
        return;
      }
    }
    
    try {
      setLoading(true);
      // 이메일과 생년월일은 제외하고 전송 (수정 불가)
      const { customerEmail, customerBirth, ...updateData } = profileData;
      await updateProfile(updateData);
      alert("프로필 정보가 수정되었습니다.");
      // 프로필 정보 다시 로드
      const updatedResponse = await getMe();
      const updatedData = updatedResponse.data || updatedResponse;
      if (updatedData) {
        setProfileData({
          customerName: updatedData.customerName || "",
          customerEmail: updatedData.customerEmail || "",
          customerBirth: updatedData.customerBirth ? updatedData.customerBirth.split('T')[0] : "",
        });
      }
    } catch (error) {
      alert(error.message || "프로필 수정에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 비밀번호 변경
  const handleChangePassword = async (e) => {
    e.preventDefault();
    
    // 비밀번호 확인
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      alert("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
      return;
    }

    if (passwordData.newPassword.length < 8) {
      alert("비밀번호는 최소 8자 이상이어야 합니다.");
      return;
    }

    try {
      setLoading(true);
      await changePassword(passwordData);
      alert("비밀번호가 변경되었습니다.");
      // 비밀번호 필드 초기화
      setPasswordData({
        currentPassword: "",
        newPassword: "",
        confirmPassword: "",
      });
    } catch (error) {
      alert(error.message || "비밀번호 변경에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="my-account-content">
      <div className="account-details">
        {/* 프로필 정보 수정 폼 */}
        <form
          onSubmit={handleUpdateProfile}
          className="form-account-details form-has-password"
        >
          <div className="account-info">
            <h5 className="title">기본 정보</h5>
            <div className="cols mb_20">
              <fieldset className="">
                <input
                  className=""
                  type="text"
                  placeholder="이름*"
                  name="customerName"
                  value={profileData.customerName}
                  onChange={(e) =>
                    setProfileData({ ...profileData, customerName: e.target.value })
                  }
                  required
                />
              </fieldset>
              <fieldset className="">
                <input
                  className=""
                  type="email"
                  placeholder="이메일*"
                  name="customerEmail"
                  value={profileData.customerEmail}
                  disabled
                  style={{ backgroundColor: "#f5f5f5", cursor: "not-allowed" }}
                />
              </fieldset>
            </div>
            <div className="cols mb_20">
              <fieldset className="">
                <input
                  className=""
                  type="date"
                  placeholder="생년월일*"
                  name="customerBirth"
                  value={profileData.customerBirth}
                  disabled
                  style={{ backgroundColor: "#f5f5f5", cursor: "not-allowed" }}
                />
              </fieldset>
            </div>
          </div>
          <div className="button-submit">
            <button 
              className="tf-btn btn-fill" 
              type="submit"
              disabled={loading}
            >
              <span className="text text-button">
                {loading ? "저장 중..." : "프로필 수정"}
              </span>
            </button>
          </div>
        </form>
        <div
          style={{
            height: "1px",
            width: "100%",
            backgroundColor: "#d9d9d9",
            margin: "32px 0",
          }}
        />
        <div style={{ marginTop: "40px" }}>
          <h5 className="title mb_20">배송지 관리</h5>
          <Address embedded />
        </div>
        <div
          style={{
            height: "1px",
            width: "100%",
            backgroundColor: "#d9d9d9",
            margin: "32px 0",
          }}
        />

        {/* 비밀번호 변경 폼 */}
        <form
          onSubmit={handleChangePassword}
          className="form-account-details form-has-password"
          style={{ marginTop: "40px" }}
        >
          <div className="account-password">
            <h5 className="title">비밀번호 변경</h5>
            <fieldset className="position-relative password-item mb_20">
              <input
                className="input-password"
                type={passwordType}
                placeholder="현재 비밀번호*"
                name="currentPassword"
                value={passwordData.currentPassword}
                onChange={(e) =>
                  setPasswordData({ ...passwordData, currentPassword: e.target.value })
                }
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
            <fieldset className="position-relative password-item mb_20">
              <input
                className="input-password"
                type={newPasswordType}
                placeholder="새 비밀번호* (최소 8자)"
                name="newPassword"
                value={passwordData.newPassword}
                onChange={(e) =>
                  setPasswordData({ ...passwordData, newPassword: e.target.value })
                }
                required
                minLength={8}
              />
              <span
                className={`toggle-password ${
                  !(newPasswordType === "text") ? "unshow" : ""
                }`}
                onClick={toggleNewPassword}
              >
                <i
                  className={`icon-eye-${
                    !(newPasswordType === "text") ? "hide" : "show"
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
                value={passwordData.confirmPassword}
                onChange={(e) =>
                  setPasswordData({ ...passwordData, confirmPassword: e.target.value })
                }
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
          </div>
          <div className="button-submit">
            <button 
              className="tf-btn btn-fill" 
              type="submit"
              disabled={loading}
            >
              <span className="text text-button">
                {loading ? "변경 중..." : "비밀번호 변경"}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
