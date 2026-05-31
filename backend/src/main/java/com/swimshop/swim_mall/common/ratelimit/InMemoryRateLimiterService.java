package com.swimshop.swim_mall.common.ratelimit;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class InMemoryRateLimiterService {

    private final Map<String, Deque<Long>> requestBuckets = new ConcurrentHashMap<>();

    public boolean isAllowed(String key, int limit, long windowSeconds) {
        if (key == null || key.isBlank() || limit <= 0 || windowSeconds <= 0) {
            return true;
        }

        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSeconds;
        Deque<Long> bucket = requestBuckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst() < windowStart) {
                bucket.pollFirst();
            }
            if (bucket.size() >= limit) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
    }
}
