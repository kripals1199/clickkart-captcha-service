// src/test/java/com/clickkart/captcha/serviceImpl/CaptchaServiceImplTest.java
package com.clickkart.captcha.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clickkart.captcha.config.CaptchaProperties;
import com.clickkart.captcha.dto.response.CaptchaChallengeResponse;
import com.clickkart.captcha.exception.DownstreamServiceUnavailableException;
import com.clickkart.captcha.service.CaptchaImageGenerator;
import com.clickkart.captcha.service.GeneratedCaptcha;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    private static final byte[] FAKE_PNG_BYTES = {1, 2, 3, 4};

    @Mock
    private CaptchaImageGenerator captchaImageGenerator;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CaptchaProperties captchaProperties;
    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        captchaProperties = new CaptchaProperties();
        captchaProperties.setCodeLength(6);
        captchaProperties.setTtlSeconds(120);
        captchaService = new CaptchaServiceImpl(captchaImageGenerator, redisTemplate, captchaProperties);
    }

    @Test
    void generateChallengeStoresHashedAnswerWithConfiguredTtlAndNeverLeaksPlaintext() {
        when(captchaImageGenerator.generate()).thenReturn(new GeneratedCaptcha("ABC123", FAKE_PNG_BYTES));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CaptchaChallengeResponse response = captchaService.generateChallenge();

        assertThat(response.challengeId()).isNotBlank();
        assertThat(response.expiresInSeconds()).isEqualTo(120);
        assertThat(response.imageBase64()).isEqualTo(java.util.Base64.getEncoder().encodeToString(FAKE_PNG_BYTES));
        // The response must not, in any field, contain the plaintext answer.
        assertThat(response.imageBase64()).doesNotContain("ABC123");
        assertThat(response.challengeId()).doesNotContain("ABC123");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofSeconds(120)));
        assertThat(keyCaptor.getValue()).endsWith(response.challengeId());
        assertThat(valueCaptor.getValue()).isEqualTo(sha256("ABC123"));
    }

    @Test
    void verifyWithCorrectAnswerSucceedsCaseInsensitivelyAndConsumesTheChallenge() {
        when(captchaImageGenerator.generate()).thenReturn(new GeneratedCaptcha("ABC123", FAKE_PNG_BYTES));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        CaptchaChallengeResponse response = captchaService.generateChallenge();

        when(valueOperations.getAndDelete(anyString())).thenReturn(sha256("ABC123"));

        boolean valid = captchaService.verify(response.challengeId(), "abc123");

        assertThat(valid).isTrue();
        verify(valueOperations).getAndDelete("captcha:challenge:" + response.challengeId());
    }

    @Test
    void verifyWithWrongAnswerFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(sha256("ABC123"));

        boolean valid = captchaService.verify("some-challenge-id", "WRONGWRONG");

        assertThat(valid).isFalse();
    }

    @Test
    void verifyWithUnknownOrExpiredChallengeIdFailsWithoutError() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenReturn(null);

        boolean valid = captchaService.verify("never-issued", "ANYTHING");

        assertThat(valid).isFalse();
    }

    @Test
    void generateChallengeFailsClosedWhenRedisIsUnavailable() {
        when(captchaImageGenerator.generate()).thenReturn(new GeneratedCaptcha("ABC123", FAKE_PNG_BYTES));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("redis down"))
                .when(valueOperations)
                .set(anyString(), anyString(), any(Duration.class));

        assertThatThrownBy(() -> captchaService.generateChallenge())
                .isInstanceOf(DownstreamServiceUnavailableException.class);
    }

    @Test
    void verifyFailsClosedWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(anyString())).thenThrow(new DataAccessResourceFailureException("redis down"));

        assertThatThrownBy(() -> captchaService.verify("some-id", "ANYTHING"))
                .isInstanceOf(DownstreamServiceUnavailableException.class);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.trim().toUpperCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
