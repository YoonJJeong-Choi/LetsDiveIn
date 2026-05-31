import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import MyAccountAuthGate from "@/components/my-account/MyAccountAuthGate";
import QnaPage from "@/components/otherPages/QnaPage";
import Link from "next/link";

export const metadata = {
  title: "1:1 QnA || Let's Dive In",
  description: "고객 1:1 QnA",
};

export default function QnaRoutePage() {
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
              <h3 className="heading text-center">고객센터</h3>
              <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                <li>
                  <Link className="link" href="/">
                    홈
                  </Link>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>1:1 QnA</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <MyAccountAuthGate>
        <QnaPage />
      </MyAccountAuthGate>
      <Footer1 />
    </>
  );
}
