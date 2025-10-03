package com.cvitae.controller;

import com.cvitae.service.ExportService;
import com.cvitae.service.HealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final HealthService healthService;
    private final ExportService exportService;
    
    // Simple in-memory log storage for demo purposes
    private static final List<String> recentLogs = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LOGS = 100;
    
    public static void addLog(String message) {
        String timestampedMessage = LocalDateTime.now() + " - " + message;
        recentLogs.add(0, timestampedMessage); // Add to beginning
        if (recentLogs.size() > MAX_LOGS) {
            recentLogs.remove(recentLogs.size() - 1); // Remove oldest
        }
    }

    @GetMapping("/ui")
    public ResponseEntity<String> getAdminUI() {
        try {
            // First try to load the full admin UI
            Resource resource = new ClassPathResource("static/admin-ui.html");
            if (resource.exists() && resource.isReadable()) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                log.info("✅ Successfully loaded admin-ui.html");
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(content);
            } else {
                log.warn("⚠️ admin-ui.html not found or not readable, using fallback");
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(generateFallbackUI());
            }
        } catch (Exception e) {
            log.error("❌ Failed to load admin UI: {}", e.getMessage(), e);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(generateFallbackUI());
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        log.info("🔍 Admin: Getting system status");
        addLog("Admin: System status requested");
        
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Overall health
            Map<String, Object> health = healthService.getHealthStatus();
            status.put("health", health);
        } catch (Exception e) {
            log.warn("Failed to get health status: {}", e.getMessage());
            Map<String, Object> health = new HashMap<>();
            health.put("healthy", false);
            health.put("error", e.getMessage());
            status.put("health", health);
        }
        
        try {
            // Export service status
            Map<String, Object> exportHealth = exportService.getExportHealthStatus();
            status.put("exportService", exportHealth);
        } catch (Exception e) {
            log.warn("Failed to get export service status: {}", e.getMessage());
            Map<String, Object> exportHealth = new HashMap<>();
            exportHealth.put("healthy", false);
            exportHealth.put("error", e.getMessage());
            status.put("exportService", exportHealth);
        }
        
        // System info
        status.put("timestamp", LocalDateTime.now());
        status.put("javaVersion", System.getProperty("java.version"));
        status.put("activeProfiles", System.getProperty("spring.profiles.active", "default"));
        
        // Memory info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        memory.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        memory.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        status.put("memory", memory);
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getRecentLogs() {
        log.info("🔍 Admin: Getting recent logs");
        
        Map<String, Object> response = new HashMap<>();
        response.put("logs", new ArrayList<>(recentLogs));
        response.put("count", recentLogs.size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logs/clear")
    public ResponseEntity<Map<String, Object>> clearLogs() {
        log.info("🔍 Admin: Clearing logs");
        addLog("Admin: Logs cleared by user");
        
        recentLogs.clear();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logs cleared successfully");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test/latex")
    public ResponseEntity<Map<String, Object>> testLatexGeneration(@RequestBody Map<String, Object> request) {
        log.info("🔍 Admin: Testing LaTeX generation");
        addLog("Admin: LaTeX generation test started");
        
        try {
            String testLatex = """
                \\documentclass[letterpaper,11pt]{article}
                \\usepackage{latexsym}
                \\usepackage[empty]{fullpage}
                \\begin{document}
                \\section{Test Section}
                This is a test LaTeX document for admin testing.
                \\end{document}
                """;
                
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("latexCode", testLatex);
            response.put("message", "Test LaTeX generated successfully");
            response.put("timestamp", LocalDateTime.now());
            
            addLog("Admin: LaTeX generation test completed successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("🔥 Admin: LaTeX generation test failed: {}", e.getMessage(), e);
            addLog("Admin: LaTeX generation test failed - " + e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/endpoints")
    public ResponseEntity<Map<String, Object>> getAvailableEndpoints() {
        log.info("🔍 Admin: Getting available endpoints");
        
        Map<String, Object> endpoints = new HashMap<>();
        
        // Health endpoints
        Map<String, String> health = new HashMap<>();
        health.put("GET /api/health", "Application health status");
        health.put("GET /api/health/detailed", "Detailed health information");
        endpoints.put("Health", health);
        
        // Resume endpoints
        Map<String, String> resume = new HashMap<>();
        resume.put("POST /api/resumes/generate", "Generate tailored resume");
        resume.put("GET /api/resumes/{id}", "Get resume by ID");
        resume.put("POST /api/resumes/analyze-job", "Analyze job posting");
        endpoints.put("Resume", resume);
        
        // Export endpoints
        Map<String, String> export = new HashMap<>();
        export.put("POST /api/export/latex", "Export resume as LaTeX");
        export.put("POST /api/export/pdf", "Export resume as PDF");
        export.put("POST /api/export/image", "Export resume as image");
        export.put("GET /api/export/debug/{sessionId}", "Get debug information");
        endpoints.put("Export", export);
        
        // Chat endpoints
        Map<String, String> chat = new HashMap<>();
        chat.put("POST /api/chat/message", "Send chat message");
        chat.put("GET /api/chat/suggestions", "Get chat suggestions");
        endpoints.put("Chat", chat);
        
        // Admin endpoints
        Map<String, String> admin = new HashMap<>();
        admin.put("GET /api/admin/status", "Get system status");
        admin.put("GET /api/admin/logs", "Get recent logs");
        admin.put("POST /api/admin/logs/clear", "Clear logs");
        admin.put("POST /api/admin/test/latex", "Test LaTeX generation");
        endpoints.put("Admin", admin);
        
        return ResponseEntity.ok(endpoints);
    }

    private String generateFallbackUI() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>CVitae Admin UI</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .container { max-width: 1200px; margin: 0 auto; }
                    .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
                    .status-good { background-color: #d4edda; border-color: #c3e6cb; }
                    .status-bad { background-color: #f8d7da; border-color: #f5c6cb; }
                    button { padding: 8px 16px; margin: 5px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
                    button:hover { background: #0056b3; }
                    pre { background: #f8f9fa; padding: 10px; border-radius: 4px; overflow-x: auto; }
                    .logs { max-height: 300px; overflow-y: auto; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🔧 CVitae Backend Admin UI</h1>
                    <p>⚠️ Fallback UI - admin-ui.html not found in resources/static/</p>
                    
                    <div class="section">
                        <h2>Quick Links</h2>
                        <button onclick="window.open('/api/admin/status', '_blank')">System Status</button>
                        <button onclick="window.open('/api/admin/logs', '_blank')">View Logs</button>
                        <button onclick="window.open('/api/admin/endpoints', '_blank')">Available Endpoints</button>
                        <button onclick="window.open('/api/health', '_blank')">Health Check</button>
                    </div>
                    
                    <div class="section">
                        <h2>Instructions</h2>
                        <p>This is a minimal fallback interface. To get the full admin UI:</p>
                        <ol>
                            <li>Create <code>backend/src/main/resources/static/admin-ui.html</code></li>
                            <li>Restart the application</li>
                            <li>Visit <code>/api/admin/ui</code> again</li>
                        </ol>
                        <p>Available API endpoints are listed at <a href="/api/admin/endpoints" target="_blank">/api/admin/endpoints</a></p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}
