package com.cvitae.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a specific suggestion from the chatbot
 */
@Data
@Builder
public class ChatbotSuggestion {
    
    private String text;
    private String priority; // HIGH, MEDIUM, LOW
    private String category; // ATS_OPTIMIZATION, FORMATTING, SKILLS, EXPERIENCE, CONTENT
    private String actionType; // ADD, MODIFY, REMOVE, REORGANIZE
    private String specificExample; // Optional example of the suggested change
    
    /**
     * Create a high priority suggestion
     */
    public static ChatbotSuggestion highPriority(String text, String category) {
        return ChatbotSuggestion.builder()
            .text(text)
            .priority("HIGH")
            .category(category)
            .build();
    }
    
    /**
     * Create a medium priority suggestion
     */
    public static ChatbotSuggestion mediumPriority(String text, String category) {
        return ChatbotSuggestion.builder()
            .text(text)
            .priority("MEDIUM")
            .category(category)
            .build();
    }
    
    /**
     * Create a low priority suggestion
     */
    public static ChatbotSuggestion lowPriority(String text, String category) {
        return ChatbotSuggestion.builder()
            .text(text)
            .priority("LOW")
            .category(category)
            .build();
    }
}
