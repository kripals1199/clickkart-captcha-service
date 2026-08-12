// src/test/java/com/clickkart/captcha/serviceImpl/CaptchaImageGeneratorImplTest.java
package com.clickkart.captcha.serviceImpl;

import static org.assertj.core.api.Assertions.assertThat;

import com.clickkart.captcha.config.CaptchaProperties;
import com.clickkart.captcha.service.GeneratedCaptcha;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CaptchaImageGeneratorImplTest {

    private static final String SAFE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Test
    void generatesTextOfConfiguredLengthFromSafeAlphabetAndAValidPngImage() throws Exception {
        CaptchaProperties properties = new CaptchaProperties();
        properties.setCodeLength(6);
        properties.setImageWidth(200);
        properties.setImageHeight(70);
        CaptchaImageGeneratorImpl generator = new CaptchaImageGeneratorImpl(properties);

        GeneratedCaptcha generated = generator.generate();

        assertThat(generated.plainText()).hasSize(6);
        assertThat(generated.plainText()).matches("^[" + SAFE_ALPHABET + "]+$");
        // Ambiguous characters must never appear, whatever the random draw.
        assertThat(generated.plainText()).doesNotContain("0", "O", "1", "I", "L");

        assertThat(generated.pngBytes()).isNotEmpty();
        java.awt.image.BufferedImage decoded =
                ImageIO.read(new java.io.ByteArrayInputStream(generated.pngBytes()));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(200);
        assertThat(decoded.getHeight()).isEqualTo(70);
    }

    @Test
    void successiveChallengesProduceDifferentText() {
        CaptchaProperties properties = new CaptchaProperties();
        CaptchaImageGeneratorImpl generator = new CaptchaImageGeneratorImpl(properties);

        String first = generator.generate().plainText();
        String second = generator.generate().plainText();

        // Statistically astronomically unlikely to collide (31^6 keyspace) - a hard equality
        // assertion here would be flaky in principle but not in any practical sense.
        assertThat(first).isNotEqualTo(second);
    }
}
