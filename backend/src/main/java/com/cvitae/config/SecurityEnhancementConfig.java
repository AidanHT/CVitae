package com.cvitae.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Enhanced security configuration with proper headers and policies
 */
//@Configuration
//@EnableWebSecurity
@Slf4j
public class SecurityEnhancementConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring enhanced security settings...");
        
        http
            // Disable CSRF for API endpoints (using stateless authentication)
            .csrf(csrf -> csrf.disable())
            
            // Configure session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Allow health check endpoints
                .requestMatchers("/api/health/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Allow all API endpoints for now (add auth later)
                .requestMatchers("/api/**").permitAll()
                
                // Allow static resources
                .requestMatchers("/", "/static/**", "/public/**").permitAll()
                
                // Require authentication for everything else
                .anyRequest().permitAll() // For development - change to authenticated() in production
            )
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            );

        log.info("✅ Enhanced security configuration applied");
        return http.build();
    }
}
