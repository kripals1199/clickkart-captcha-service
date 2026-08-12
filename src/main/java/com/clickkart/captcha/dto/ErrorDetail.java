// src/main/java/com/clickkart/captcha/dto/ErrorDetail.java
package com.clickkart.captcha.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * The {@code error} payload inside {@link ApiResponse} on every failure response. {@code code}
 * is the stable, machine-readable identifier a caller branches on; {@code fieldErrors} only for
 * bean-validation failures.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(String code, Map<String, String> fieldErrors) {

    public static ErrorDetail of(String code) {
        return new ErrorDetail(code, null);
    }

    public static ErrorDetail withFieldErrors(String code, Map<String, String> fieldErrors) {
        return new ErrorDetail(code, fieldErrors);
    }
}
