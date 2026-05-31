import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Link from "next/link";
import { STORE_EMAIL, STORE_PHONE_DISPLAY } from "@/data/storeContact";

export default function PolicyPage({
  title,
  lead,
  effectiveDate,
  sections,
}) {
  return (
    <>
      <Header1 />
      <div
        className="page-title"
        style={{ backgroundImage: "url(/images/section/page-title.jpg)" }}
      >
        <div className="container-full">
          <div className="row">
            <div className="col-12">
              <h3 className="heading text-center">{title}</h3>
              <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                <li>
                  <Link className="link" href={`/`}>
                    홈
                  </Link>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>
                  <span className="link">고객 지원</span>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>{title}</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <section className="flat-spacing">
        <div className="container">
          <div className="mx-auto" style={{ maxWidth: 960 }}>
            <div className="mb_32">
              <h4 className="mb_12">{title}</h4>
              <p className="text-secondary mb_12">{lead}</p>
              <p className="text-caption-1 mb-0">시행일: {effectiveDate}</p>
            </div>

            <div className="d-grid" style={{ gap: 24 }}>
              {sections.map((section, index) => (
                <div
                  key={section.title}
                  className="p-4"
                  style={{
                    border: "1px solid #e9e9e9",
                    borderRadius: 16,
                    backgroundColor: "#fff",
                  }}
                >
                  <h5 className="mb_16">
                    {index + 1}. {section.title}
                  </h5>
                  <ul className="mb-0 ps-3">
                    {section.bullets.map((bullet) => (
                      <li key={bullet} className="text-secondary mb_12">
                        {bullet}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>

            <div
              className="mt-5 p-4"
              style={{
                borderRadius: 16,
                backgroundColor: "#f7f7f7",
              }}
            >
              <h6 className="mb_12">문의 안내</h6>
              <p className="text-secondary mb-1">
                정책 관련 문의는 고객센터를 통해 접수해 주세요.
              </p>
              <p className="text-secondary mb-1">이메일: {STORE_EMAIL}</p>
              <p className="text-secondary mb-0">전화: {STORE_PHONE_DISPLAY}</p>
            </div>
          </div>
        </div>
      </section>
      <Footer1 />
    </>
  );
}
