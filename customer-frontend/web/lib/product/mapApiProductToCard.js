import { getApplicableSale } from "@/lib/api/sale";
import {
  DEFAULT_PRODUCT_PLACEHOLDER,
  pickProductListThumbnail,
} from "@/lib/media/productImage";

function calculateDiscountAmount(salePolicy, basePrice) {
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
}

/**
 * 백엔드 ProductListDto 등 → ProductCard1용 객체 (세일 반영)
 */
export async function mapApiProductToCard(product) {
  const sizes = product.options?.map((opt) => opt.size).filter(Boolean) || [];
  const colors = product.options?.map((opt) => opt.color).filter(Boolean) || [];

  const basePrice = product.minPrice ? product.minPrice : parseFloat(product.productPrice) || 0;
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

  return {
    id: product.productNo,
    title: product.productName,
    price: hasSale ? salePrice : basePrice,
    oldPrice: hasSale ? basePrice : null,
    imgSrc: pickProductListThumbnail(product, DEFAULT_PRODUCT_PLACEHOLDER),
    imgHover: pickProductListThumbnail(product, DEFAULT_PRODUCT_PLACEHOLDER),
    isOnSale: hasSale,
    inStock: true,
    filterBrands: [],
    filterColor: colors,
    filterSizes: sizes,
    sizes: sizes.length ? sizes : undefined,
    tabFilterOptions: product.productType ? [product.productType] : [],
    tabFilterOptions2: [],
    brandName: product.brandName || null,
    productNo: product.productNo,
    productType: product.productType,
    productSubType: product.productSubType,
    productDescription: product.productDescription,
    options: product.options || [],
    minPrice: product.minPrice,
    maxPrice: product.maxPrice,
  };
}
