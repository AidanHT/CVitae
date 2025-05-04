package com.cvitae.service.impl;

import com.cvitae.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of health checking service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthServiceImpl implements HealthService {

    private final DataSource dataSource;
    private final WebClient webClient;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${latex.service.url:http://localhost:8082}")
    private String latexServiceUrl;

    @Override
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean dbHealthy = checkDatabaseHealth();
            boolean groqConfigured = checkGroqConfiguration();
            
            boolean overallHealthy = dbHealthy && groqConfigured;
            
            health.put("status", overallHealthy ? "healthy" : "unhealthy");
            health.put("healthy", overallHealthy);
            health.put("timestamp", LocalDateTime.now());
            health.put("version", "1.0.0");
            health.put("service", "cvitae-backend");
            
        } catch (Exception e) {
            log.error("Error checking health status", e);
            health.put("status", "error");
            health.put("healthy", false);
            health.put("timestamp", LocalDateTime.now());
            health.put("error", e.getMessage());
        }
        
        return health;
    }

    @Override
    public Map<String, Object> getDetailedHealthStatus() {
        Map<String, Object> detailedHealth = new HashMap<>();
        
        try {
            // Check database health
            Map<String, Object> dbHealth = checkDatabaseHealthDetailed();
            
            // Check Groq AI configuration
            Map<String, Object> groqHealth = checkGroqHealthDetailed();
            
            // Check LaTeX service health
            Map<String, Object> latexHealth = checkLatexServiceHealthDetailed();
            
            // Overall health calculation
            boolean dbHealthy = (Boolean) dbHealth.get("healthy");
            boolean groqHealthy = (Boolean) groqHealth.get("healthy");
            boolean latexHealthy = (Boolean) latexHealth.get("healthy");
            
            boolean overallHealthy = dbHealthy && groqHealthy && latexHealthy;
            
            detailedHealth.put("status", overallHealthy ? "healthy" : "unhealthy");
            detailedHealth.put("healthy", overallHealthy);
            detailedHealth.put("timestamp", LocalDateTime.now());
            detailedHealth.put("version", "1.0.0");
            detailedHealth.put("service", "cvitae-backend");
            
            // Detailed component status
            Map<String, Object> components = new HashMap<>();
            components.put("database", dbHealth);
            components.put("groq_ai", groqHealth);
            components.put("latex_service", latexHealth);
            
            detailedHealth.put("components", components);
            
        } catch (Exception e) {
            log.error("Error checking detailed health status", e);
            detailedHealth.put("status", "error");
            detailedHealth.put("healthy", false);
            detailedHealth.put("timestamp", LocalDateTime.now());
            detailedHealth.put("error", e.getMessage());
        }
        
        return detailedHealth;
    }

    private boolean checkDatabaseHealth() {
        try {
            return checkDatabaseConnection();
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> checkDatabaseHealthDetailed() {
        Map<String, Object> dbHealth = new HashMap<>();
        
        try {
            boolean connected = checkDatabaseConnection();
            
            dbHealth.put("healthy", connected);
            dbHealth.put("status", connected ? "connected" : "disconnected");
            dbHealth.put("type", "postgresql");
            
        } catch (Exception e) {
            log.warn("Database detailed health check failed: {}", e.getMessage());
            dbHealth.put("healthy", false);
            dbHealth.put("status", "error");
            dbHealth.put("error", e.getMessage());
        }
        
        return dbHealth;
    }

    private boolean checkDatabaseConnection() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 second timeout
        }
    }

    private boolean checkGroqConfiguration() {
        return groqApiKey != null && 
               !groqApiKey.trim().isEmpty() && 
               !groqApiKey.equals("your-groq-api-key") &&
               !groqApiKey.startsWith("${");
    }

    private Map<String, Object> checkGroqHealthDetailed() {
        Map<String, Object> groqHealth = new HashMap<>();
        
        boolean configured = checkGroqConfiguration();
        
        groqHealth.put("healthy", configured);
        groqHealth.put("status", configured ? "configured" : "not_configured");
        groqHealth.put("api_key_present", groqApiKey != null && !groqApiKey.trim().isEmpty());
        groqHealth.put("api_key_valid_format", configured);
        
        return groqHealth;
    }

    private Map<String, Object> checkLatexServiceHealthDetailed() {
        Map<String, Object> latexHealth = new HashMap<>();
        
        try {
            // Try to reach LaTeX service health endpoint
            String response = webClient.get()
                .uri(latexServiceUrl + "/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(5))
                .block();
            
            boolean healthy = response != null && response.contains("healthy");
            
            latexHealth.put("healthy", healthy);
            latexHealth.put("status", healthy ? "available" : "unavailable");
            latexHealth.put("url", latexServiceUrl);
            
        } catch (Exception e) {
            log.warn("LaTeX service health check failed: {}", e.getMessage());
            latexHealth.put("healthy", false);
            latexHealth.put("status", "unreachable");
            latexHealth.put("error", e.getMessage());
            latexHealth.put("url", latexServiceUrl);
        }
        
        return latexHealth;
    }
}
