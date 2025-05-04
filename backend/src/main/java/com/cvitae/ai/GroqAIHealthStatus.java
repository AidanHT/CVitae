package com.cvitae.ai;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Health status information for Groq AI service
 */
@Data
@Builder
public class GroqAIHealthStatus {
    private boolean available;
    private boolean apiKeyConfigured;
    private String status;
    private String message;
    private LocalDateTime lastChecked;
    private Integer lastResponseTimeMs;
    private String errorDetails;
    private boolean mockMode;
}
