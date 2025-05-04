package com.cvitae.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Validates critical configuration at application startup
 */
@Component
@Slf4j
public class StartupValidator implements CommandLineRunner {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${latex.service.url:http://localhost:8082}")
    private String latexServiceUrl;

    @Override
    public void run(String... args) throws Exception {
        log.info("🔍 Starting CVitae application validation...");

        validateGroqConfiguration();
        validateLatexServiceConfiguration();

        log.info("✅ CVitae application validation completed successfully");
    }

    private void validateGroqConfiguration() {
        log.info("Validating Groq AI configuration...");

        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            log.warn("⚠️  GROQ_API_KEY is not configured - AI features will use fallback responses");
            return;
        }

        if (groqApiKey.equals("your-groq-api-key") || groqApiKey.startsWith("${")) {
            log.warn("⚠️  GROQ_API_KEY appears to be placeholder value - AI features will use fallback responses");
            return;
        }

        if (groqApiKey.length() < 20) {
            log.warn("⚠️  GROQ_API_KEY appears to be too short - please verify it's correct");
            return;
        }

        log.info("✅ Groq AI configuration validated successfully");
    }

    private void validateLatexServiceConfiguration() {
        log.info("Validating LaTeX service configuration...");

        if (latexServiceUrl == null || latexServiceUrl.trim().isEmpty()) {
            log.error("❌ LATEX_SERVICE_URL is not configured - export features will fail");
            throw new RuntimeException("LaTeX service URL must be configured");
        }

        if (!latexServiceUrl.startsWith("http://") && !latexServiceUrl.startsWith("https://")) {
            log.error("❌ LATEX_SERVICE_URL must be a valid HTTP/HTTPS URL");
            throw new RuntimeException("Invalid LaTeX service URL format");
        }

        log.info("✅ LaTeX service configuration validated: {}", latexServiceUrl);
    }
}
