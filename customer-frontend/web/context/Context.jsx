"use client";
import { openCartModal } from "@/utlis/openCartModal";
import { addCartItem, updateCartItem, getCart } from "@/lib/api/cart";
import { getMe } from "@/lib/api/auth";
import {
  DEFAULT_PRODUCT_PLACEHOLDER,
  productDisplayImageSrc,
} from "@/lib/media/productImage";

import React, { useEffect } from "react";
import { useContext, useState } from "react";
const dataContext = React.createContext();
export const useContextElement = () => {
  return useContext(dataContext);
};

export default function Context({ children }) {
  const GUEST_CART_KEY = "cart_guest";
  const getNormalizedUser = (user) => user?.data || user || null;
  const getUserCartKey = (user) => {
    const normalized = getNormalizedUser(user);
    const userId =
      normalized?.customerNo ||
      normalized?.id ||
      normalized?.customerId ||
      normalized?.userNo ||
      normalized?.email ||
      null;
    return userId ? `cart_user_${userId}` : null;
  };

  const [cartProducts, setCartProducts] = useState([]);
  const [quickViewItem, setQuickViewItem] = useState({
    id: null,
    title: "",
    price: 0,
    imgSrc: DEFAULT_PRODUCT_PLACEHOLDER,
  });
  const [quickAddItem, setQuickAddItem] = useState(null); // product 객체 또는 null
  const [totalPrice, setTotalPrice] = useState(0);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [cartStorageKey, setCartStorageKey] = useState(GUEST_CART_KEY);
  const [isCartStorageReady, setIsCartStorageReady] = useState(false);

  const normalizeValidCartItems = (items = [], { allowMemberItems = true } = {}) => {
    return items.filter((item) => {
      if (item.id === 2 && !item.cartItemNo) {
        return false;
      }
      if (!allowMemberItems && item.cartItemNo != null) {
        return false;
      }
      return item.cartItemNo !== undefined || (item.id && (item.title || item.imgSrc));
    });
  };

  const syncCartForLogout = () => {
    // 로그아웃 시 직전 로그인 사용자의 로컬 장바구니 캐시는 제거
    if (cartStorageKey && cartStorageKey.startsWith("cart_user_")) {
      localStorage.removeItem(cartStorageKey);
    }
    setIsLoggedIn(false);
    setCartStorageKey(GUEST_CART_KEY);
    const raw = localStorage.getItem(GUEST_CART_KEY);
    if (!raw) {
      setCartProducts([]);
      return;
    }
    try {
      const parsed = JSON.parse(raw);
      const validItems = Array.isArray(parsed)
        ? normalizeValidCartItems(parsed, { allowMemberItems: false })
        : [];
      setCartProducts(validItems);
      if (validItems.length > 0) {
        localStorage.setItem(GUEST_CART_KEY, JSON.stringify(validItems));
      } else {
        localStorage.removeItem(GUEST_CART_KEY);
      }
    } catch (e) {
      setCartProducts([]);
      localStorage.removeItem(GUEST_CART_KEY);
    }
  };
  useEffect(() => {
    const subtotal = cartProducts.reduce((accumulator, product) => {
      return accumulator + product.quantity * product.price;
    }, 0);
    setTotalPrice(subtotal);
  }, [cartProducts]);

  const isAddedToCartProducts = (id, optionNo = null) => {
    const existingItem = cartProducts.find((elm) => {
      if (optionNo !== null && optionNo !== undefined) {
        // 옵션이 있는 경우: 상품 ID와 옵션 번호가 모두 일치해야 함
        return elm.id == id && elm.selectedOptionNo === optionNo;
      } else {
        // 옵션이 없는 경우: 상품 ID만 일치하면 됨
        return elm.id == id && (elm.selectedOptionNo === null || elm.selectedOptionNo === undefined);
      }
    });
    return !!existingItem;
  };
  const addProductToCart = async (id, qty, optionNo = null, productData = null, isModal = true) => {
    const quantity = qty ? qty : 1;
    
    // 실제 상품 데이터가 없으면 (템플릿 더미 등) 장바구니에 추가하지 않고 조용히 무시
    if (!productData) {
      return;
    }
    
    // 상품에 실제 옵션이 있는데 optionNo가 null이면 추가하지 않음
    // 옵션이 없는 상품은 가상 옵션(optionNo: null) 하나만 있으므로 이를 제외해야 함
    const realOptions = productData.options?.filter(opt => opt.optionNo !== null) || [];
    if (realOptions.length > 0 && (optionNo === null || optionNo === undefined)) {
      // 옵션이 있는 상품은 옵션을 반드시 선택해야 함
      alert("옵션을 선택해주세요.");
      return;
    }
    
    // 같은 상품+옵션이 이미 있는지 확인
    const existingItem = cartProducts.find((elm) => {
      if (optionNo !== null && optionNo !== undefined) {
        return elm.id == id && elm.selectedOptionNo === optionNo;
      } else {
        return elm.id == id && (elm.selectedOptionNo === null || elm.selectedOptionNo === undefined);
      }
    });

    if (existingItem) {
      // 이미 있으면 수량 증가
      const newQuantity = existingItem.quantity + quantity;
      
      
      const updatedItems = cartProducts.map((item) => {
        // id와 selectedOptionNo로 비교 (객체 참조 비교 대신)
        const isSameItem = item.id == id && 
          ((optionNo !== null && optionNo !== undefined) 
            ? item.selectedOptionNo === optionNo 
            : (item.selectedOptionNo === null || item.selectedOptionNo === undefined));
        
        if (isSameItem) {
          
          return {
            ...item,
            quantity: newQuantity,
          };
        }
        return item;
      });
      
      
      setCartProducts(updatedItems);

      // 비로그인(게스트) 상태에서는 로컬 장바구니만 사용
      if (!isLoggedIn) {
        // no-op
      }
      // 로그인 상태에서만 백엔드 장바구니 동기화
      // cartItemNo가 있으면 updateCartItem 사용, 없으면 addCartItem 사용
      else if (existingItem.cartItemNo) {
        
        updateCartItem(existingItem.cartItemNo, { quantity: newQuantity })
          .then(() => {
            
          })
          .catch((error) => {
            // 401 에러(로그인 필요)인 경우 조용히 처리 (프론트엔드 상태는 유지)
            if (error.response?.status === 401) {
              
              return;
            }
            
            console.error("백엔드 장바구니 수량 증가 실패:", error);
            // 프론트엔드 상태 롤백 (401이 아닌 경우에만)
            setCartProducts((pre) => pre.map(item => {
              const isSameItem = item.id == id && 
                ((optionNo !== null && optionNo !== undefined) 
                  ? item.selectedOptionNo === optionNo 
                  : (item.selectedOptionNo === null || item.selectedOptionNo === undefined));
              if (isSameItem) {
                return { ...item, quantity: existingItem.quantity }; // 원래 수량으로 복구
              }
              return item;
            }));
            
            // 에러 메시지 표시
            if (error.response?.data) {
              const errorData = error.response.data;
              let errorMessage = "장바구니 수량 증가에 실패했습니다.";
              
              if (typeof errorData === 'object' && 'message' in errorData) {
                errorMessage = errorData.message || errorMessage;
              } else if (typeof errorData === 'string') {
                errorMessage = errorData;
              }
              
              alert(errorMessage);
            } else {
              alert("장바구니 수량 증가에 실패했습니다.");
            }
          });
      } else {
        // cartItemNo가 없으면 addCartItem 사용 (백엔드가 자동으로 수량 증가시킴)
        
        addCartItem({
          productNo: Number(id),
          optionNo: optionNo !== null && optionNo !== undefined ? optionNo : null,
          quantity, // 추가할 수량만 전달 (백엔드가 기존 수량에 더함)
        })
          .then(() => {
            
            // 백엔드에서 수량이 증가했으므로 장바구니를 다시 조회하여 cartItemNo를 받아옴
            // 하지만 여기서는 조회하지 않고, 다음 장바구니 조회 시 동기화됨
          })
          .catch((error) => {
            console.error("백엔드 장바구니 수량 증가 실패:", error);
            // 프론트엔드 상태 롤백
            setCartProducts((pre) => pre.map(item => {
              const isSameItem = item.id == id && 
                ((optionNo !== null && optionNo !== undefined) 
                  ? item.selectedOptionNo === optionNo 
                  : (item.selectedOptionNo === null || item.selectedOptionNo === undefined));
              if (isSameItem && existingItem) {
                return { ...item, quantity: existingItem.quantity }; // 원래 수량으로 복구
              }
              return item;
            }));
            
            // 에러 메시지 표시
            if (error.response?.data) {
              const errorData = error.response.data;
              let errorMessage = "장바구니 수량 증가에 실패했습니다.";
              
              if (typeof errorData === 'object' && 'message' in errorData) {
                errorMessage = errorData.message || errorMessage;
              } else if (typeof errorData === 'string') {
                errorMessage = errorData;
              }
              
              alert(errorMessage);
            } else if (error.response?.status === 401) {
              alert("로그인이 필요합니다.");
            } else {
              alert("장바구니 수량 증가에 실패했습니다.");
            }
          });
      }
    } else {
      // 없으면 새로 추가 (항상 실제 productData 사용)
      const selectedOption = optionNo && productData.options
        ? productData.options.find((opt) => opt.optionNo === optionNo)
        : null;
      
      // color와 size로 optionName 구성
      let optionDisplay = null;
      if (selectedOption) {
        if (selectedOption.color && selectedOption.size) {
          optionDisplay = `${selectedOption.color} / ${selectedOption.size}`;
        } else if (selectedOption.color) {
          optionDisplay = selectedOption.color;
        } else if (selectedOption.size) {
          optionDisplay = selectedOption.size;
        }
      }
      
      const item = {
        ...productData,
        imgSrc: productDisplayImageSrc(productData.imgSrc),
        quantity,
        selectedOptionNo: optionNo,
        color: selectedOption?.color || null,
        size: selectedOption?.size || null,
        optionName: optionDisplay, // color/size로 구성한 옵션명
        price:
          selectedOption?.totalPrice ||
          productData.price ||
          productData.minPrice ||
          0,
        cartItemNo: null, // 백엔드 추가 후 업데이트됨
        // 게스트 전용 고유키 (동일 상품/옵션 중복 라인 개별 식별)
        guestKey: `guest_${id}_${optionNo ?? 'none'}_${Date.now()}_${Math.floor(Math.random() * 1e6)}`,
      };
      
      
      
      // 프론트엔드에 먼저 추가 (Optimistic Update)
      setCartProducts((pre) => [...pre, item]);

      // 백엔드 장바구니에도 추가 (로그인 상태에서만)
      if (isLoggedIn) {
        addCartItem({
          productNo: Number(id),
          optionNo: optionNo !== null && optionNo !== undefined ? optionNo : null,
          quantity,
        })
          .then(async () => {
            
            // 백엔드 추가 성공 후 장바구니를 다시 조회하여 cartItemNo 받아오기
            try {
              const cartData = await getCart();
              const transformedItems = cartData.items.map((cartItem) => {
                let optionDisplay = null;
                if (cartItem.color || cartItem.size) {
                  if (cartItem.color && cartItem.size) {
                    optionDisplay = `${cartItem.color} / ${cartItem.size}`;
                  } else if (cartItem.color) {
                    optionDisplay = cartItem.color;
                  } else if (cartItem.size) {
                    optionDisplay = cartItem.size;
                  }
                }
                return {
                  id: cartItem.productNo,
                  title: cartItem.productName,
                  imgSrc: productDisplayImageSrc(cartItem.productImageUrl),
                  price: cartItem.itemPrice,
                  quantity: cartItem.quantity,
                  selectedOptionNo: cartItem.optionNo,
                  color: cartItem.color,
                  size: cartItem.size,
                  optionName: optionDisplay,
                  cartItemNo: cartItem.cartItemNo, // 백엔드에서 받은 cartItemNo
                };
              });
              
              setCartProducts(transformedItems);
            } catch (fetchError) {
              console.error("장바구니 조회 실패:", fetchError);
              // 조회 실패해도 프론트엔드에는 이미 추가되어 있음
            }
          })
          .catch((error) => {
              // 401 에러(로그인 필요)인 경우 조용히 처리 (프론트엔드 상태는 유지, localStorage에 저장됨)
            if (error.response?.status === 401) {
              // 비로그인 상태에서 정상 동작이므로 프론트엔드 상태 유지 (롤백하지 않음)
              return;
            }
            
            // 401이 아닌 다른 에러인 경우에만 프론트엔드 상태 롤백
            setCartProducts((pre) => pre.filter(item => 
              !(item.id == id && 
                ((optionNo !== null && optionNo !== undefined) 
                  ? item.selectedOptionNo === optionNo 
                  : (item.selectedOptionNo === null || item.selectedOptionNo === undefined)))
            ));
            
            // 에러 메시지 표시
            if (error.response?.data) {
              const errorData = error.response.data;
              let errorMessage = "장바구니 추가에 실패했습니다.";
              
              if (typeof errorData === 'object' && 'message' in errorData) {
                errorMessage = errorData.message || errorMessage;
              } else if (typeof errorData === 'string') {
                errorMessage = errorData;
              }
              
              alert(errorMessage);
            } else {
              alert("장바구니 추가에 실패했습니다.");
            }
            // 다른 에러인 경우에만 로그 표시 (프론트엔드 상태는 유지)
            console.error("장바구니 추가 실패:", error);
          });
      }
    }

    if (isModal) {
      openCartModal();
    }
  };

  const updateQuantity = (id, qty, optionNo = null) => {
    if (!isAddedToCartProducts(id, optionNo)) {
      return;
    }

    const itemIndex = cartProducts.findIndex((elm) => {
      if (optionNo !== null && optionNo !== undefined) {
        return elm.id == id && elm.selectedOptionNo === optionNo;
      }
      return elm.id == id && (elm.selectedOptionNo === null || elm.selectedOptionNo === undefined);
    });

    if (itemIndex < 0) {
      return;
    }

    const items = [...cartProducts];
    items[itemIndex] = {
      ...items[itemIndex],
      quantity: qty / 1,
    };
    setCartProducts(items);
  };

  // 로그인 상태 확인 및 장바구니 동기화
  useEffect(() => {
    const checkLoginStatusAndSyncCart = async () => {
      try {
        const user = await getMe();
        const loggedIn = !!user;
        const userCartKey = getUserCartKey(user);
        setIsLoggedIn(loggedIn);
        setCartStorageKey(loggedIn && userCartKey ? userCartKey : GUEST_CART_KEY);
        
        if (loggedIn) {
          // 로그인되어 있으면 백엔드 장바구니 조회
          try {
            const cartData = await getCart();
            const transformedItems = cartData.items.map((cartItem) => {
              let optionDisplay = null;
              if (cartItem.color || cartItem.size) {
                if (cartItem.color && cartItem.size) {
                  optionDisplay = `${cartItem.color} / ${cartItem.size}`;
                } else if (cartItem.color) {
                  optionDisplay = cartItem.color;
                } else if (cartItem.size) {
                  optionDisplay = cartItem.size;
                }
              }
              return {
                id: cartItem.productNo,
                title: cartItem.productName,
                imgSrc: productDisplayImageSrc(cartItem.productImageUrl),
                price: cartItem.itemPrice,
                quantity: cartItem.quantity,
                selectedOptionNo: cartItem.optionNo,
                color: cartItem.color,
                size: cartItem.size,
                optionName: optionDisplay,
                cartItemNo: cartItem.cartItemNo,
              };
            });
            setCartProducts(transformedItems);
            // 백엔드 데이터로 localStorage 업데이트
            if (loggedIn && userCartKey) {
              localStorage.setItem(userCartKey, JSON.stringify(transformedItems));
            }
          } catch (cartError) {
            // 401(인증 필요) 또는 403(권한 없음) 에러는 비로그인 상태에서 정상 동작이므로 조용히 처리
            if (cartError.response?.status !== 401 && cartError.response?.status !== 403) {
              console.error("백엔드 장바구니 조회 실패:", cartError);
            }
            // 조회 실패 시 localStorage에서 불러오기 (fallback)
            const items = userCartKey
              ? JSON.parse(localStorage.getItem(userCartKey) || "null")
              : null;
            if (items?.length) {
              const validItems = items.filter(item => {
                if (item.id === 2 && !item.cartItemNo) {
                  return false;
                }
                return item.cartItemNo !== undefined || 
                       (item.id && item.title && item.price !== undefined);
              });
              if (validItems.length > 0) {
                setCartProducts(validItems);
              } else {
                setCartProducts([]);
              }
            } else {
              setCartProducts([]);
            }
          }
        } else {
          // 로그인하지 않았으면 localStorage에서만 불러오기
          const itemsStr =
            localStorage.getItem(GUEST_CART_KEY) || localStorage.getItem("cartList");
          
          if (itemsStr) {
            try {
              const items = JSON.parse(itemsStr);
              
              if (items && Array.isArray(items) && items.length > 0) {
                // 더미 데이터 필터링
                const validItems = normalizeValidCartItems(items, {
                  allowMemberItems: false,
                });
                
                
                
                if (validItems.length > 0) {
                  setCartProducts(validItems);
                  localStorage.setItem(GUEST_CART_KEY, JSON.stringify(validItems));
                } else {
                  setCartProducts([]);
                  localStorage.removeItem(GUEST_CART_KEY);
                }
              }
            } catch (parseError) {
              console.error("localStorage 파싱 에러:", parseError);
              setCartProducts([]);
              localStorage.removeItem(GUEST_CART_KEY);
            }
          } else {
            setCartProducts([]);
          }
          // 이전 단일 키에서 게스트 키로 마이그레이션한 뒤 정리
          localStorage.removeItem("cartList");
        }
      } catch (error) {
        setIsLoggedIn(false);
        setCartStorageKey(GUEST_CART_KEY);
        // 에러 발생 시 localStorage에서만 불러오기
        const items = JSON.parse(localStorage.getItem(GUEST_CART_KEY) || "null");
        if (items?.length) {
          const validItems = normalizeValidCartItems(items, {
            allowMemberItems: false,
          });
          if (validItems.length > 0) {
            setCartProducts(validItems);
          } else {
            setCartProducts([]);
          }
        } else {
          setCartProducts([]);
        }
      } finally {
        setIsCartStorageReady(true);
      }
    };
    
    checkLoginStatusAndSyncCart();
  }, []);

  // cartProducts 변경 시 localStorage에 저장
  useEffect(() => {
    if (!isCartStorageReady) return;
    // 초기화 중이 아닐 때만 저장 (무한 루프 방지)
    if (cartProducts && cartProducts.length > 0) {
      try {
        const cartStr = JSON.stringify(cartProducts);
        localStorage.setItem(cartStorageKey, cartStr);
      } catch (storageError) {
        console.error("localStorage 저장 실패:", storageError);
      }
    } else if (cartProducts && cartProducts.length === 0) {
      // 빈 배열이면 localStorage에서 제거
      localStorage.removeItem(cartStorageKey);
    }
  }, [cartProducts, cartStorageKey, isCartStorageReady]);
  useEffect(() => {
    localStorage.removeItem("wishlist");
  }, []);

  const contextElement = {
    cartProducts,
    setCartProducts,
    totalPrice,
    addProductToCart,
    isAddedToCartProducts,
    quickViewItem,
    setQuickViewItem,
    quickAddItem,
    setQuickAddItem,
    updateQuantity,
    isLoggedIn,
    setIsLoggedIn,
    syncCartForLogout,
  };
  return (
    <dataContext.Provider value={contextElement}>
      {children}
    </dataContext.Provider>
  );
}
