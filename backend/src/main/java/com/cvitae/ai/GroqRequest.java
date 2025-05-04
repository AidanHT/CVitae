package com.cvitae.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Request object for Groq API calls with structured prompts and parameters
 */
@Data
@Builder
public class GroqRequest {
    
    private String systemPrompt;
    private String userPrompt;
    
    @Builder.Default
    private Integer maxTokens = 2000;
    
    @Builder.Default
    private Double temperature = 0.7;
    
    @Builder.Default
    private Double topP = 1.0;
    
    @Builder.Default
    private Integer presencePenalty = 0;
    
    @Builder.Default
    private Integer frequencyPenalty = 0;
    
    /**
     * Create a request for job analysis
     */
    public static GroqRequest forJobAnalysis(String jobPosting, String jobTitle, String companyName) {
        String systemPrompt = """
            You are an expert job market analyst and ATS specialist. Analyze job postings to extract 
            key requirements, skills, and optimization opportunities for resume tailoring.
            """;
            
        String userPrompt = String.format("""
            Analyze this job posting and provide structured output:
            
            Job Title: %s
            Company: %s
            
            Job Posting:
            %s
            
            Provide analysis in the following structure:
            
            REQUIRED_SKILLS:
            - [List technical and soft skills explicitly required]
            
            PREFERRED_SKILLS:
            - [List preferred qualifications and nice-to-have skills]
            
            KEY_KEYWORDS:
            - [Important keywords for ATS optimization]
            
            EXPERIENCE_LEVEL:
            [ENTRY/MID/SENIOR/EXECUTIVE]
            
            RESPONSIBILITIES:
            - [Key job responsibilities and duties]
            
            COMPANY_CULTURE:
            - [Indicators of company culture and values]
            
            OPTIMIZATION_TIPS:
            - [Specific recommendations for resume optimization]
            """, jobTitle, companyName, jobPosting);
            
        return GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(3000)
            .temperature(0.3)
            .build();
    }
    
    /**
     * Create a request for resume tailoring
     */
    public static GroqRequest forResumeTailoring(String masterResume, String jobAnalysis, String preferences) {
        String systemPrompt = """
            You are an expert resume writer with deep knowledge of ATS systems and hiring practices. 
            Create tailored resumes that highlight relevant experiences and optimize for job requirements 
            while maintaining truthfulness and professional quality.
            """;
            
        String userPrompt = String.format("""
            Create a tailored resume based on the following inputs:
            
            MASTER RESUME:
            %s
            
            JOB ANALYSIS:
            %s
            
            TAILORING PREFERENCES:
            %s
            
            INSTRUCTIONS:
            1. Select and prioritize experiences most relevant to the job
            2. Optimize descriptions with job-relevant keywords
            3. Quantify achievements where possible
            4. Maintain ATS-friendly formatting
            5. Ensure all information is truthful and from the master resume
            6. Use strong action verbs and professional language
            
            Provide the tailored resume content with clear section headers.
            """, masterResume, jobAnalysis, preferences);
            
        return GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(4000)
            .temperature(0.4)
            .build();
    }
    
    /**
     * Create a request for LaTeX conversion
     */
    public static GroqRequest forLatexConversion(String resumeContent, String targetLength) {
        String systemPrompt = """
            You are an expert LaTeX formatter specializing in Jake's resume template. Convert resume 
            content to properly formatted LaTeX code that compiles cleanly and looks professional.
            """;
            
        String userPrompt = String.format("""
            Convert the following resume content to Jake's LaTeX format:
            
            RESUME CONTENT:
            %s
            
            TARGET LENGTH: %s page(s)
            
            Requirements:
            1. Use Jake's LaTeX resume template structure
            2. Include all necessary packages and formatting
            3. Maintain professional spacing and layout
            4. Ensure proper LaTeX syntax and compilation
            5. Optimize for the target page length
            
            Provide complete, compilable LaTeX code.
            """, resumeContent, targetLength);
            
        return GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(4000)
            .temperature(0.2)
            .build();
    }
    
    /**
     * Create a request for chat assistance
     */
    public static GroqRequest forChatAssistance(String message, String context, String resumeContent, String jobContent) {
        String systemPrompt = """
            You are a friendly and professional AI resume advisor. Help users improve their resumes 
            and job applications with specific, actionable advice. Be encouraging and provide concrete examples.
            """;
            
        String userPrompt = String.format("""
            Context: %s
            User Message: %s
            
            Current Resume: %s
            
            Job Posting: %s
            
            Provide helpful, specific advice to the user's question or request.
            """, context, message, resumeContent, jobContent);
            
        return GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(1500)
            .temperature(0.7)
            .build();
    }
}
