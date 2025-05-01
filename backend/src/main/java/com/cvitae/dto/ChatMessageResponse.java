package com.cvitae.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChatMessageResponse {
    
    private String messageId;
    private String message;
    private String response;
    private String sessionId;
    
    private LocalDateTime timestamp;
    private String messageType; // USER, ASSISTANT, SYSTEM
    
    // AI Response metadata
    private Double confidence;
    private List<String> suggestedActions;
    private Map<String, Object> actionableChanges; // Specific changes AI suggests
    
    // If the response includes resume modifications
    private String modifiedResumeContent;
    private List<String> changesExplanation;
    
    // Context information
    private String context;
    private boolean requiresUserAction;
}
