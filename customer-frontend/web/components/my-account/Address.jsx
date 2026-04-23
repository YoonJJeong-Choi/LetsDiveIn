"use client";
import React, { useState, useEffect } from "react";
import { getAddresses, addAddress, updateAddress, deleteAddress } from "@/lib/api/customer";

export default function Address() {
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showAddForm, setShowAddForm] = useState(false);
  const [editingAddressNo, setEditingAddressNo] = useState(null);

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

  if (loading && addresses.length === 0) {
    return (
      <div className="my-account-content">
        <div className="text-center p-4">주소를 불러오는 중...</div>
      </div>
    );
  }

  return (
    <div className="my-account-content">
      <div className="account-address">
        <div className="text-center widget-inner-address">
          <button
            className="tf-btn btn-fill radius-4 mb_20 btn-address"
            onClick={() => setShowAddForm(!showAddForm)}
          >
            <span className="text text-caption-1">새 주소 추가</span>
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
                  onChange={(e) =>
                    setNewAddress({ ...newAddress, deliveryAddress: e.target.value })
                  }
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
                  placeholder="우편번호"
                  value={newAddress.deliveryZipCode}
                  onChange={(e) =>
                    setNewAddress({ ...newAddress, deliveryZipCode: e.target.value })
                  }
              />
            </fieldset>
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
            <div className="d-flex align-items-center justify-content-center gap-20">
                <button type="submit" className="tf-btn btn-fill radius-4" disabled={loading}>
                  <span className="text">{loading ? "추가 중..." : "주소 추가"}</span>
              </button>
                <button
                  type="button"
                  className="tf-btn btn-fill radius-4"
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
                  <span className="text">취소</span>
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
                <div className="account-address-item" key={address.addressNo}>
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
                      className="tf-btn radius-4 btn-fill justify-content-center"
                      onClick={() => handleEditToggle(address)}
                  >
                    <span className="text">
                        {editingAddressNo === address.addressNo ? "닫기" : "수정"}
                    </span>
                  </button>
                  <button
                      className="tf-btn radius-4 btn-outline justify-content-center"
                      onClick={() => handleDeleteAddress(address.addressNo)}
                      disabled={loading}
                  >
                      <span className="text">삭제</span>
                  </button>
                  </div>

                  {/* 주소 수정 폼 */}
                  {editingAddressNo === address.addressNo && (
                    <AddressEditForm
                      address={address}
                      onSave={(addressData) => handleUpdateAddress(address.addressNo, addressData)}
                      onCancel={() => setEditingAddressNo(null)}
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
function AddressEditForm({ address, onSave, onCancel, loading }) {
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
          onChange={(e) =>
            setEditData({ ...editData, deliveryAddress: e.target.value })
          }
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
          placeholder="우편번호"
          value={editData.deliveryZipCode}
          onChange={(e) =>
            setEditData({ ...editData, deliveryZipCode: e.target.value })
          }
                      />
                    </fieldset>
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
                        className="tf-btn btn-fill radius-4"
          disabled={loading}
                      >
          <span className="text">{loading ? "저장 중..." : "저장"}</span>
                      </button>
        <button
          type="button"
          onClick={onCancel}
          className="tf-btn btn-fill radius-4"
        >
          <span className="text">취소</span>
        </button>
      </div>
    </form>
  );
}
