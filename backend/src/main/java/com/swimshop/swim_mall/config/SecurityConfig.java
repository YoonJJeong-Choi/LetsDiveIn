package com.swimshop.swim_mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity //웹 보안
public class SecurityConfig {

    @Bean //비밀번호 암호화
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean //보안필터
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {}) // CorsConfig 빈 사용 (고객 프론트 localhost:3000 허용)
            .csrf(csrf -> csrf.disable()) // 세션 기반이므로 CSRF는 필요시 활성화
            //요청 인증 설정 
            .authorizeHttpRequests(auth -> auth
                //인증없이 접근 가능
                .requestMatchers("/api/customer/join", "/api/customer/check").permitAll()
                .requestMatchers("/api/admin/bootstrap").permitAll()
                //인증 필요
                .anyRequest().permitAll() // 나머지도 허용 (세션 체크는 서비스 레벨에서 처리)
            )
            // 세션 생성 정책 (없으면 생성, 있으면 기존 사용)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) 
            );

        return http.build();
    }
}
