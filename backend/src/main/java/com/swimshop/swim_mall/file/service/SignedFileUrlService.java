package com.swimshop.swim_mall.file.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SignedFileUrlService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGN_CONTEXT = "file-download";

    private final String publicBaseUrl;
    private final String signingSecret;
    private final long ttlSeconds;

    public SignedFileUrlService(
            @Value("${app.public-base-url:${app.base-url:http://localhost:8080}}") String publicBaseUrl,
            @Value("${app.file.signed-url.signing-secret:}") String signingSecret,
            @Value("${app.file.signed-url.ttl-seconds:600}") long ttlSeconds) {
        this.publicBaseUrl = trimSlash(publicBaseUrl);
        this.signingSecret = signingSecret;
        this.ttlSeconds = ttlSeconds;
    }

    public String createSignedDownloadUrl(Long fileId) {
        if (fileId == null) {
            throw new IllegalArgumentException("fileId is required");
        }
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new IllegalStateException("File signed URL signing secret is missing (app.file.signed-url.signing-secret)");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalStateException("File signed URL ttl seconds must be positive");
        }
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String sig = createSignature(fileId, exp);
        return String.format("%s/api/files/%d/download?exp=%d&sig=%s", publicBaseUrl, fileId, exp, sig);
    }

    public boolean isValidSignedDownloadRequest(Long fileId, Long exp, String sig) {
        if (fileId == null || exp == null || sig == null || sig.isBlank()) {
            return false;
        }
        if (signingSecret == null || signingSecret.isBlank()) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        if (exp < now) {
            return false;
        }
        String expected = createSignature(fileId, exp);
        return constantTimeEquals(expected, sig);
    }

    private String createSignature(Long fileId, long exp) {
        try {
            String payload = SIGN_CONTEXT + ":" + fileId + ":" + exp;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create signed file url", e);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        if (left.length != right.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length; i++) {
            diff |= left[i] ^ right[i];
        }
        return diff == 0;
    }

    private static String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        return value.replaceAll("/+$", "");
    }
}
