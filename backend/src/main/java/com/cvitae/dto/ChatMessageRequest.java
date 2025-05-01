package com.cvitae.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class ChatMessageRequest {
    
    @NotBlank(message = "Message cannot be blank")
    private String message;
    
    @NotBlank(message = "Session ID cannot be blank")
    private String sessionId;
    
    private UUID resumeId; // If discussing a specific resume
    private String context; // RESUME_OPTIMIZATION, GENERAL_ADVICE, JOB_ANALYSIS
    
    // Additional context
    private String currentResumeContent;
    private String jobPostingContent;
    private Map<String, Object> userPreferences;
}
