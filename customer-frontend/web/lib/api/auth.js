import axios from "axios";

const baseURL =
  typeof window !== "undefined"
    ? (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080")
    : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});

export async function login(email, password) {
  const { data } = await api.post("/api/auth/login", { email, password });
  return data;
}

export async function logout() {
  await api.post("/api/auth/logout");
}

export async function getMe() {
  try {
    const { data } = await api.get("/api/auth/me");
    return data;
  } catch (err) {
    if (err.response?.status === 401) return null;
    if (err.code === "ERR_NETWORK" || err.message === "Network Error") {
      return null;
    }
    throw err;
  }
}
