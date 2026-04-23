import axios from "axios";

const baseURL =
  typeof window !== "undefined"
    ? (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080")
    : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

/**
 * 고객 회원가입
 * @param {{ customerName: string, customerEmail: string, customerPassword: string, customerBirth: string }} body
 *   customerBirth: "YYYY-MM-DD" (필수)
 * @returns {Promise<string>} 서버 메시지 (예: "입력하신 이메일로 인증 요청하였습니다.")
 * @throws {Error} 에러 발생 시 ApiResponse 형태 또는 에러 메시지
 */
export async function join({ customerName, customerEmail, customerPassword, customerBirth }) {
  try {
    const { data } = await api.post("/api/customer/join", {
      customerName,
      customerEmail,
      customerPassword,
      customerBirth, // 필수 필드
    });
    return data;
  } catch (error) {
    // ApiResponse 형태의 에러 응답 처리
    if (error.response?.data) {
      const errorData = error.response.data;
      
      // ApiResponse 형태인 경우
      if (typeof errorData === 'object' && 'message' in errorData) {
        throw new Error(errorData.message || errorData.code || '회원가입에 실패했습니다.');
      }
      
      // 단순 문자열인 경우
      if (typeof errorData === 'string') {
        throw new Error(errorData);
      }
    }
    
    // 네트워크 에러 등
    throw error;
  }
}

/**
 * 이메일 인증 확인 (인증 링크에서 백엔드 직접 호출되는 경우가 많음.
 * 프론트에서 리다이렉트 페이지로 쓸 때만 사용)
 */
export async function checkEmail(token) {
  const { data } = await api.get("/api/customer/check", { params: { token } });
  return data;
}

/**
 * 현재 로그인한 고객 정보 조회
 * @returns {Promise<Object>} 고객 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getMe() {
  try {
    const { data } = await api.get("/api/customer/me");
    return data;
  } catch (error) {
    console.error("고객 정보 조회 실패:", error);
    throw error;
  }
}

/**
 * 프로필 정보 수정
 * @param {Object} profileData - 프로필 데이터
 * @param {string} profileData.customerName - 고객 이름
 * @param {string} profileData.customerEmail - 고객 이메일
 * @param {string} profileData.customerBirth - 생년월일 (YYYY-MM-DD)
 * @returns {Promise<Object>} 업데이트된 고객 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function updateProfile(profileData) {
  try {
    const { data } = await api.put("/api/customer/me", profileData);
    return data;
  } catch (error) {
    console.error("프로필 수정 실패:", error);
    const errorMessage = error.response?.data?.message || "프로필 수정에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 비밀번호 변경
 * @param {Object} passwordData - 비밀번호 데이터
 * @param {string} passwordData.currentPassword - 현재 비밀번호
 * @param {string} passwordData.newPassword - 새 비밀번호
 * @param {string} passwordData.confirmPassword - 비밀번호 확인
 * @returns {Promise<Object>} 성공 메시지
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function changePassword(passwordData) {
  try {
    const { data } = await api.put("/api/customer/password", passwordData);
    return data;
  } catch (error) {
    console.error("비밀번호 변경 실패:", error);
    const errorMessage = error.response?.data?.message || "비밀번호 변경에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 주소 목록 조회
 * @returns {Promise<Object[]>} 주소 목록
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getAddresses() {
  try {
    const { data } = await api.get("/api/customer/addresses");
    return data;
  } catch (error) {
    console.error("주소 목록 조회 실패:", error);
    throw error;
  }
}

/**
 * 주소 추가
 * @param {Object} addressData - 주소 데이터
 * @param {string} addressData.recipientName - 수령인 이름
 * @param {string} addressData.recipientPhone - 수령인 전화번호
 * @param {string} addressData.deliveryAddress - 배송지 주소
 * @param {string} [addressData.deliveryAddressDetail] - 배송지 상세 주소
 * @param {string} [addressData.deliveryZipCode] - 우편번호
 * @param {boolean} [addressData.isDefault] - 기본 주소 여부
 * @returns {Promise<Object>} 추가된 주소 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function addAddress(addressData) {
  try {
    const { data } = await api.post("/api/customer/addresses", addressData);
    return data;
  } catch (error) {
    console.error("주소 추가 실패:", error);
    const errorMessage = error.response?.data?.message || "주소 추가에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 주소 수정
 * @param {number} addressNo - 주소 번호
 * @param {Object} addressData - 주소 데이터
 * @returns {Promise<Object>} 수정된 주소 정보
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function updateAddress(addressNo, addressData) {
  try {
    const { data } = await api.put(`/api/customer/addresses/${addressNo}`, addressData);
    return data;
  } catch (error) {
    console.error("주소 수정 실패:", error);
    const errorMessage = error.response?.data?.message || "주소 수정에 실패했습니다.";
    throw new Error(errorMessage);
  }
}

/**
 * 주소 삭제
 * @param {number} addressNo - 주소 번호
 * @returns {Promise<Object>} 성공 메시지
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function deleteAddress(addressNo) {
  try {
    const { data } = await api.delete(`/api/customer/addresses/${addressNo}`);
    return data;
  } catch (error) {
    console.error("주소 삭제 실패:", error);
    const errorMessage = error.response?.data?.message || "주소 삭제에 실패했습니다.";
    throw new Error(errorMessage);
  }
}
