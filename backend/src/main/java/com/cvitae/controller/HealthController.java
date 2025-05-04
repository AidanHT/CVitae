package com.cvitae.controller;

import com.cvitae.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check endpoints for monitoring and observability
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Health check requested");
        
        Map<String, Object> healthStatus = healthService.getHealthStatus();
        
        boolean isHealthy = (Boolean) healthStatus.get("healthy");
        
        return isHealthy ? 
            ResponseEntity.ok(healthStatus) : 
            ResponseEntity.status(503).body(healthStatus);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        log.debug("Detailed health check requested");
        
        Map<String, Object> detailedHealth = healthService.getDetailedHealthStatus();
        
        boolean isHealthy = (Boolean) detailedHealth.get("healthy");
        
        return isHealthy ? 
            ResponseEntity.ok(detailedHealth) : 
            ResponseEntity.status(503).body(detailedHealth);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        log.debug("Readiness check requested");
        
        Map<String, Object> readinessStatus = Map.of(
            "status", "ready",
            "timestamp", LocalDateTime.now(),
            "message", "CVitae backend is ready to serve requests"
        );
        
        return ResponseEntity.ok(readinessStatus);
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        log.debug("Liveness check requested");
        
        Map<String, Object> livenessStatus = Map.of(
            "status", "alive",
            "timestamp", LocalDateTime.now(),
            "message", "CVitae backend is alive"
        );
        
        return ResponseEntity.ok(livenessStatus);
    }
}
