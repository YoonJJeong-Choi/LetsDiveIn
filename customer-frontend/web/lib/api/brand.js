import { api } from "./http";

export async function getActiveBrands() {
  const { data } = await api.get("/api/brands");
  return data;
}
