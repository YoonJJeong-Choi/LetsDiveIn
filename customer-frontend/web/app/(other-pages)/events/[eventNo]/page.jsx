import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import EventDetail from "@/components/otherPages/EventDetail";
import Link from "next/link";

export const metadata = {
  title: "이벤트 상세 || Let’s Dive In",
  description: "이벤트 상세",
};

export default async function EventDetailPage({ params }) {
  const { eventNo } = await params;

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
              <h3 className="heading text-center">이벤트 상세</h3>
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
                  <Link className="link" href={`/events`}>
                    이벤트
                  </Link>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <EventDetail eventNo={eventNo} />
      <Footer1 />
    </>
  );
}
