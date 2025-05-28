package com.cvitae.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Slf4j
public class MonitoringController {

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        log.info("🔍 Getting application metrics");
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Memory metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memory = new HashMap<>();
        memory.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024);
        memory.put("heapMax", memoryBean.getHeapMemoryUsage().getMax() / 1024 / 1024);
        memory.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024);
        metrics.put("memory", memory);
        
        // Runtime metrics
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> runtime = new HashMap<>();
        runtime.put("uptime", runtimeBean.getUptime());
        runtime.put("startTime", runtimeBean.getStartTime());
        runtime.put("vmName", runtimeBean.getVmName());
        runtime.put("vmVersion", runtimeBean.getVmVersion());
        metrics.put("runtime", runtime);
        
        // System properties
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("javaVendor", System.getProperty("java.vendor"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("processors", Runtime.getRuntime().availableProcessors());
        metrics.put("system", system);
        
        metrics.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/gc")
    public ResponseEntity<Map<String, Object>> triggerGc() {
        log.info("🔍 Triggering garbage collection");
        
        Runtime runtime = Runtime.getRuntime();
        long beforeMem = runtime.totalMemory() - runtime.freeMemory();
        
        System.gc();
        
        long afterMem = runtime.totalMemory() - runtime.freeMemory();
        
        Map<String, Object> result = new HashMap<>();
        result.put("memoryBeforeGC", beforeMem / 1024 / 1024 + " MB");
        result.put("memoryAfterGC", afterMem / 1024 / 1024 + " MB");
        result.put("memoryFreed", (beforeMem - afterMem) / 1024 / 1024 + " MB");
        result.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/threads")
    public ResponseEntity<Map<String, Object>> getThreadInfo() {
        log.info("🔍 Getting thread information");
        
        Map<String, Object> threadInfo = new HashMap<>();
        
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while (rootGroup.getParent() != null) {
            rootGroup = rootGroup.getParent();
        }
        
        threadInfo.put("activeThreads", rootGroup.activeCount());
        threadInfo.put("activeGroups", rootGroup.activeGroupCount());
        
        // Get current thread info
        Thread currentThread = Thread.currentThread();
        Map<String, Object> current = new HashMap<>();
        current.put("name", currentThread.getName());
        current.put("state", currentThread.getState().toString());
        current.put("priority", currentThread.getPriority());
        current.put("isDaemon", currentThread.isDaemon());
        threadInfo.put("currentThread", current);
        
        threadInfo.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(threadInfo);
    }

    @PostMapping("/log-test")
    public ResponseEntity<Map<String, Object>> testLogging(@RequestBody Map<String, String> request) {
        String level = request.getOrDefault("level", "INFO");
        String message = request.getOrDefault("message", "Test log message from monitoring controller");
        
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
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("level", level);
        result.put("message", message);
        result.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(result);
    }
}
