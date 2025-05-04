package com.cvitae.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter for logging all HTTP requests and responses with trace IDs
 */
@Component
@Slf4j
public class RequestResponseLoggingFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Generate trace ID
        String traceId = generateTraceId();
        
        // Set trace ID in MDC for logging
        MDC.put(TRACE_ID_KEY, traceId);
        
        // Add trace ID to response headers
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Log incoming request
            logRequest(httpRequest, traceId);
            
            // Process request
            chain.doFilter(request, response);
            
            // Log response
            long duration = System.currentTimeMillis() - startTime;
            logResponse(httpRequest, httpResponse, traceId, duration);
            
        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }

    private void logRequest(HttpServletRequest request, String traceId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = getClientIpAddress(request);
        
        log.info("→ {} {} {} [TraceID: {}] from {} UA: {}", 
            method, 
            uri, 
            queryString != null ? "?" + queryString : "",
            traceId,
            remoteAddr,
            userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 100)) : "unknown"
        );
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, String traceId, long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        String logLevel = status >= 400 ? "ERROR" : "INFO";
        
        if (status >= 400) {
            log.error("← {} {} → {} [TraceID: {}] in {}ms", method, uri, status, traceId, duration);
        } else {
            log.info("← {} {} → {} [TraceID: {}] in {}ms", method, uri, status, traceId, duration);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
