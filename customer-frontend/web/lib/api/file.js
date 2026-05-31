import { api } from "./http";

export async function uploadFile(file, category) {
  const formData = new FormData();
  formData.append("file", file);
  if (category) formData.append("category", category);

  // axios 기본 Content-Type: application/json 이면 FormData에 boundary가 붙지 않아
  // Spring이 multipart를 파싱하지 못하고 500이 날 수 있음 → 업로드 요청만 헤더 제거
  const { data } = await api.post("/api/files/upload", formData, {
    transformRequest: [
      (body, headers) => {
        delete headers["Content-Type"];
        return body;
      },
    ],
  });
  return data;
}
