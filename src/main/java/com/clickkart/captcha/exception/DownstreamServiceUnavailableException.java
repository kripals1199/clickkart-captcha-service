// src/main/java/com/clickkart/captcha/exception/DownstreamServiceUnavailableException.java
package com.clickkart.captcha.exception;

/**
 * Redis (challenge storage / rate-limit counters) could not be reached. Deliberately fails the
 * whole request rather than degrading silently - see {@code ChallengeRateLimitFilter} and {@code
 * CaptchaServiceImpl} for the call sites that throw this, and {@code CaptchaExceptionHandler}
 * for the resulting 503 response.
 */
public class DownstreamServiceUnavailableException extends RuntimeException {

    public DownstreamServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " is currently unavailable - please try again shortly", cause);
    }
}
