package com.cvitae.ai;

import com.cvitae.dto.GenerateResumeRequest;
import com.cvitae.dto.JobAnalysisResponse;

/**
 * Enhanced Groq AI service interface with better error handling and reliability
 */
public interface GroqAIService {
    
    /**
     * Analyze a job posting to extract requirements and keywords
     */
    JobAnalysisResponse analyzeJobPosting(String jobPosting, String jobTitle, String companyName);
    
    /**
     * Generate a tailored resume based on master resume and job analysis
     */
    String generateTailoredResume(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request);
    
    /**
     * Process a chat message for resume optimization advice
     */
    String processChatMessage(String message, String context, String resumeContent, String jobContent);
    
    /**
     * Check if the AI service is available and properly configured
     */
    boolean isServiceAvailable();
    
    /**
     * Get service health information
     */
    GroqAIHealthStatus getHealthStatus();
}
