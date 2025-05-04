package com.cvitae.ai;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response object for chatbot interactions
 */
@Data
@Builder
public class ChatbotResponse {
    
    private String response;
    private String sessionId;
    private String intent;
    private double confidence;
    private List<String> followUpSuggestions;
    private boolean requiresUserAction;
    private boolean success;
    private String errorMessage;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Create a successful chatbot response
     */
    public static ChatbotResponse success(String response, String sessionId) {
        return ChatbotResponse.builder()
            .response(response)
            .sessionId(sessionId)
            .success(true)
            .confidence(0.8)
            .requiresUserAction(false)
            .build();
    }
    
    /**
     * Create an error chatbot response
     */
    public static ChatbotResponse error(String errorMessage, String sessionId) {
        return ChatbotResponse.builder()
            .response("I apologize, but I'm experiencing technical difficulties. Please try again in a moment.")
            .sessionId(sessionId)
            .success(false)
            .errorMessage(errorMessage)
            .confidence(0.0)
            .requiresUserAction(false)
            .build();
    }
}
