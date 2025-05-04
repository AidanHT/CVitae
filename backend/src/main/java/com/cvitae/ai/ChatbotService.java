package com.cvitae.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for handling AI-powered chatbot interactions
 * Provides contextual resume and career advice
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final GroqClient groqClient;

    /**
     * Process a chat message with full context awareness
     */
    public ChatbotResponse processMessage(String message, String sessionId, String context, 
                                        String resumeContent, String jobContent) {
        log.info("Processing chat message for session: {}", sessionId);

        try {
            // Analyze message intent
            MessageIntent intent = analyzeMessageIntent(message);
            
            // Generate appropriate response based on intent
            GroqResponse groqResponse = generateContextualResponse(message, intent, context, resumeContent, jobContent);
            
            // Process the response and add suggestions if applicable
            return buildChatbotResponse(groqResponse, intent, message, sessionId);

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ChatbotResponse.error(
                "I apologize, but I'm having trouble processing your message right now. Please try again.",
                sessionId
            );
        }
    }

    /**
     * Generate suggestions for resume improvement
     */
    public List<ChatbotSuggestion> generateResumeImprovementSuggestions(String resumeContent, String jobContent) {
        log.info("Generating resume improvement suggestions");

        List<ChatbotSuggestion> suggestions = new ArrayList<>();

        try {
            // Use AI to generate contextual suggestions
            String prompt = buildSuggestionsPrompt(resumeContent, jobContent);
            GroqRequest request = GroqRequest.builder()
                .systemPrompt("You are an expert resume advisor. Provide specific, actionable improvement suggestions.")
                .userPrompt(prompt)
                .maxTokens(1500)
                .temperature(0.5)
                .build();

            GroqResponse response = groqClient.callGroqAPI(request);
            
            if (response.isSuccess()) {
                suggestions = parseSuggestionsFromResponse(response.getContent());
            }

            // Add fallback suggestions if none were generated
            if (suggestions.isEmpty()) {
                suggestions = generateFallbackSuggestions(resumeContent, jobContent);
            }

        } catch (Exception e) {
            log.error("Error generating suggestions", e);
            suggestions = generateFallbackSuggestions(resumeContent, jobContent);
        }

        return suggestions;
    }

    /**
     * Analyze message to determine user intent
     */
    private MessageIntent analyzeMessageIntent(String message) {
        String lowerMessage = message.toLowerCase().trim();

        // Keywords for different intents
        if (containsAny(lowerMessage, "improve", "better", "optimize", "enhance", "suggestion")) {
            return MessageIntent.IMPROVEMENT_REQUEST;
        } else if (containsAny(lowerMessage, "keyword", "ats", "applicant tracking", "optimize")) {
            return MessageIntent.ATS_OPTIMIZATION;
        } else if (containsAny(lowerMessage, "format", "layout", "design", "appearance")) {
            return MessageIntent.FORMATTING_QUESTION;
        } else if (containsAny(lowerMessage, "experience", "job", "work", "career")) {
            return MessageIntent.EXPERIENCE_ADVICE;
        } else if (containsAny(lowerMessage, "skill", "technical", "ability", "competency")) {
            return MessageIntent.SKILLS_ADVICE;
        } else if (containsAny(lowerMessage, "cover letter", "interview", "application")) {
            return MessageIntent.APPLICATION_ADVICE;
        } else if (containsAny(lowerMessage, "hello", "hi", "help", "start", "begin")) {
            return MessageIntent.GREETING_OR_HELP;
        } else {
            return MessageIntent.GENERAL_QUESTION;
        }
    }

    /**
     * Generate contextual response based on intent and available information
     */
    private GroqResponse generateContextualResponse(String message, MessageIntent intent, String context, 
                                                  String resumeContent, String jobContent) {
        
        String systemPrompt = buildSystemPromptForIntent(intent);
        String enhancedPrompt = buildEnhancedUserPrompt(message, intent, context, resumeContent, jobContent);

        GroqRequest request = GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(enhancedPrompt)
            .maxTokens(1000)
            .temperature(0.7)
            .build();

        return groqClient.callGroqAPI(request);
    }

    /**
     * Build system prompt based on message intent
     */
    private String buildSystemPromptForIntent(MessageIntent intent) {
        return switch (intent) {
            case IMPROVEMENT_REQUEST -> """
                You are an expert resume writer and career coach. Provide specific, actionable advice 
                for resume improvement. Focus on concrete changes the user can make immediately.
                """;
            case ATS_OPTIMIZATION -> """
                You are an ATS (Applicant Tracking System) expert. Help users optimize their resumes 
                for automated scanning systems while maintaining readability for humans.
                """;
            case FORMATTING_QUESTION -> """
                You are a professional document formatting expert specializing in resumes. Provide 
                clear guidance on resume layout, design, and formatting best practices.
                """;
            case EXPERIENCE_ADVICE -> """
                You are a career advisor specializing in helping professionals present their work 
                experience effectively. Focus on storytelling and impact demonstration.
                """;
            case SKILLS_ADVICE -> """
                You are a skills assessment and presentation expert. Help users identify, categorize, 
                and effectively present their technical and soft skills.
                """;
            case APPLICATION_ADVICE -> """
                You are a job application strategist. Provide comprehensive advice on the entire 
                job application process including resumes, cover letters, and interview preparation.
                """;
            case GREETING_OR_HELP -> """
                You are a friendly and helpful AI resume assistant. Welcome users warmly and guide 
                them on how you can help with their resume and career goals.
                """;
            default -> """
                You are a knowledgeable resume and career advisor. Provide helpful, professional 
                advice tailored to the user's specific question or situation.
                """;
        };
    }

    /**
     * Build enhanced user prompt with context
     */
    private String buildEnhancedUserPrompt(String message, MessageIntent intent, String context, 
                                         String resumeContent, String jobContent) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("User Message: ").append(message).append("\n\n");
        
        if (context != null && !context.trim().isEmpty()) {
            prompt.append("Context: ").append(context).append("\n\n");
        }
        
        if (resumeContent != null && !resumeContent.trim().isEmpty()) {
            prompt.append("Current Resume Content:\n").append(truncateIfTooLong(resumeContent, 1000)).append("\n\n");
        }
        
        if (jobContent != null && !jobContent.trim().isEmpty()) {
            prompt.append("Target Job Posting:\n").append(truncateIfTooLong(jobContent, 800)).append("\n\n");
        }
        
        // Add intent-specific instructions
        prompt.append(getIntentSpecificInstructions(intent));
        
        return prompt.toString();
    }

    /**
     * Get specific instructions based on message intent
     */
    private String getIntentSpecificInstructions(MessageIntent intent) {
        return switch (intent) {
            case IMPROVEMENT_REQUEST -> "Provide 3-5 specific, actionable improvements the user can make.";
            case ATS_OPTIMIZATION -> "Focus on keyword optimization and ATS-friendly formatting advice.";
            case FORMATTING_QUESTION -> "Provide clear formatting guidelines and best practices.";
            case EXPERIENCE_ADVICE -> "Help the user better present their work experience and achievements.";
            case SKILLS_ADVICE -> "Guide the user on skills presentation and categorization.";
            case APPLICATION_ADVICE -> "Provide comprehensive job application strategy advice.";
            case GREETING_OR_HELP -> "Welcome the user and explain how you can help with their resume.";
            default -> "Provide helpful, specific advice related to the user's question.";
        };
    }

    /**
     * Build chatbot response with suggestions and metadata
     */
    private ChatbotResponse buildChatbotResponse(GroqResponse groqResponse, MessageIntent intent, 
                                               String originalMessage, String sessionId) {
        
        String responseText = groqResponse.isSuccess() ? 
            groqResponse.getContent() : 
            generateFallbackResponse(intent, originalMessage);

        // Generate follow-up suggestions based on intent
        List<String> followUpSuggestions = generateFollowUpSuggestions(intent);

        return ChatbotResponse.builder()
            .response(responseText)
            .sessionId(sessionId)
            .intent(intent.name())
            .confidence(groqResponse.isSuccess() ? 0.85 : 0.5)
            .followUpSuggestions(followUpSuggestions)
            .requiresUserAction(intent == MessageIntent.IMPROVEMENT_REQUEST)
            .success(true)
            .build();
    }

    /**
     * Parse suggestions from AI response
     */
    private List<ChatbotSuggestion> parseSuggestionsFromResponse(String response) {
        List<ChatbotSuggestion> suggestions = new ArrayList<>();
        
        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
                String suggestionText = line.replaceFirst("^[-*•]\\s*", "").trim();
                if (!suggestionText.isEmpty()) {
                    suggestions.add(ChatbotSuggestion.builder()
                        .text(suggestionText)
                        .priority(determinePriority(suggestionText))
                        .category(determineCategory(suggestionText))
                        .build());
                }
            }
        }
        
        return suggestions;
    }

    /**
     * Generate fallback suggestions when AI fails
     */
    private List<ChatbotSuggestion> generateFallbackSuggestions(String resumeContent, String jobContent) {
        List<ChatbotSuggestion> suggestions = new ArrayList<>();
        
        suggestions.add(ChatbotSuggestion.builder()
            .text("Add quantified achievements (e.g., 'Increased sales by 25%')")
            .priority("HIGH")
            .category("CONTENT")
            .build());
            
        suggestions.add(ChatbotSuggestion.builder()
            .text("Use strong action verbs to start bullet points")
            .priority("MEDIUM")
            .category("WRITING")
            .build());
            
        suggestions.add(ChatbotSuggestion.builder()
            .text("Ensure resume matches job posting keywords")
            .priority("HIGH")
            .category("ATS_OPTIMIZATION")
            .build());
            
        return suggestions;
    }

    /**
     * Generate fallback response based on intent
     */
    private String generateFallbackResponse(MessageIntent intent, String originalMessage) {
        return switch (intent) {
            case IMPROVEMENT_REQUEST -> """
                I'd be happy to help improve your resume! Here are some general recommendations:
                
                • Add specific metrics and numbers to quantify your achievements
                • Use strong action verbs like 'achieved,' 'developed,' 'led,' or 'implemented'
                • Tailor your experience descriptions to match the job requirements
                • Ensure your skills section includes both technical and soft skills
                • Keep formatting clean and ATS-friendly
                
                Would you like me to focus on any specific section of your resume?
                """;
            case ATS_OPTIMIZATION -> """
                For better ATS compatibility, consider these key points:
                
                • Use standard section headings (Experience, Education, Skills)
                • Include keywords from the job posting naturally in your descriptions
                • Avoid headers, footers, and complex formatting
                • Use standard fonts like Arial or Calibri
                • Save as both PDF and plain text versions
                
                What specific ATS concerns do you have?
                """;
            case FORMATTING_QUESTION -> """
                For professional resume formatting:
                
                • Keep it clean and easy to scan
                • Use consistent spacing and alignment
                • Stick to 1-2 pages maximum
                • Use bullet points for easy reading
                • Maintain consistent date formatting
                
                What formatting aspect would you like help with?
                """;
            default -> """
                I'm here to help with your resume and career questions! I can assist with:
                
                • Resume content and structure optimization
                • ATS compatibility improvements
                • Interview preparation advice
                • Job application strategies
                • Skills presentation and development
                
                What would you like to work on today?
                """;
        };
    }

    /**
     * Generate follow-up suggestions based on intent
     */
    private List<String> generateFollowUpSuggestions(MessageIntent intent) {
        return switch (intent) {
            case IMPROVEMENT_REQUEST -> List.of(
                "Can you help me quantify my achievements?",
                "How can I better match job requirements?",
                "What skills should I highlight?"
            );
            case ATS_OPTIMIZATION -> List.of(
                "How do I find the right keywords?",
                "What formatting should I avoid?",
                "Can you check my keyword density?"
            );
            case FORMATTING_QUESTION -> List.of(
                "What's the best resume length?",
                "Should I use a template?",
                "How do I organize my sections?"
            );
            default -> List.of(
                "How can I improve my resume?",
                "Help me with ATS optimization",
                "What about my cover letter?"
            );
        };
    }

    /**
     * Build suggestions prompt for AI
     */
    private String buildSuggestionsPrompt(String resumeContent, String jobContent) {
        return String.format("""
            Analyze this resume and job posting to provide specific improvement suggestions:
            
            RESUME:
            %s
            
            JOB POSTING:
            %s
            
            Provide 3-5 specific, actionable suggestions for improvement. 
            For each suggestion, format as:
            - [Specific improvement with examples]
            
            Focus on:
            1. Keyword optimization for ATS
            2. Quantifying achievements
            3. Better skill presentation
            4. Experience relevance
            5. Professional formatting
            """, 
            truncateIfTooLong(resumeContent, 1500), 
            truncateIfTooLong(jobContent, 1000)
        );
    }

    /**
     * Utility methods
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String truncateIfTooLong(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... [truncated]";
    }

    private String determinePriority(String suggestionText) {
        String lower = suggestionText.toLowerCase();
        if (containsAny(lower, "critical", "important", "must", "essential", "required")) {
            return "HIGH";
        } else if (containsAny(lower, "should", "recommend", "consider", "better")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String determineCategory(String suggestionText) {
        String lower = suggestionText.toLowerCase();
        if (containsAny(lower, "keyword", "ats", "search")) {
            return "ATS_OPTIMIZATION";
        } else if (containsAny(lower, "format", "layout", "design")) {
            return "FORMATTING";
        } else if (containsAny(lower, "skill", "technical", "ability")) {
            return "SKILLS";
        } else if (containsAny(lower, "experience", "achievement", "accomplish")) {
            return "EXPERIENCE";
        }
        return "CONTENT";
    }

    /**
     * Message intent enumeration
     */
    public enum MessageIntent {
        IMPROVEMENT_REQUEST,
        ATS_OPTIMIZATION,
        FORMATTING_QUESTION,
        EXPERIENCE_ADVICE,
        SKILLS_ADVICE,
        APPLICATION_ADVICE,
        GREETING_OR_HELP,
        GENERAL_QUESTION
    }
}
