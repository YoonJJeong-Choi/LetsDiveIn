import { api } from "./http";

/**
 * 파트너 입점 신청
 * @param {Object} applicationData - 파트너 입점 신청 정보
 * @param {string} applicationData.email - 이메일
 * @param {string} applicationData.partnerName - 파트너명
 * @param {string} applicationData.partnerContact - 연락처 (010-XXXX-XXXX)
 * @param {string} applicationData.partnerBankAccount - 정산계좌 (XXX-XXX-XXXXXX)
 * @param {string} applicationData.businessRegistrationNumber - 사업자등록번호 (XXX-XX-XXXXX)
 * @returns {Promise<Object>} 응답 데이터
 */
export async function applyForPartnership(applicationData) {
  try {
    const { data } = await api.post("/api/partner/apply", applicationData);
    return data;
  } catch (error) {
    // ApiResponse 형태의 에러 응답 처리
    if (error.response?.data) {
      const errorData = error.response.data;

      // ApiResponse 형태인 경우
      if (typeof errorData === "object" && "message" in errorData) {
        throw new Error(
          errorData.message || errorData.code || "파트너 입점 신청에 실패했습니다."
        );
      }

      // 단순 문자열인 경우
      if (typeof errorData === "string") {
        throw new Error(errorData);
      }
    }

    // 네트워크 에러 등
    throw error;
  }
}

/**
 * 파트너 휴업 신청
 * @param {Object} requestData - 휴업 신청 정보
 * @param {string} requestData.deactivationReason - 휴업 신청 사유 (필수)
 * @returns {Promise<Object>} 응답 데이터
 */
export async function requestDeactivation(requestData) {
  try {
    const { data } = await api.post("/api/partner/deactivation/request", requestData);
    return data;
  } catch (error) {
    // ApiResponse 형태의 에러 응답 처리
    if (error.response?.data) {
      const errorData = error.response.data;

      // ApiResponse 형태인 경우
      if (typeof errorData === "object" && "message" in errorData) {
        throw new Error(
          errorData.message || errorData.code || "휴업 신청에 실패했습니다."
        );
      }

      // 단순 문자열인 경우
      if (typeof errorData === "string") {
        throw new Error(errorData);
      }
    }

    // 네트워크 에러 등
    throw error;
  }
}

/**
 * 파트너 재활성화 신청
 * @param {Object} requestData - 재활성화 신청 정보
 * @param {string} requestData.reactivationReason - 재활성화 신청 사유 (필수)
 * @returns {Promise<Object>} 응답 데이터
 */
export async function requestReactivation(requestData) {
  try {
    const { data } = await api.post("/api/partner/reactivation/request", requestData);
    return data;
  } catch (error) {
    // ApiResponse 형태의 에러 응답 처리
    if (error.response?.data) {
      const errorData = error.response.data;

      // ApiResponse 형태인 경우
      if (typeof errorData === "object" && "message" in errorData) {
        throw new Error(
          errorData.message || errorData.code || "재활성화 신청에 실패했습니다."
        );
      }

      // 단순 문자열인 경우
      if (typeof errorData === "string") {
        throw new Error(errorData);
      }
    }

    // 네트워크 에러 등
    throw error;
  }
}

/**
 * 파트너 자신의 정보 조회
 * @returns {Promise<Object>} 파트너 정보
 */
export async function getMyPartnerInfo() {
  try {
    const { data } = await api.get("/api/partner/me");
    return data;
  } catch (error) {
    console.error("getMyPartnerInfo API 에러:", error);
    console.error("에러 응답:", error.response?.data);
    console.error("에러 상태:", error.response?.status);
    
    // ApiResponse 형태의 에러 응답 처리
    if (error.response?.data) {
      const errorData = error.response.data;

      // ApiResponse 형태인 경우
      if (typeof errorData === "object" && "message" in errorData) {
        throw new Error(
          errorData.message || errorData.code || "파트너 정보 조회에 실패했습니다."
        );
      }

      // 단순 문자열인 경우
      if (typeof errorData === "string") {
        throw new Error(errorData);
      }
    }

    // 네트워크 에러 등
    throw error;
  }
}

/**
 * 파트너 자신의 이력 조회
 * @param {Object} params - 쿼리 파라미터 (선택)
 * @param {string} params.actionType - 액션 타입 필터 (APPLICATION, APPROVAL, REJECTION, DEACTIVATION_REQUEST, DEACTIVATION_APPROVED, DEACTIVATION_REJECTION, REACTIVATION_REQUEST, REACTIVATION_APPROVED, REACTIVATION_REJECTION, DEACTIVATED, ACTIVATED)
 * @returns {Promise<Object>} 파트너 이력 목록
 */
export async function getMyHistory(params = {}) {
  try {
    const queryParams = new URLSearchParams();
    if (params.actionType) queryParams.append('actionType', params.actionType);
    
    const queryString = queryParams.toString();
    const url = `/api/partner/history${queryString ? `?${queryString}` : ''}`;
    
    const { data } = await api.get(url);
    return data;
  } catch (error) {
    // ApiResponse 형태의 에러 응답 처리
    if (error.response?.data) {
      const errorData = error.response.data;

      // ApiResponse 형태인 경우
      if (typeof errorData === "object" && "message" in errorData) {
        throw new Error(
          errorData.message || errorData.code || "이력 조회에 실패했습니다."
        );
      }

      // 단순 문자열인 경우
      if (typeof errorData === "string") {
        throw new Error(errorData);
      }
    }

    // 네트워크 에러 등
    throw error;
  }
}