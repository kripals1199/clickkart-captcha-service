// src/main/java/com/clickkart/captcha/constant/ApiPaths.java
package com.clickkart.captcha.constant;

/** Single source of truth for this service's route strings - used in controller mappings. */
public final class ApiPaths {

    private ApiPaths() {}

    public static final String BASE = "/api/v1/captcha";

    /** Public, client-facing - the only endpoint the Gateway routes/allow-lists (see GatewaySecurityProperties). */
    public static final String CHALLENGE = BASE + "/challenge";
    /** Server-to-server only, called via Feign from Auth Service - never Gateway-routed. */
    public static final String VERIFY = BASE + "/verify";

    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_HEALTH_WILDCARD = "/actuator/health/**";
    public static final String ACTUATOR_PROMETHEUS = "/actuator/prometheus";
    public static final String SWAGGER_UI = "/swagger-ui.html";
    public static final String SWAGGER_UI_WILDCARD = "/swagger-ui/**";
    public static final String API_DOCS_WILDCARD = "/v3/api-docs/**";
}
