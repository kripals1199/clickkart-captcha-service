// src/main/java/com/clickkart/captcha/constant/LoggerNames.java
package com.clickkart.captcha.constant;

/**
 * Named loggers routed to their own dedicated appender/file by {@code logback-spring.xml}
 * (ACCESS -> access.log, CAPTCHA -> captcha.log, SECURITY -> security.log). Kept as constants so
 * every {@code @Slf4j(topic=...)} call site matches the logback configuration exactly.
 */
public final class LoggerNames {

    private LoggerNames() {}

    public static final String ACCESS = "ACCESS";
    public static final String CAPTCHA = "CAPTCHA";
    public static final String SECURITY = "SECURITY";
}
