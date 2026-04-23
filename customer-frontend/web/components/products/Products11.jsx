"use client";

import LayoutHandler from "./LayoutHandler";
import Sorting from "./Sorting";
import Listview from "./Listview";
import GridView from "./GridView";
import { useEffect, useReducer, useState } from "react";
import FilterModal from "./FilterModal";
import { initialState, reducer } from "@/reducer/filterReducer";
import { searchProducts } from "@/lib/api/product";
import { getActiveBrands } from "@/lib/api/brand";
import { getActiveSizes } from "@/lib/api/size";
import FilterMeta from "./FilterMeta";
import FilterSidebar from "./FilterSidebar";

export default function Products11() {
  const [activeLayout, setActiveLayout] = useState(4);
  const [brandOptions, setBrandOptions] = useState([]);
  const [sizeOptions, setSizeOptions] = useState([]);
  const [state, dispatch] = useReducer(reducer, initialState);
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

    setBrands: (newBrandCode) => {
      const updated = [...brands].includes(newBrandCode) ? [] : [newBrandCode];
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

  useEffect(() => {
    let abort = false;
    const fetch = async () => {
      try {
        const params = {};
        // 가격 필터
        if (Array.isArray(price) && price.length === 2) {
          params.minPrice = price[0];
          params.maxPrice = price[1];
        }
        // 옵션명: 색상 우선, 없으면 사이즈
        if (color !== "All" && color?.name) {
          params.optionName = color.name;
        } else if (size !== "All" && size !== "Free Size") {
          params.optionName = size;
        }
        if (brands.length > 0) {
          params.partnerBrandCode = brands[0];
        }
        const result = await searchProducts(params);
        if (!abort) {
          const list = Array.isArray(result?.data) ? result.data : Array.isArray(result) ? result : [];
          dispatch({ type: "SET_FILTERED", payload: list });
        }
      } catch (e) {
        if (!abort) {
          dispatch({ type: "SET_FILTERED", payload: [] });
        }
      }
    };
    fetch();
    return () => {
      abort = true;
    };
  }, [price, color, size, brands]);

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
  return (
    <>
      <section className="flat-spacing">
        <div className="container">
          <div className="tf-shop-control">
            <div className="tf-control-filter">
              <button className="filterShop tf-btn-filter hidden-mx-1200">
                <span className="icon icon-filter" />
                <span className="text">Filters</span>
              </button>
              <a
                href="#filterShop"
                data-bs-toggle="offcanvas"
                aria-controls="filterShop"
                className="tf-btn-filter show-mx-1200"
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
                hasSidebar
              />
            </ul>
            <div className="tf-control-sorting">
              <p className="d-none d-lg-block text-caption-1">Sort by:</p>
              <Sorting allProps={allProps} />
            </div>
          </div>
          <div className="wrapper-control-shop">
            <FilterMeta productLength={sorted.length} allProps={allProps} />
            <div className="row">
              <div className="col-xl-3">
                <FilterSidebar allProps={allProps} />
              </div>
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
              </div>
            </div>
          </div>
        </div>
      </section>{" "}
      <FilterModal allProps={allProps} />
    </>
  );
}
