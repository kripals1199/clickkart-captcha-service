// src/main/java/com/clickkart/captcha/config/CaptchaProperties.java
package com.clickkart.captcha.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    private int codeLength = 6;
    private int ttlSeconds = 120;
    private int imageWidth = 200;
    private int imageHeight = 70;

    /** Per-IP fixed-window rate limit on POST /challenge only - image generation + Redis writes are not free. */
    private int challengeRateLimitMaxRequests = 20;
    private int challengeRateLimitWindowSeconds = 60;

    /** Comma-separated CIDRs/IPs of proxies allowed to set X-Forwarded-For (e.g. the Gateway's own
     * address) - unset/empty means "trust nothing but the immediate socket address". See
     * clickkart-auth-service's identically-named property for the full rationale. */
    private List<String> trustedProxyCidrs = List.of();
}
