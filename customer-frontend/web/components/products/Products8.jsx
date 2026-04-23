"use client";

import LayoutHandler from "./LayoutHandler";
import Sorting from "./Sorting";
import Listview from "./Listview";
import GridView from "./GridView";
import { useEffect, useReducer, useState } from "react";
import FilterModal from "./FilterModal";
import { initialState, reducer } from "@/reducer/filterReducer";
import { productMain } from "@/data/products";
import FilterMeta from "./FilterMeta";
import { getActiveProductList } from "@/lib/api/product";
import { getApplicableSale } from "@/lib/api/sale";

export default function Products8() {
  const [activeLayout, setActiveLayout] = useState(1);
  const [state, dispatch] = useReducer(reducer, initialState);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [allProducts, setAllProducts] = useState([]); // 백엔드에서 가져온 원본 데이터
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

  const allProps = {
    ...state,
    setPrice: (value) => dispatch({ type: "SET_PRICE", payload: value }),

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

  const calculateDiscountAmount = (salePolicy, basePrice) => {
    if (!salePolicy || !salePolicy.discountType || salePolicy.discountValue == null) return 0;
    if (!basePrice || basePrice <= 0) return 0;

    const type = salePolicy.discountType;
    const value = Number(salePolicy.discountValue || 0);
    const maxDiscountAmount =
      salePolicy.maxDiscountAmount != null ? Number(salePolicy.maxDiscountAmount) : null;

    let discount = 0;
    if (type === "PERCENT") {
      discount = (basePrice * value) / 100;
    } else {
      discount = value;
    }
    if (discount <= 0) return 0;
    if (maxDiscountAmount != null && maxDiscountAmount > 0) {
      discount = Math.min(discount, maxDiscountAmount);
    }
    return Math.min(basePrice, Math.max(0, discount));
  };

  // 백엔드에서 상품 목록 가져오기
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true);
        setError(null);
        const products = await getActiveProductList();

        // 목록에서도 현재 적용 세일을 반영해 원가/세일가를 함께 표시
        const convertedProducts = await Promise.all(
          products.map(async (product) => {
            const basePrice = Number(product.minPrice ?? product.productPrice ?? 0);
            let salePolicy = null;
            try {
              const saleResult = await getApplicableSale({
                productNo: product.productNo,
                optionNo: null,
              });
              salePolicy = saleResult?.data ?? saleResult;
            } catch (e) {
              salePolicy = null;
            }

            const discountAmount = calculateDiscountAmount(salePolicy, basePrice);
            const salePrice = Math.max(0, basePrice - discountAmount);
            const hasSale = discountAmount > 0 && salePrice < basePrice;
            const salePercentage = hasSale ? Math.round((discountAmount / basePrice) * 100) : null;

            return {
              id: product.productNo,
              title: product.productName,
              price: hasSale ? salePrice : basePrice,
              oldPrice: hasSale ? basePrice : null,
              salePercentage: hasSale ? `${salePercentage}%` : null,
              imgSrc: product.productImageUrl || "/images/products/default.jpg",
              imgHover: product.productImageUrl || "/images/products/default.jpg",
              isOnSale: hasSale,
              filterBrands: [],
              inStock: true,
              filterColor: [],
              filterSizes:
                product.options
                  ?.map((opt) => opt.optionName?.split("/")?.[1]?.trim())
                  .filter(Boolean) || [],
              tabFilterOptions: [product.productType, product.productSubType].filter(Boolean),
              description: product.productDescription,
              _original: product,
            };
          })
        );
        
        // 원본 데이터 저장
        setAllProducts(convertedProducts);
        // 초기 필터링된 데이터 설정
        dispatch({ type: "SET_FILTERED", payload: convertedProducts });
        setLoading(false);
      } catch (err) {
        console.error("상품 목록 로드 실패:", err);
        setError(err.message || "상품 목록을 불러오는데 실패했습니다.");
        setLoading(false);
        // 에러 발생 시 더미 데이터 사용
        dispatch({ type: "SET_FILTERED", payload: productMain });
      }
    };
    
    fetchProducts();
  }, []);

  useEffect(() => {
    // allProducts가 없으면 필터링하지 않음
    if (allProducts.length === 0) return;
    
    let filteredArrays = [];
    const currentProducts = allProducts; // 원본 데이터 사용

    if (brands.length) {
      const filteredByBrands = [...currentProducts].filter((elm) =>
        brands.every((el) => elm.filterBrands?.includes(el))
      );
      filteredArrays = [...filteredArrays, filteredByBrands];
    }
    if (availability !== "All") {
      const filteredByavailability = [...currentProducts].filter(
        (elm) => availability.value === elm.inStock
      );
      filteredArrays = [...filteredArrays, filteredByavailability];
    }
    if (color !== "All") {
      const filteredByColor = [...currentProducts].filter((elm) =>
        elm.filterColor?.includes(color.name)
      );
      filteredArrays = [...filteredArrays, filteredByColor];
    }
    if (size !== "All" && size !== "Free Size") {
      const filteredBysize = [...currentProducts].filter((elm) =>
        elm.filterSizes?.includes(size)
      );
      filteredArrays = [...filteredArrays, filteredBysize];
    }
    if (activeFilterOnSale) {
      const filteredByonSale = [...currentProducts].filter((elm) => elm.oldPrice);
      filteredArrays = [...filteredArrays, filteredByonSale];
    }

    const filteredByPrice = [...currentProducts].filter(
      (elm) => elm.price >= price[0] && elm.price <= price[1]
    );
    filteredArrays = [...filteredArrays, filteredByPrice];

    const commonItems = filteredArrays.length > 0
      ? [...currentProducts].filter((item) =>
          filteredArrays.every((array) => array.includes(item))
        )
      : currentProducts; // 필터가 없으면 전체 상품
    
    dispatch({ type: "SET_FILTERED", payload: commonItems });
  }, [price, availability, color, size, brands, activeFilterOnSale, allProducts]);

  useEffect(() => {
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
      <section className="flat-spacing">
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
      <section className="flat-spacing">
        <div className="container">
          <div className="alert alert-danger" role="alert">
            {error}
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
