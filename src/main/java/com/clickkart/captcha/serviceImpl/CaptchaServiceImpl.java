// src/main/java/com/clickkart/captcha/serviceImpl/CaptchaServiceImpl.java
package com.clickkart.captcha.serviceImpl;

import com.clickkart.captcha.config.CaptchaProperties;
import com.clickkart.captcha.constant.LoggerNames;
import com.clickkart.captcha.dto.response.CaptchaChallengeResponse;
import com.clickkart.captcha.exception.DownstreamServiceUnavailableException;
import com.clickkart.captcha.service.CaptchaImageGenerator;
import com.clickkart.captcha.service.CaptchaService;
import com.clickkart.captcha.service.GeneratedCaptcha;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis is the challenge store, not a durable database: a challenge is short-lived by design
 * (TTL, plus deleted outright on first verification attempt whether right or wrong), so there is
 * no "restart loses data" concern the way there would be for Auth/Notification/Audit's Postgres
 * state.
 *
 * <p>Only the SHA-256 hash of the answer is ever stored - never the plaintext - so a Redis
 * data-at-rest compromise doesn't hand an attacker a bank of solved answers.
 */
@Slf4j(topic = LoggerNames.CAPTCHA)
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final String REDIS_KEY_PREFIX = "captcha:challenge:";

    private final CaptchaImageGenerator captchaImageGenerator;
    private final StringRedisTemplate redisTemplate;
    private final CaptchaProperties captchaProperties;

    @Override
    public CaptchaChallengeResponse generateChallenge() {
        GeneratedCaptcha generated = captchaImageGenerator.generate();
        String challengeId = UUID.randomUUID().toString();
        String key = REDIS_KEY_PREFIX + challengeId;

        try {
            redisTemplate
                    .opsForValue()
                    .set(key, hash(generated.plainText()), Duration.ofSeconds(captchaProperties.getTtlSeconds()));
        } catch (DataAccessException e) {
            log.warn("CAPTCHA_CHALLENGE_STORE_FAILED challengeId={} cause={}", challengeId, e.toString());
            throw new DownstreamServiceUnavailableException("Captcha challenge storage (Redis)", e);
        }

        log.info("CAPTCHA_CHALLENGE_ISSUED challengeId={} ttlSeconds={}", challengeId, captchaProperties.getTtlSeconds());
        String imageBase64 = Base64.getEncoder().encodeToString(generated.pngBytes());
        return new CaptchaChallengeResponse(challengeId, imageBase64, captchaProperties.getTtlSeconds());
    }

    @Override
    public boolean verify(String challengeId, String answer) {
        String key = REDIS_KEY_PREFIX + challengeId;
        String storedHash;
        try {
            storedHash = redisTemplate.opsForValue().getAndDelete(key);
        } catch (DataAccessException e) {
        	e.printStackTrace();
            log.warn("CAPTCHA_VERIFY_UNAVAILABLE challengeId={} cause={}", challengeId, e.toString());
            throw new DownstreamServiceUnavailableException("Captcha verification (Redis)", e);
        }

        if (storedHash == null) {
            log.info("CAPTCHA_VERIFY_UNKNOWN_OR_EXPIRED challengeId={}", challengeId);
            return false;
        }

        boolean valid = storedHash.equals(hash(answer));
        log.info("CAPTCHA_VERIFY_RESULT challengeId={} valid={}", challengeId, valid);
        return valid;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalize(value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-guaranteed algorithm (JLS platform requirement) - unreachable in practice.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
