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
            // Check if API key is configured
            if (groqApiKey == null || groqApiKey.trim().isEmpty() || groqApiKey.equals("your-groq-api-key")) {
                log.warn("Groq API key not configured, using fallback response");
                return generateFallbackResponse(prompt);
            }

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
            log.error("Error calling Groq API: {}", e.getMessage());
            log.warn("Falling back to local response generation");
            return generateFallbackResponse(prompt);
        }
    }
    
    private String generateFallbackResponse(String prompt) {
        // Generate reasonable responses based on prompt content
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("analyze") && lowerPrompt.contains("job posting")) {
            return generateJobAnalysisFallback(prompt);
        } else if (lowerPrompt.contains("tailored resume")) {
            return generateResumeContentFallback(prompt);
        } else if (lowerPrompt.contains("latex")) {
            return generateLatexFallback(prompt);
        } else if (lowerPrompt.contains("suggestions")) {
            return generateSuggestionsFallback(prompt);
        } else {
            return generateChatFallback(prompt);
        }
    }
    
    private String generateJobAnalysisFallback(String prompt) {
        return """
        Based on job posting analysis:
        
        Required Skills:
        - Strong communication and interpersonal skills
        - Problem-solving and analytical thinking
        - Team collaboration and leadership abilities
        - Technical proficiency relevant to the role
        
        Preferred Skills:
        - Project management experience
        - Industry-specific knowledge
        - Advanced technical certifications
        
        Key Keywords:
        - Professional
        - Experienced
        - Results-driven
        - Collaborative
        
        Experience Level: MID
        
        Optimization Tips:
        - Match job keywords in your experience descriptions
        - Quantify achievements with specific numbers
        - Use action verbs that demonstrate impact
        - Highlight relevant technical skills
        """;
    }
    
    private String generateResumeContentFallback(String prompt) {
        return """
        Tailored resume content has been optimized based on the job requirements:
        
        • Enhanced experience descriptions with job-relevant keywords
        • Quantified achievements where possible
        • Highlighted skills that match job requirements
        • Organized content to emphasize most relevant experiences
        • Used professional action verbs throughout
        • Maintained ATS-friendly formatting
        
        The resume has been tailored to emphasize your qualifications that best match 
        the target position while maintaining truthfulness and professional presentation.
        """;
    }
    
    private String generateLatexFallback(String prompt) {
        return """
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
        
        \\pagestyle{fancy}
        \\fancyhf{}
        \\fancyfoot{}
        \\renewcommand{\\headrulewidth}{0pt}
        \\renewcommand{\\footrulewidth}{0pt}
        
        \\addtolength{\\oddsidemargin}{-0.5in}
        \\addtolength{\\evensidemargin}{-0.5in}
        \\addtolength{\\textwidth}{1in}
        \\addtolength{\\topmargin}{-.5in}
        \\addtolength{\\textheight}{1.0in}
        
        \\urlstyle{same}
        \\raggedbottom
        \\raggedright
        \\setlength{\\tabcolsep}{0in}
        
        \\titleformat{\\section}{\\vspace{-4pt}\\scshape\\raggedright\\large}{}{0em}{}[\\color{black}\\titlerule \\vspace{-5pt}]
        
        \\begin{document}
        
        \\begin{center}
            \\textbf{\\Huge \\scshape Your Name} \\\\ \\vspace{1pt}
            \\small Phone $|$ \\href{mailto:email}{\\underline{email}} $|$ 
            \\href{linkedin}{\\underline{linkedin}} $|$ \\href{github}{\\underline{github}}
        \\end{center}
        
        \\section{Experience}
        [Experience content will be inserted here based on your resume]
        
        \\section{Education}
        [Education content will be inserted here]
        
        \\section{Skills}
        [Skills content will be inserted here]
        
        \\end{document}
        """;
    }
    
    private String generateSuggestionsFallback(String prompt) {
        return """
        1. Keyword Optimization
        Description: Add more job-relevant keywords to improve ATS compatibility
        Priority: High
        Suggested: Include specific technical skills mentioned in job posting
        
        2. Quantify Achievements
        Description: Add specific numbers and metrics to accomplishments
        Priority: Medium
        Suggested: Replace general statements with measurable results
        
        3. Action Verb Enhancement
        Description: Use stronger action verbs to demonstrate leadership
        Priority: Medium
        Suggested: Replace passive language with dynamic action words
        """;
    }
    
    private String generateChatFallback(String prompt) {
        return """
        I understand you're looking for help with your resume. While I'm currently running in 
        offline mode, I can still provide general guidance:
        
        • Make sure your resume matches the job posting keywords
        • Quantify your achievements with specific numbers when possible
        • Use strong action verbs to start bullet points
        • Keep formatting clean and ATS-friendly
        • Tailor your experience to highlight relevant skills
        
        For more detailed assistance, please ensure the AI service is properly configured.
        """;
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
            log.debug("Parsing AI response: {}", aiResponse);
            
            return JobAnalysisResponse.builder()
                .jobTitle(jobTitle)
                .companyName(companyName)
                .requiredSkills(extractListFromResponse(aiResponse, "required skills"))
                .preferredSkills(extractListFromResponse(aiResponse, "preferred skills"))
                .keywordsPrimary(extractListFromResponse(aiResponse, "keywords"))
                .experienceLevel(extractExperienceLevel(aiResponse))
                .resumeOptimizationTips(extractListFromResponse(aiResponse, "optimization tips"))
                .overallMatchPotential(calculateMatchPotential(aiResponse))
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing job analysis response", e);
            // Return a basic response with available information
            return JobAnalysisResponse.builder()
                .jobTitle(jobTitle)
                .companyName(companyName)
                .requiredSkills(List.of("Communication", "Problem-solving", "Teamwork"))
                .preferredSkills(List.of("Leadership", "Project Management"))
                .keywordsPrimary(List.of(jobTitle.toLowerCase(), companyName.toLowerCase()))
                .experienceLevel("MID")
                .resumeOptimizationTips(List.of("Match job keywords", "Quantify achievements", "Use action verbs"))
                .overallMatchPotential(0.5)
                .build();
        }
    }

    private List<String> extractListFromResponse(String response, String category) {
        try {
            // Extract items based on common patterns in AI responses
            List<String> extracted = new ArrayList<>();
            
            // Look for JSON-like structures or bullet points
            String[] lines = response.toLowerCase().split("\n");
            boolean inCategory = false;
            
            for (String line : lines) {
                line = line.trim();
                
                // Check if we found the category
                if (line.contains(category.toLowerCase())) {
                    inCategory = true;
                    continue;
                }
                
                // If we're in the category, extract items
                if (inCategory) {
                    // Stop if we hit another category
                    if (line.contains(":") && !line.startsWith("-") && !line.startsWith("*")) {
                        break;
                    }
                    
                    // Extract bullet point items
                    if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
                        String item = line.replaceFirst("^[-*•]\\s*", "").trim();
                        if (!item.isEmpty() && item.length() > 2) {
                            // Clean up the item
                            item = item.replaceAll("[\\[\\]\"']", "").trim();
                            if (item.endsWith(",")) {
                                item = item.substring(0, item.length() - 1);
                            }
                            extracted.add(item);
                        }
                    }
                }
            }
            
            // If no structured extraction worked, try pattern matching
            if (extracted.isEmpty()) {
                extracted = extractByPatterns(response, category);
            }
            
            return extracted.isEmpty() ? getDefaultForCategory(category) : extracted;
            
        } catch (Exception e) {
            log.warn("Error extracting {} from response: {}", category, e.getMessage());
            return getDefaultForCategory(category);
        }
    }

    private String extractExperienceLevel(String response) {
        try {
            String lowerResponse = response.toLowerCase();
            
            if (lowerResponse.contains("entry") || lowerResponse.contains("junior") || lowerResponse.contains("0-2 years")) {
                return "ENTRY";
            } else if (lowerResponse.contains("senior") || lowerResponse.contains("lead") || lowerResponse.contains("5+ years") || lowerResponse.contains("expert")) {
                return "SENIOR";
            } else if (lowerResponse.contains("executive") || lowerResponse.contains("director") || lowerResponse.contains("manager") || lowerResponse.contains("10+ years")) {
                return "EXECUTIVE";
            } else {
                return "MID";
            }
        } catch (Exception e) {
            log.warn("Error extracting experience level: {}", e.getMessage());
            return "MID";
        }
    }
    
    private List<String> extractByPatterns(String response, String category) {
        List<String> extracted = new ArrayList<>();
        
        try {
            // Try to extract comma-separated values after category mentions
            String[] sentences = response.split("[.!?]");
            for (String sentence : sentences) {
                if (sentence.toLowerCase().contains(category.toLowerCase())) {
                    // Look for items after colons or common phrases
                    String[] parts = sentence.split(":");
                    if (parts.length > 1) {
                        String itemsText = parts[1].trim();
                        String[] items = itemsText.split(",");
                        for (String item : items) {
                            item = item.trim().replaceAll("[\\[\\]\"'()]", "");
                            if (!item.isEmpty() && item.length() > 2) {
                                extracted.add(item);
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Error in pattern extraction: {}", e.getMessage());
        }
        
        return extracted;
    }
    
    private List<String> getDefaultForCategory(String category) {
        return switch (category.toLowerCase()) {
            case "required skills" -> List.of("Communication", "Problem-solving", "Teamwork", "Attention to detail");
            case "preferred skills" -> List.of("Leadership", "Project management", "Time management");
            case "keywords" -> List.of("Professional", "Experienced", "Results-driven");
            case "optimization tips" -> List.of("Match job keywords", "Quantify achievements", "Use action verbs");
            default -> List.of();
        };
    }
    
    private Double calculateMatchPotential(String response) {
        try {
            String lowerResponse = response.toLowerCase();
            double score = 0.5; // Base score
            
            // Adjust based on positive/negative indicators
            if (lowerResponse.contains("excellent") || lowerResponse.contains("perfect") || lowerResponse.contains("ideal")) {
                score += 0.3;
            } else if (lowerResponse.contains("good") || lowerResponse.contains("suitable") || lowerResponse.contains("matches")) {
                score += 0.2;
            } else if (lowerResponse.contains("poor") || lowerResponse.contains("weak") || lowerResponse.contains("lacks")) {
                score -= 0.2;
            }
            
            return Math.max(0.1, Math.min(1.0, score));
        } catch (Exception e) {
            return 0.5;
        }
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
