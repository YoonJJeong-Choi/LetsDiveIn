package com.swimshop.swim_mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 고객 프론트(Next.js 등)에서 쿠키(세션) 포함 요청을 허용하기 위한 CORS 설정.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 개발: localhost / 127.0.0.1·포트 혼용 시에도 동일하게 허용 (브라우저 Origin 문자열이 달라짐)
        // credentials 사용 시 와일드카드 Origin 불가 → 패턴 사용
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://43.200.179.17:3000",
                "http://letsdivein-admin.s3-website.ap-northeast-2.amazonaws.com",

                "https://wolo3.store",
                "https://www.wolo3.store",
                "https://admin.wolo3.store"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
