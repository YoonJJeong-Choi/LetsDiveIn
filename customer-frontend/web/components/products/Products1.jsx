"use client";

import LayoutHandler from "./LayoutHandler";
import Sorting from "./Sorting";
import Listview from "./Listview";
import GridView from "./GridView";
import { useEffect, useReducer, useState, useRef } from "react";
import FilterModal from "./FilterModal";
import { initialState, reducer } from "@/reducer/filterReducer";
import { productMain } from "@/data/products";
import FilterMeta from "./FilterMeta";
import { getActiveProductList, searchProducts } from "@/lib/api/product";

export default function Products1({ parentClass = "flat-spacing" }) {
  const [activeLayout, setActiveLayout] = useState(4);
  const [state, dispatch] = useReducer(reducer, initialState);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [priceRange, setPriceRange] = useState({ min: 0, max: 1000000 }); // 최소 0원, 최대 100만원
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

  // 백엔드 DTO를 프론트엔드 형식으로 변환
  const mapProductToFrontend = (product) => {
    // 옵션에서 사이즈 추출 (color와 size 필드 직접 사용)
    const sizes = product.options?.map(opt => opt.size).filter(Boolean) || [];

    // 옵션에서 색상 추출 (color와 size 필드 직접 사용)
    const colors = product.options?.map(opt => opt.color).filter(Boolean) || [];

    // 가격 변환 (백엔드는 원 단위, minPrice 사용)
    const displayPrice = product.minPrice ? product.minPrice : parseFloat(product.productPrice) || 0;

    return {
      id: product.productNo,
      title: product.productName,
      price: displayPrice,
      imgSrc: product.productImageUrl || "/images/products/womens/women-19.jpg",
      imgHover: product.productImageUrl || "/images/products/womens/women-19.jpg",
      isOnSale: false,
      inStock: true,
      filterBrands: [], // 나중에 파트너 정보로 채울 수 있음
      filterColor: colors,
      filterSizes: sizes,
      tabFilterOptions: product.productType ? [product.productType] : [],
      tabFilterOptions2: [],
      // 추가 필드
      productNo: product.productNo,
      productType: product.productType,
      productSubType: product.productSubType,
      productDescription: product.productDescription,
      options: product.options || [],
      minPrice: product.minPrice,
      maxPrice: product.maxPrice,
    };
  };

  // 검색 입력값 (입력 중인 값)
  const [searchInput, setSearchInput] = useState("");
  
  // 검색/필터링 파라미터 상태 (실제 API 호출에 사용)
  const [searchParams, setSearchParams] = useState({
    keyword: null,
    productType: null,
    productSubType: null,
    minPrice: null,
    maxPrice: null,
    optionName: null,
  });

  // 백엔드 API로 상품 검색/필터링
  useEffect(() => {
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
        };
        
        // 검색 파라미터가 하나라도 있으면 searchProducts 사용, 없으면 getActiveProductList 사용
        // 가격 필터도 백엔드에서 처리
        const hasSearchParams = Object.values(params).some(val => val !== null && val !== undefined);
        const data = hasSearchParams 
          ? await searchProducts(params)
          : await getActiveProductList();
        
        const mappedProducts = data.map(mapProductToFrontend);
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
        setProducts(productMain);
        dispatch({ type: "SET_FILTERED", payload: productMain });
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, [searchParams]); // searchParams 변경 시 API 호출 (가격 필터 포함)

  // UI 상태: 어떤 대분류가 펼쳐져 있는지 (필터링과 별개)
  const [expandedProductType, setExpandedProductType] = useState(null);

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
    // "전체" 클릭 또는 소분류가 없는 대분류 클릭: 대분류로 필터링
    setSearchParams(prev => ({
      ...prev,
      productType: productType,
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

  const allProps = {
    ...state,
    setPrice: handlePriceInput, // 슬라이더/입력 필드용 (UI만 업데이트)
    applyPriceFilter: handlePriceApply, // 적용 버튼용 (API 호출)

    // 가격 범위 (슬라이더용)
    priceRange: priceRange,
    
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
      const updated = [...brands].includes(newBrand)
        ? [...brands].filter((elm) => elm != newBrand)
        : [...brands, newBrand];
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
  };

  // 클라이언트 사이드 추가 필터링 (색상, 사이즈, 브랜드, 가격 등)
  useEffect(() => {
    // products가 없으면 필터링하지 않음
    if (products.length === 0) return;

    let filteredArrays = [];
    const currentProducts = products; // 백엔드에서 필터링된 결과

    // 브랜드 필터 (클라이언트 사이드)
    if (brands.length) {
      const filteredByBrands = [...currentProducts].filter((elm) =>
        brands.every((el) => elm.filterBrands?.includes(el))
      );
      filteredArrays = [...filteredArrays, filteredByBrands];
    }
    
    // 재고 상태 필터 (클라이언트 사이드)
    if (availability !== "All") {
      const filteredByavailability = [...currentProducts].filter(
        (elm) => availability.value === elm.inStock
      );
      filteredArrays = [...filteredArrays, filteredByavailability];
    }
    
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
    
    // 할인 상품 필터 (클라이언트 사이드)
    if (activeFilterOnSale) {
      const filteredByonSale = [...currentProducts].filter((elm) => elm.oldPrice);
      filteredArrays = [...filteredArrays, filteredByonSale];
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
  }, [availability, color, size, brands, activeFilterOnSale, products]); // price 제거 (백엔드에서 처리)

  useEffect(() => {
    if (!filtered || filtered.length === 0) {
      dispatch({ type: "SET_SORTED", payload: [] });
      return;
    }

    if (sortingOption === "Price Ascending") {
      dispatch({
        type: "SET_SORTED",
        payload: [...filtered].sort((a, b) => a.price - b.price),
      });
    } else if (sortingOption === "Price Descending") {
      dispatch({
        type: "SET_SORTED",
        payload: [...filtered].sort((a, b) => b.price - a.price),
      });
    } else if (sortingOption === "Title Ascending") {
      dispatch({
        type: "SET_SORTED",
        payload: [...filtered].sort((a, b) => a.title.localeCompare(b.title)),
      });
    } else if (sortingOption === "Title Descending") {
      dispatch({
        type: "SET_SORTED",
        payload: [...filtered].sort((a, b) => b.title.localeCompare(a.title)),
      });
    } else {
      dispatch({ type: "SET_SORTED", payload: filtered });
    }
    dispatch({ type: "SET_CURRENT_PAGE", payload: 1 });
  }, [filtered, sortingOption]);
  if (loading) {
    return (
      <section className={parentClass}>
        <div className="container">
          <div className="text-center py-5">
            <p>상품 목록을 불러오는 중...</p>
          </div>
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className={parentClass}>
        <div className="container">
          <div className="text-center py-5">
            <p className="text-danger">오류: {error}</p>
            <p className="text-muted">더미 데이터를 표시합니다.</p>
          </div>
        </div>
      </section>
    );
  }

  // 검색 핸들러 (Enter 키나 검색 버튼 클릭 시에만 호출)
  const handleSearch = () => {
    const keyword = searchInput.trim();
    setSearchParams(prev => ({
      ...prev,
      keyword: keyword || null,
    }));
  };

  // 검색 초기화
  const handleSearchReset = () => {
    setSearchInput("");
    setSearchParams(prev => ({
      ...prev,
      keyword: null,
    }));
  };

  return (
    <>
      <section className={parentClass}>
        <div className="container">
          {/* 검색 입력창 */}
          <div className="mb-4">
            <div className="row">
              <div className="col-md-6">
                <div className="input-group">
                  <input
                    type="text"
                    className="form-control"
                    placeholder="상품명 또는 설명으로 검색..."
                    value={searchInput}
                    onChange={(e) => {
                      setSearchInput(e.target.value); // 입력값만 업데이트, API 호출 안 함
                    }}
                    onKeyPress={(e) => {
                      if (e.key === 'Enter') {
                        handleSearch(); // Enter 키 눌렀을 때만 검색
                      }
                    }}
                  />
                  <button
                    className="btn btn-primary"
                    type="button"
                    onClick={handleSearch}
                  >
                    검색
                  </button>
                  {searchParams.keyword && (
                    <button
                      className="btn btn-secondary"
                      type="button"
                      onClick={handleSearchReset}
                    >
                      초기화
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
          
          <div className="tf-shop-control">
            <div className="tf-control-filter">
              <a
                href="#filterShop"
                data-bs-toggle="offcanvas"
                aria-controls="filterShop"
                className="tf-btn-filter"
              >
                <span className="icon icon-filter" />
                <span className="text">Filters</span>
              </a>
              <div
                onClick={allProps.toggleFilterWithOnSale}
                className={`d-none d-lg-flex shop-sale-text ${
                  activeFilterOnSale ? "active" : ""
                }`}
              >
                <i className="icon icon-checkCircle" />
                <p className="text-caption-1">Shop sale items only</p>
              </div>
            </div>
            <ul className="tf-control-layout">
              <LayoutHandler
                setActiveLayout={setActiveLayout}
                activeLayout={activeLayout}
              />
            </ul>
            <div className="tf-control-sorting">
              <p className="d-none d-lg-block text-caption-1">Sort by:</p>
              <Sorting allProps={allProps} />
            </div>
          </div>
          <div className="wrapper-control-shop">
            <FilterMeta productLength={sorted.length} allProps={allProps} />

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
          </div>
        </div>
      </section>

      <FilterModal allProps={allProps} />
    </>
  );
}
