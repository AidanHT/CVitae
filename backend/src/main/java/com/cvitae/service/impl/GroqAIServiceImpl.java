package com.cvitae.service.impl;

import com.cvitae.dto.*;
import com.cvitae.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqAIServiceImpl implements GroqAIService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama3-8b-8192}")
    private String groqModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Override
    public JobAnalysisResponse analyzeJobPosting(String jobPosting, String jobTitle, String companyName) {
        log.info("Analyzing job posting with Groq AI");
        
        String prompt = buildJobAnalysisPrompt(jobPosting, jobTitle, companyName);
        String aiResponse = callGroqAPI(prompt);
        
        return parseJobAnalysisResponse(aiResponse, jobTitle, companyName);
    }

    @Override
    public String generateTailoredResume(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request) {
        log.info("Generating tailored resume with Groq AI");
        
        String prompt = buildResumeGenerationPrompt(masterResume, jobAnalysis, request);
        return callGroqAPI(prompt);
    }

    @Override
    public String processChatMessage(String message, String context, String resumeContent, String jobContent) {
        log.info("Processing chat message with Groq AI");
        
        String prompt = buildChatPrompt(message, context, resumeContent, jobContent);
        return callGroqAPI(prompt);
    }

    @Override
    public String generateSuggestions(String resumeContent, String jobContent, String suggestionType) {
        log.info("Generating suggestions with Groq AI");
        
        String prompt = buildSuggestionsPrompt(resumeContent, jobContent, suggestionType);
        return callGroqAPI(prompt);
    }

    @Override
    public String convertToJakesLatex(String resumeContent, GenerateResumeRequest request) {
        log.info("Converting resume to Jake's LaTeX format");
        
        String prompt = buildLatexConversionPrompt(resumeContent, request);
        return callGroqAPI(prompt);
    }

    private String callGroqAPI(String prompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                "model", groqModel,
                "messages", List.of(
                    Map.of("role", "system", "content", "You are an expert resume writer and career advisor with deep knowledge of ATS systems, hiring practices, and Jake's LaTeX resume format."),
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 4000,
                "temperature", 0.7
            );

            String response = webClient.post()
                .uri(groqApiUrl)
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode responseNode = objectMapper.readTree(response);
            return responseNode.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Error calling Groq API", e);
            throw new RuntimeException("Failed to process AI request", e);
        }
    }

    private String buildJobAnalysisPrompt(String jobPosting, String jobTitle, String companyName) {
        return String.format("""
            Analyze the following job posting and extract key information for resume optimization:
            
            Job Title: %s
            Company: %s
            
            Job Posting:
            %s
            
            Please provide a detailed analysis including:
            1. Required skills (technical and soft skills)
            2. Preferred skills and qualifications
            3. Key responsibilities and experience requirements
            4. Important keywords for ATS optimization
            5. Company culture indicators
            6. Experience level (entry/mid/senior/executive)
            7. Industry-specific requirements
            8. Recommended action verbs for this role
            
            Format the response as structured JSON with clear categories.
            """, jobTitle, companyName, jobPosting);
    }

    private String buildResumeGenerationPrompt(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request) {
        return String.format("""
            Generate a tailored resume optimized for the target job based on the following inputs:
            
            MASTER RESUME:
            %s
            
            JOB ANALYSIS:
            - Required Skills: %s
            - Key Keywords: %s
            - Experience Level: %s
            - Target Length: %d pages
            
            REQUIREMENTS:
            1. Select and prioritize experiences most relevant to the job
            2. Optimize descriptions with job-relevant keywords
            3. Ensure ATS compatibility
            4. Maintain truthfulness - only include actual experiences
            5. Target length: %d page(s)
            6. Include education (always required)
            7. Prioritize these sections: %s
            
            STYLE GUIDELINES:
            - Use strong action verbs
            - Quantify achievements where possible
            - Focus on impact and results
            - Match language tone to job posting
            - Optimize for ATS scanning
            
            Generate the tailored resume content in a clear, professional format.
            """, 
            masterResume,
            String.join(", ", jobAnalysis.getRequiredSkills()),
            String.join(", ", jobAnalysis.getKeywordsPrimary()),
            jobAnalysis.getExperienceLevel(),
            request.getTargetLength(),
            request.getTargetLength(),
            getSectionPriorities(request)
        );
    }

    private String buildChatPrompt(String message, String context, String resumeContent, String jobContent) {
        return String.format("""
            You are an expert resume and career advisor. The user is asking for help with their resume.
            
            CONTEXT: %s
            
            USER MESSAGE: %s
            
            CURRENT RESUME:
            %s
            
            JOB POSTING:
            %s
            
            Provide helpful, actionable advice. If the user is asking for specific changes, 
            provide clear instructions or examples. Be encouraging and professional.
            """, context, message, resumeContent, jobContent);
    }

    private String buildSuggestionsPrompt(String resumeContent, String jobContent, String suggestionType) {
        return String.format("""
            Analyze the resume and job posting to provide specific improvement suggestions.
            
            RESUME:
            %s
            
            JOB POSTING:
            %s
            
            SUGGESTION TYPE: %s
            
            Provide 3-5 specific, actionable suggestions for improvement. 
            For each suggestion, include:
            1. What to change (specific text if applicable)
            2. Why the change would help
            3. Priority level (High/Medium/Low)
            4. Expected impact on ATS and human reviewers
            
            Focus on concrete, implementable changes.
            """, resumeContent, jobContent, suggestionType);
    }

    private String buildLatexConversionPrompt(String resumeContent, GenerateResumeRequest request) {
        return String.format("""
            Convert the following resume content to Jake's LaTeX format:
            
            RESUME CONTENT:
            %s
            
            REQUIREMENTS:
            - Use Jake's LaTeX resume template format
            - Maintain professional formatting
            - Ensure proper spacing and margins
            - Target length: %d page(s)
            - Include all sections with proper LaTeX commands
            
            Generate clean, compilable LaTeX code that follows Jake's template structure.
            """, resumeContent, request.getTargetLength());
    }

    private JobAnalysisResponse parseJobAnalysisResponse(String aiResponse, String jobTitle, String companyName) {
        try {
            // Parse AI response and create JobAnalysisResponse
            // This is a simplified implementation - in practice, you'd want more robust parsing
            
            return JobAnalysisResponse.builder()
                .jobTitle(jobTitle)
                .companyName(companyName)
                .requiredSkills(extractListFromResponse(aiResponse, "required skills"))
                .preferredSkills(extractListFromResponse(aiResponse, "preferred skills"))
                .keywordsPrimary(extractListFromResponse(aiResponse, "keywords"))
                .experienceLevel(extractExperienceLevel(aiResponse))
                .resumeOptimizationTips(extractListFromResponse(aiResponse, "optimization tips"))
                .overallMatchPotential(0.8) // Default value - could be calculated
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing job analysis response", e);
            // Return a basic response with available information
            return JobAnalysisResponse.builder()
                .jobTitle(jobTitle)
                .companyName(companyName)
                .requiredSkills(List.of())
                .preferredSkills(List.of())
                .keywordsPrimary(List.of())
                .experienceLevel("MID")
                .overallMatchPotential(0.5)
                .build();
        }
    }

    private List<String> extractListFromResponse(String response, String category) {
        // Simplified extraction - in practice, use more sophisticated parsing
        return List.of("Java", "Spring Boot", "React", "TypeScript"); // Placeholder
    }

    private String extractExperienceLevel(String response) {
        // Simplified extraction
        return "MID"; // Placeholder
    }

    private String getSectionPriorities(GenerateResumeRequest request) {
        List<String> priorities = new ArrayList<>();
        if (request.isIncludeExperience()) priorities.add("Experience");
        if (request.isIncludeEducation()) priorities.add("Education");
        if (request.isIncludeProjects()) priorities.add("Projects");
        if (request.isIncludeSkills()) priorities.add("Skills");
        if (request.isIncludeLeadership()) priorities.add("Leadership");
        return String.join(", ", priorities);
    }
}
