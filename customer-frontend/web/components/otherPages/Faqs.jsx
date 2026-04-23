"use client";
import React, { useState, useEffect } from "react";
import { getFaqList } from "@/lib/api/faq";

export default function Faqs() {
  const [faqs, setFaqs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 카테고리별 FAQ 그룹화
  const groupFaqsByCategory = (faqList) => {
    const grouped = {};
    faqList.forEach((faq) => {
      const category = faq.faqCategory || "기타";
      if (!grouped[category]) {
        grouped[category] = [];
      }
      grouped[category].push(faq);
    });
    return grouped;
  };

  // 카테고리 표시 순서
  const categoryOrder = [
    "주문/결제",
    "배송",
    "취소/반품/교환",
    "회원정보",
    "상품",
    "포인트/쿠폰",
    "기타",
  ];

  useEffect(() => {
    const fetchFaqs = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await getFaqList();
        const faqList = response.data || response || [];
        setFaqs(Array.isArray(faqList) ? faqList : []);
      } catch (err) {
        console.error("FAQ 조회 실패:", err);
        setError("FAQ를 불러오는데 실패했습니다.");
        setFaqs([]);
      } finally {
        setLoading(false);
      }
    };

    fetchFaqs();
  }, []);

  const groupedFaqs = groupFaqsByCategory(faqs);

  // 카테고리 순서대로 정렬
  const sortedCategories = categoryOrder.filter(
    (cat) => groupedFaqs[cat] && groupedFaqs[cat].length > 0
  );

  // 순서에 없는 카테고리도 추가
  Object.keys(groupedFaqs).forEach((cat) => {
    if (!sortedCategories.includes(cat)) {
      sortedCategories.push(cat);
    }
  });

  // 카테고리 한글명 매핑
  const categoryLabels = {
    "주문/결제": "주문/결제",
    배송: "배송",
    "취소/반품/교환": "취소/반품/교환",
    회원정보: "회원정보",
    상품: "상품",
    "포인트/쿠폰": "포인트/쿠폰",
    기타: "기타",
  };

  if (loading) {
    return (
      <section className="flat-spacing">
        <div className="container">
          <div className="page-faqs-wrap">
            <div className="text-center py-5">
              <p>FAQ를 불러오는 중...</p>
            </div>
          </div>
        </div>
      </section>
    );
  }

  if (error) {
    return (
      <section className="flat-spacing">
        <div className="container">
          <div className="page-faqs-wrap">
            <div className="text-center py-5">
              <p className="text-danger">{error}</p>
            </div>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="flat-spacing">
      <div className="container">
        <div className="page-faqs-wrap">
          <div className="list-faqs">
            {sortedCategories.length === 0 ? (
              <div className="text-center py-5">
                <p>등록된 FAQ가 없습니다.</p>
              </div>
            ) : (
              sortedCategories.map((category, categoryIndex) => {
                const categoryFaqs = groupedFaqs[category];
                const accordionId = `accordion-faq-${categoryIndex + 1}`;

                return (
                  <div key={category}>
                    <h5 className="faqs-title">
                      {categoryLabels[category] || category}
                    </h5>
                    <ul
                      className="accordion-product-wrap style-faqs"
                      id={accordionId}
                    >
                      {categoryFaqs.map((faq, faqIndex) => {
                        const itemId = `accordion-${categoryIndex + 1}-${faqIndex + 1}`;
                        const isFirst = faqIndex === 0;

                        return (
                          <li key={faq.faqNo} className="accordion-product-item">
                            <a
                              href={`#${itemId}`}
                              className={`accordion-title ${isFirst ? "current" : "collapsed current"}`}
                              data-bs-toggle="collapse"
                              aria-expanded={isFirst ? "true" : "false"}
                              aria-controls={itemId}
                            >
                              <h6>{faq.faqQuestion}</h6>
                              <span className="btn-open-sub" />
                            </a>
                            <div
                              id={itemId}
                              className={`collapse ${isFirst ? "show" : ""}`}
                              data-bs-parent={`#${accordionId}`}
                            >
                              <div className="accordion-faqs-content">
                                <p className="text-secondary">
                                  {faq.faqAnswer}
                                </p>
                              </div>
                            </div>
                          </li>
                        );
                      })}
                    </ul>
                  </div>
                );
              })
            )}
          </div>
          <div className="ask-question sticky-top">
            <div className="ask-question-wrap">
              <h5 className="mb_4">문의하기</h5>
              <p className="mb_20 text-secondary">
                궁금한 점이 있으시면 문의해주세요
              </p>
              <form
                className="form-leave-comment"
                onSubmit={(e) => e.preventDefault()}
              >
                <fieldset className="mb_20">
                  <div className="text-caption-1 mb_8">이름</div>
                  <input
                    className=""
                    type="text"
                    placeholder="이름을 입력하세요*"
                    name="name"
                    tabIndex={2}
                    defaultValue=""
                    aria-required="true"
                    required
                  />
                </fieldset>
                <fieldset className="mb_20">
                  <div className="text-caption-1 mb_8">
                    문의 유형을 선택하세요
                  </div>
                  <div className="tf-select">
                    <select className="">
                      <option>주문/결제</option>
                      <option>배송</option>
                      <option>취소/반품/교환</option>
                      <option>회원정보</option>
                      <option>상품</option>
                      <option>포인트/쿠폰</option>
                      <option>기타</option>
                    </select>
                  </div>
                </fieldset>
                <fieldset className="mb_20">
                  <div className="text-caption-1 mb_8">문의 내용</div>
                  <textarea
                    className=""
                    rows={4}
                    placeholder="문의 내용을 입력하세요*"
                    tabIndex={2}
                    aria-required="true"
                    required
                    defaultValue={""}
                  />
                </fieldset>
                <div className="button-submit">
                  <button className="btn-style-2 w-100" type="submit">
                    <span className="text text-button">문의하기</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
