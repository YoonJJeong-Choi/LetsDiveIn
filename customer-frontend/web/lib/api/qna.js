import { api } from "./http";

export async function createQna(payload) {
  const { data } = await api.post("/api/qna", payload);
  return data;
}

export async function getMyQnaList(params = {}) {
  const { data } = await api.get("/api/qna", { params });
  return data;
}

export async function getMyQnaDetail(qnaNo) {
  const { data } = await api.get(`/api/qna/${qnaNo}`);
  return data;
}

export async function getInquiryCategories() {
  const { data } = await api.get("/api/inquiry-categories");
  return data;
}
