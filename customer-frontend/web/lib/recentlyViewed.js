const STORAGE_KEY = "recentlyViewed";
const MAX_ITEMS = 20;

function isBrowser() {
  return typeof window !== "undefined";
}

export function getRecentlyViewed() {
  if (!isBrowser()) return [];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function addRecentlyViewed(product) {
  if (!isBrowser() || !product) return;
  const id = product.productNo ?? product.id;
  if (!id) return;

  const entry = {
    id,
    title: product.productName ?? product.title ?? "",
    brandName: product.brandName ?? null,
    imgSrc: product.imgSrc ?? product.productImageUrl ?? "",
    imgHover: product.imgSrc ?? product.productImageUrl ?? "",
    price: Number(product.price ?? product.productPrice ?? 0),
    viewedAt: Date.now(),
  };

  try {
    const list = getRecentlyViewed().filter((p) => p.id !== id);
    list.unshift(entry);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list.slice(0, MAX_ITEMS)));
  } catch {
    // storage full or unavailable
  }
}
