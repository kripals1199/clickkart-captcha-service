// src/main/java/com/clickkart/captcha/filter/MdcCleanupFilter.java
package com.clickkart.captcha.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs outermost (see {@code WebConfig}'s {@code FilterRegistrationBean} ordering). Guarantees
 * MDC never leaks a correlationId onto a later, unrelated request handled by the same pooled
 * Tomcat thread, regardless of which code path put one there or whether the request succeeded
 * or threw.
 */
public class MdcCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
