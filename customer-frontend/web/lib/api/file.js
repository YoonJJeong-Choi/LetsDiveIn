import axios from "axios";

const baseURL =
  typeof window !== "undefined"
    ? (process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080")
    : process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL,
  withCredentials: true,
});

export async function uploadFile(file, category) {
  const formData = new FormData();
  formData.append("file", file);
  if (category) formData.append("category", category);

  const { data } = await api.post("/api/files/upload", formData);
  return data;
}
