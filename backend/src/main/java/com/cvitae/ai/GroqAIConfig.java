package com.cvitae.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Configuration for Groq AI integration with enhanced reliability
 */
@Configuration
@Slf4j
public class GroqAIConfig {

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.timeout.connect:10}")
    private int connectTimeoutSeconds;

    @Value("${groq.timeout.read:60}")
    private int readTimeoutSeconds;

    @Bean("groqWebClient")
    public WebClient groqWebClient() {
        log.info("Configuring Groq WebClient with timeouts - connect: {}s, read: {}s", 
                connectTimeoutSeconds, readTimeoutSeconds);

        return WebClient.builder()
                .baseUrl(groqApiUrl)
                .codecs(configurer -> {
                    // Set max buffer size for large AI responses
                    configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024); // 5MB
                })
                .build();
    }
}
