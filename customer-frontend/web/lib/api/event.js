import axios from "axios";

const baseURL =
  typeof window !== "undefined"
    ? process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
    : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

export async function getPublicEventList() {
  const { data } = await api.get("/api/events");
  return data;
}

export async function getPublicEventDetail(eventNo) {
  const { data } = await api.get(`/api/events/${eventNo}`);
  return data;
}
