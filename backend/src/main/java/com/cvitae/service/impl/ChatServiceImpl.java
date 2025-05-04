package com.cvitae.service.impl;

import com.cvitae.ai.ChatbotService;
import com.cvitae.ai.ChatbotResponse;
import com.cvitae.dto.*;
import com.cvitae.entity.ChatMessage;
import com.cvitae.repository.ChatMessageRepository;
import com.cvitae.service.ChatService;
import com.cvitae.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final GroqAIService groqAIService;
    private final ChatbotService chatbotService;

    @Override
    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        log.info("Processing chat message for session: {}", request.getSessionId());
        
        try {
            // Use the new chatbot service for enhanced AI processing
            ChatbotResponse aiResponse = chatbotService.processMessage(
                request.getMessage(),
                request.getSessionId(),
                request.getContext(),
                request.getCurrentResumeContent(),
                request.getJobPostingContent()
            );

            // Save user message
            ChatMessage userMessage = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .message(request.getMessage())
                .messageType("USER")
                .context(request.getContext())
                .resumeId(request.getResumeId())
                .build();
            chatMessageRepository.save(userMessage);

            // Save AI response
            ChatMessage assistantMessage = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .message(request.getMessage())
                .response(aiResponse.getResponse())
                .messageType("ASSISTANT")
                .context(request.getContext())
                .resumeId(request.getResumeId())
                .build();
            assistantMessage = chatMessageRepository.save(assistantMessage);

            return ChatMessageResponse.builder()
                .messageId(assistantMessage.getId().toString())
                .message(request.getMessage())
                .response(aiResponse.getResponse())
                .sessionId(request.getSessionId())
                .timestamp(assistantMessage.getCreatedAt())
                .messageType("ASSISTANT")
                .context(request.getContext())
                .confidence(aiResponse.getConfidence())
                .requiresUserAction(aiResponse.isRequiresUserAction())
                .build();

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            throw new RuntimeException("Failed to process chat message", e);
        }
    }

    @Override
    public List<SuggestionResponse> generateSuggestions(SuggestionRequest request) {
        log.info("Generating suggestions for resume optimization");
        
        try {
            String aiSuggestions = groqAIService.generateSuggestions(
                request.getResumeContent(),
                request.getJobPostingContent(),
                request.getSuggestionType()
            );

            // Parse AI response into structured suggestions
            return parseAISuggestions(aiSuggestions, request.getSuggestionType());

        } catch (Exception e) {
            log.error("Error generating suggestions", e);
            throw new RuntimeException("Failed to generate suggestions", e);
        }
    }

    @Override
    public List<ChatMessageResponse> getConversationHistory(String sessionId) {
        log.info("Retrieving conversation history for session: {}", sessionId);
        
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        return messages.stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    public void clearConversation(String sessionId) {
        log.info("Clearing conversation for session: {}", sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
    }

    private ChatMessageResponse mapToResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
            .messageId(message.getId().toString())
            .message(message.getMessage())
            .response(message.getResponse())
            .sessionId(message.getSessionId())
            .timestamp(message.getCreatedAt())
            .messageType(message.getMessageType())
            .context(message.getContext())
            .confidence(0.85) // Default confidence
            .requiresUserAction(false)
            .build();
    }
    
    private List<SuggestionResponse> parseAISuggestions(String aiResponse, String suggestionType) {
        List<SuggestionResponse> suggestions = new ArrayList<>();
        
        try {
            // Parse AI response for structured suggestions
            String[] lines = aiResponse.split("\n");
            SuggestionResponse.SuggestionResponseBuilder currentSuggestion = null;
            
            for (String line : lines) {
                line = line.trim();
                
                if (line.isEmpty()) continue;
                
                // Look for suggestion markers
                if (line.matches("^\\d+\\.|^[-*•]\\s+.*") || line.toLowerCase().startsWith("suggestion")) {
                    // Save previous suggestion if exists
                    if (currentSuggestion != null) {
                        suggestions.add(finalizeSuggestion(currentSuggestion, suggestionType));
                    }
                    
                    // Start new suggestion
                    currentSuggestion = SuggestionResponse.builder()
                        .id(UUID.randomUUID().toString())
                        .category(mapSuggestionTypeToCategory(suggestionType));
                    
                    // Extract title from line
                    String title = line.replaceFirst("^\\d+\\.|^[-*•]\\s+|^suggestion:?\\s*", "").trim();
                    if (title.contains(":")) {
                        String[] parts = title.split(":", 2);
                        currentSuggestion.title(parts[0].trim());
                        currentSuggestion.description(parts[1].trim());
                    } else {
                        currentSuggestion.title(title);
                    }
                }
                // Look for specific fields
                else if (currentSuggestion != null) {
                    if (line.toLowerCase().startsWith("description:") || line.toLowerCase().startsWith("reason:")) {
                        String desc = line.replaceFirst("^(description|reason):?\\s*", "").trim();
                        currentSuggestion.reasoning(desc);
                    } else if (line.toLowerCase().startsWith("change:") || line.toLowerCase().startsWith("suggested:")) {
                        String change = line.replaceFirst("^(change|suggested):?\\s*", "").trim();
                        currentSuggestion.suggestedChange(change);
                    } else if (line.toLowerCase().startsWith("impact:") || line.toLowerCase().startsWith("priority:")) {
                        if (line.toLowerCase().contains("high")) {
                            currentSuggestion.priority("HIGH").impactScore(0.9);
                        } else if (line.toLowerCase().contains("medium")) {
                            currentSuggestion.priority("MEDIUM").impactScore(0.7);
                        } else {
                            currentSuggestion.priority("LOW").impactScore(0.5);
                        }
                    }
                    // Accumulate description if no specific field found
                    else {
                        // Check if description is already set to avoid infinite builder calls
                        String currentDesc = "";
                        try {
                            SuggestionResponse temp = currentSuggestion.build();
                            currentDesc = temp.getDescription();
                        } catch (Exception ex) {
                            // Builder not ready yet, safe to add description
                        }
                        
                        if (currentDesc == null || currentDesc.isEmpty()) {
                            currentSuggestion.description(line);
                        }
                    }
                }
            }
            
            // Add the last suggestion
            if (currentSuggestion != null) {
                suggestions.add(finalizeSuggestion(currentSuggestion, suggestionType));
            }
            
        } catch (Exception e) {
            log.warn("Error parsing AI suggestions, using fallback: {}", e.getMessage());
        }
        
        // If no suggestions parsed, return default ones based on type
        if (suggestions.isEmpty()) {
            suggestions = getDefaultSuggestions(suggestionType);
        }
        
        return suggestions;
    }
    
    private SuggestionResponse finalizeSuggestion(SuggestionResponse.SuggestionResponseBuilder builder, String suggestionType) {
        SuggestionResponse suggestion = builder.build();
        
        // Set defaults for missing fields
        if (suggestion.getTitle() == null || suggestion.getTitle().isEmpty()) {
            builder.title("Resume Improvement Suggestion");
        }
        if (suggestion.getDescription() == null || suggestion.getDescription().isEmpty()) {
            builder.description("Consider improving this aspect of your resume");
        }
        if (suggestion.getPriority() == null) {
            builder.priority("MEDIUM").impactScore(0.7);
        }
        if (suggestion.getCategory() == null) {
            builder.category(mapSuggestionTypeToCategory(suggestionType));
        }
        
        builder.requiresUserAction(true);
        
        return builder.build();
    }
    
    private String mapSuggestionTypeToCategory(String suggestionType) {
        if (suggestionType == null) return "GENERAL";
        
        return switch (suggestionType.toLowerCase()) {
            case "keywords" -> "KEYWORDS";
            case "content" -> "CONTENT";
            case "format" -> "FORMAT";
            case "skills" -> "SKILLS";
            default -> "GENERAL";
        };
    }
    
    private List<SuggestionResponse> getDefaultSuggestions(String suggestionType) {
        return List.of(
            SuggestionResponse.builder()
                .id(UUID.randomUUID().toString())
                .title("Optimize Keywords")
                .description("Review job posting for specific keywords and incorporate them naturally into your resume")
                .priority("HIGH")
                .category("KEYWORDS")
                .suggestedChange("Add job-relevant keywords to experience descriptions")
                .reasoning("ATS systems scan for specific keywords from job postings")
                .impactScore(0.9)
                .requiresUserAction(true)
                .build(),
            
            SuggestionResponse.builder()
                .id(UUID.randomUUID().toString())
                .title("Quantify Achievements")
                .description("Add specific numbers, percentages, or metrics to your accomplishments")
                .priority("MEDIUM")
                .category("CONTENT")
                .suggestedChange("Replace general statements with quantified results")
                .reasoning("Numbers make achievements more credible and impactful")
                .impactScore(0.8)
                .requiresUserAction(true)
                .build(),
            
            SuggestionResponse.builder()
                .id(UUID.randomUUID().toString())
                .title("Use Strong Action Verbs")
                .description("Replace weak verbs with powerful action words that demonstrate leadership")
                .priority("MEDIUM")
                .category("CONTENT")
                .suggestedChange("Start bullet points with impactful action verbs")
                .reasoning("Strong verbs convey confidence and proactive attitude")
                .impactScore(0.7)
                .requiresUserAction(true)
                .build()
        );
    }
}
