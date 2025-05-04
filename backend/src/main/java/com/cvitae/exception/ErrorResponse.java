package com.cvitae.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response structure for all API endpoints
 */
@Data
@Builder
public class ErrorResponse {
    private String traceId;
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, Object> details;
}
