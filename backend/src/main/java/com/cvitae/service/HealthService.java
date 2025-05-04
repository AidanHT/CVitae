package com.cvitae.service;

import java.util.Map;

/**
 * Service interface for health checking and system monitoring
 */
public interface HealthService {
    
    /**
     * Get basic health status of the application
     * @return Map containing health status information
     */
    Map<String, Object> getHealthStatus();
    
    /**
     * Get detailed health status including dependencies
     * @return Map containing detailed health information
     */
    Map<String, Object> getDetailedHealthStatus();
}
