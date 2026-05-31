package com.swimshop.swim_mall.common.ratelimit;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;

/**
 * 로그인 실패 누적 잠금 (이메일 기준, 인메모리).
 * 10분 내 5회 {@link ErrorCode#INVALID_CREDENTIALS} 실패 시 10분 잠금.
 */
@Service
public class LoginLockoutService {

    private static final int MAX_FAILURES = 5;
    private static final long FAILURE_WINDOW_SECONDS = 600;
    private static final long LOCKOUT_SECONDS = 600;

    private final Map<String, Deque<Long>> failureBuckets = new ConcurrentHashMap<>();
    private final Map<String, Long> lockedUntilEpoch = new ConcurrentHashMap<>();

    public void assertNotLocked(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        Long lockedUntil = lockedUntilEpoch.get(normalizedEmail);
        if (lockedUntil != null) {
            if (now < lockedUntil) {
                throw new BusinessException(
                        ErrorCode.RATE_LIMIT_EXCEEDED,
                        "로그인 시도가 너무 많습니다. 10분 후 다시 시도해 주세요.");
            }
            lockedUntilEpoch.remove(normalizedEmail);
        }
    }

    public void recordFailure(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        long windowStart = now - FAILURE_WINDOW_SECONDS;
        Deque<Long> bucket = failureBuckets.computeIfAbsent(normalizedEmail, ignored -> new ArrayDeque<>());

        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst() < windowStart) {
                bucket.pollFirst();
            }
            bucket.addLast(now);
            if (bucket.size() >= MAX_FAILURES) {
                lockedUntilEpoch.put(normalizedEmail, now + LOCKOUT_SECONDS);
            }
        }
    }

    public void clearFailures(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return;
        }
        failureBuckets.remove(normalizedEmail);
        lockedUntilEpoch.remove(normalizedEmail);
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}
