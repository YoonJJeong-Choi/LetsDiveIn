export function formatKrw(value) {
  const num = Number(value);
  if (!Number.isFinite(num)) return "0원";
  return `${Math.round(num).toLocaleString("ko-KR")}원`;
}

