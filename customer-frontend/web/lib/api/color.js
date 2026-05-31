import { api } from "./http";

export async function getActiveColors() {
  const { data } = await api.get("/api/colors");
  return data;
}
