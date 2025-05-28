package com.cvitae.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple test controller for API verification
 */
@RestController
@RequestMapping("/api/test")
@Slf4j
public class TestController {

    // Add logging to admin system
    private void addAdminLog(String message) {
        AdminController.addLog("TestController: " + message);
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("🏓 Ping endpoint called");
        addAdminLog("Ping endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "pong");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "healthy");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody Map<String, Object> request) {
        log.info("🔊 Echo endpoint called with: {}", request);
        addAdminLog("Echo endpoint called with " + request.size() + " parameters");
        
        Map<String, Object> response = new HashMap<>();
        response.put("echo", request);
        response.put("timestamp", LocalDateTime.now());
        response.put("receivedKeys", request.keySet());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, Object>> simulateError() {
        log.error("🔥 Simulated error endpoint called");
        addAdminLog("❌ Simulated error endpoint called");
        
        throw new RuntimeException("This is a simulated error for testing purposes");
    }

    @PostMapping("/log")
    public ResponseEntity<Map<String, Object>> testLogging(@RequestBody Map<String, String> request) {
        String level = request.getOrDefault("level", "INFO");
        String message = request.getOrDefault("message", "Test message");
        
        switch (level.toUpperCase()) {
            case "DEBUG":
                log.debug("🧪 Test DEBUG: {}", message);
                break;
            case "INFO":
                log.info("🧪 Test INFO: {}", message);
                break;
            case "WARN":
                log.warn("🧪 Test WARN: {}", message);
                break;
            case "ERROR":
                log.error("🧪 Test ERROR: {}", message);
                break;
            default:
                log.info("🧪 Test DEFAULT: {}", message);
        }
        
        addAdminLog("Log test: " + level + " - " + message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("level", level);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTestStatus() {
        log.info("📊 Test status endpoint called");
        addAdminLog("Test status endpoint called");
        
        Map<String, Object> response = new HashMap<>();
        response.put("service", "CVitae Backend");
        response.put("version", "1.0.0");
        response.put("environment", System.getProperty("spring.profiles.active", "default"));
        response.put("timestamp", LocalDateTime.now());
        response.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        
        return ResponseEntity.ok(response);
    }
}
