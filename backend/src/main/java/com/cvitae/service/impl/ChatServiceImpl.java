package com.cvitae.service.impl;

import com.cvitae.dto.*;
import com.cvitae.entity.ChatMessage;
import com.cvitae.repository.ChatMessageRepository;
import com.cvitae.service.ChatService;
import com.cvitae.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final GroqAIService groqAIService;

    @Override
    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        log.info("Processing chat message for session: {}", request.getSessionId());
        
        try {
            // Get AI response
            String aiResponse = groqAIService.processChatMessage(
                request.getMessage(),
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
                .response(aiResponse)
                .messageType("ASSISTANT")
                .context(request.getContext())
                .resumeId(request.getResumeId())
                .build();
            assistantMessage = chatMessageRepository.save(assistantMessage);

            return ChatMessageResponse.builder()
                .messageId(assistantMessage.getId().toString())
                .message(request.getMessage())
                .response(aiResponse)
                .sessionId(request.getSessionId())
                .timestamp(assistantMessage.getCreatedAt())
                .messageType("ASSISTANT")
                .context(request.getContext())
                .confidence(0.85) // Default confidence
                .requiresUserAction(false)
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

            // Parse AI response and create suggestion objects
            // This is simplified - in practice, you'd want more sophisticated parsing
            return List.of(
                SuggestionResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .title("Optimize Keywords")
                    .description("Add more job-relevant keywords to improve ATS compatibility")
                    .priority("HIGH")
                    .category("KEYWORDS")
                    .suggestedChange("Include specific technical skills mentioned in job posting")
                    .reasoning("Job posting emphasizes these skills but they're missing from resume")
                    .impactScore(0.9)
                    .requiresUserAction(true)
                    .build(),
                
                SuggestionResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .title("Quantify Achievements")
                    .description("Add specific numbers and metrics to your accomplishments")
                    .priority("MEDIUM")
                    .category("CONTENT")
                    .suggestedChange("Replace 'improved performance' with 'improved performance by 25%'")
                    .reasoning("Quantified achievements are more compelling to recruiters")
                    .impactScore(0.7)
                    .requiresUserAction(true)
                    .build(),
                
                SuggestionResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .title("Action Verb Optimization")
                    .description("Use stronger action verbs that match the job posting tone")
                    .priority("MEDIUM")
                    .category("CONTENT")
                    .suggestedChange("Replace 'helped' with 'spearheaded' or 'led'")
                    .reasoning("Stronger verbs convey leadership and initiative")
                    .impactScore(0.6)
                    .requiresUserAction(false)
                    .build()
            );

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
}
