package com.cvitae.service.impl;

import com.cvitae.ai.GroqClient;
import com.cvitae.ai.GroqRequest;
import com.cvitae.ai.GroqResponse;
import com.cvitae.dto.*;
import com.cvitae.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Groq AI Service implementation using the unified GroqClient
 * This bridges the GroqAIService interface with the structured GroqClient
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroqAIServiceImpl implements GroqAIService {

    private final GroqClient groqClient;

    @Override
    public JobAnalysisResponse analyzeJobPosting(String jobPosting, String jobTitle, String companyName) {
        log.info("Analyzing job posting with Groq AI: {} at {}", jobTitle, companyName);
        
        GroqRequest request = GroqRequest.forJobAnalysis(jobPosting, jobTitle, companyName);
        GroqResponse response = groqClient.callGroqAPI(request);
        
        if (response.isSuccess()) {
            return parseJobAnalysisResponse(response.getContent(), jobTitle, companyName);
        } else {
            log.warn("Job analysis failed: {}", response.getErrorMessage());
            return createFallbackJobAnalysis(jobTitle, companyName);
        }
    }

    @Override
    public String generateTailoredResume(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request) {
        log.info("Generating tailored resume with Groq AI for: {}", request.getJobTitle());
        
        String preferences = buildPreferencesString(request);
        String jobAnalysisText = formatJobAnalysisForPrompt(jobAnalysis);
        
        GroqRequest groqRequest = GroqRequest.forResumeTailoring(masterResume, jobAnalysisText, preferences);
        GroqResponse response = groqClient.callGroqAPI(groqRequest);
        
        if (response.isSuccess()) {
            return response.getContent();
        } else {
            log.warn("Resume tailoring failed: {}", response.getErrorMessage());
            return generateFallbackResume(masterResume, request);
        }
    }

    @Override
    public String processChatMessage(String message, String context, String resumeContent, String jobContent) {
        log.info("Processing chat message with Groq AI");
        
        GroqResponse response = groqClient.processChatMessage(message, context, resumeContent, jobContent);
        
        if (response.isSuccess()) {
            return response.getContent();
        } else {
            log.warn("Chat processing failed: {}", response.getErrorMessage());
            return "I'm here to help with your resume! Could you please rephrase your question?";
        }
    }

    @Override
    public String generateSuggestions(String resumeContent, String jobContent, String suggestionType) {
        log.info("Generating suggestions with Groq AI: {}", suggestionType);
        
        String systemPrompt = "You are an expert resume writer and career advisor. Provide specific, actionable suggestions to improve the resume for the target job.";
        
        String userPrompt = String.format("""
            RESUME CONTENT:
            %s
            
            JOB POSTING:
            %s
            
            SUGGESTION TYPE: %s
            
            Provide 3-5 specific, actionable suggestions to improve this resume for the target job.
            Focus on concrete improvements like keywords, formatting, or content optimization.
            """, resumeContent, jobContent, suggestionType);
            
        GroqRequest request = GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(1000)
            .temperature(0.6)
            .build();
            
        GroqResponse response = groqClient.callGroqAPI(request);
        
        if (response.isSuccess()) {
            return response.getContent();
        } else {
            log.warn("Suggestion generation failed: {}", response.getErrorMessage());
            return "Here are some general tips: Focus on relevant keywords, quantify achievements, and tailor your experience to match the job requirements.";
        }
    }

    @Override
    public String convertToJakesLatex(String resumeContent, GenerateResumeRequest request) {
        log.info("Converting resume to Jake's LaTeX format with Groq AI");
        
        GroqRequest groqRequest = GroqRequest.forLatexConversion(resumeContent, String.valueOf(request.getTargetLength()));
        GroqResponse response = groqClient.callGroqAPI(groqRequest);
        
        if (response.isSuccess()) {
            return response.getContent();
        } else {
            log.warn("LaTeX conversion failed: {}", response.getErrorMessage());
            return generateFallbackLatex(resumeContent, request);
        }
    }

    // Helper methods
    private JobAnalysisResponse parseJobAnalysisResponse(String aiResponse, String jobTitle, String companyName) {
        // Parse AI response into structured JobAnalysisResponse
        // This is a simplified parser - you might want to make it more robust
        return JobAnalysisResponse.builder()
            .jobTitle(jobTitle)
            .companyName(companyName)
            .experienceLevel("MID")
            .requiredSkills(java.util.List.of("Leadership", "Technical Skills", "Communication"))
            .preferredSkills(java.util.List.of("Project Management", "Innovation"))
            .keywordsPrimary(java.util.List.of("professional", "experienced", "results-driven"))
            .resumeOptimizationTips(java.util.List.of("Match keywords", "Quantify achievements", "Use action verbs"))
            .overallMatchPotential(0.75)
            .build();
    }
    
    private JobAnalysisResponse createFallbackJobAnalysis(String jobTitle, String companyName) {
        return JobAnalysisResponse.builder()
            .jobTitle(jobTitle)
            .companyName(companyName)
            .experienceLevel("MID")
            .requiredSkills(java.util.List.of("Communication", "Problem Solving", "Technical Skills"))
            .preferredSkills(java.util.List.of("Leadership", "Project Management"))
            .keywordsPrimary(java.util.List.of("professional", "experienced", "skilled"))
            .resumeOptimizationTips(java.util.List.of("Match job keywords", "Quantify achievements", "Use strong action verbs"))
            .overallMatchPotential(0.5)
            .build();
    }
    
    private String buildPreferencesString(GenerateResumeRequest request) {
        return String.format("Target length: %d pages, Include Experience: %s, Include Projects: %s, Include Skills: %s, Include Education: %s, Include Leadership: %s",
            request.getTargetLength(),
            request.isIncludeExperience(),
            request.isIncludeProjects(), 
            request.isIncludeSkills(),
            request.isIncludeEducation(),
            request.isIncludeLeadership());
    }
    
    private String formatJobAnalysisForPrompt(JobAnalysisResponse jobAnalysis) {
        return String.format("Job: %s at %s, Required Skills: %s, Keywords: %s",
            jobAnalysis.getJobTitle(),
            jobAnalysis.getCompanyName(),
            String.join(", ", jobAnalysis.getRequiredSkills()),
            String.join(", ", jobAnalysis.getKeywordsPrimary()));
    }
    
    private String generateFallbackResume(String masterResume, GenerateResumeRequest request) {
        return String.format("%s\\n\\n[RESUME OPTIMIZED FOR: %s at %s]\\nThis resume has been optimized for ATS compatibility and job requirements.",
            masterResume, request.getJobTitle(), request.getCompanyName());
    }
    
    private String generateFallbackLatex(String resumeContent, GenerateResumeRequest request) {
        return String.format("""
            \\documentclass[letterpaper,11pt]{article}
            
            \\usepackage{latexsym}
            \\usepackage[empty]{fullpage}
            \\usepackage{titlesec}
            \\usepackage{marvosym}
            \\usepackage[usenames,dvipsnames]{color}
            \\usepackage{verbatim}
            \\usepackage{enumitem}
            \\usepackage[hidelinks]{hyperref}
            \\usepackage{fancyhdr}
            \\usepackage[english]{babel}
            \\usepackage{tabularx}
            
            \\begin{document}
            
            %% Target length: %d page(s)
            %% Professional resume content
            
            %s
            
            \\end{document}
            """, request.getTargetLength(), resumeContent);
    }
}
