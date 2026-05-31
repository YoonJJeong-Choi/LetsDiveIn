"use client";
import React, { useState, useEffect } from "react";
import { getAddresses, addAddress, updateAddress, deleteAddress } from "@/lib/api/customer";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

export default function Address({ embedded = false }) {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingAddressNo, setEditingAddressNo] = useState(null);
  const [isAddressScriptReady, setIsAddressScriptReady] = useState(false);

  // 새 주소 폼 데이터
  const [newAddress, setNewAddress] = useState({
    recipientName: "",
    recipientPhone: "",
    deliveryAddress: "",
    deliveryAddressDetail: "",
    deliveryZipCode: "",
    isDefault: false,
  });

  // 주소 목록 조회
  useEffect(() => {
    fetchAddresses();
  }, []);

  // 다음(카카오) 주소 검색 스크립트 로드
  useEffect(() => {
    if (typeof window === "undefined") return;
    if (window?.daum?.Postcode) {
      setIsAddressScriptReady(true);
      return;
    }

    const existingScript = document.querySelector('script[data-daum-postcode="true"]');
    if (existingScript) {
      existingScript.addEventListener("load", () => setIsAddressScriptReady(true));
      return;
    }

    const script = document.createElement("script");
    script.src = "//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.async = true;
    script.dataset.daumPostcode = "true";
    script.onload = () => setIsAddressScriptReady(true);
    script.onerror = () => setIsAddressScriptReady(false);
    document.head.appendChild(script);
  }, []);

  const fetchAddresses = async () => {
    try {
      setLoading(true);
      const response = await getAddresses();
      const addressesData = response.data || response || [];
      setAddresses(Array.isArray(addressesData) ? addressesData : []);
    } catch (error) {
      console.error("주소 목록 조회 실패:", error);
      setAddresses([]);
    } finally {
      setLoading(false);
    }
  };

  // 주소 추가
  const handleAddAddress = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      await addAddress(newAddress);
      alert("주소가 추가되었습니다.");
      setNewAddress({
        recipientName: "",
        recipientPhone: "",
        deliveryAddress: "",
        deliveryAddressDetail: "",
        deliveryZipCode: "",
        isDefault: false,
      });
      setShowAddForm(false);
      fetchAddresses();
    } catch (error) {
      alert(error.message || "주소 추가에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 주소 수정
  const handleUpdateAddress = async (addressNo, addressData) => {
    try {
      setLoading(true);
      await updateAddress(addressNo, addressData);
      alert("주소가 수정되었습니다.");
      setEditingAddressNo(null);
      fetchAddresses();
    } catch (error) {
      alert(error.message || "주소 수정에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 주소 삭제
  const handleDeleteAddress = async (addressNo) => {
    if (!confirm("정말 이 주소를 삭제하시겠습니까?")) {
      return;
    }
    try {
      setLoading(true);
      await deleteAddress(addressNo);
      alert("주소가 삭제되었습니다.");
      fetchAddresses();
    } catch (error) {
      alert(error.message || "주소 삭제에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 주소 수정 폼 초기화
  const handleEditToggle = (address) => {
    if (editingAddressNo === address.addressNo) {
      setEditingAddressNo(null);
    } else {
      setEditingAddressNo(address.addressNo);
    }
  };

  const openAddressSearch = (onComplete) => {
    if (!window?.daum?.Postcode) {
      alert("주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
      return;
    }
    new window.daum.Postcode({
      oncomplete: (data) => {
        const selectedAddress = data.roadAddress || data.jibunAddress || "";
        const selectedZipCode = data.zonecode || "";
        onComplete(selectedAddress, selectedZipCode);
      },
    }).open();
  };

  if (loading && addresses.length === 0) {
    return (
      <div className={embedded ? "" : "my-account-content"}>
        <div className="p-4 d-flex justify-content-center">
          <InlineTemplateLoader />
        </div>
      </div>
    );
  }

  return (
    <div className={embedded ? "" : "my-account-content"}>
      <div className="account-address">
        <div className="widget-inner-address">
          <button
            className="tf-btn btn-fill mb_20"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            <span className="text text-button">새 주소 추가</span>
          </button>

          {/* 주소 추가 폼 */}
          {showAddForm && (
          <form
              className={`show-form-address wd-form-address ${showAddForm ? "d-block" : ""}`}
              onSubmit={handleAddAddress}
          >
              <div className="title">새 주소 추가</div>
              <fieldset className="mb_20">
                <input
                  type="text"
                  placeholder="수령인 이름*"
                  value={newAddress.recipientName}
                  onChange={(e) =>
                    setNewAddress({ ...newAddress, recipientName: e.target.value })
                  }
                  required
                />
              </fieldset>
              <fieldset className="mb_20">
                <input
                  type="text"
                  placeholder="수령인 전화번호*"
                  value={newAddress.recipientPhone}
                  onChange={(e) =>
                    setNewAddress({ ...newAddress, recipientPhone: e.target.value })
                  }
                  required
                />
              </fieldset>
              <fieldset className="mb_20">
                <input
                  type="text"
                  placeholder="배송지 주소*"
                  value={newAddress.deliveryAddress}
                  readOnly
                  required
                />
              </fieldset>
              <fieldset className="mb_20">
                <input
                  type="text"
                  placeholder="배송지 상세 주소"
                  value={newAddress.deliveryAddressDetail}
                  onChange={(e) =>
                    setNewAddress({ ...newAddress, deliveryAddressDetail: e.target.value })
                  }
                />
              </fieldset>
            <fieldset className="mb_20">
              <input
                type="text"
                  placeholder="우편번호*"
                  value={newAddress.deliveryZipCode}
                  readOnly
                  required
              />
            </fieldset>
            <div className="mb_20">
              <button
                type="button"
                className="tf-btn btn-fill"
                style={{ width: "100%" }}
                onClick={() =>
                  openAddressSearch((address, zipCode) =>
                    setNewAddress((prev) => ({
                      ...prev,
                      deliveryAddress: address,
                      deliveryZipCode: zipCode,
                    }))
                  )
                }
                disabled={!isAddressScriptReady}
              >
                <span className="text text-button">
                  {isAddressScriptReady ? "주소 검색" : "주소 검색 준비 중..."}
                </span>
              </button>
            </div>
            <div className="tf-cart-checkbox mb_20">
              <div className="tf-checkbox-wrapp">
                <input
                  type="checkbox"
                    checked={newAddress.isDefault}
                    onChange={(e) =>
                      setNewAddress({ ...newAddress, isDefault: e.target.checked })
                    }
                />
                <div>
                  <i className="icon-check" />
                </div>
              </div>
                <label>기본 주소로 설정</label>
            </div>
            <div className="d-flex align-items-center justify-content-end gap-20">
                <button type="submit" className="tf-btn btn-fill" disabled={loading}>
                  <span className="text text-button">{loading ? "추가 중..." : "주소 추가"}</span>
              </button>
                <button
                  type="button"
                  className="tf-btn btn-fill"
                  onClick={() => {
                    setShowAddForm(false);
                    setNewAddress({
                      recipientName: "",
                      recipientPhone: "",
                      deliveryAddress: "",
                      deliveryAddressDetail: "",
                      deliveryZipCode: "",
                      isDefault: false,
                    });
                  }}
                >
                  <span className="text text-button">취소</span>
                </button>
            </div>
          </form>
          )}

          {/* 주소 목록 */}
          <div className="list-account-address">
            {addresses.length === 0 ? (
              <div className="text-center p-4">
                <p>등록된 주소가 없습니다.</p>
              </div>
            ) : (
              addresses.map((address) => (
                <div
                  className="account-address-item"
                  key={address.addressNo}
                  style={{
                    border: "1px solid #e9e9e9",
                    borderRadius: "8px",
                    width: "min(100%, 560px)",
                    textAlign: "center",
                  }}
                >
                  <h6 className="mb_20">
                    {address.isDefault ? "기본 주소" : "주소"}
                  </h6>
                  <p>{address.recipientName}</p>
                  <p>{address.recipientPhone}</p>
                  <p>{address.deliveryAddress}</p>
                  {address.deliveryAddressDetail && <p>{address.deliveryAddressDetail}</p>}
                  {address.deliveryZipCode && <p>우편번호: {address.deliveryZipCode}</p>}
                  <div className="d-flex gap-10 justify-content-center mt-3">
                  <button
                      className="tf-btn btn-fill justify-content-center"
                      onClick={() => handleEditToggle(address)}
                  >
                    <span className="text text-button">
                        {editingAddressNo === address.addressNo ? "닫기" : "수정"}
                    </span>
                  </button>
                  <button
                      className="tf-btn btn-fill justify-content-center"
                      onClick={() => handleDeleteAddress(address.addressNo)}
                      disabled={loading}
                  >
                      <span className="text text-button">삭제</span>
                  </button>
                  </div>

                  {/* 주소 수정 폼 */}
                  {editingAddressNo === address.addressNo && (
                    <AddressEditForm
                      address={address}
                      onSave={(addressData) => handleUpdateAddress(address.addressNo, addressData)}
                      onCancel={() => setEditingAddressNo(null)}
                      onSearchAddress={openAddressSearch}
                      isAddressScriptReady={isAddressScriptReady}
                      loading={loading}
                    />
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// 주소 수정 폼 컴포넌트
function AddressEditForm({ address, onSave, onCancel, onSearchAddress, isAddressScriptReady, loading }) {
  const [editData, setEditData] = useState({
    recipientName: address.recipientName || "",
    recipientPhone: address.recipientPhone || "",
    deliveryAddress: address.deliveryAddress || "",
    deliveryAddressDetail: address.deliveryAddressDetail || "",
    deliveryZipCode: address.deliveryZipCode || "",
    isDefault: address.isDefault || false,
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(editData);
  };

  return (
                  <form
                    className="edit-form-address wd-form-address d-block"
      onSubmit={handleSubmit}
                  >
      <div className="title">주소 수정</div>
                    <fieldset className="mb_20">
                      <input
                        type="text"
          placeholder="수령인 이름*"
          value={editData.recipientName}
          onChange={(e) =>
            setEditData({ ...editData, recipientName: e.target.value })
          }
                        required
                      />
                    </fieldset>
                    <fieldset className="mb_20">
                      <input
                        type="text"
          placeholder="수령인 전화번호*"
          value={editData.recipientPhone}
          onChange={(e) =>
            setEditData({ ...editData, recipientPhone: e.target.value })
          }
                        required
                      />
                    </fieldset>
                    <fieldset className="mb_20">
                      <input
          type="text"
          placeholder="배송지 주소*"
          value={editData.deliveryAddress}
          readOnly
                        required
                      />
                    </fieldset>
                    <fieldset className="mb_20">
                      <input
                        type="text"
          placeholder="배송지 상세 주소"
          value={editData.deliveryAddressDetail}
          onChange={(e) =>
            setEditData({ ...editData, deliveryAddressDetail: e.target.value })
          }
                      />
                    </fieldset>
                    <fieldset className="mb_20">
                      <input
                        type="text"
          placeholder="우편번호*"
          value={editData.deliveryZipCode}
          readOnly
          required
                      />
                    </fieldset>
                    <div className="mb_20">
                      <button
                        type="button"
                        className="tf-btn btn-fill"
                        style={{ width: "100%" }}
                        onClick={() =>
                          onSearchAddress((selectedAddress, selectedZipCode) =>
                            setEditData((prev) => ({
                              ...prev,
                              deliveryAddress: selectedAddress,
                              deliveryZipCode: selectedZipCode,
                            }))
                          )
                        }
                        disabled={!isAddressScriptReady}
                      >
                        <span className="text text-button">
                          {isAddressScriptReady ? "주소 검색" : "주소 검색 준비 중..."}
                        </span>
                      </button>
                    </div>
                    <div className="tf-cart-checkbox mb_20">
                      <div className="tf-checkbox-wrapp">
                        <input
                          type="checkbox"
            checked={editData.isDefault}
            onChange={(e) =>
              setEditData({ ...editData, isDefault: e.target.checked })
            }
                        />
                        <div>
            <i className="icon-check" />
                        </div>
                      </div>
        <label>기본 주소로 설정</label>
                    </div>
                    <div className="d-flex flex-column gap-20">
                      <button
                        type="submit"
                        className="tf-btn btn-fill"
          disabled={loading}
                      >
          <span className="text text-button">{loading ? "저장 중..." : "저장"}</span>
                      </button>
        <button type="button" onClick={onCancel} className="tf-btn btn-fill">
          <span className="text text-button">취소</span>
        </button>
      </div>
    </form>
  );
}
