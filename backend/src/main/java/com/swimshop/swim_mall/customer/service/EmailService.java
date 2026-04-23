package com.swimshop.swim_mall.customer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
     * 이메일 인증 메일 발송
     */
    public void sendCheckEmail(String toEmail, String emailCheckToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[Swim Mall] 이메일 인증을 완료해주세요");
        
        String checkUrl = baseUrl + "/api/customer/check?token=" + emailCheckToken;
        message.setText("안녕하세요.\n\n"
                + "Swim Mall 회원가입을 환영합니다.\n\n"
                + "아래 링크를 클릭하여 이메일 인증을 완료해주세요:\n"
                + checkUrl + "\n\n"
                + "이 링크는 24시간 동안 유효합니다.\n\n"
                + "감사합니다.");
        
        mailSender.send(message);
    }

    /**
     * 파트너 입점 승인 이메일 발송
     */
    public void sendPartnerApprovalEmail(String toEmail, String partnerName, String email, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("[Swim Mall] 파트너 입점이 승인되었습니다");
        
        String loginUrl = baseUrl.replace("/api", "") + "/login";
        message.setText("안녕하세요, " + partnerName + "님.\n\n"
                + "Swim Mall 파트너 입점 신청이 승인되었습니다.\n\n"
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
        message.setSubject("[Swim Mall] 파트너 입점 신청 결과 안내");
        
        message.setText("안녕하세요, " + partnerName + "님.\n\n"
                + "Swim Mall 파트너 입점 신청에 대해 검토한 결과, 입점이 불허되었습니다.\n\n"
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
        message.setSubject("[Swim Mall] 비밀번호가 초기화되었습니다");
        
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
