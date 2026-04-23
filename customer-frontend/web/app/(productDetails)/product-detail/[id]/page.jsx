"use client";

import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Topbar6 from "@/components/headers/Topbar6";
import Breadcumb from "@/components/productDetails/Breadcumb";
import Descriptions1 from "@/components/productDetails/descriptions/Descriptions1";
import Details1 from "@/components/productDetails/details/Details1";
import RelatedProducts from "@/components/productDetails/RelatedProducts";
import { getProductDetail } from "@/lib/api/product";
import React, { useEffect, useState } from "react";
import { useParams } from "next/navigation";

export default function ProductDetailPage() {
  const params = useParams();
  const productNo = params?.id ? parseInt(params.id) : null;
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProduct = async () => {
      if (!productNo) {
        setError("상품 번호가 없습니다.");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        const data = await getProductDetail(productNo);
        
        // 백엔드 데이터를 프론트엔드 형식으로 변환
        const transformedProduct = {
          id: data.productNo,
          productNo: data.productNo,
          title: data.productName,
          imgSrc: data.productImageUrl,
          images: Array.isArray(data.images) ? data.images.map((img, idx) => ({
            src: img.imageUrl,
            alt: data.productName || `image-${idx+1}`,
            width: 800,
            height: 800,
            color: "gray",
            id: idx + 1,
            isPrimary: !!img.isPrimary,
            sortOrder: typeof img.sortOrder === 'number' ? img.sortOrder : idx
          })) : [],
          price: data.minPrice || parseInt(data.productPrice),
          oldPrice: data.maxPrice !== data.minPrice ? data.maxPrice : null,
          description: data.productDescription,
          category: data.productType,
          subCategory: data.productSubType,
          options: (data.options || []).map(opt => {
            // 재고 정보 처리: 백엔드에서 stockQuantity와 inStock이 전달됨
            // stockQuantity가 0이면 품절, null이면 재고 정보 없음
            // inStock이 false이면 품절, true이면 재고 있음, null이면 재고 정보 없음
            return {
              ...opt,
              stockQuantity: opt.stockQuantity !== undefined ? opt.stockQuantity : null,
              inStock: opt.inStock !== undefined ? opt.inStock : null
            };
          }),
        };
        
        setProduct(transformedProduct);
      } catch (err) {
        console.error("상품 상세 조회 실패:", err);
        console.error("에러 상세:", err.response?.data || err);
        setError(err.message || err.response?.data?.message || "상품 상세 정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [productNo]);

  if (loading) {
    return (
      <>
        <Topbar6 bgColor="bg-main" />
        <Header1 />
        <div className="container" style={{ padding: "100px 0", textAlign: "center" }}>
          <p>로딩 중...</p>
        </div>
        <Footer1 hasPaddingBottom />
      </>
    );
  }

  if (error || !product) {
    return (
      <>
        <Topbar6 bgColor="bg-main" />
        <Header1 />
        <div className="container" style={{ padding: "100px 0", textAlign: "center" }}>
          <p>{error || "상품을 찾을 수 없습니다."}</p>
        </div>
        <Footer1 hasPaddingBottom />
      </>
    );
  }

  return (
    <>
      <Topbar6 bgColor="bg-main" />
      <Header1 />
      <Breadcumb product={product} />
      <Details1 key={product.id} product={product} />
      <Descriptions1 product={product} />
      <RelatedProducts />
      <Footer1 hasPaddingBottom />
    </>
  );
}
