"use client";
import React, { useRef, useState } from "react";
import emailjs from "@emailjs/browser";
import {
  STORE_ADDRESS,
  STORE_EMAIL,
  STORE_PHONE_DISPLAY,
  STORE_PHONE_TEL,
} from "@/data/storeContact";
export default function Contact2() {
  const formRef = useRef();
  const [success, setSuccess] = useState(true);
  const [showMessage, setShowMessage] = useState(false);

  const handleShowMessage = () => {
    setShowMessage(true);
    setTimeout(() => {
      setShowMessage(false);
    }, 2000);
  };

  const sendMail = (e) => {
    e.preventDefault();
    emailjs
      .sendForm("service_noj8796", "template_fs3xchn", formRef.current, {
        publicKey: "iG4SCmR-YtJagQ4gV",
      })
      .then((res) => {
        if (res.status === 200) {
          setSuccess(true);
          handleShowMessage();

          formRef.current.reset();
        } else {
          setSuccess(false);
          handleShowMessage();
        }
      })
      .catch((err) => {
        console.log(err);
      });
  };
  return (
    <section className="flat-spacing">
      <div className="container">
        <div className="contact-us-content">
          <div className="left">
            <h4>문의 보내기</h4>
            <p className="text-secondary-2">
              아래 양식을 작성해 주시면 확인 후 답변 드리겠습니다.
            </p>
            <div
              className={`tfSubscribeMsg  footer-sub-element ${
                showMessage ? "active" : ""
              }`}
            >
              {success ? (
                <p style={{ color: "rgb(52, 168, 83)" }}>
                  문의가 정상적으로 전송되었습니다.
                </p>
              ) : (
                <p style={{ color: "red" }}>전송에 실패했습니다. 잠시 후 다시 시도해 주세요.</p>
              )}
            </div>
            <form
              onSubmit={sendMail}
              ref={formRef}
              id="contactform"
              className="form-leave-comment"
            >
              <div className="wrap">
                <div className="cols">
                  <fieldset className="">
                    <input
                      className=""
                      type="text"
                      placeholder="이름*"
                      name="name"
                      id="name"
                      tabIndex={2}
                      defaultValue=""
                      aria-required="true"
                      required
                    />
                  </fieldset>
                  <fieldset className="">
                    <input
                      className=""
                      type="email"
                      placeholder="Your Email*"
                      name="email"
                      id="email"
                      tabIndex={2}
                      defaultValue=""
                      aria-required="true"
                      required
                    />
                  </fieldset>
                </div>
                <fieldset className="">
                  <textarea
                    name="message"
                    id="message"
                    rows={4}
                    placeholder="문의 내용*"
                    tabIndex={2}
                    aria-required="true"
                    required
                    defaultValue={""}
                  />
                </fieldset>
              </div>
              <div className="button-submit send-wrap">
                <button className="tf-btn btn-fill" type="submit">
                  <span className="text text-button">문의 보내기</span>
                </button>
              </div>
            </form>
          </div>
          <div className="right">
            <h4>연락처</h4>
            <div className="mb_20">
              <div className="text-title mb_8">전화</div>
              <p className="text-secondary">
                <a href={`tel:${STORE_PHONE_TEL}`}>{STORE_PHONE_DISPLAY}</a>
              </p>
            </div>
            <div className="mb_20">
              <div className="text-title mb_8">이메일</div>
              <p className="text-secondary">
                <a href={`mailto:${STORE_EMAIL}`}>{STORE_EMAIL}</a>
              </p>
            </div>
            <div className="mb_20">
              <div className="text-title mb_8">주소</div>
              <p className="text-secondary">{STORE_ADDRESS}</p>
            </div>
            <div>
              <div className="text-title mb_8">운영 시간</div>
              <p className="mb_4 open-time">
                <span className="text-secondary">평일:</span> 09:00 – 18:00
              </p>
              <p className="open-time">
                <span className="text-secondary">토·일·공휴일:</span> 휴무
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
