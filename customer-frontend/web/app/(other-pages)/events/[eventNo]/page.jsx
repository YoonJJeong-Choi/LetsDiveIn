import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import Topbar6 from "@/components/headers/Topbar6";
import EventDetail from "@/components/otherPages/EventDetail";
import Link from "next/link";

export const metadata = {
  title: "이벤트 상세 || Swim Mall",
  description: "이벤트 상세",
};

export default async function EventDetailPage({ params }) {
  const { eventNo } = await params;

  return (
    <>
      <Topbar6 bgColor="bg-main" />
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
                    Homepage
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
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>{eventNo}</li>
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
