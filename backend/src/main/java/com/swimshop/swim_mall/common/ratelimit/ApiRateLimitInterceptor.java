package com.swimshop.swim_mall.common.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApiRateLimitInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final InMemoryRateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String ip = resolveClientIp(request);

        if ("POST".equals(method) && "/api/customer/join".equals(path)) {
            checkLimit("signup:ip:" + ip, 5, 60, "회원가입 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            return true;
        }

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            checkLimit("login:ip:" + ip, 10, 60, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            return true;
        }

        if ("POST".equals(method) && "/api/files/upload".equals(path)) {
            checkLimit("upload:ip:" + ip, 20, 60, "파일 업로드 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            Object subjectId = request.getSession(false) != null ? request.getSession(false).getAttribute("subjectId") : null;
            if (subjectId != null) {
                checkLimit("upload:account:" + subjectId, 10, 60, "계정당 파일 업로드 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
            }
            return true;
        }

        if ("POST".equals(method) && isAiPath(path)) {
            checkLimit("ai:ip:" + ip, 20, 60, "AI 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            Object subjectId = request.getSession(false) != null ? request.getSession(false).getAttribute("subjectId") : null;
            if (subjectId != null) {
                checkLimit("ai:account:" + subjectId, 5, 60, "계정당 AI 분당 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
            }
            return true;
        }

        return true;
    }

    private boolean isAiPath(String path) {
        return PATH_MATCHER.match("/api/admin/qna/*/draft-assist", path)
                || PATH_MATCHER.match("/api/partner/qna/*/draft-assist", path)
                || "/api/ai/review-analysis/partner".equals(path);
    }

    private void checkLimit(String key, int limit, long seconds, String message) {
        if (!rateLimiterService.isAllowed(key, limit, seconds)) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, message);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
