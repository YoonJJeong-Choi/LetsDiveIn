"use client";

import { useContextElement } from "@/context/Context";
import Image from "next/image";
import Link from "next/link";
import { useState, useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { createOrder, startPriceLock } from "@/lib/api/order";
import { getCart, addCartItem } from "@/lib/api/cart";
import { createPayment } from "@/lib/api/payment";
import { getMe, isPortalAccessError } from "@/lib/api/auth";
import { getAddresses, addAddress } from "@/lib/api/customer";
import { getPointBalance } from "@/lib/api/point";
import { getApplicableSale } from "@/lib/api/sale";
import { useBlockQuickCartModal } from "@/hooks/useBlockQuickCartModal";
import { formatKrw } from "@/lib/price/formatKrw";
export default function Checkout() {
  useBlockQuickCartModal();
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
  const [isReloadingMyInfo, setIsReloadingMyInfo] = useState(false);
  const [saveNewAddress, setSaveNewAddress] = useState(false);
  const [isAddressScriptReady, setIsAddressScriptReady] = useState(false);
  const [isAddressVerifiedByPostcode, setIsAddressVerifiedByPostcode] = useState(false);
  const deliveryAddressDetailInputRef = useRef(null);
  const paymentWidgetRef = useRef(null);
  const paymentMethodsRef = useRef(null);
  const paymentWidgetReadyRef = useRef(false);
  /** 금액은 매 렌더마다 갱신 — 위젯 비동기 초기화 완료 시점의 최신 금액용 */
  const latestPayAmountRef = useRef(0);
  
  // 페이지 로드 시 로그인 상태 확인 및 사용자 정보 로드
  useEffect(() => {
    const checkLogin = async () => {
      try {
        const user = await getMe({ throwOnForbidden: true });
        const loggedIn = !!user;
        setLocalIsLoggedIn(loggedIn);
        
        // 비로그인 상태면 로그인 페이지로 리다이렉트 (추가 confirm 없이 단일 처리)
        if (!loggedIn) {
          router.replace("/login?next=/checkout");
          return;
        }
        
        // 로그인한 경우 이메일 자동 입력 및 장바구니 동기화
        if (user) {
          const userData = user.data || user;
          setFormData(prev => ({
            ...prev,
            ...{
              email: userData.email || userData.customerEmail || "",
              recipientName:
                prev.recipientName ||
                userData.name ||
                userData.customerName ||
                userData.recipientName ||
                "",
              phone:
                prev.phone ||
                userData.phone ||
                userData.customerPhone ||
                userData.phoneNumber ||
                userData.mobile ||
                "",
            },
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
        if (isPortalAccessError(error)) {
          router.replace("/login?reason=portal&next=/checkout");
        } else {
          router.replace("/login?next=/checkout");
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

  const applyMyInfoToForm = (userData) => {
    setFormData((prev) => ({
      ...prev,
      email: userData?.email || userData?.customerEmail || prev.email || "",
      recipientName:
        userData?.name ||
        userData?.customerName ||
        userData?.recipientName ||
        prev.recipientName ||
        "",
      phone:
        userData?.phone ||
        userData?.customerPhone ||
        userData?.phoneNumber ||
        userData?.mobile ||
        prev.phone ||
        "",
    }));
  };

  const handleReloadMyInfo = async () => {
    if (!actuallyLoggedIn || isReloadingMyInfo) return;
    setIsReloadingMyInfo(true);
    try {
      const latestUser = await getMe();
      const latestUserData = latestUser?.data || latestUser;
      if (!latestUserData) {
        alert("회원 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
        return;
      }
      applyMyInfoToForm(latestUserData);
    } catch (error) {
      alert("회원 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setIsReloadingMyInfo(false);
    }
  };

  // 포인트 잔액
  const [pointBalance, setPointBalance] = useState(0);
  const [pointLoading, setPointLoading] = useState(false);

  // 다음(카카오) 주소 검색 스크립트 로드
  useEffect(() => {
    if (typeof window === "undefined") return;
    if (window?.daum?.Postcode) {
      setIsAddressScriptReady(true);
      return;
    }

    const existingScript = document.querySelector('script[data-daum-postcode="true"]');
    if (existingScript) {
      existingScript.addEventListener("load", () => setIsAddressScriptReady(true));
      return;
    }

    const script = document.createElement("script");
    script.src = "//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    script.async = true;
    script.dataset.daumPostcode = "true";
    script.onload = () => setIsAddressScriptReady(true);
    script.onerror = () => setIsAddressScriptReady(false);
    document.head.appendChild(script);
  }, []);

  const handleSearchAddress = () => {
    if (!window?.daum?.Postcode) {
      alert("주소 검색 서비스를 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
      return;
    }

    new window.daum.Postcode({
      oncomplete: (data) => {
        const selectedAddress = data.roadAddress || data.jibunAddress || "";
        const selectedZipCode = data.zonecode || "";
        setFormData((prev) => ({
          ...prev,
          deliveryAddress: selectedAddress,
          deliveryZipCode: selectedZipCode,
        }));
        setIsAddressVerifiedByPostcode(true);

        setTimeout(() => {
          deliveryAddressDetailInputRef.current?.focus();
        }, 0);
      },
    }).open();
  };
  
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
      setSaveNewAddress(false);
      setIsAddressVerifiedByPostcode(false);
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
      setSaveNewAddress(false);
      setIsAddressVerifiedByPostcode(true);
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

  latestPayAmountRef.current = Number(finalPrice || selectedTotalPrice || 0);

  // 결제위젯: 로그인 확인 후 1회만 마운트 (finalPrice 변경마다 재초기화하면 렌더 도중 requestPayment → "결제 UI가 아직 렌더링되지 않았습니다")
  useEffect(() => {
    if (!actuallyLoggedIn || isCheckingLogin) {
      return undefined;
    }

    let cancelled = false;

    const loadV2Script = () =>
      new Promise((resolve, reject) => {
        if (window?.PaymentWidget) return resolve();
        const script = document.createElement("script");
        script.src = "https://js.tosspayments.com/v1/payment-widget";
        script.onload = () => resolve();
        script.onerror = (e) => reject(e);
        document.head.appendChild(script);
      });

    const initWidget = async () => {
      paymentWidgetReadyRef.current = false;
      paymentMethodsRef.current = null;
      try {
        await loadV2Script();
        if (cancelled) return;
        const clientKey =
          process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ||
          "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";
        const customerKey = `cust-${Math.max(1, Number(Date.now() % 1000000))}`;
        if (typeof window.PaymentWidget !== "function") {
          throw new Error("PaymentWidget not found on window");
        }
        const widget = window.PaymentWidget(clientKey, customerKey);
        paymentWidgetRef.current = widget;
        const amount = latestPayAmountRef.current;
        const methods = await widget.renderPaymentMethods("#payment-methods", { value: amount });
        if (cancelled) return;
        paymentMethodsRef.current = methods;
        await widget.renderAgreement("#agreement");
        if (cancelled) return;
        paymentWidgetReadyRef.current = true;
        try {
          await paymentMethodsRef.current.updateAmount(latestPayAmountRef.current);
        } catch (e) {
          console.warn("[Checkout] 위젯 초기 금액 동기화:", e);
        }
      } catch (e) {
        console.error("[Checkout] 결제위젯 초기화 실패:", e);
        paymentWidgetReadyRef.current = false;
      }
    };

    initWidget();

    return () => {
      cancelled = true;
      paymentWidgetReadyRef.current = false;
    };
  }, [actuallyLoggedIn, isCheckingLogin]);

  // 포인트·세일 등으로 금액만 바뀔 때 — 위젯 재생성 없이 금액만 반영
  useEffect(() => {
    const run = async () => {
      if (!paymentWidgetReadyRef.current || !paymentMethodsRef.current) return;
      try {
        await paymentMethodsRef.current.updateAmount(latestPayAmountRef.current);
      } catch (e) {
        console.warn("[Checkout] updateAmount:", e);
      }
    };
    run();
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
    if (useNewAddress && !isAddressVerifiedByPostcode) {
      alert("신규 배송지는 주소 검색을 통해 선택해주세요.");
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

      if (actuallyLoggedIn && useNewAddress && saveNewAddress) {
        try {
          await addAddress({
            recipientName: formData.recipientName.trim(),
            recipientPhone: formData.phone.trim(),
            deliveryAddress: formData.deliveryAddress.trim(),
            deliveryAddressDetail: formData.deliveryAddressDetail.trim() || null,
            deliveryZipCode: formData.deliveryZipCode.trim() || null,
            isDefault: false,
          });

          const response = await getAddresses();
          const refreshedAddresses = response.data || response || [];
          setSavedAddresses(refreshedAddresses);
        } catch (saveAddressError) {
          console.error("주소 저장 실패:", saveAddressError);
          alert("주소 저장에 실패했습니다. 입력한 주소로 주문은 계속 진행됩니다.");
        }
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
    <>
      <style>{`
        @media (min-width: 1200px) {
          .checkout-scroll-pane {
            max-height: calc(100dvh - 140px);
            overflow-y: auto;
            overscroll-behavior: contain;
            -webkit-overflow-scrolling: touch;
            padding-right: 4px;
            -ms-overflow-style: none;
            scrollbar-width: none;
          }

          .checkout-scroll-pane::-webkit-scrollbar {
            width: 0;
            height: 0;
          }
        }
      `}</style>
      <section>
      <div className="container">
        <div className="row align-items-start">
          <div className="col-xl-6">
            <div className="flat-spacing tf-page-checkout checkout-scroll-pane">
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
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    gap: "10px",
                    marginBottom: "12px",
                  }}
                >
                  <h5 className="title" style={{ marginBottom: 0 }}>배송 정보</h5>
                  {actuallyLoggedIn && (
                    <button
                      type="button"
                      className="tf-btn btn-reset btn-md radius-4"
                      onClick={handleReloadMyInfo}
                      disabled={isReloadingMyInfo}
                      style={{
                        padding: "8px 14px",
                        fontSize: "12px",
                        lineHeight: 1.2,
                        whiteSpace: "nowrap",
                        cursor: isReloadingMyInfo ? "default" : "pointer",
                      }}
                    >
                      <span className="text">
                        {isReloadingMyInfo ? "불러오는 중..." : "내 정보 불러오기"}
                      </span>
                    </button>
                  )}
                </div>
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
                  {actuallyLoggedIn && (
                    <div style={{ marginBottom: "16px" }}>
                      <select
                        value={useNewAddress ? "new" : selectedAddressId}
                        onChange={(e) => handleAddressSelect(e.target.value)}
                        style={{
                          width: "100%",
                          padding: "10px",
                          border: "1px solid #ddd",
                          borderRadius: "6px",
                          fontSize: "14px",
                          backgroundColor: "#fff",
                        }}
                      >
                        <option value="new">직접 입력</option>
                        {savedAddresses.length > 0 ? (
                          savedAddresses.map((address) => (
                            <option key={address.addressNo} value={address.addressNo}>
                              {address.isDefault && "[기본] "}
                              {address.recipientName} - {address.deliveryAddress}
                              {address.deliveryAddressDetail ? ` ${address.deliveryAddressDetail}` : ""}
                            </option>
                          ))
                        ) : (
                          <option value="new" disabled>
                            저장된 주소가 없습니다
                          </option>
                        )}
                      </select>
                    </div>
                  )}
                  <div className="grid-2">
                    <input 
                      type="text" 
                      placeholder="기본 주소*" 
                      value={formData.deliveryAddress}
                      readOnly
                    />
                    <input 
                      ref={deliveryAddressDetailInputRef}
                      type="text" 
                      placeholder="상세 주소*" 
                      value={formData.deliveryAddressDetail}
                      onChange={(e) => setFormData({...formData, deliveryAddressDetail: e.target.value})}
                    />
                  </div>
                  <div className="grid-2">
                    <input 
                      type="text" 
                      placeholder="우편번호*" 
                      value={formData.deliveryZipCode}
                      readOnly
                    />
                    <button
                      type="button"
                      className="tf-btn btn-reset btn-md radius-4"
                      onClick={handleSearchAddress}
                      disabled={!isAddressScriptReady}
                    >
                      <span className="text">
                        {isAddressScriptReady ? "주소 검색" : "주소 검색 준비 중..."}
                      </span>
                    </button>
                  </div>
                  {useNewAddress && (
                    <p className="text-caption-1 text-secondary mb-2">
                      신규 주소는 "주소 검색"으로 선택해주세요.
                    </p>
                  )}
                  {actuallyLoggedIn && useNewAddress && (
                    <label style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px" }}>
                      <input
                        type="checkbox"
                        checked={saveNewAddress}
                        onChange={(e) => setSaveNewAddress(e.target.checked)}
                      />
                      <span className="text-caption-1">이번 배송지를 내 주소록에 저장</span>
                    </label>
                  )}
                  <textarea 
                    placeholder="배송 메모 (선택사항)..." 
                    value={formData.orderMemo}
                    onChange={(e) => setFormData({...formData, orderMemo: e.target.value})}
                  />
                </form>
              </div>
              <div className="wrap">
                <h5 className="title">결제 수단</h5>
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
            <div className="flat-spacing flat-sidebar-checkout checkout-scroll-pane">
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
                          alt="상품 이미지"
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
                              <span className="text-secondary">—</span>
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
                                      {formatKrw(baseUnitPrice)}
                                    </span>
                                  )}
                                  <span>{formatKrw(adjustedUnitPrice)}</span>
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
                        {formatKrw(selectedBaseTotalPrice)}
                      </span>
                    </div>
                    {selectedSaleDiscountAmount > 0 && (
                      <div className="item d-flex align-items-center justify-content-between text-button text-danger">
                        <span>세일 할인</span>
                        <span>-{formatKrw(selectedSaleDiscountAmount)}</span>
                      </div>
                    )}
                    {selectedSaleDiscountAmount > 0 && (
                      <div className="item d-flex align-items-center justify-content-between text-button">
                        <span>세일 적용 금액</span>
                        <span>{formatKrw(selectedTotalPrice)}</span>
                      </div>
                    )}
                    {validUsePointAmount > 0 && (
                      <div className="item d-flex align-items-center justify-content-between text-button text-success">
                        <span>포인트 할인</span>
                        <span>-{formatKrw(validUsePointAmount)}</span>
                      </div>
                    )}
                    <p className="text-caption-1 text-secondary mb-1 small">
                      전 상품 무료배송
                    </p>
                    <div className="item d-flex align-items-center justify-content-between text-button">
                      <span>배송비</span>
                      <span>
                        {selectedItems.length === 0 ? "—" : "무료"}
                      </span>
                    </div>
                  </div>
                  <div className="bottom">
                    <h5 className="d-flex justify-content-between">
                      <span>최종 결제 금액</span>
                      <span className="total-price-checkout">
                        {formatKrw(finalPrice)}
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
    </>
  );
}
