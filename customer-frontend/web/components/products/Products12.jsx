"use client";

import LayoutHandler from "./LayoutHandler";
import Sorting from "./Sorting";
import Listview from "./Listview";
import GridView from "./GridView";
import { useEffect, useReducer, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import FilterModal from "./FilterModal";
import Pagination from "@/components/common/Pagination";
import { initialState, reducer } from "@/reducer/filterReducer";
import FilterMeta from "./FilterMeta";
import FilterSidebar from "./FilterSidebar";
import { getActiveProductList, searchProducts } from "@/lib/api/product";
import { mapApiProductToCard } from "@/lib/product/mapApiProductToCard";
import { getActiveBrands } from "@/lib/api/brand";
import { getActiveSizes } from "@/lib/api/size";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

export default function Products12() {
  const sp = useSearchParams();
  const queryProductType = sp.get("productType");
  const queryProductSubType = sp.get("productSubType");
  const [activeLayout, setActiveLayout] = useState(3); // 3열 그리드 기본값
  const [brandOptions, setBrandOptions] = useState([]);
  const [sizeOptions, setSizeOptions] = useState([]);
  const [state, dispatch] = useReducer(reducer, initialState);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const {
    price,
    availability,
    color,
    size,
    brands,

    filtered,
    sortingOption,
    sorted,

    activeFilterOnSale,
    currentPage,
    itemPerPage,
  } = state;
  const [totalProducts, setTotalProducts] = useState(0);

  // 검색/필터링 파라미터 상태 (실제 API 호출에 사용)
  const [searchParams, setSearchParams] = useState({
    keyword: null,
    productType: null,
    productSubType: null,
    minPrice: null,
    maxPrice: null,
    optionName: null,
    partnerBrandCode: null,
    saleOnly: false,
    inStock: null,
    sortBy: "latest",
    sortDir: "desc",
  });

  const [expandedProductType, setExpandedProductType] = useState(null);
  const [filtersReady, setFiltersReady] = useState(false);

  // URL 쿼리가 바뀔 때만(헤더 링크·주소창) 대분류/소분류 동기화
  useEffect(() => {
    setSearchParams((prev) => ({
      ...prev,
      productType: queryProductType,
      productSubType: queryProductSubType,
    }));
    if (queryProductType) setExpandedProductType(queryProductType);
    setFiltersReady(true);
  }, [queryProductType, queryProductSubType]);

  // 백엔드 API로 상품 검색/필터링
  useEffect(() => {
    if (!filtersReady) return;

    const fetchProducts = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // 검색 파라미터 구성
        const params = {
          keyword: searchParams.keyword || null,
          productType: searchParams.productType || null,
          productSubType: searchParams.productSubType || null,
          minPrice: searchParams.minPrice || null,
          maxPrice: searchParams.maxPrice || null,
          optionName: searchParams.optionName || null,
          partnerBrandCode: searchParams.partnerBrandCode || null,
          saleOnly: Boolean(searchParams.saleOnly),
          inStock:
            searchParams.inStock === null || searchParams.inStock === undefined
              ? null
              : Boolean(searchParams.inStock),
          sortBy: searchParams.sortBy || "latest",
          sortDir: searchParams.sortDir || "desc",
        };
        
        // 검색 파라미터가 하나라도 있으면 searchProducts 사용, 없으면 getActiveProductList 사용
        const hasSearchParams = Object.values(params).some(val => val !== null && val !== undefined);
        const page = state.currentPage || 1;
        const size = state.itemPerPage || 12;
        const resp = hasSearchParams 
          ? await searchProducts({ ...params, page, size })
          : await getActiveProductList({ page, size });
        const payload = resp?.data || resp;
        const meta = payload?.meta;
        const dataItems = payload?.items || [];
        const total = meta?.total || 0;
        setTotalProducts(Number(total) || 0);

        const mappedProducts = await Promise.all(dataItems.map(mapApiProductToCard));
        setProducts(mappedProducts);
        
        // 초기 로드 시 가격 필터 범위 설정 (0원 ~ 100만원)
        if (!hasSearchParams && mappedProducts.length > 0) {
          dispatch({ type: "SET_PRICE", payload: [0, 1000000] });
        }
        
        // 초기 필터링된 목록 설정
        dispatch({ type: "SET_FILTERED", payload: mappedProducts });
      } catch (err) {
        console.error("상품 목록 조회 실패:", err);
        setError(err.message || "상품 목록을 불러오는데 실패했습니다.");
        // 에러 발생 시 더미 데이터 사용 (fallback)
        setProducts([]);
        dispatch({ type: "SET_FILTERED", payload: [] });
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, [searchParams, state.currentPage, state.itemPerPage, filtersReady]); // 페이지/사이즈 변경 시 API 호출

  // 카테고리 필터링 핸들러
  const handleProductTypeChange = (productType) => {
    // 대분류 클릭: UI 상태만 변경 (소분류 펼치기/접기), 필터링은 하지 않음
    if (expandedProductType === productType) {
      setExpandedProductType(null); // 접기
    } else {
      setExpandedProductType(productType); // 펼치기
    }
  };

  const handleProductSubTypeChange = (productSubType, parentProductType) => {
    // 소분류 클릭: 필터링 적용 (대분류 + 소분류)
    setSearchParams(prev => ({
      ...prev,
      productType: parentProductType, // 대분류도 함께 설정
      productSubType: productSubType,
    }));
  };

  const handleProductTypeFilter = (productType) => {
    // "전체" 클릭 (null) 또는 소분류가 없는 대분류 클릭: 대분류로 필터링
    setSearchParams(prev => ({
      ...prev,
      productType: productType || null,
      productSubType: null,
    }));
  };

  // 가격 필터 UI 업데이트만 (API 호출 없음)
  const handlePriceInput = (value) => {
    dispatch({ type: "SET_PRICE", payload: value });
  };

  // 가격 필터 적용 버튼 클릭 핸들러 (API 호출)
  const handlePriceApply = () => {
    const currentPrice = state.price;
    setSearchParams(prev => ({
      ...prev,
      minPrice: currentPrice[0],
      maxPrice: currentPrice[1],
    }));
  };

  // 색상/사이즈/브랜드 선택을 백엔드 검색 파라미터로 동기화
  useEffect(() => {
    const nextOptionName =
      color !== "All" && color?.name
        ? color.name
        : size !== "All" && size !== "Free Size"
        ? size
        : null;
    const nextPartnerBrandCode = brands.length > 0 ? brands[0] : null;

    setSearchParams((prev) => ({
      ...prev,
      optionName: nextOptionName,
      partnerBrandCode: nextPartnerBrandCode,
    }));
  }, [color, size, brands]);

  useEffect(() => {
    setSearchParams((prev) => {
      const nextSaleOnly = Boolean(activeFilterOnSale);
      if (prev.saleOnly === nextSaleOnly) return prev;
      return { ...prev, saleOnly: nextSaleOnly };
    });
  }, [activeFilterOnSale]);

  useEffect(() => {
    const nextInStock =
      availability !== "All" && availability?.value !== undefined
        ? Boolean(availability.value)
        : null;
    setSearchParams((prev) => {
      if (prev.inStock === nextInStock) return prev;
      return { ...prev, inStock: nextInStock };
    });
  }, [availability]);

  const allProps = {
    ...state,
    setPrice: handlePriceInput, // 슬라이더/입력 필드용 (UI만 업데이트)
    applyPriceFilter: handlePriceApply, // 적용 버튼용 (API 호출)

    // 카테고리 필터링
    selectedProductType: searchParams.productType,
    selectedProductSubType: searchParams.productSubType,
    expandedProductType: expandedProductType, // UI 상태 (펼쳐짐)
    setProductType: handleProductTypeChange, // 대분류 클릭 (펼치기/접기만)
    setProductSubType: handleProductSubTypeChange, // 소분류 클릭 (필터링)
    setProductTypeFilter: handleProductTypeFilter, // 대분류로 필터링 ("전체" 클릭)

    setColor: (value) => {
      value == color
        ? dispatch({ type: "SET_COLOR", payload: "All" })
        : dispatch({ type: "SET_COLOR", payload: value });
    },
    setSize: (value) => {
      value == size
        ? dispatch({ type: "SET_SIZE", payload: "All" })
        : dispatch({ type: "SET_SIZE", payload: value });
    },
    setAvailability: (value) => {
      value == availability
        ? dispatch({ type: "SET_AVAILABILITY", payload: "All" })
        : dispatch({ type: "SET_AVAILABILITY", payload: value });
    },

    setBrands: (newBrand) => {
      const updated = [...brands].includes(newBrand) ? [] : [newBrand];
      dispatch({ type: "SET_BRANDS", payload: updated });
    },
    removeBrand: (newBrand) => {
      const updated = [...brands].filter((brand) => brand != newBrand);

      dispatch({ type: "SET_BRANDS", payload: updated });
    },
    setSortingOption: (value) =>
      dispatch({ type: "SET_SORTING_OPTION", payload: value }),
    toggleFilterWithOnSale: () => dispatch({ type: "TOGGLE_FILTER_ON_SALE" }),
    setCurrentPage: (value) =>
      dispatch({ type: "SET_CURRENT_PAGE", payload: value }),
    setItemPerPage: (value) => {
      dispatch({ type: "SET_CURRENT_PAGE", payload: 1 }),
        dispatch({ type: "SET_ITEM_PER_PAGE", payload: value });
    },
    clearFilter: () => {
      dispatch({ type: "CLEAR_FILTER" });
    },
    brandOptions,
    sizeOptions,
    getBrandLabel: (code) => {
      const found = brandOptions.find((b) => b.code === code);
      return found?.displayName || code;
    },
  };

  useEffect(() => {
    let abort = false;
    (async () => {
      try {
        const result = await getActiveBrands();
        const list = Array.isArray(result?.data) ? result.data : [];
        if (!abort) setBrandOptions(list);
      } catch (e) {
        if (!abort) setBrandOptions([]);
      }
    })();
    return () => {
      abort = true;
    };
  }, []);

  useEffect(() => {
    let abort = false;
    (async () => {
      try {
        const result = await getActiveSizes();
        const list = Array.isArray(result?.data) ? result.data : [];
        if (!abort) setSizeOptions(list);
      } catch (e) {
        if (!abort) setSizeOptions([]);
      }
    })();
    return () => {
      abort = true;
    };
  }, []);

  // 클라이언트 사이드 추가 필터링 (색상, 사이즈, 브랜드 등)
  useEffect(() => {
    // products가 없으면 필터링하지 않음
    if (products.length === 0) return;

    let filteredArrays = [];
    const currentProducts = products; // 백엔드에서 필터링된 결과

    // 브랜드는 백엔드 partnerBrandCode 필터로 처리함
    
    // 색상 필터 (클라이언트 사이드) - 옵션명에서 추출한 색상으로 필터링
    if (color !== "All") {
      const filteredByColor = [...currentProducts].filter((elm) =>
        elm.filterColor?.includes(color.name)
      );
      filteredArrays = [...filteredArrays, filteredByColor];
    }
    
    // 사이즈 필터 (클라이언트 사이드) - 옵션명에서 추출한 사이즈로 필터링
    if (size !== "All" && size !== "Free Size") {
      const filteredBysize = [...currentProducts].filter((elm) =>
        elm.filterSizes?.includes(size)
      );
      filteredArrays = [...filteredArrays, filteredBysize];
    }
    
    // 가격 필터는 백엔드에서 처리하므로 클라이언트 사이드 필터링 제거
    // (백엔드 API에서 이미 가격 필터링된 결과를 받음)

    // 필터가 없으면 전체 상품, 있으면 교집합
    const commonItems = filteredArrays.length > 0
      ? [...currentProducts].filter((item) =>
          filteredArrays.every((array) => array.includes(item))
        )
      : currentProducts; // 필터가 없으면 전체 상품
    
    dispatch({ type: "SET_FILTERED", payload: commonItems });
  }, [color, size, brands, activeFilterOnSale, products]); // price/availability 제거 (백엔드에서 처리)

  const prevSearchParamsRef = useRef(searchParams);
  useEffect(() => {
    dispatch({ type: "SET_SORTED", payload: filtered || [] });
    const paramsChanged = prevSearchParamsRef.current !== searchParams;
    if (paramsChanged) {
      dispatch({ type: "SET_CURRENT_PAGE", payload: 1 });
      prevSearchParamsRef.current = searchParams;
    }
  }, [filtered, sortingOption, searchParams]);

  useEffect(() => {
    const sortMap = {
      "신상품순": { sortBy: "latest", sortDir: "desc" },
      "인기순": { sortBy: "popularity", sortDir: "desc" },
      "가격 낮은순": { sortBy: "price", sortDir: "asc" },
      "가격 높은순": { sortBy: "price", sortDir: "desc" },
    };
    const next = sortMap[sortingOption] || sortMap["신상품순"];
    setSearchParams((prev) => {
      if (prev.sortBy === next.sortBy && prev.sortDir === next.sortDir) return prev;
      return { ...prev, ...next };
    });
  }, [sortingOption]);

  if (loading) {
    return (
      <section className="flat-spacing">
        <div className="container py-5 d-flex justify-content-center">
          <InlineTemplateLoader />
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="flat-spacing">
        <div className="container">
          <div className="text-center py-5">
            <p className="text-danger">오류: {error}</p>
            <p className="text-muted">더미 데이터를 표시합니다.</p>
          </div>
        </div>
      </section>
    );
  }

  return (
    <>
      <section className="flat-spacing">
        <div className="container">
          <div className="tf-shop-control">
            <div className="tf-control-filter">
              <div
                onClick={allProps.toggleFilterWithOnSale}
                className={`d-none d-lg-flex shop-sale-text ${
                  activeFilterOnSale ? "active" : ""
                }`}
              >
                <i className="icon icon-checkCircle" />
                <p className="text-caption-1">세일 상품</p>
              </div>
            </div>
            <ul className="tf-control-layout">
              <LayoutHandler
                setActiveLayout={setActiveLayout}
                activeLayout={activeLayout}
                hasSidebar
              />
            </ul>
            <div className="tf-control-sorting">
              <p className="d-none d-lg-block text-caption-1">정렬</p>
              <Sorting allProps={allProps} />
            </div>
          </div>
          <div className="wrapper-control-shop">
            <FilterMeta productLength={totalProducts} allProps={allProps} />
            <div className="row">
              <div className="col-xl-9">
                {activeLayout == 1 ? (
                  <div className="tf-list-layout wrapper-shop" id="listLayout">
                    <Listview products={sorted} />
                  </div>
                ) : (
                  <div
                    className={`tf-grid-layout wrapper-shop tf-col-${activeLayout}`}
                    id="gridLayout"
                  >
                    <GridView products={sorted} />
                  </div>
                )}
              </div>{" "}
              <div className="col-xl-3">
                <FilterSidebar allProps={allProps} />
              </div>
            </div>
          </div>
        </div>
      </section>{" "}
      {/* Pagination (템플릿 스타일) */}
      <section className="flat-spacing pt-0">
        <div className="container">
          {totalProducts > 0 && (
            <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mt-3">
              <div className="text-muted small">
                총 {totalProducts}건 • 페이지 {state.currentPage}/{Math.max(1, Math.ceil((totalProducts || 0) / (state.itemPerPage || 12)))}
              </div>
              <div className="d-flex align-items-center gap-3">
                <ul className="wg-pagination mb-0">
                  <Pagination
                    totalPages={Math.max(1, Math.ceil((totalProducts || 0) / (state.itemPerPage || 12)))}
                    currentPage={state.currentPage || 1}
                    onChange={(p) => allProps.setCurrentPage(p)}
                    maxButtons={5}
                  />
                </ul>
                <select
                  className="form-select form-select-sm"
                  style={{ width: 100 }}
                  value={state.itemPerPage || 12}
                  onChange={(e) => allProps.setItemPerPage(Number(e.target.value))}
                >
                  <option value={12}>12개씩</option>
                  <option value={24}>24개씩</option>
                  <option value={48}>48개씩</option>
                </select>
              </div>
            </div>
          )}
        </div>
      </section>
      <FilterModal allProps={allProps} />
    </>
  );
}
