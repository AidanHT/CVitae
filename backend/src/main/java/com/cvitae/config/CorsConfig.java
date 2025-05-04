package com.cvitae.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://localhost:5173}")
    private String allowedOrigins;

    @Value("${spring.profiles.active:development}")
    private String activeProfile;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("Configuring CORS for profile: {}", activeProfile);
        
        if ("development".equals(activeProfile)) {
            // Development: Allow all origins for easier local development
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")  // Use patterns for wildcards
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                    .allowedHeaders("*")
                    .exposedHeaders("X-Trace-ID", "Content-Disposition")
                    .allowCredentials(true)
                    .maxAge(3600);
                    
            log.info("✅ CORS configured for development - allowing all origins");
        } else {
            // Production: Use specific allowed origins
            String[] origins = allowedOrigins.split(",");
            registry.addMapping("/**")
                    .allowedOrigins(origins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders(
                        "Origin", 
                        "Content-Type", 
                        "Accept", 
                        "Authorization", 
                        "X-Requested-With",
                        "X-Trace-ID"
                    )
                    .exposedHeaders("X-Trace-ID", "Content-Disposition")
                    .allowCredentials(true)
                    .maxAge(86400); // 24 hours for production
                    
            log.info("✅ CORS configured for production with origins: {}", allowedOrigins);
        }
    }
}
