// src/main/java/com/clickkart/captcha/config/OpenApiConfig.java
package com.clickkart.captcha.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Backs the Swagger UI at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI captchaServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ClickKart Captcha Service")
                        .version("1.0.0")
                        .description("Self-hosted image CAPTCHA challenge generation and one-time-use verification - no third-party provider."));
    }
}
