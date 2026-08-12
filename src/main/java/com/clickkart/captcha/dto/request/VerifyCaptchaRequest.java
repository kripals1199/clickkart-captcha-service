// src/main/java/com/clickkart/captcha/dto/request/VerifyCaptchaRequest.java
package com.clickkart.captcha.dto.request;

import jakarta.validation.constraints.NotBlank;

/** {@code challengeId} from a prior {@code POST /api/v1/captcha/challenge}, plus the caller's typed answer. */
public record VerifyCaptchaRequest(@NotBlank String challengeId, @NotBlank String answer) {}
