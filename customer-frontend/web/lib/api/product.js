import { api } from "./http";

function getOrCreateGuestId() {
  if (typeof window === "undefined") return null;
  try {
    const key = "guest_view_id";
    let guestId = localStorage.getItem(key);
    if (!guestId) {
      guestId = `guest_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
      localStorage.setItem(key, guestId);
    }
    return guestId;
  } catch (e) {
    return null;
  }
}

/**
 * 고객용 활성 상품 목록 조회
 * @returns {Promise<Array>} 상품 목록 (각 상품에 옵션 포함)
 * @throws {Error} 에러 발생 시 에러 메시지
 */
export async function getActiveProductList({
  page = 1,
  size = 12,
  saleOnly = false,
  inStock = null,
  sortBy = "latest",
  sortDir = "desc",
} = {}) {
  try {
    const qp = {
      page: Math.max(0, Number(page) - 1),
      size,
      saleOnly,
      sortBy,
      sortDir,
    };
    if (inStock !== null && inStock !== undefined) {
      qp.inStock = inStock;
    }
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

    if (
      error?.code === "ERR_NETWORK" ||
      error?.message === "Network Error" ||
      String(error?.message || "").includes("Network Error")
    ) {
      throw new Error(
        "서버에 연결할 수 없습니다. Spring 백엔드(기본 http://localhost:8080)가 실행 중인지 확인하세요."
      );
    }

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
    if (searchParams.saleOnly !== undefined && searchParams.saleOnly !== null) {
      params.append('saleOnly', String(Boolean(searchParams.saleOnly)));
    }
    if (searchParams.inStock !== undefined && searchParams.inStock !== null) {
      params.append('inStock', String(Boolean(searchParams.inStock)));
    }
    if (searchParams.sortBy) params.append('sortBy', searchParams.sortBy);
    if (searchParams.sortDir) params.append('sortDir', searchParams.sortDir);
    
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

export async function incrementProductView(productNo) {
  try {
    const guestId = getOrCreateGuestId();
    const { data } = await api.post(
      `/api/product/${productNo}/view`,
      {},
      {
        headers: guestId ? { "X-Guest-Id": guestId } : undefined,
      }
    );
    const payload = data?.data ?? data;
    return typeof payload === "number" ? payload : Number(payload || 0);
  } catch (error) {
    return 0;
  }
}

export async function getProductView(productNo) {
  try {
    const { data } = await api.get(`/api/product/${productNo}/view`);
    const payload = data?.data ?? data;
    return typeof payload === "number" ? payload : Number(payload || 0);
  } catch (error) {
    return 0;
  }
}

export async function getRelatedProducts(productNo, { color = null, limit = 8 } = {}) {
  try {
    const params = new URLSearchParams();
    params.append("limit", String(limit));
    if (color) params.append("color", color);
    const { data } = await api.get(`/api/product/related/${productNo}?${params.toString()}`);
    return data?.data ?? data ?? [];
  } catch (error) {
    return [];
  }
}
