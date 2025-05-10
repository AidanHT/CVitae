package com.cvitae.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Basic security configuration - kept for compatibility
 * For enhanced security, see SecurityEnhancementConfig
 */
@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain basicFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring basic security settings...");
        
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOriginPatterns(java.util.List.of(
                    "http://localhost:3000",
                    "http://localhost:3001", 
                    "http://localhost:5173",
                    "http://localhost"
                ));
                corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                corsConfig.setAllowedHeaders(java.util.List.of("*"));
                corsConfig.setExposedHeaders(java.util.List.of("X-Trace-ID", "Content-Disposition"));
                corsConfig.setAllowCredentials(false);
                corsConfig.setMaxAge(3600L);
                return corsConfig;
            }))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/**").permitAll()  // Allow all requests for development
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions().disable());

        log.info("✅ Basic security configuration applied");
        return http.build();
    }
}
