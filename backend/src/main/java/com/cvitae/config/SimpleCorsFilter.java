package com.cvitae.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Simple CORS filter that bypasses all Spring Security configurations
 * This is a last resort to fix persistent CORS issues
 */
@Component
@Order(1)
@Slf4j
public class SimpleCorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        String origin = request.getHeader("Origin");
        
        // Log the request for debugging
        log.debug("🌐 CORS Filter: {} {} from origin: {}", 
                request.getMethod(), request.getRequestURI(), origin);

        // Set CORS headers for allowed origins
        if (origin != null && (
                origin.equals("http://localhost:3000") ||
                origin.equals("http://localhost:3001") ||
                origin.equals("http://localhost:5173") ||
                origin.equals("http://localhost")
        )) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "false");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Expose-Headers", "X-Trace-ID, Content-Disposition");
            response.setHeader("Access-Control-Max-Age", "3600");
            
            log.debug("✅ CORS headers set for origin: {}", origin);
        }

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("🔄 Handling OPTIONS preflight request");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("🌐 SimpleCorsFilter initialized - bypassing Spring Security CORS");
    }

    @Override
    public void destroy() {
        log.info("🌐 SimpleCorsFilter destroyed");
    }
}
