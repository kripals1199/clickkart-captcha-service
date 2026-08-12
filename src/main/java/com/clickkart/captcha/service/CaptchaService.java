// src/main/java/com/clickkart/captcha/service/CaptchaService.java
package com.clickkart.captcha.service;

import com.clickkart.captcha.dto.response.CaptchaChallengeResponse;

public interface CaptchaService {

    CaptchaChallengeResponse generateChallenge();

    /**
     * One-time-use: the stored challenge is consumed (deleted) on the first verification
     * attempt regardless of outcome, so a wrong guess burns the challenge exactly like a right
     * one - a caller gets exactly one attempt per generated image.
     */
    boolean verify(String challengeId, String answer);
}
