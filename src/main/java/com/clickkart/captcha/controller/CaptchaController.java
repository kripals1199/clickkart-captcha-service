// src/main/java/com/clickkart/captcha/controller/CaptchaController.java
package com.clickkart.captcha.controller;

import com.clickkart.captcha.constant.ApiPaths;
import com.clickkart.captcha.constant.MdcKeys;
import com.clickkart.captcha.dto.ApiResponse;
import com.clickkart.captcha.dto.request.VerifyCaptchaRequest;
import com.clickkart.captcha.dto.response.CaptchaChallengeResponse;
import com.clickkart.captcha.dto.response.CaptchaVerificationResponse;
import com.clickkart.captcha.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Captcha", description = "Self-hosted image CAPTCHA challenge generation and one-time-use verification")
@RestController
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 200 OK, {@code data}: {"challengeId":"...","imageBase64":"iVBORw0KG...","expiresInSeconds":120}.
     * Public - reached directly by a browser through the Gateway, no request body needed.
     */
    @Operation(summary = "Generate a new captcha challenge", description = "Public.")
    @PostMapping(ApiPaths.CHALLENGE)
    public ResponseEntity<ApiResponse<CaptchaChallengeResponse>> challenge(HttpServletRequest httpRequest) {
        CaptchaChallengeResponse response = captchaService.generateChallenge();
        return envelope(HttpStatus.OK.value(), response, httpRequest);
    }

    /**
     * 200 OK, {@code data}: {"valid":true|false} - never a 401/403 for a wrong answer, since a
     * failed captcha isn't an authorization failure, just a negative result the caller (Auth
     * Service, server-to-server) acts on itself. Server-to-server only - not Gateway-routed.
     */
    @Operation(summary = "Verify a challenge answer", description = "Internal, server-to-server only - not reachable through the Gateway.")
    @PostMapping(ApiPaths.VERIFY)
    public ResponseEntity<ApiResponse<CaptchaVerificationResponse>> verify(
            @Valid @RequestBody VerifyCaptchaRequest request, HttpServletRequest httpRequest) {
        boolean valid = captchaService.verify(request.challengeId(), request.answer());
        return envelope(HttpStatus.OK.value(), new CaptchaVerificationResponse(valid), httpRequest);
    }

    private <T> ResponseEntity<ApiResponse<T>> envelope(int status, T data, HttpServletRequest request) {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        ApiResponse<T> body = ApiResponse.success(status, data, request.getRequestURI(), correlationId);
        return ResponseEntity.status(status).body(body);
    }
}
