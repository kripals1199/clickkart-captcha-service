// src/main/java/com/clickkart/captcha/web/ClientIpResolver.java
package com.clickkart.captcha.web;

import com.clickkart.captcha.config.CaptchaProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

/**
 * Feeds {@code ChallengeRateLimitFilter}'s per-IP throttle on {@code POST /challenge} - unlike
 * Notification/Audit Log Service's identically-named class (a simple, unconditional-trust
 * static utility), this one gates an actual security decision, so it follows
 * clickkart-auth-service's hardened pattern instead: only honors {@code X-Forwarded-For} when
 * the immediate socket address matches a configured trusted-proxy CIDR ({@code
 * captcha.trusted-proxy-cidrs}), otherwise a client reaching this service directly could set the
 * header to whatever it wants and defeat the rate limit entirely.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final CaptchaProperties captchaProperties;

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }
        return Optional.ofNullable(request.getHeader(X_FORWARDED_FOR_HEADER))
                .filter(header -> !header.isBlank())
                .map(header -> header.split(",")[0].trim())
                .orElse(remoteAddr);
    }

    private boolean isTrustedProxy(String remoteAddr) {
        List<String> trustedProxyCidrs = captchaProperties.getTrustedProxyCidrs();
        if (trustedProxyCidrs.isEmpty()) {
            return false;
        }
        for (String cidr : trustedProxyCidrs) {
            if (new IpAddressMatcher(cidr).matches(remoteAddr)) {
                return true;
            }
        }
        return false;
    }
}
