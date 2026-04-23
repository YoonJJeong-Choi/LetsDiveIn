"use client";

import { useEffect, useState } from "react";

const sectionIds = [
  "terms",
  "limitations",
  "revisions-and-errata",
  "site-terms",
  "risks",
];
const sections = [
  { id: 1, text: "약관 (입점 신청)", scroll: "terms" },
  { id: 2, text: "입점 심사/의무", scroll: "limitations" },
  {
    id: 3,
    text: "지식재산권/서비스 제한",
    scroll: "revisions-and-errata",
  },
  {
    id: 4,
    text: "면책/약관 변경",
    scroll: "site-terms",
  },
  { id: 5, text: "준거법/관할", scroll: "risks" },
];

export default function Terms() {
  const [activeSection, setActiveSection] = useState(sectionIds[0]);

  useEffect(() => {
    // Create an IntersectionObserver to track visibility of sections
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            // Update active section when the section is visible in the viewport
            setActiveSection(entry.target.id);
          }
        });
      },
      {
        rootMargin: "-50% 0px", // Trigger when section is 50% visible
      }
    );

    // Observe each section
    sectionIds.forEach((id) => {
      const element = document.getElementById(id);
      if (element) {
        observer.observe(element);
      }
    });

    return () => {
      // Cleanup the observer when the component unmounts
      observer.disconnect();
    };
  }, [sectionIds]);

  const handleClick = (id) => {
    document
      .getElementById(id)
      .scrollIntoView({ behavior: "smooth", block: "center" });
  };

  return (
    <section className="flat-spacing">
      <div className="container">
        <div className="terms-of-use-wrap">
          <div className="left sticky-top">
            {sections.map(({ id, text, scroll, isActive }) => (
              <h6
                key={id}
                onClick={() => handleClick(scroll)}
                className={`btn-scroll-target ${
                  activeSection == scroll ? "active" : ""
                }`}
              >
                {id}. {text}
              </h6>
            ))}
          </div>
          <div className="right">
            <h4 className="heading">Terms of use</h4>
            <div className="terms-of-use-item item-scroll-target" id="terms">
              <h5 className="terms-of-use-title">1. Terms</h5>
              <div className="terms-of-use-content">
                <p>
                  <strong>제1조 (목적)</strong>
                  <br />
                  본 약관은 수영 커머스(이하 &quot;회사&quot;)와 파트너 입점을 신청하는 사업자(이하 &quot;파트너&quot;) 간의 서비스 이용과 관련한 권리, 의무 및 책임 사항을 규정함을 목적으로 합니다.
                </p>

                <p>
                  <strong>제2조 (용어의 정의)</strong>
                  <br />
                  1. &quot;파트너&quot;란 회사의 커머스 플랫폼에 상품 또는 서비스를 판매하기 위해 입점을 신청하거나 승인받은 사업자를 의미합니다.
                  <br />
                  2. &quot;서비스&quot;란 회사가 운영하는 온라인 플랫폼을 통해 제공되는 상품 판매 및 관련 부가 서비스를 의미합니다.
                </p>

                <p>
                  <strong>제3조 (입점 신청)</strong>
                  <br />
                  1. 파트너는 회사가 정한 절차에 따라 입점 신청서를 작성하고 필요한 정보를 제공하여야 합니다.
                  <br />
                  2. 파트너는 입점 심사를 위해 회사가 지정한 서류(예: 사업자등록증 사본, 통장 사본)를 전자적으로 제출할 수 있으며, 해당 서류가 입점 심사 및 판단 목적을 위해 수집·보관·처리(열람 포함)됨에 동의합니다. 제출된 서류는 비공개로 저장되며, 회사의 관리자(또는 승인된 담당자)에 의해 열람·다운로드될 수 있습니다.
                  <br />
                  3. 파트너는 제공한 정보가 사실과 다르거나 부정확한 경우 발생하는 모든 책임을 부담합니다.
                </p>

                <p>
                  <strong>제4조 (입점 심사 및 승인)</strong>
                  <br />
                  1. 회사는 파트너의 입점 신청에 대해 자체 기준에 따라 심사를 진행할 수 있습니다.
                  <br />
                  2. 회사는 다음 각 호에 해당하는 경우 입점을 승인하지 않을 수 있습니다.
                  <br />
                  &nbsp;&nbsp;* 허위 정보를 제공한 경우
                  <br />
                  &nbsp;&nbsp;* 관련 법령을 위반한 이력이 있는 경우
                  <br />
                  &nbsp;&nbsp;* 플랫폼 운영 정책에 부합하지 않는 경우
                  <br />
                  3. 회사는 파트너가 제출한 서류를 입점 심사 및 승인/거절 판단 목적을 위해서만 이용하며, 해당 목적 달성 후 내부 절차에 따라 보관 또는 파기합니다.
                </p>
              </div>
            </div>
            <div
              className="terms-of-use-item item-scroll-target"
              id="limitations"
            >
              <h5 className="terms-of-use-title">2. 입점 심사/의무</h5>
              <div className="terms-of-use-content">
                <p>
                  <strong>제5조 (파트너의 의무)</strong>
                  <br />
                  1. 파트너는 관련 법령 및 본 약관을 준수하여 상품 또는 서비스를 판매하여야 합니다.
                  <br />
                  2. 파트너는 상품 정보, 가격, 배송, 환불 등 거래와 관련된 사항에 대해 정확한 정보를 제공하여야 합니다.
                  <br />
                  3. 파트너는 소비자 보호 관련 법령을 준수하여야 합니다.
                </p>
                <p>
                  <strong>제6조 (수수료 및 정산)</strong>
                  <br />
                  1. 회사는 플랫폼 이용에 대한 수수료를 부과할 수 있으며, 수수료율은 별도의 정책에 따릅니다.
                  <br />
                  2. 판매 대금의 정산 방식 및 일정은 회사의 정산 정책에 따릅니다.
                </p>
                <p>
                  <strong>제7조 (지식재산권) 및 제8조 (서비스 제한 및 계약 해지) 안내</strong>
                  <br />
                  (세부 내용은 아래 섹션 3~4에 이어서 제공됩니다.)
                </p>
              </div>
            </div>
            <div
              className="terms-of-use-item item-scroll-target"
              id="revisions-and-errata"
            >
              <h5 className="terms-of-use-title">3. 지식재산권/서비스 제한</h5>
              <div className="terms-of-use-content">
                <p>
                  <strong>제7조 (지식재산권)</strong>
                  <br />
                  1. 플랫폼 및 서비스에 대한 지식재산권은 회사에 귀속됩니다.
                  <br />
                  2. 파트너는 회사의 사전 동의 없이 플랫폼의 콘텐츠를 무단으로 사용할 수 없습니다.
                </p>
                <p>
                  <strong>제8조 (서비스 제한 및 계약 해지)</strong>
                  <br />
                  1. 파트너가 본 약관을 위반하거나 서비스 운영에 중대한 지장을 초래하는 경우 회사는 서비스 이용을 제한하거나 계약을 해지할 수 있습니다.
                  <br />
                  2. 파트너는 언제든지 회사에 요청하여 입점 계약을 해지할 수 있습니다.
                </p>
                <p>
                  (이 섹션의 다음 조항은 아래 섹션 4에 이어서 제공됩니다.)
                </p>
              </div>
            </div>
            <div
              className="terms-of-use-item item-scroll-target"
              id="site-terms"
            >
              <h5 className="terms-of-use-title">4. 면책/약관 변경</h5>
              <div className="terms-of-use-content">
                <p>
                  <strong>제9조 (면책)</strong>
                  <br />
                  회사는 천재지변, 시스템 장애 등 불가항력적인 사유로 인해 발생한 손해에 대해 책임을 지지 않습니다.
                </p>
                <p>
                  <strong>제10조 (약관의 변경)</strong>
                  <br />
                  회사는 필요한 경우 관련 법령을 준수하는 범위 내에서 본 약관을 변경할 수 있습니다.
                </p>
                <p>
                  (이 섹션의 다음 조항은 아래 섹션 5에 이어서 제공됩니다.)
                </p>
              </div>
            </div>
            <div className="terms-of-use-item item-scroll-target" id="risks">
              <h5 className="terms-of-use-title">5. 준거법/관할</h5>
              <div className="terms-of-use-content">
                <p>
                  <strong>제11조 (준거법 및 관할)</strong>
                  <br />
                  본 약관은 대한민국 법률을 준거법으로 하며, 서비스 이용과 관련하여 발생한 분쟁은 회사의 본점 소재지를 관할하는 법원을 전속 관할로 합니다.
                </p>

              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
