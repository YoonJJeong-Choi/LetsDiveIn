import { PRODUCT_TYPES } from "@/data/productTaxonomy";
import { shopHref } from "@/data/navMain";

const CIRCLE_IMAGES = [
  "/images/collections/collection-circle/Man.png",
  "/images/collections/collection-circle/Woman.png",
  "/images/collections/collection-circle/Kid.png",
  "/images/collections/collection-circle/Cap.png",
  "/images/collections/collection-circle/Glasses.png",
  "/images/collections/collection-circle/Pin.png",
  "/images/collections/collection-circle/Accessories.png",
  "/images/collections/collection-circle/Etc.jpg",
];

export const homeCategoryCollections = PRODUCT_TYPES.map((t, index) => ({
  productType: t.value,
  title: t.label,
  imgSrc: CIRCLE_IMAGES[index % CIRCLE_IMAGES.length],
  alt: `${t.label} 카테고리`,
  href: shopHref(t.value),
}));
