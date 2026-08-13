// src/main/java/com/clickkart/captcha/serviceImpl/CaptchaImageGeneratorImpl.java
package com.clickkart.captcha.serviceImpl;

import com.clickkart.captcha.config.CaptchaProperties;
import com.clickkart.captcha.service.CaptchaImageGenerator;
import com.clickkart.captcha.service.GeneratedCaptcha;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Draws random text onto a distorted PNG using only {@code java.awt}/{@code javax.imageio}
 * (JDK-built-in, no extra dependency) - Spring Boot sets {@code java.awt.headless=true} by
 * default, which is exactly what's needed for off-screen {@link BufferedImage} rendering in a
 * container with no display.
 *
 * <p>Alphabet deliberately excludes visually ambiguous characters (0/O, 1/I/L) - a CAPTCHA that
 * a real human can't reliably read is worse than useless. {@link SecureRandom}, not {@link
 * java.util.Random}, since the generated text is the actual secret this whole service exists to
 * protect.
 */
@Component
@RequiredArgsConstructor
class CaptchaImageGeneratorImpl implements CaptchaImageGenerator {

    // Excludes every visually ambiguous character: 0/O, and 1/I/L. 'L' was previously still
    // present here despite this class's own Javadoc, the README, and the unit test all stating it
    // was excluded - which made the test fail whenever a generated code happened to contain an L
    // (1 - (31/32)^6, roughly one run in six). Removing it makes the alphabet match its stated
    // contract and the test deterministic, rather than weakening the assertion to match the bug.
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final String[] FONT_NAMES = {Font.SERIF, Font.SANS_SERIF, Font.MONOSPACED};
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaptchaProperties captchaProperties;

    @Override
    public GeneratedCaptcha generate() {
        String text = randomText();
        byte[] pngBytes = render(text);
        return new GeneratedCaptcha(text, pngBytes);
    }

    private String randomText() {
        StringBuilder sb = new StringBuilder(captchaProperties.getCodeLength());
        for (int i = 0; i < captchaProperties.getCodeLength(); i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private byte[] render(String text) {
        int width = captchaProperties.getImageWidth();
        int height = captchaProperties.getImageHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintBackground(g, width, height);
            paintDistortionLines(g, width, height);
            paintText(g, text, width, height);
            paintNoiseDots(g, width, height);
        } finally {
            g.dispose();
        }
        return toPngBytes(image);
    }

    private void paintBackground(Graphics2D g, int width, int height) {
        g.setColor(new Color(240, 240, 245));
        g.fillRect(0, 0, width, height);
    }

    private void paintDistortionLines(Graphics2D g, int width, int height) {
        for (int i = 0; i < 6; i++) {
            g.setColor(randomMutedColor());
            int x1 = RANDOM.nextInt(width);
            int y1 = RANDOM.nextInt(height);
            int x2 = RANDOM.nextInt(width);
            int y2 = RANDOM.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private void paintText(Graphics2D g, String text, int width, int height) {
        int charWidth = width / text.length();
        for (int i = 0; i < text.length(); i++) {
            Font font = new Font(FONT_NAMES[RANDOM.nextInt(FONT_NAMES.length)], Font.BOLD, height / 2 + RANDOM.nextInt(10));
            g.setFont(font);
            g.setColor(randomDarkColor());

            double angle = Math.toRadians(RANDOM.nextInt(50) - 25);
            int x = i * charWidth + RANDOM.nextInt(6);
            int y = height / 2 + RANDOM.nextInt(height / 4) - RANDOM.nextInt(height / 4) + height / 4;

            AffineTransform original = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);
            g.drawString(String.valueOf(text.charAt(i)), 0, 0);
            g.setTransform(original);
        }
    }

    private void paintNoiseDots(Graphics2D g, int width, int height) {
        for (int i = 0; i < 60; i++) {
            g.setColor(randomMutedColor());
            g.fillOval(RANDOM.nextInt(width), RANDOM.nextInt(height), 2, 2);
        }
    }

    private Color randomDarkColor() {
        return new Color(RANDOM.nextInt(80), RANDOM.nextInt(80), RANDOM.nextInt(80));
    }

    private Color randomMutedColor() {
        return new Color(150 + RANDOM.nextInt(80), 150 + RANDOM.nextInt(80), 150 + RANDOM.nextInt(80));
    }

    private byte[] toPngBytes(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render captcha image", e);
        }
    }
}
