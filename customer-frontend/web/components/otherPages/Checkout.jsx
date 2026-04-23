"use client";

import { useContextElement } from "@/context/Context";
import Image from "next/image";
import Link from "next/link";
import { useState, useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Swiper, SwiperSlide } from "swiper/react";
import { createOrder, startPriceLock } from "@/lib/api/order";
import { getCart, addCartItem } from "@/lib/api/cart";
import { createPayment } from "@/lib/api/payment";
import { getMe } from "@/lib/api/auth";
import { getAddresses } from "@/lib/api/customer";
import { getPointBalance } from "@/lib/api/point";
import { getApplicableSale } from "@/lib/api/sale";
const discounts = [
  {
    discount: "10% OFF",
    details: "For all orders from 200$",
    code: "Mo234231",
  },
  {
    discount: "10% OFF",
    details: "For all orders from 200$",
    code: "Mo234231",
  },
  {
    discount: "10% OFF",
    details: "For all orders from 200$",
    code: "Mo234231",
  },
];
export default function Checkout() {
  const [activeDiscountIndex, setActiveDiscountIndex] = useState(1);
  const { cartProducts, setCartProducts, totalPrice, isLoggedIn } = useContextElement();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isApplyingSales, setIsApplyingSales] = useState(false);
  const [saleAdjustedTotalByCartItemNo, setSaleAdjustedTotalByCartItemNo] = useState({});
  const priceLockStartedRef = useRef(false);
  const [localIsLoggedIn, setLocalIsLoggedIn] = useState(false);
  const [isCheckingLogin, setIsCheckingLogin] = useState(true);
  const [isSyncingCart, setIsSyncingCart] = useState(false);
  const paymentWidgetRef = useRef(null);
  const paymentMethodsRef = useRef(null);
  const paymentWidgetReadyRef = useRef(false);
  
  // 페이지 로드 시 로그인 상태 확인 및 사용자 정보 로드
  useEffect(() => {
    const checkLogin = async () => {
      try {
        const user = await getMe();
        const loggedIn = !!user;
        setLocalIsLoggedIn(loggedIn);
        
        // 비로그인 상태면 로그인 페이지로 리다이렉트
        if (!loggedIn) {
          const confirmMessage = "주문을 진행하려면 로그인이 필요합니다.\n\n로그인 페이지로 이동하시겠습니까?";
          if (confirm(confirmMessage)) {
            router.push("/login");
          } else {
            router.push("/shop-cart");
          }
          return;
        }
        
        // 로그인한 경우 이메일 자동 입력 및 장바구니 동기화
        if (user) {
          const userData = user.data || user;
          setFormData(prev => ({
            ...prev,
            email: userData.email || userData.customerEmail || "",
          }));
          
          // 비로그인 상태에서 장바구니에 담은 아이템(cartItemNo가 없는 아이템)을 백엔드에 동기화
          const itemsWithoutCartItemNo = cartProducts.filter(item => !item.cartItemNo);
          if (itemsWithoutCartItemNo.length > 0) {
            setIsSyncingCart(true);
            try {
              // 백엔드 장바구니 조회하여 현재 상태 확인
              const cartData = await getCart();
              const backendCartItemNos = new Set(cartData.items.map(item => item.cartItemNo));
              
              // 프론트엔드에만 있는 아이템들을 백엔드에 추가
              for (const item of itemsWithoutCartItemNo) {
                try {
                  await addCartItem({
                    productNo: Number(item.id),
                    optionNo: item.selectedOptionNo !== null && item.selectedOptionNo !== undefined ? item.selectedOptionNo : null,
                    quantity: item.quantity,
                  });
                } catch (error) {
                  console.error(`장바구니 동기화 실패 (상품 ${item.id}):`, error);
                }
              }
              
              // 동기화 후 장바구니 다시 조회하여 cartItemNo 업데이트
              const updatedCartData = await getCart();
              const transformedItems = updatedCartData.items.map((cartItem) => {
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
                  imgSrc: cartItem.productImageUrl,
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
            } catch (error) {
              console.error("장바구니 동기화 중 오류:", error);
            } finally {
              setIsSyncingCart(false);
            }
          } else {
            // 모든 아이템이 이미 cartItemNo를 가지고 있으면 백엔드 장바구니와 동기화만 수행
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
                  imgSrc: cartItem.productImageUrl,
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
            } catch (error) {
              console.error("장바구니 조회 실패:", error);
            }
          }
        }
      } catch (error) {
        setLocalIsLoggedIn(false);
        // 에러 발생 시에도 로그인 페이지로 리다이렉트
        const confirmMessage = "주문을 진행하려면 로그인이 필요합니다.\n\n로그인 페이지로 이동하시겠습니까?";
        if (confirm(confirmMessage)) {
          router.push("/login");
        } else {
          router.push("/shop-cart");
        }
      } finally {
        setIsCheckingLogin(false);
      }
    };
    checkLogin();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [router]);
  
  // Context의 isLoggedIn과 localIsLoggedIn 중 하나라도 true면 로그인된 것으로 간주
  const actuallyLoggedIn = isLoggedIn || localIsLoggedIn;
  
  // 저장된 주소 목록
  const [savedAddresses, setSavedAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [useNewAddress, setUseNewAddress] = useState(true);
  
  // 폼 입력값 state
  const [formData, setFormData] = useState({
    recipientName: "",
    email: "",
    phone: "",
    deliveryAddress: "",
    deliveryAddressDetail: "",
    deliveryZipCode: "",
    orderMemo: "",
    paymentMethod: "CARD", // 기본값: 신용카드
    usePointAmount: 0, // 포인트 사용 금액
  });

  // 포인트 잔액
  const [pointBalance, setPointBalance] = useState(0);
  const [pointLoading, setPointLoading] = useState(false);
  
  // 주소 선택 시 폼 자동 채우기
  const fillFormFromAddress = (address) => {
    setFormData(prev => ({
      ...prev,
      recipientName: address.recipientName || "",
      phone: address.recipientPhone || "",
      deliveryAddress: address.deliveryAddress || "",
      deliveryAddressDetail: address.deliveryAddressDetail || "",
      deliveryZipCode: address.deliveryZipCode || "",
    }));
  };
  
  // 저장된 주소 선택 핸들러
  const handleAddressSelect = (addressNo) => {
    if (addressNo === "new") {
      setUseNewAddress(true);
      setSelectedAddressId(null);
      // 폼 초기화 (이메일은 유지)
      setFormData(prev => ({
        ...prev,
        recipientName: "",
        phone: "",
        deliveryAddress: "",
        deliveryAddressDetail: "",
        deliveryZipCode: "",
      }));
    } else {
      setUseNewAddress(false);
      setSelectedAddressId(addressNo);
      const selectedAddress = savedAddresses.find(addr => addr.addressNo === Number(addressNo));
      if (selectedAddress) {
        fillFormFromAddress(selectedAddress);
      }
    }
  };
  
  // 저장된 주소 목록 불러오기
  useEffect(() => {
    const fetchAddresses = async () => {
      if (actuallyLoggedIn) {
        try {
          const response = await getAddresses();
          const addresses = response.data || response || [];
          setSavedAddresses(addresses);
          
          // 기본 주소가 있으면 자동 선택
          const defaultAddress = addresses.find(addr => addr.isDefault);
          if (defaultAddress) {
            setSelectedAddressId(defaultAddress.addressNo);
            setUseNewAddress(false);
            fillFormFromAddress(defaultAddress);
          }
        } catch (error) {
          console.error("주소 목록 조회 실패:", error);
        }
      }
    };
    fetchAddresses();
  }, [actuallyLoggedIn]);

  // 포인트 잔액 조회
  useEffect(() => {
    const fetchPointBalance = async () => {
      if (actuallyLoggedIn) {
        try {
          setPointLoading(true);
          const response = await getPointBalance();
          const balanceData = response?.data || response;
          if (balanceData) {
            setPointBalance(balanceData.pointBalance || 0);
          }
        } catch (error) {
          console.error("포인트 잔액 조회 실패:", error);
          setPointBalance(0);
        } finally {
          setPointLoading(false);
        }
      }
    };
    fetchPointBalance();
  }, [actuallyLoggedIn]);
  
  // URL 파라미터에서 선택된 cartItemNos 읽기
  const selectedCartItemNosParam = searchParams.get('cartItemNos');
  const selectedCartItemNos = selectedCartItemNosParam 
    ? new Set(selectedCartItemNosParam.split(',').map(no => Number(no)))
    : new Set();
  
  // 선택된 아이템만 필터링
  // cartItemNos 파라미터가 있으면 해당 아이템만, 없으면 모든 장바구니 아이템 표시
  const selectedItems = selectedCartItemNos.size > 0
    ? cartProducts.filter(item => 
        item.cartItemNo && selectedCartItemNos.has(item.cartItemNo)
      )
    : cartProducts.filter(item => item.cartItemNo); // 파라미터가 없으면 cartItemNo가 있는 모든 아이템 표시
  
  // 선택된 아이템의 총액 계산
  const selectedTotalPrice = selectedItems.reduce((sum, item) => {
    const adjustedTotal = saleAdjustedTotalByCartItemNo[item.cartItemNo];
    const baseTotal = Number(item.price ?? 0) * Number(item.quantity ?? 0);
    return sum + (adjustedTotal != null ? adjustedTotal : baseTotal);
  }, 0);
  const selectedBaseTotalPrice = selectedItems.reduce(
    (sum, item) => sum + Number(item.price ?? 0) * Number(item.quantity ?? 0),
    0
  );
  const selectedSaleDiscountAmount = Math.max(0, selectedBaseTotalPrice - selectedTotalPrice);

  const calculateDiscountAmount = (salePolicy, baseTotalPrice) => {
    if (!salePolicy || !salePolicy.discountType || salePolicy.discountValue == null) return 0;
    if (!baseTotalPrice || baseTotalPrice <= 0) return 0;

    const type = salePolicy.discountType;
    const value = Number(salePolicy.discountValue || 0);
    const maxDiscountAmount =
      salePolicy.maxDiscountAmount != null ? Number(salePolicy.maxDiscountAmount) : null;

    let rawDiscount = 0;
    if (type === "PERCENT") {
      rawDiscount = (baseTotalPrice * value) / 100;
    } else {
      // FIXED: discountValue는 할인 금액(원)
      rawDiscount = value;
    }

    if (rawDiscount <= 0) return 0;
    if (maxDiscountAmount != null && maxDiscountAmount > 0) {
      rawDiscount = Math.min(rawDiscount, maxDiscountAmount);
    }
    rawDiscount = Math.max(0, rawDiscount);
    return Math.min(rawDiscount, baseTotalPrice);
  };

  // price-lock + sales/applicable 조회로, 화면/포인트 계산에 세일 반영
  useEffect(() => {
    const adjustSales = async () => {
      if (!actuallyLoggedIn) return;

      const targetItems = (cartProducts || []).filter((it) => it.cartItemNo != null);
      if (targetItems.length === 0) {
        setSaleAdjustedTotalByCartItemNo({});
        return;
      }

      try {
        setIsApplyingSales(true);

        // 세일 적용 조회는 price-lock 기준시각을 쓰도록 (at 미지정) 처리합니다.
        if (!priceLockStartedRef.current) {
          await startPriceLock().catch(() => {});
          priceLockStartedRef.current = true;
        }

        const resultEntries = {};
        await Promise.all(
          targetItems.map(async (item) => {
            const productNo = item.id;
            const optionNo = item.selectedOptionNo ?? null;

            const unitPrice = Number(item.price ?? 0);
            const qty = Number(item.quantity ?? 0);
            const baseTotal = unitPrice * qty;

            const saleResult = await getApplicableSale({ productNo, optionNo });
            const salePolicy = saleResult?.data ?? saleResult;

            const discountAmount = calculateDiscountAmount(salePolicy, baseTotal);
            const discountedTotal = Math.max(0, baseTotal - discountAmount);
            resultEntries[item.cartItemNo] = discountedTotal;
          })
        );

        setSaleAdjustedTotalByCartItemNo(resultEntries);
      } catch (e) {
        setSaleAdjustedTotalByCartItemNo({});
      } finally {
        setIsApplyingSales(false);
      }
    };

    adjustSales();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [actuallyLoggedIn, cartProducts]);

  // 포인트 사용 금액 검증 및 최종 결제 금액 계산
  const usePointAmount = Number(formData.usePointAmount) || 0;
  const maxUsablePoint = Math.min(pointBalance, selectedTotalPrice);
  const validUsePointAmount = Math.max(0, Math.min(usePointAmount, maxUsablePoint));
  const finalPrice = Math.max(0, selectedTotalPrice - validUsePointAmount);

  // v2 결제위젯 스크립트 로드 및 렌더링
  useEffect(() => {
    // Checkout 페이지에서는 헤더의 "퀵 장바구니" 버튼(모달 오픈)을 비활성화한다
    // 정확 타겟: [data-bs-target="#shoppingCart"], a[href="#shoppingCart"]
    const selector = '[data-bs-target="#shoppingCart"], a[href="#shoppingCart"]';

    // 1) 마운트 시점에 즉시 비활성화(첫 클릭부터 차단)
    const quickCartLinks = Array.from(document.querySelectorAll(selector));
    quickCartLinks.forEach((el) => {
      try {
        el.setAttribute("aria-disabled", "true");
        el.style.pointerEvents = "none";
        el.style.opacity = "0.5";
        // 개별 안전장치: 요소 자체 클릭 차단
        const onClick = (e) => {
          e.preventDefault();
          e.stopPropagation();
        };
        el.__quickCartBlocker__ = onClick;
        el.addEventListener("click", onClick, true);
      } catch {}
    });

    // 2) 문서 단위 캡처 리스너(동적 요소 대비). 정확 셀렉터만 차단
    const disableQuickCart = (e) => {
      const target = e.target && e.target.closest ? e.target.closest(selector) : null;
      if (!target) return;
      e.preventDefault();
      e.stopPropagation();
    };
    document.addEventListener("click", disableQuickCart, true);

    const loadV2Script = () =>
      new Promise((resolve, reject) => {
        if (window?.PaymentWidget) return resolve();
        const script = document.createElement("script");
        // v2 결제위젯(브라우저 스크립트 버전)
        script.src = "https://js.tosspayments.com/v1/payment-widget";
        script.onload = () => resolve();
        script.onerror = (e) => reject(e);
        document.head.appendChild(script);
      });
    const initWidget = async () => {
      try {
        await loadV2Script();
        const clientKey =
          process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ||
          "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
        const customerKey = `cust-${Math.max(1, Number(Date.now() % 1000000))}`;
        // 스크립트 버전: 전역 PaymentWidget 함수 사용 (constructor 아님)
        if (typeof window.PaymentWidget !== "function") {
          throw new Error("PaymentWidget not found on window");
        }
        const widget = window.PaymentWidget(clientKey, customerKey);
        paymentWidgetRef.current = widget;
        // 금액 설정 및 UI 렌더
        const amount = Number(finalPrice || selectedTotalPrice || 0);
        const methods = await widget.renderPaymentMethods("#payment-methods", { value: amount });
        paymentMethodsRef.current = methods;
        await widget.renderAgreement("#agreement");
        paymentWidgetReadyRef.current = true;
      } catch (e) {
        console.error("[Checkout v2] widget init failed:", e);
      }
    };
    // 선택 금액/포인트 변경 시 금액 업데이트
    const updateAmount = async () => {
      try {
        if (!paymentMethodsRef.current) return;
        await paymentMethodsRef.current.updateAmount(Number(finalPrice || selectedTotalPrice || 0));
      } catch (e) {
        // ignore
      }
    };
    // 최초 로드 시 위젯 초기화
    initWidget();
    // 금액 변경 시 업데이트
    updateAmount();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    return () => {
      // 리스너/스타일 정리 (다른 페이지로 이동 시 부작용 방지)
      document.removeEventListener("click", disableQuickCart, true);
      quickCartLinks.forEach((el) => {
        try {
          el.removeAttribute("aria-disabled");
          el.style.pointerEvents = "";
          el.style.opacity = "";
          if (el.__quickCartBlocker__) {
            el.removeEventListener("click", el.__quickCartBlocker__, true);
            delete el.__quickCartBlocker__;
          }
        } catch {}
      });
    };
  }, [finalPrice, selectedTotalPrice]);
  
  // 주문 버튼 클릭 핸들러
  const handlePayment = async (e) => {
    e.preventDefault();
    
    // 선택된 아이템이 없으면 알림
    if (selectedItems.length === 0) {
      alert("주문할 상품을 선택해주세요.");
      return;
    }
    
    // 장바구니 동기화 중이면 대기
    if (isSyncingCart) {
      alert("장바구니 동기화 중입니다. 잠시 후 다시 시도해주세요.");
      return;
    }

    // 세일 적용 계산 중이면 대기
    if (isApplyingSales) {
      alert("세일 적용 계산 중입니다. 잠시 후 다시 시도해주세요.");
      return;
    }
    
    // cartItemNo가 없는 아이템이 있으면 (아직 동기화 안 됨) 알림
    const itemsWithoutCartItemNo = selectedItems.filter(item => !item.cartItemNo);
    if (itemsWithoutCartItemNo.length > 0) {
      alert("장바구니 동기화가 완료되지 않았습니다. 잠시 후 다시 시도해주세요.");
      return;
    }
    
    // 로그인하지 않은 경우 회원가입 유도
    if (!actuallyLoggedIn) {
      const confirmMessage = "주문을 진행하려면 로그인이 필요합니다.\n\n회원가입 페이지로 이동하시겠습니까?";
      if (confirm(confirmMessage)) {
        router.push("/register");
      }
      return;
    }
    
    // 필수 입력값 검증
    if (!formData.recipientName.trim()) {
      alert("이름을 입력해주세요.");
      return;
    }
    if (!formData.phone.trim()) {
      alert("전화번호를 입력해주세요.");
      return;
    }
    if (!formData.deliveryAddress.trim()) {
      alert("배송지 주소를 입력해주세요.");
      return;
    }
    if (!formData.paymentMethod) {
      alert("결제 방법을 선택해주세요.");
      return;
    }
    
    // 주문 생성
    setIsSubmitting(true);
    try {
      // 배송지 주소를 하나로 합치기 (시/군/구 + 상세 주소)
      const addressPart1 = formData.deliveryAddress.trim();
      const addressPart2 = formData.deliveryAddressDetail.trim();
      const fullDeliveryAddress = addressPart2 
        ? `${addressPart1} ${addressPart2}`.trim()
        : addressPart1;
      
      // 최종 검증
      if (!fullDeliveryAddress) {
        alert("배송지 주소를 입력해주세요.");
        return;
      }
      
      // 주문 생성 시 cartItemNos 추출 (동기화 후 업데이트된 cartItemNo 사용)
      const cartItemNosForOrder = selectedItems
        .filter(item => item.cartItemNo !== null && item.cartItemNo !== undefined)
        .map(item => item.cartItemNo);
      
      if (cartItemNosForOrder.length === 0) {
        alert("주문할 상품이 없습니다. 장바구니를 확인해주세요.");
        return;
      }
      
      // 포인트 사용 금액 검증
      const usePointAmount = Number(formData.usePointAmount) || 0;
      if (usePointAmount < 0) {
        alert("포인트 사용 금액은 0 이상이어야 합니다.");
        return;
      }
      if (usePointAmount > pointBalance) {
        alert(`포인트 잔액이 부족합니다. (보유: ${pointBalance.toLocaleString()}원)`);
        return;
      }
      if (usePointAmount > selectedTotalPrice) {
        alert("포인트 사용 금액은 주문 금액을 초과할 수 없습니다.");
        return;
      }

      const orderData = {
        cartItemNos: cartItemNosForOrder,
        recipientName: formData.recipientName.trim(),
        recipientPhone: formData.phone.trim(),
        deliveryAddress: fullDeliveryAddress,
        deliveryAddressDetail: null, // 상세 주소는 deliveryAddress에 포함
        deliveryZipCode: formData.deliveryZipCode.trim() || null,
        paymentMethod: formData.paymentMethod || "CARD", // 기본값 설정
        orderMemo: formData.orderMemo.trim() || null,
        usePointAmount: usePointAmount > 0 ? usePointAmount : null, // 포인트 사용 금액
      };
      
      
      
      const orderResult = await createOrder(orderData);
      
      const orderNo = orderResult.data?.orderNo || orderResult.orderNo;
      const orderTotalPrice = orderResult.data?.orderTotalPrice || orderResult.orderTotalPrice;
      if (!orderNo) {
        alert(`주문 생성에 실패했습니다.`);
        return;
      }
      
      // v2 결제위젯 요청
      const amount = Number(orderTotalPrice || finalPrice || 0);
      const orderId = `ORD-${orderNo}-${Date.now()}`;
      const orderName = `주문번호 ${orderNo}`;
      const successUrl = `${window.location.origin}/payment/success`;
      const failUrl = `${window.location.origin}/payment/fail`;

      if (!paymentWidgetReadyRef.current || !paymentWidgetRef.current) {
        alert("결제위젯이 아직 준비되지 않았습니다. 잠시 후 다시 시도해주세요.");
        return;
      }

      
      try {
        await paymentWidgetRef.current.requestPayment({
          orderId,
          orderName,
          successUrl,
          failUrl,
        });
      } catch (e) {
        const msg = (e && (e.message || e.toString())) || "";
        // 사용자가 결제창을 닫거나 취소한 경우, 에러로 표시하지 않고 조용히 종료
        if (msg.includes("취소") || e?.code === "USER_CANCEL") {
          return;
        }
        console.error("결제창 호출 실패:", e);
        alert("결제창 호출에 실패했습니다. 잠시 후 다시 시도해주세요.");
      }
    } catch (error) {
      console.error("주문 생성 실패:", error);
      const errorMessage = error.message || "주문 생성에 실패했습니다. 다시 시도해주세요.";
      alert(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };
  return (
    <section>
      <div className="container">
        <div className="row">
          <div className="col-xl-6">
            <div className="flat-spacing tf-page-checkout">
              {!actuallyLoggedIn && !isCheckingLogin && (
                <div className="wrap">
                  <div className="title-login">
                    <p>이미 계정이 있으신가요?</p>{" "}
                    <Link href={`/login`} className="text-button">
                      여기서 로그인
                    </Link>
                  </div>
                  <form
                    className="login-box"
                    onSubmit={(e) => e.preventDefault()}
                  >
                    <div className="grid-2">
                      <input type="text" placeholder="이름 또는 이메일" />
                      <input type="password" placeholder="비밀번호" />
                    </div>
                    <button className="tf-btn" type="submit">
                      <span className="text">로그인</span>
                    </button>
                  </form>
                </div>
              )}
              <div className="wrap">
                <h5 className="title">배송 정보</h5>
                {actuallyLoggedIn && savedAddresses.length > 0 && (
                  <div style={{ marginBottom: "20px", padding: "15px", backgroundColor: "#f9f9f9", borderRadius: "4px" }}>
                    <label style={{ display: "block", marginBottom: "10px", fontWeight: "bold", fontSize: "14px" }}>
                      저장된 주소 선택
                    </label>
                    <select
                      value={useNewAddress ? "new" : selectedAddressId}
                      onChange={(e) => handleAddressSelect(e.target.value)}
                      style={{
                        width: "100%",
                        padding: "10px",
                        marginBottom: "10px",
                        border: "1px solid #ddd",
                        borderRadius: "4px",
                        fontSize: "14px",
                      }}
                    >
                      <option value="new">새 주소 입력</option>
                      {savedAddresses.map((address) => (
                        <option key={address.addressNo} value={address.addressNo}>
                          {address.isDefault && "[기본] "}
                          {address.recipientName} - {address.deliveryAddress}
                          {address.deliveryAddressDetail ? ` ${address.deliveryAddressDetail}` : ""}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
                <form className="info-box" onSubmit={(e) => e.preventDefault()}>
                  <div className="grid-2">
                    <input 
                      type="text" 
                      placeholder="이름*" 
                      value={formData.recipientName}
                      onChange={(e) => setFormData({...formData, recipientName: e.target.value})}
                      required
                    />
                    <input 
                      type="text" 
                      placeholder="전화번호*" 
                      value={formData.phone}
                      onChange={(e) => setFormData({...formData, phone: e.target.value})}
                      required
                    />
                  </div>
                  <div className="grid-2">
                    <div>
                      <input 
                        type="email" 
                        placeholder="이메일 주소*" 
                        value={formData.email}
                        onChange={(e) => setFormData({...formData, email: e.target.value})}
                        required
                      />
                    </div>
                    <div></div>
                  </div>
                  <div className="grid-2">
                    <input 
                      type="text" 
                      placeholder="시/군/구*" 
                      value={formData.deliveryAddress}
                      onChange={(e) => setFormData({...formData, deliveryAddress: e.target.value})}
                    />
                    <input 
                      type="text" 
                      placeholder="상세 주소*" 
                      value={formData.deliveryAddressDetail}
                      onChange={(e) => setFormData({...formData, deliveryAddressDetail: e.target.value})}
                    />
                  </div>
                  <div className="grid-2">
                    <input 
                      type="text" 
                      placeholder="우편번호 (선택사항)" 
                      value={formData.deliveryZipCode}
                      onChange={(e) => setFormData({...formData, deliveryZipCode: e.target.value})}
                    />
                    <div></div>
                  </div>
                  <textarea 
                    placeholder="배송 메모 (선택사항)..." 
                    value={formData.orderMemo}
                    onChange={(e) => setFormData({...formData, orderMemo: e.target.value})}
                  />
                </form>
              </div>
              <div className="wrap">
                <h5 className="title">결제 방법 선택:</h5>
                <form
                  className="form-payment"
                  onSubmit={(e) => e.preventDefault()}
                >
                  <div className="payment-box" id="payment-box">
                    {/* v2 결제위젯 영역 */}
                    <div style={{ marginBottom: "16px" }}>
                      <div id="payment-methods" />
                    </div>
                    <div style={{ marginBottom: "16px" }}>
                      <div id="agreement" />
                    </div>
                  </div>
                  <button 
                    className="tf-btn btn-reset" 
                    onClick={handlePayment}
                    type="button"
                    disabled={isSubmitting || isApplyingSales}
                  >
                    {isSubmitting ? "주문 처리 중..." : (actuallyLoggedIn ? "결제하기" : "로그인 후 주문하기")}
                  </button>
                  {!actuallyLoggedIn && !isCheckingLogin && (
                    <div className="mt-3 text-center">
                      <p className="text-caption-1 text-secondary mb-2">
                        주문을 진행하려면 로그인이 필요합니다.
                      </p>
                      <div className="d-flex gap-2 justify-content-center">
                        <Link href="/login" className="text-button">
                          로그인
                        </Link>
                        <span className="text-secondary">|</span>
                        <Link href="/register" className="text-button">
                          회원가입
                        </Link>
                      </div>
                    </div>
                  )}
                </form>
              </div>
            </div>
          </div>
          <div className="col-xl-1">
            <div className="line-separation" />
          </div>
          <div className="col-xl-5">
            <div className="flat-spacing flat-sidebar-checkout">
              <div className="sidebar-checkout-content">
                <h5 className="title">장바구니 {selectedItems.length > 0 && `(${selectedItems.length})`}</h5>
                <div className="list-product">
                  {selectedItems.length > 0 ? (
                    selectedItems.map((elm, i) => (
                    <div key={i} className="item-product">
                      <Link
                        href={`/product-detail/${elm.id}`}
                        className="img-product"
                      >
                        <Image
                          alt="img-product"
                          src={elm.imgSrc}
                          width={600}
                          height={800}
                        />
                      </Link>
                      <div className="content-box">
                        <div className="info">
                          <Link
                            href={`/product-detail/${elm.id}`}
                            className="name-product link text-title"
                          >
                            {elm.title}
                          </Link>
                          <div className="variant text-caption-1 text-secondary">
                            {elm.color || elm.size ? (
                              <>
                                {elm.size && <span className="size">{elm.size}</span>}
                                {elm.color && elm.size && <span>/</span>}
                                {elm.color && <span className="color">{elm.color}</span>}
                              </>
                            ) : (
                              <>
                                <span className="size">XL</span>/
                                <span className="color">Blue</span>
                              </>
                            )}
                          </div>
                        </div>
                        <div className="total-price text-button">
                          <span className="count">{elm.quantity}</span>X
                          <span className="price d-flex flex-column align-items-end">
                            {(() => {
                              const baseUnitPrice = Number(elm.price ?? 0);
                              const adjustedUnitPrice =
                                saleAdjustedTotalByCartItemNo[elm.cartItemNo] != null
                                  ? Math.floor(
                                      saleAdjustedTotalByCartItemNo[elm.cartItemNo] /
                                        Number(elm.quantity ?? 1)
                                    )
                                  : baseUnitPrice;
                              const hasSale = adjustedUnitPrice < baseUnitPrice;
                              return (
                                <>
                                  {hasSale && (
                                    <span
                                      className="text-caption-1 text-secondary"
                                      style={{ textDecoration: "line-through" }}
                                    >
                                      ₩{baseUnitPrice.toLocaleString()}
                                    </span>
                                  )}
                                  <span>₩{adjustedUnitPrice.toLocaleString()}</span>
                                </>
                              );
                            })()}
                          </span>
                        </div>
                      </div>
                    </div>
                    ))
                  ) : (
                    <div className="text-center py-4">
                      <p className="text-caption-1 text-secondary">선택된 상품이 없습니다.</p>
                      <Link href="/shop-cart" className="text-button">
                        장바구니로 돌아가기
                      </Link>
                    </div>
                  )}
                </div>
                <div className="sec-discount">
                  <Swiper
                    dir="ltr"
                    className="swiper tf-sw-categories"
                    slidesPerView={2.25} // data-preview="2.25"
                    breakpoints={{
                      1024: {
                        slidesPerView: 2.25, // data-tablet={3}
                      },
                      768: {
                        slidesPerView: 3, // data-tablet={3}
                      },
                      640: {
                        slidesPerView: 2.5, // data-mobile-sm="2.5"
                      },
                      0: {
                        slidesPerView: 1.2, // data-mobile="1.2"
                      },
                    }}
                    spaceBetween={20}
                  >
                    {discounts.map((item, index) => (
                      <SwiperSlide key={index}>
                        <div
                          className={`box-discount ${
                            activeDiscountIndex === index ? "active" : ""
                          }`}
                          onClick={() => setActiveDiscountIndex(index)}
                        >
                          <div className="discount-top">
                            <div className="discount-off">
                              <div className="text-caption-1">Discount</div>
                              <span className="sale-off text-btn-uppercase">
                                {item.discount}
                              </span>
                            </div>
                            <div className="discount-from">
                              <p className="text-caption-1">{item.details}</p>
                            </div>
                          </div>
                          <div className="discount-bot">
                            <span className="text-btn-uppercase">
                              {item.code}
                            </span>
                            <button className="tf-btn">
                              <span className="text">Apply Code</span>
                            </button>
                          </div>
                        </div>{" "}
                      </SwiperSlide>
                    ))}
                  </Swiper>
                  <div className="ip-discount-code">
                    <input type="text" placeholder="할인 쿠폰 코드 입력" />
                    <button className="tf-btn">
                      <span className="text">적용</span>
                    </button>
                  </div>
                  <p>
                    할인 코드는 상품 총액이 50만원 이상인 주문에만 사용 가능합니다.
                  </p>
                </div>
                {/* 포인트 사용 섹션 */}
                {actuallyLoggedIn && pointBalance > 0 && (
                  <div className="sec-point-usage mb-3" style={{ padding: "15px", backgroundColor: "#f9f9f9", borderRadius: "4px" }}>
                    <h6 className="mb-3">포인트 사용</h6>
                    <div className="mb-2">
                      <small className="text-secondary">보유 포인트: <strong>{pointBalance.toLocaleString()}원</strong></small>
                    </div>
                    <div className="d-flex gap-2 align-items-center mb-2">
                      <input
                        type="number"
                        min="0"
                        max={maxUsablePoint}
                        value={formData.usePointAmount || ""}
                        onChange={(e) => {
                          const value = e.target.value === "" ? "" : Number(e.target.value);
                          if (value === "" || (value >= 0 && value <= maxUsablePoint)) {
                            setFormData({...formData, usePointAmount: value});
                          }
                        }}
                        placeholder="사용할 포인트"
                        style={{
                          flex: 1,
                          padding: "8px 12px",
                          border: "1px solid #ddd",
                          borderRadius: "4px",
                        }}
                      />
                      <button
                        type="button"
                        className="tf-btn btn-sm"
                        onClick={() => {
                          setFormData({...formData, usePointAmount: maxUsablePoint});
                        }}
                      >
                        <span className="text">전액 사용</span>
                      </button>
                      <button
                        type="button"
                        className="tf-btn btn-sm btn-outline"
                        onClick={() => {
                          setFormData({...formData, usePointAmount: 0});
                        }}
                      >
                        <span className="text">취소</span>
                      </button>
                    </div>
                    {validUsePointAmount > 0 && (
                      <div className="text-success small">
                        포인트 {validUsePointAmount.toLocaleString()}원 사용 시 최종 결제 금액: <strong>{finalPrice.toLocaleString()}원</strong>
                      </div>
                    )}
                  </div>
                )}
                <div className="sec-total-price">
                  <div className="top">
                    <div className="item d-flex align-items-center justify-content-between text-button">
                      <span>상품 총액</span>
                      <span
                        style={
                          selectedSaleDiscountAmount > 0
                            ? { color: "#999", textDecoration: "line-through" }
                            : undefined
                        }
                      >
                        ₩{selectedBaseTotalPrice.toLocaleString()}
                      </span>
                    </div>
                    {selectedSaleDiscountAmount > 0 && (
                      <div className="item d-flex align-items-center justify-content-between text-button text-danger">
                        <span>세일 할인</span>
                        <span>-₩{selectedSaleDiscountAmount.toLocaleString()}</span>
                      </div>
                    )}
                    {selectedSaleDiscountAmount > 0 && (
                      <div className="item d-flex align-items-center justify-content-between text-button">
                        <span>세일 적용 금액</span>
                        <span>₩{selectedTotalPrice.toLocaleString()}</span>
                      </div>
                    )}
                    {validUsePointAmount > 0 && (
                      <div className="item d-flex align-items-center justify-content-between text-button text-success">
                        <span>포인트 할인</span>
                        <span>-₩{validUsePointAmount.toLocaleString()}</span>
                      </div>
                    )}
                    <div className="item d-flex align-items-center justify-content-between text-button">
                      <span>배송비</span>
                      <span>무료</span>
                    </div>
                  </div>
                  <div className="bottom">
                    <h5 className="d-flex justify-content-between">
                      <span>최종 결제 금액</span>
                      <span className="total-price-checkout">
                        ₩{finalPrice.toLocaleString()}
                      </span>
                    </h5>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
