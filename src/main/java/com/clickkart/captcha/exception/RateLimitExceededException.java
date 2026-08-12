// src/main/java/com/clickkart/captcha/exception/RateLimitExceededException.java
package com.clickkart.captcha.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
