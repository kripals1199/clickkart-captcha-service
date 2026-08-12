// src/main/java/com/clickkart/captcha/filter/ChallengeRateLimitFilter.java
package com.clickkart.captcha.filter;

import com.clickkart.captcha.config.CaptchaProperties;
import com.clickkart.captcha.constant.LoggerNames;
import com.clickkart.captcha.exception.DownstreamServiceUnavailableException;
import com.clickkart.captcha.exception.RateLimitExceededException;
import com.clickkart.captcha.web.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Fixed-window, per-IP rate limit on {@code POST /challenge} only - own copy of
 * clickkart-auth-service's {@code RateLimitFilter} pattern (Rule 4: no shared library), scoped
 * down since this service only has the one abuse-prone public endpoint. Without this, an
 * attacker could mass-generate challenges (CPU cost of image generation, Redis storage) to
 * enumerate the answer keyspace offline or simply flood storage - the CAPTCHA's one-time-use
 * design only protects a single already-issued challenge, not the generation endpoint itself.
 *
 * <p>Fails closed: if Redis is unreachable the request is rejected with 503, matching {@code
 * CaptchaServiceImpl.generateChallenge}'s own behavior on the exact same outage (challenge
 * storage needs the same Redis instance), so this filter doesn't change the effective
 * availability story - it just fails a beat earlier.
 */
@Slf4j(topic = LoggerNames.SECURITY)
@RequiredArgsConstructor
public class ChallengeRateLimitFilter extends OncePerRequestFilter {

    private static final String REDIS_KEY_PREFIX = "captcha:ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final CaptchaProperties captchaProperties;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final ClientIpResolver clientIpResolver;
    private final List<String> limitedPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (!isLimitedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = clientIpResolver.resolve(request);
        String key = REDIS_KEY_PREFIX + path + ":" + clientIp;
        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(captchaProperties.getChallengeRateLimitWindowSeconds()));
            }
        } catch (DataAccessException e) {
            log.warn("CHALLENGE_RATE_LIMIT_CHECK_UNAVAILABLE path={} ipAddress={} cause={}", path, clientIp, e.toString());
            handlerExceptionResolver.resolveException(
                    request, response, null, new DownstreamServiceUnavailableException("Rate limiting (Redis)", e));
            return;
        }

        if (count != null && count > captchaProperties.getChallengeRateLimitMaxRequests()) {
            log.warn("CHALLENGE_RATE_LIMIT_EXCEEDED path={} ipAddress={} count={}", path, clientIp, count);
            handlerExceptionResolver.resolveException(
                    request, response, null, new RateLimitExceededException("Too many requests - please try again later"));
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isLimitedPath(String path) {
        for (String pattern : limitedPaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
