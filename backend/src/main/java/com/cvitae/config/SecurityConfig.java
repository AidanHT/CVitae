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
