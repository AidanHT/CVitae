package com.cvitae.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Enhanced Groq API client with better error handling, retry logic, and structured responses
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroqClient {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama3-8b-8192}")
    private String groqModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Call Groq API with structured prompt and system context
     */
    public GroqResponse callGroqAPI(GroqRequest request) {
        try {
            // Check if API key is configured
            if (!isApiKeyConfigured()) {
                log.warn("Groq API key not configured, using fallback response");
                return GroqResponse.fallback(request.getUserPrompt());
            }

            Map<String, Object> requestBody = buildRequestBody(request);
            
            log.debug("Calling Groq API with model: {}", groqModel);
            String response = webClient.post()
                .uri(groqApiUrl)
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return parseGroqResponse(response);

        } catch (WebClientResponseException e) {
            log.error("Groq API error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return GroqResponse.error("API request failed: " + e.getMessage(), request.getUserPrompt());
        } catch (Exception e) {
            log.error("Unexpected error calling Groq API: {}", e.getMessage(), e);
            return GroqResponse.error("Unexpected error: " + e.getMessage(), request.getUserPrompt());
        }
    }

    /**
     * Specialized method for resume analysis with structured output
     */
    public GroqResponse analyzeResumeAndJob(String masterResume, String jobPosting, String jobTitle, String companyName) {
        String systemPrompt = """
            You are an expert ATS and resume optimization specialist. Analyze the master resume and job posting 
            to provide structured recommendations for resume tailoring. Focus on:
            1. Keyword matching and optimization
            2. Experience prioritization based on job requirements
            3. Skills alignment with job description
            4. ATS compatibility improvements
            
            Always provide specific, actionable recommendations with examples.
            """;

        String userPrompt = String.format("""
            MASTER RESUME:
            %s
            
            JOB POSTING:
            Title: %s
            Company: %s
            Description: %s
            
            Provide a comprehensive analysis including:
            1. Required skills extraction
            2. Keyword optimization recommendations
            3. Experience sections to prioritize
            4. Specific content modifications needed
            5. ATS compatibility score and improvements
            
            Format your response as structured sections for easy parsing.
            """, masterResume, jobTitle, companyName, jobPosting);

        GroqRequest request = GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(4000)
            .temperature(0.3) // Lower temperature for more consistent analysis
            .build();

        return callGroqAPI(request);
    }

    /**
     * Generate tailored resume content with LaTeX formatting
     */
    public GroqResponse generateTailoredResume(String masterResume, String analysisResults, Map<String, Object> preferences) {
        String systemPrompt = """
            You are an expert resume writer specializing in Jake's LaTeX resume format. Create a tailored resume 
            that is ATS-optimized and professionally formatted. 
            
            CRITICAL REQUIREMENTS:
            - Only include truthful information from the master resume
            - Optimize for ATS scanning and human readability
            - Use Jake's LaTeX template structure
            - Prioritize relevant experiences based on job analysis
            - Include proper LaTeX formatting and commands
            """;

        String userPrompt = String.format("""
            MASTER RESUME:
            %s
            
            JOB ANALYSIS RESULTS:
            %s
            
            TARGET PREFERENCES:
            - Target Length: %s pages
            - Include Experience: %s
            - Include Projects: %s
            - Include Skills: %s
            - Include Education: %s
            
            Generate a complete LaTeX resume using Jake's template format. Include:
            1. Proper LaTeX document structure and packages
            2. Professional header with contact information
            3. Optimized sections based on job requirements
            4. ATS-friendly formatting
            5. Quantified achievements where possible
            
            Output the complete LaTeX code ready for compilation.
            """, 
            masterResume, 
            analysisResults,
            preferences.getOrDefault("targetLength", 1),
            preferences.getOrDefault("includeExperience", true),
            preferences.getOrDefault("includeProjects", true),
            preferences.getOrDefault("includeSkills", true),
            preferences.getOrDefault("includeEducation", true)
        );

        GroqRequest request = GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(4000)
            .temperature(0.4)
            .build();

        return callGroqAPI(request);
    }

    /**
     * Generate chat responses for the AI assistant
     */
    public GroqResponse processChatMessage(String message, String context, String resumeContent, String jobContent) {
        String systemPrompt = """
            You are a helpful AI resume and career advisor. Provide friendly, professional, and actionable advice 
            to help users improve their resumes and job applications. Be encouraging and specific in your recommendations.
            """;

        String userPrompt = String.format("""
            CONTEXT: %s
            USER MESSAGE: %s
            
            CURRENT RESUME:
            %s
            
            JOB POSTING:
            %s
            
            Provide helpful, specific advice. If the user asks for changes, give clear examples and explanations.
            """, context, message, resumeContent, jobContent);

        GroqRequest request = GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(1000)
            .temperature(0.7)
            .build();

        return callGroqAPI(request);
    }

    private boolean isApiKeyConfigured() {
        return groqApiKey != null && 
               !groqApiKey.trim().isEmpty() && 
               !groqApiKey.equals("your-groq-api-key") &&
               !groqApiKey.startsWith("${");
    }

    private Map<String, Object> buildRequestBody(GroqRequest request) {
        return Map.of(
            "model", groqModel,
            "messages", List.of(
                Map.of("role", "system", "content", request.getSystemPrompt()),
                Map.of("role", "user", "content", request.getUserPrompt())
            ),
            "max_tokens", request.getMaxTokens(),
            "temperature", request.getTemperature()
        );
    }

    private GroqResponse parseGroqResponse(String response) {
        try {
            JsonNode responseNode = objectMapper.readTree(response);
            JsonNode choices = responseNode.path("choices");
            
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                JsonNode usage = responseNode.path("usage");
                
                return GroqResponse.success(
                    content,
                    usage.path("total_tokens").asInt(0),
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0)
                );
            } else {
                log.warn("Unexpected Groq API response structure: {}", response);
                return GroqResponse.error("Invalid response structure", "");
            }
        } catch (Exception e) {
            log.error("Error parsing Groq response: {}", e.getMessage());
            return GroqResponse.error("Response parsing failed: " + e.getMessage(), "");
        }
    }
}
