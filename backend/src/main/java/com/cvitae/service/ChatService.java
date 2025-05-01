package com.cvitae.service;

import com.cvitae.dto.*;

import java.util.List;

public interface ChatService {
    
    /**
     * Process a chat message and return AI response
     */
    ChatMessageResponse processMessage(ChatMessageRequest request);
    
    /**
     * Generate AI suggestions for resume improvement
     */
    List<SuggestionResponse> generateSuggestions(SuggestionRequest request);
    
    /**
     * Get conversation history for a session
     */
    List<ChatMessageResponse> getConversationHistory(String sessionId);
    
    /**
     * Clear conversation history
     */
    void clearConversation(String sessionId);
}
