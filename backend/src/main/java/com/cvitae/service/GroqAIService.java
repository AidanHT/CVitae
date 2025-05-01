package com.cvitae.service;

import com.cvitae.dto.*;

public interface GroqAIService {
    
    /**
     * Analyze job posting using Groq AI
     */
    JobAnalysisResponse analyzeJobPosting(String jobPosting, String jobTitle, String companyName);
    
    /**
     * Generate tailored resume using Groq AI
     */
    String generateTailoredResume(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request);
    
    /**
     * Process chat message using Groq AI
     */
    String processChatMessage(String message, String context, String resumeContent, String jobContent);
    
    /**
     * Generate suggestions using Groq AI
     */
    String generateSuggestions(String resumeContent, String jobContent, String suggestionType);
    
    /**
     * Convert resume content to Jake's LaTeX format
     */
    String convertToJakesLatex(String resumeContent, GenerateResumeRequest request);
}
