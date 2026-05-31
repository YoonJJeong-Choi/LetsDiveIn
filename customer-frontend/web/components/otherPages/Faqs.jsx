"use client";
import React, { useState, useEffect } from "react";
import Link from "next/link";
import { getFaqList } from "@/lib/api/faq";
import { INQUIRY_CATEGORIES, getCategoryLabel } from "@/data/inquiryCategories";
import InlineTemplateLoader from "@/components/common/InlineTemplateLoader";

export default function Faqs() {
  const [faqs, setFaqs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const normalizeFaqCategory = (category) => {
    if (!category) return "ETC";
    if (category === "포인트/쿠폰" || category === "포인트") return "POINT";
    const byCode = INQUIRY_CATEGORIES.find((c) => c.code === category);
    if (byCode) return byCode.code;
    const byLabel = INQUIRY_CATEGORIES.find((c) => c.label === category);
    return byLabel ? byLabel.code : category;
  };

  const groupFaqsByCategory = (faqList) => {
    const grouped = {};
    faqList.forEach((faq) => {
      const code = normalizeFaqCategory(faq.faqCategory);
      if (!grouped[code]) grouped[code] = [];
      grouped[code].push(faq);
    });
    return grouped;
  };

  const categoryOrder = INQUIRY_CATEGORIES.map((c) => c.code);

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

  const sortedCategories = categoryOrder.filter(
    (code) => groupedFaqs[code] && groupedFaqs[code].length > 0
  );

  Object.keys(groupedFaqs).forEach((code) => {
    if (!sortedCategories.includes(code)) {
      sortedCategories.push(code);
    }
  });

  if (loading) {
    return (
      <section className="flat-spacing">
        <div className="container">
          <div className="page-faqs-wrap py-5 d-flex justify-content-center">
            <InlineTemplateLoader />
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
              sortedCategories.map((categoryCode, categoryIndex) => {
                const categoryFaqs = groupedFaqs[categoryCode];
                const accordionId = `accordion-faq-${categoryIndex + 1}`;

                return (
                  <div key={categoryCode}>
                    <h5 className="faqs-title">
                      {getCategoryLabel(categoryCode)}
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
                                <p className="text-secondary">{faq.faqAnswer}</p>
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
              <h5 className="mb_4">1:1 QnA</h5>
              <p className="mb_20 text-secondary">
                FAQ에서 답을 찾지 못하셨다면 1:1 QnA를 남겨 주세요.
              </p>
              <Link href="/qna" className="btn-style-2 w-100 text-center d-inline-block">
                <span className="text text-button">QnA 등록·내역 보기</span>
              </Link>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
