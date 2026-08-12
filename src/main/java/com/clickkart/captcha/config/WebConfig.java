// src/main/java/com/clickkart/captcha/config/WebConfig.java
package com.clickkart.captcha.config;

import com.clickkart.captcha.constant.ApiPaths;
import com.clickkart.captcha.filter.AccessLogFilter;
import com.clickkart.captcha.filter.ChallengeRateLimitFilter;
import com.clickkart.captcha.filter.CorrelationIdFilter;
import com.clickkart.captcha.filter.MdcCleanupFilter;
import com.clickkart.captcha.security.CorrelationIdGenerator;
import com.clickkart.captcha.web.ClientIpResolver;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableConfigurationProperties(CaptchaProperties.class)
public class WebConfig {

    private static final List<String> CORRELATION_ID_EXEMPT_PATHS = List.of(
            ApiPaths.ACTUATOR_HEALTH,
            ApiPaths.ACTUATOR_HEALTH_WILDCARD,
            ApiPaths.ACTUATOR_PROMETHEUS,
            ApiPaths.SWAGGER_UI,
            ApiPaths.SWAGGER_UI_WILDCARD,
            ApiPaths.API_DOCS_WILDCARD);

    private static final List<String> RATE_LIMITED_PATHS = List.of(ApiPaths.CHALLENGE);

    @Bean
    public FilterRegistrationBean<MdcCleanupFilter> mdcCleanupFilter() {
        FilterRegistrationBean<MdcCleanupFilter> registration = new FilterRegistrationBean<>(new MdcCleanupFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AccessLogFilter> accessLogFilter() {
        FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>(new AccessLogFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter(CorrelationIdGenerator correlationIdGenerator) {
        FilterRegistrationBean<CorrelationIdFilter> registration =
                new FilterRegistrationBean<>(new CorrelationIdFilter(correlationIdGenerator, CORRELATION_ID_EXEMPT_PATHS));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ChallengeRateLimitFilter> challengeRateLimitFilter(
            StringRedisTemplate redisTemplate,
            CaptchaProperties captchaProperties,
            HandlerExceptionResolver handlerExceptionResolver,
            ClientIpResolver clientIpResolver) {
        FilterRegistrationBean<ChallengeRateLimitFilter> registration = new FilterRegistrationBean<>(
                new ChallengeRateLimitFilter(
                        redisTemplate, captchaProperties, handlerExceptionResolver, clientIpResolver, RATE_LIMITED_PATHS));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
