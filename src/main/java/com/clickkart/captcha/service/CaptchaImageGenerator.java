// src/main/java/com/clickkart/captcha/service/CaptchaImageGenerator.java
package com.clickkart.captcha.service;

public interface CaptchaImageGenerator {

    /** Random text of the configured length from a safe alphabet, rendered as a distorted PNG. */
    GeneratedCaptcha generate();
}
