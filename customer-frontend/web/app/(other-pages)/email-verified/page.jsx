import Footer1 from "@/components/footers/Footer1";
import Header1 from "@/components/headers/Header1";
import EmailVerified from "@/components/otherPages/EmailVerified";
import Link from "next/link";

export const metadata = {
  title: "이메일 인증 결과 || Let's Dive In",
  description: "회원가입 이메일 인증 결과",
};

export default function EmailVerifiedPage() {
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
              <h3 className="heading text-center">이메일 인증</h3>
              <ul className="breadcrumbs d-flex align-items-center justify-content-center">
                <li>
                  <Link className="link" href="/">
                    홈
                  </Link>
                </li>
                <li>
                  <i className="icon-arrRight" />
                </li>
                <li>인증 결과</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <EmailVerified />
      <Footer1 />
    </>
  );
}
