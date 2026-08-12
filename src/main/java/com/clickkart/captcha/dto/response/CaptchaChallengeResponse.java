// src/main/java/com/clickkart/captcha/dto/response/CaptchaChallengeResponse.java
package com.clickkart.captcha.dto.response;

/**
 * {@code imageBase64} is a PNG, not a data URI - the caller prefixes {@code data:image/png;base64,}
 * itself if rendering directly into an {@code <img src>}. Deliberately carries no hint of the
 * plaintext answer; that value never leaves this service unhashed except inside the rendered
 * image pixels themselves.
 */
public record CaptchaChallengeResponse(String challengeId, String imageBase64, int expiresInSeconds) {}
