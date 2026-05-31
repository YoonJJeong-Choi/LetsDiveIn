"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getMe } from "@/lib/api/auth";
import { createQna, getMyQnaDetail, getMyQnaList } from "@/lib/api/qna";
import { getOrders } from "@/lib/api/order";
import { INQUIRY_CATEGORIES, QNA_SCOPES, getCategoryLabel } from "@/data/inquiryCategories";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

const needsOrderItem = (category) =>
  category === "PRODUCT" || category === "DELIVERY";

const needsScope = (category) => category === "RETURN_EXCHANGE";

const optionalOrder = (category) =>
  category === "ORDER_PAYMENT" || category === "POINT";

const LOGIN_NEXT = "/qna";

function isUnauthorized(err) {
  return err?.response?.status === 401;
}

export default function QnaPage() {
  const router = useRouter();
  const [view, setView] = useState("list");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [list, setList] = useState([]);
  const [detail, setDetail] = useState(null);

  const [category, setCategory] = useState("ORDER_PAYMENT");
  const [scope, setScope] = useState("POLICY");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [selectedItemKey, setSelectedItemKey] = useState("");

  const [allOrderItems, setAllOrderItems] = useState([]);

  const showOrderFields = useMemo(
    () =>
      needsOrderItem(category) ||
      optionalOrder(category) ||
      (needsScope(category) && scope === "ORDER_ITEM"),
    [category, scope]
  );

  const showOrderItemField = useMemo(
    () => needsOrderItem(category) || (needsScope(category) && scope === "ORDER_ITEM"),
    [category, scope]
  );

  const redirectToLogin = useCallback(() => {
    router.push(`/login?next=${encodeURIComponent(LOGIN_NEXT)}`);
  }, [router]);

  const loadList = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await getMyQnaList();
      const rows = res?.data ?? (Array.isArray(res) ? res : []);
      setList(Array.isArray(rows) ? rows : []);
    } catch (e) {
      if (isUnauthorized(e)) {
        redirectToLogin();
        return;
      }
      setError(e.response?.data?.message || "QnA 목록을 불러오지 못했습니다.");
      setList([]);
    } finally {
      setLoading(false);
    }
  };

  const goToCreate = async () => {
    const user = await getMe();
    if (!user) {
      redirectToLogin();
      return;
    }
    setView("create");
  };

  useEffect(() => {
    if (view === "list") loadList();
  }, [view]);

  useEffect(() => {
    if (view !== "create") return;
    (async () => {
      const user = await getMe();
      if (!user) {
        redirectToLogin();
        return;
      }
      try {
        const data = await getOrders({ page: 1, size: 100 });
        const orderList = data?.items ?? data?.content ?? data?.orders ?? (Array.isArray(data) ? data : []);
        const flat = [];
        for (const order of (Array.isArray(orderList) ? orderList : [])) {
          for (const item of (order.orderItems || [])) {
            if (item.isCancelled) continue;
            flat.push({
              orderNo: order.orderNo,
              orderDate: order.orderCreatedAt?.slice?.(0, 10) || "",
              orderItemNo: item.orderItemNo,
              productName: item.productName || `상품 #${item.orderItemNo}`,
              optionLabel: [item.color, item.size].filter(Boolean).join(" / "),
            });
          }
        }
        setAllOrderItems(flat);
      } catch (e) {
        if (isUnauthorized(e)) {
          redirectToLogin();
          return;
        }
        setAllOrderItems([]);
      }
    })();
  }, [view, redirectToLogin]);

  const openDetail = async (qnaNo) => {
    try {
      setLoading(true);
      const res = await getMyQnaDetail(qnaNo);
      setDetail(res?.data ?? res);
      setView("detail");
    } catch (e) {
      alert(e.response?.data?.message || e.message || "상세 조회 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || !body.trim()) {
      alert("제목과 내용을 입력해 주세요.");
      return;
    }
    if (needsOrderItem(category) && !selectedItemKey) {
      alert("상품·배송 문의는 주문 상품을 선택해 주세요.");
      return;
    }
    if (needsScope(category) && scope === "ORDER_ITEM" && !selectedItemKey) {
      alert("주문 상품 관련 문의는 주문 상품을 선택해 주세요.");
      return;
    }
    const payload = {
      category,
      title: title.trim(),
      body: body.trim(),
    };
    if (needsScope(category)) payload.scope = scope;

    const selected = allOrderItems.find((i) => String(i.orderItemNo) === selectedItemKey);
    if (selected) {
      payload.orderNo = selected.orderNo;
      payload.orderItemNo = selected.orderItemNo;
    }

    try {
      setSubmitting(true);
      await createQna(payload);
      alert("QnA가 등록되었습니다.");
      setView("list");
      setTitle("");
      setBody("");
      setSelectedItemKey("");
    } catch (err) {
      if (isUnauthorized(err)) {
        redirectToLogin();
        return;
      }
      alert(err.response?.data?.message || err.message || "등록에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  if (view === "create") {
    return (
      <section className="flat-spacing">
        <div className="container">
          <div className="mb-4">
            <button type="button" className="btn btn-line" onClick={() => setView("list")}>
              ← 목록
            </button>
          </div>
          <h4 className="mb-4">1:1 QnA 등록</h4>
          <form onSubmit={handleSubmit} className="form-leave-comment">
            <fieldset className="mb-3">
              <label className="text-caption-1 mb-2 d-block">문의 유형</label>
              <select
                className="form-select"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                {INQUIRY_CATEGORIES.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.label}
                  </option>
                ))}
              </select>
            </fieldset>

            {needsScope(category) && (
              <fieldset className="mb-3">
                <label className="text-caption-1 mb-2 d-block">취소/반품/교환 세부 유형</label>
                {QNA_SCOPES.map((s) => (
                  <label key={s.code} className="d-block mb-2">
                    <input
                      type="radio"
                      name="scope"
                      value={s.code}
                      checked={scope === s.code}
                      onChange={() => setScope(s.code)}
                    />{" "}
                    {s.label}
                  </label>
                ))}
              </fieldset>
            )}

            {showOrderFields && (
              <fieldset className="mb-3">
                <label className="text-caption-1 mb-2 d-block">주문 상품 선택</label>
                <select
                  className="form-select"
                  value={selectedItemKey}
                  onChange={(e) => setSelectedItemKey(e.target.value)}
                  required={showOrderItemField}
                >
                  <option value="">선택 안 함</option>
                  {allOrderItems.map((item) => (
                    <option key={item.orderItemNo} value={String(item.orderItemNo)}>
                      {item.productName}
                      {item.optionLabel ? ` (${item.optionLabel})` : ""}
                      {" — "}
                      {item.orderDate}
                    </option>
                  ))}
                </select>
                {showOrderItemField && (
                  <p className="text-secondary small mt-2">
                    상품·배송 문의는 주문 상품을 선택해 주세요. 해당 판매자에게 답변이 전달됩니다.
                  </p>
                )}
              </fieldset>
            )}

            <fieldset className="mb-3">
              <label className="text-caption-1 mb-2 d-block">제목</label>
              <input
                className="form-control"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
            </fieldset>
            <fieldset className="mb-3">
              <label className="text-caption-1 mb-2 d-block">내용</label>
              <textarea
                className="form-control"
                rows={5}
                value={body}
                onChange={(e) => setBody(e.target.value)}
                required
              />
            </fieldset>
            <button type="submit" className="btn-style-2" disabled={submitting}>
              {submitting ? "등록 중…" : "등록"}
            </button>
          </form>
        </div>
      </section>
    );
  }

  if (view === "detail" && detail) {
    return (
      <section className="flat-spacing">
        <div className="container">
          <button type="button" className="btn btn-line mb-4" onClick={() => setView("list")}>
            ← 목록
          </button>
          <h4 className="mb-2">{detail.title}</h4>
          <p className="text-secondary mb-4">
            {getCategoryLabel(detail.category)} · {detail.statusLabel || detail.status}
          </p>
          {(detail.messages || []).map((m) => (
            <div key={m.messageNo} className="border rounded p-3 mb-3">
              <div className="text-secondary small mb-2">
                {m.authorName} · {m.createdAt?.replace?.("T", " ")?.slice(0, 16)}
              </div>
              <p className="mb-0" style={{ whiteSpace: "pre-wrap" }}>
                {m.body}
              </p>
            </div>
          ))}
          {detail.status === "ANSWERED" && (
            <button type="button" className="btn-style-2 mt-3" onClick={goToCreate}>
              새 QnA 등록
            </button>
          )}
        </div>
      </section>
    );
  }

  return (
    <section className="flat-spacing">
      <div className="container">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h4 className="mb-0">1:1 QnA</h4>
          <button type="button" className="btn-style-2" onClick={goToCreate}>
            QnA 등록
          </button>
        </div>
        <p className="text-secondary mb-4">
          FAQ에서 해결되지 않으면 QnA를 남겨 주세요. 답변은 1회 등록되며, 추가 질문은 새 QnA로 등록해 주세요.
        </p>
        <p className="mb-4">
          <Link href="/FAQs">자주 묻는 질문 보기</Link>
        </p>

        {loading ? (
          <InlineTemplateLoader />
        ) : error ? (
          <p className="text-danger">{error}</p>
        ) : list.length === 0 ? (
          <p>등록한 QnA가 없습니다.</p>
        ) : (
          <ul className="list-group">
            {list.map((q) => (
              <li key={q.qnaNo} className="list-group-item list-group-item-action">
                <button
                  type="button"
                  className="btn btn-link text-start w-100 p-0 text-decoration-none"
                  onClick={() => openDetail(q.qnaNo)}
                >
                  <strong>{q.title}</strong>
                  <br />
                  <small className="text-secondary">
                    {getCategoryLabel(q.category)} · {q.statusLabel || q.status} ·{" "}
                    {q.createdAt?.replace?.("T", " ")?.slice(0, 16)}
                  </small>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
