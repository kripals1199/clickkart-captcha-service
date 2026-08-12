// src/main/java/com/clickkart/captcha/service/GeneratedCaptcha.java
package com.clickkart.captcha.service;

/**
 * Internal only - {@code plainText} must never cross the {@code /challenge} HTTP response
 * boundary (see {@link com.clickkart.captcha.dto.response.CaptchaChallengeResponse}, which
 * deliberately omits it). Kept as its own type, not inlined into {@code CaptchaImageGenerator},
 * specifically so unit tests can assert generation + hashing + verification end-to-end without
 * needing OCR against the rendered image.
 */
public record GeneratedCaptcha(String plainText, byte[] pngBytes) {}
