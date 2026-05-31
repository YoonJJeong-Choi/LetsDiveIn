import { api } from "./http";

export async function login(email, password) {
  const { data } = await api.post("/api/auth/login", {
    email,
    password,
    portal: "CUSTOMER",
  });
  return data;
}

export async function logout() {
  await api.post("/api/auth/logout");
}

export function isPortalAccessError(err) {
  return err?.response?.status === 403;
}

export async function getMe(options = {}) {
  const { throwOnForbidden = false } = options;

  try {
    const { data } = await api.get("/api/auth/me");
    return data;
  } catch (err) {
    if (err.response?.status === 401) return null;
    if (err.response?.status === 403 && !throwOnForbidden) return null;
    if (err.code === "ERR_NETWORK" || err.message === "Network Error") {
      return null;
    }
    throw err;
  }
}
