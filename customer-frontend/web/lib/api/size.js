import { api } from "./http";

export async function getActiveSizes() {
  const { data } = await api.get("/api/sizes");
  return data;
}
