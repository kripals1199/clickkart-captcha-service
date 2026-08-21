// src/main/java/com/clickkart/captcha/config/CorsConfig.java
package com.clickkart.captcha.config;

import com.clickkart.captcha.constant.ApiPaths;
import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Defense in depth - this service is independently reachable, bypassing the Gateway's own CORS
 * config. Every other ClickKart service carries the equivalent in its SecurityConfig; this one
 * deliberately has no Spring Security (see the pom), so it lives here as an MVC concern instead.
 *
 * <p>Both layers setting CORS means a Gateway-proxied response would carry each header twice,
 * which browsers reject outright. The Gateway's DedupeResponseHeader default filter collapses
 * them - that filter is what makes this config safe to add, not an optimisation on top of it.
 *
 * <p>Scoped to /challenge rather than /**, deliberately. /verify is server-to-server only - Auth
 * Service's Feign client calls it and the Gateway does not route it - so it has no business
 * answering a browser preflight.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final CaptchaProperties captchaProperties;

    CorsConfig(CaptchaProperties captchaProperties) {
        this.captchaProperties = captchaProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(ApiPaths.CHALLENGE)
                .allowedOrigins(Arrays.stream(captchaProperties.getAllowedOrigins().split(","))
                        .map(String::trim)
                        .toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
