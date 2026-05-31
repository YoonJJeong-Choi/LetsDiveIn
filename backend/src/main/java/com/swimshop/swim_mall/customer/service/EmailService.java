package com.swimshop.swim_mall.customer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 고객 회원가입 이메일 인증 — 제목·본문은 여기만 수정하면 됩니다.
     * (토큰 유효 시간 문구는 {@code CustomerService.TOKEN_EXPIRY_HOURS} 와 맞출 것)
     */
    private static final String SIGNUP_EMAIL_VERIFY_SUBJECT =
            "[Let\u2019s Dive In] 이메일 인증을 완료해 주세요";

    private String buildSignupEmailVerificationBody(String checkUrl) {
        return String.join("\n\n",
                "안녕하세요.",
                "Let\u2019s Dive In 회원가입을 환영합니다.",
                "아래 링크를 눌러 이메일 인증을 완료해 주세요.",
                checkUrl,
                "이 링크는 발송 시점부터 24시간 동안만 유효합니다.",
                "본인이 가입을 신청한 적이 없다면 이 메일은 무시하셔도 됩니다.",
                "감사합니다.",
                "Let\u2019s Dive In 드림");
    }

    /** HTML 본문: 메일 클라이언트에서 클릭 가능한 버튼형 링크 */
    private String buildSignupEmailVerificationHtml(String checkUrl) {
        String visibleUrl = HtmlUtils.htmlEscape(checkUrl);
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;"
                + "line-height:1.6;color:#222;max-width:560px;margin:0;padding:24px;\">"
                + "<p>안녕하세요.</p>"
                + "<p><strong>Let\u2019s Dive In</strong> 회원가입을 환영합니다.</p>"
                + "<p style=\"margin:28px 0;\">"
                + "<a href=\"" + checkUrl + "\" style=\"display:inline-block;padding:14px 28px;"
                + "background:#111827;color:#ffffff !important;text-decoration:none;border-radius:8px;"
                + "font-weight:600;\">이메일 인증 완료하기</a>"
                + "</p>"
                + "<p style=\"font-size:12px;color:#666;word-break:break-all;border:1px solid #eee;"
                + "padding:12px;border-radius:6px;background:#fafafa;\">" + visibleUrl + "</p>"
                + "<p style=\"font-size:13px;color:#555;\">이 링크는 발송 시점부터 <strong>24시간</strong> 동안 유효합니다.</p>"
                + "<p style=\"margin-top:28px;\">감사합니다.<br><strong>Let\u2019s Dive In</strong> 드림</p>"
                + "</body></html>";
    }

    /**
     * 이메일 인증 메일 발송 (회원가입 · 관리자 재발송 동일 본문).
     * multipart(alternative): 텍스트 + HTML(하이퍼링크 버튼)
     */
    public void sendCheckEmail(String toEmail, String emailCheckToken) {
        String checkUrl = baseUrl.replaceAll("/$", "") + "/api/customer/check?token=" + emailCheckToken;
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(SIGNUP_EMAIL_VERIFY_SUBJECT);
            helper.setText(buildSignupEmailVerificationBody(checkUrl), buildSignupEmailVerificationHtml(checkUrl));
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new IllegalStateException("이메일 인증 메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 파트너 입점 승인 이메일 발송
     */
    public void sendPartnerApprovalEmail(String toEmail, String partnerName, String email, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[Let\u2019s Dive In] 파트너 입점이 승인되었습니다");
        
        String loginUrl = baseUrl.replace("/api", "") + "/login";
        message.setText("안녕하세요, " + partnerName + "님.\n\n"
                + "Let\u2019s Dive In 파트너 입점 신청이 승인되었습니다.\n\n"
                + "아래 계정 정보로 로그인하실 수 있습니다:\n"
                + "이메일: " + email + "\n"
                + "비밀번호: " + password + "\n\n"
                + "로그인 페이지: " + loginUrl + "\n\n"
                + "로그인 후 상품 등록 및 주문 관리를 시작하실 수 있습니다.\n\n"
                + "감사합니다.");
        
        mailSender.send(message);
    }

    /**
     * 파트너 입점 거절 이메일 발송
     */
    public void sendPartnerRejectionEmail(String toEmail, String partnerName, String rejectionReason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[Let\u2019s Dive In] 파트너 입점 신청 결과 안내");
        
        message.setText("안녕하세요, " + partnerName + "님.\n\n"
                + "Let\u2019s Dive In 파트너 입점 신청에 대해 검토한 결과, 입점이 불허되었습니다.\n\n"
                + "거절 사유:\n"
                + rejectionReason + "\n\n"
                + "추가 문의사항이 있으시면 고객센터로 연락 부탁드립니다.\n\n"
                + "감사합니다.");
        
        mailSender.send(message);
    }
    
    /**
     * 고객 비밀번호 초기화 이메일 발송
     */
    public void sendPasswordResetEmail(String toEmail, String customerName, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[Let\u2019s Dive In] 비밀번호가 초기화되었습니다");
        
        message.setText("안녕하세요, " + customerName + "님.\n\n"
                + "관리자에 의해 비밀번호가 초기화되었습니다.\n\n"
                + "임시 비밀번호: " + tempPassword + "\n\n"
                + "로그인 후 반드시 비밀번호를 변경해주세요.\n\n"
                + "감사합니다.");
        
        mailSender.send(message);
    }
    
    /**
     * 고객 이메일 인증 재발송
     */
    public void resendEmailVerification(String toEmail, String emailCheckToken) {
        sendCheckEmail(toEmail, emailCheckToken);
    }
}
