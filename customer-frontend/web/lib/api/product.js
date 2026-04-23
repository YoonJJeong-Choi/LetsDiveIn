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
 * 고객용 활성 상품 목록 조회
 * @returns {Promise<Array>} 상품 목록 (각 상품에 옵션 포함)
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getActiveProductList({ page = 1, size = 12 } = {}) {
  try {
    const qp = { page: Math.max(0, Number(page) - 1), size };
    const { data } = await api.get("/api/product/list/active", { params: qp });
    return data?.data || data;
  } catch (error) {
    // 에러 응답 처리
    if (error.response?.data) {
      const errorData = error.response.data;
      
      // ApiResponse 형태인 경우
      if (typeof errorData === 'object' && 'message' in errorData) {
        throw new Error(errorData.message || errorData.code || '상품 목록을 불러오는데 실패했습니다.');
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
 * 상품 검색/필터링
 * @param {Object} searchParams 검색 파라미터
 * @param {string} [searchParams.keyword] 검색 키워드 (상품명, 설명)
 * @param {string} [searchParams.productType] 대분류 (예: SWIM_CAP, SWIMSUIT_WOMEN)
 * @param {string} [searchParams.productSubType] 소분류 (예: CAP_SILICONE, ONE_PIECE)
 * @param {number} [searchParams.minPrice] 최소 가격
 * @param {number} [searchParams.maxPrice] 최대 가격
 * @param {string} [searchParams.optionName] 옵션명 (색상, 사이즈 등)
 * @param {string} [searchParams.partnerBrandCode] 파트너 대표 브랜드 코드
 * @returns {Promise<Array>} 검색된 상품 목록
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function searchProducts(searchParams = {}) {
  try {
    const params = new URLSearchParams();
    
    if (searchParams.keyword) params.append('keyword', searchParams.keyword);
    if (searchParams.productType) params.append('productType', searchParams.productType);
    if (searchParams.productSubType) params.append('productSubType', searchParams.productSubType);
    if (searchParams.minPrice !== undefined && searchParams.minPrice !== null) {
      params.append('minPrice', searchParams.minPrice.toString());
    }
    if (searchParams.maxPrice !== undefined && searchParams.maxPrice !== null) {
      params.append('maxPrice', searchParams.maxPrice.toString());
    }
    if (searchParams.optionName) params.append('optionName', searchParams.optionName);
    if (searchParams.partnerBrandCode) params.append('partnerBrandCode', searchParams.partnerBrandCode);
    
    if (searchParams.page !== undefined && searchParams.page !== null) {
      const p = Math.max(0, Number(searchParams.page) - 1);
      params.append('page', String(p));
    }
    if (searchParams.size) params.append('size', String(searchParams.size));

    const { data } = await api.get(`/api/product/search?${params.toString()}`);
    return data?.data || data;
  } catch (error) {
    // 에러 응답 처리
    if (error.response?.data) {
      const errorData = error.response.data;
      
      // ApiResponse 형태인 경우
      if (typeof errorData === 'object' && 'message' in errorData) {
        throw new Error(errorData.message || errorData.code || '상품 검색에 실패했습니다.');
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
 * 상품 상세 조회
 * @param {number} productNo 상품 번호
 * @returns {Promise<Object>} 상품 상세 정보 (옵션 포함)
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getProductDetail(productNo) {
  try {
    const { data } = await api.get(`/api/product/detail/${productNo}`);
    return data;
  } catch (error) {
    if (error.response?.data) {
      const errorData = error.response.data;
      if (typeof errorData === 'object' && 'message' in errorData) {
        throw new Error(errorData.message || errorData.code || '상품 상세 정보를 불러오는데 실패했습니다.');
      }
      if (typeof errorData === 'string') {
        throw new Error(errorData);
      }
    }
    throw error;
  }
}
