import { api } from "./http";

export async function getPublicEventList() {
  const { data } = await api.get("/api/events");
  return data;
}

export async function getPublicEventDetail(eventNo) {
  const { data } = await api.get(`/api/events/${eventNo}`);
  return data;
}
