import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Events from "@/components/otherPages/Events";
import Link from "next/link";

export const metadata = {
  title: "이벤트 || Let’s Dive In",
  description: "이벤트 목록",
};

export default function EventsPage() {
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
              <h3 className="heading text-center">이벤트</h3>
              <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                <li>
                  <Link className="link" href={`/`}>
                    홈
                  </Link>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>이벤트</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <Events />
      <Footer1 />
    </>
  );
}
