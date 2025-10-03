package com.cvitae.ai;

import com.cvitae.dto.GenerateResumeRequest;
import com.cvitae.dto.JobAnalysisResponse;
import com.cvitae.dto.ResumeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cvitae.service.LatexTemplateService;
import com.cvitae.service.LatexResumeBuilder;

/**
 * Service for orchestrating AI-powered resume tailoring workflow
 * Handles job analysis, content optimization, and LaTeX generation
 * 
 * NEW APPROACH: Uses structured data extraction + programmatic template filling
 * to ensure perfect LaTeX output every time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeTailorService {

    private final GroqClient groqClient;
    private final LatexTemplateService latexTemplateService;
    private final LatexResumeBuilder latexResumeBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Complete resume tailoring workflow:
     * 1. Analyze job posting
     * 2. Tailor resume content
     * 3. Generate LaTeX code
     */
    public ResumeTailoringResult tailorResume(String masterResume, String jobPosting, GenerateResumeRequest request) {
        log.info("Starting complete resume tailoring for job: {}", request.getJobTitle());

        try {
            // Step 1: Analyze job posting
            JobAnalysisResponse jobAnalysis = analyzeJobPosting(jobPosting, request.getJobTitle(), request.getCompanyName());
            
            // Step 2: Generate tailored resume content
            String tailoredContent = generateTailoredContent(masterResume, jobAnalysis, request);
            
            // Step 3: Convert to LaTeX
            String latexCode = convertToLatex(tailoredContent, request);
            
            // Step 4: Calculate ATS compatibility score
            double atsScore = calculateATSCompatibilityScore(tailoredContent, jobAnalysis);

            return ResumeTailoringResult.builder()
                .jobAnalysis(jobAnalysis)
                .tailoredContent(tailoredContent)
                .latexCode(latexCode)
                .atsCompatibilityScore(atsScore)
                .processingNotes(generateProcessingNotes(jobAnalysis, atsScore))
                .success(true)
                .build();

        } catch (Exception e) {
            log.error("Error in resume tailoring workflow", e);
            return ResumeTailoringResult.builder()
                .success(false)
                .errorMessage("Resume tailoring failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * Analyze job posting and extract structured requirements
     */
    public JobAnalysisResponse analyzeJobPosting(String jobPosting, String jobTitle, String companyName) {
        log.info("Analyzing job posting for: {} at {}", jobTitle, companyName);

        GroqRequest request = GroqRequest.forJobAnalysis(jobPosting, jobTitle, companyName);
        GroqResponse response = groqClient.callGroqAPI(request);

        if (response.isSuccess()) {
            return parseJobAnalysisFromAI(response.getContent(), jobTitle, companyName);
        } else {
            log.warn("Job analysis failed, using fallback: {}", response.getErrorMessage());
            return createFallbackJobAnalysis(jobTitle, companyName, jobPosting);
        }
    }

    /**
     * Generate tailored resume content optimized for the job
     */
    public String generateTailoredContent(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request) {
        log.info("Generating tailored resume content");

        // Build preferences string
        String preferences = buildPreferencesString(request);
        
        // Create structured job analysis for the prompt
        String jobAnalysisText = formatJobAnalysisForPrompt(jobAnalysis);

        GroqRequest groqRequest = GroqRequest.forResumeTailoring(masterResume, jobAnalysisText, preferences);
        GroqResponse response = groqClient.callGroqAPI(groqRequest);

        if (response.isSuccess()) {
            return response.getContent();
        } else {
            log.warn("Resume tailoring failed, using enhanced fallback: {}", response.getErrorMessage());
            return generateEnhancedFallback(masterResume, jobAnalysis, request);
        }
    }

    /**
     * Convert tailored content to Jake's LaTeX format using the NEW structured approach:
     * 1. Extract resume data as JSON using AI
     * 2. Programmatically build LaTeX from structured data
     * 
     * This ensures perfect LaTeX syntax every time.
     */
    public String convertToLatex(String resumeContent, GenerateResumeRequest request) {
        log.info("=== CONVERT TO LATEX (STRUCTURED APPROACH) ===");
        log.info("Converting resume to LaTeX format using extraction + builder");
        log.info("Resume content length: {}", resumeContent != null ? resumeContent.length() : "null");
        log.info("Target length: {}", request.getTargetLength());

        try {
            // Step 1: Extract structured data from resume using AI
            ResumeData resumeData = extractResumeData(resumeContent, request);
            
            if (resumeData != null) {
                log.info("Successfully extracted resume data, building LaTeX");
                
                // Step 2: Build LaTeX programmatically from structured data
                String latexCode = latexResumeBuilder.buildResume(resumeData);
                log.info("LaTeX built successfully, length: {}", latexCode.length());
                
                return latexCode;
            } else {
                log.warn("Resume data extraction returned null, falling back to old method");
                return convertToLatexLegacy(resumeContent, request);
            }
            
        } catch (Exception e) {
            log.error("Error in structured LaTeX conversion: {}", e.getMessage(), e);
            log.warn("Falling back to legacy conversion method");
            return convertToLatexLegacy(resumeContent, request);
        }
    }
    
    /**
     * Extract structured resume data from raw text using AI.
     * The AI outputs JSON which is then parsed into ResumeData.
     */
    private ResumeData extractResumeData(String resumeContent, GenerateResumeRequest request) {
        log.info("Extracting structured resume data using AI");
        
        try {
            // Create extraction request
            GroqRequest extractionRequest = GroqRequest.forResumeExtraction(
                resumeContent,
                request.getJobPosting(),
                request.getJobTitle(),
                request.getCompanyName()
            );
            
            // Call AI to extract data
            GroqResponse response = groqClient.callGroqAPI(extractionRequest);
            
            if (!response.isSuccess()) {
                log.error("AI extraction failed: {}", response.getErrorMessage());
                return null;
            }
            
            String jsonContent = response.getContent();
            log.debug("AI extraction response: {}", jsonContent);
            
            // Clean up the JSON response (remove any markdown formatting)
            jsonContent = cleanJsonResponse(jsonContent);
            
            // Parse JSON into ResumeData
            ResumeData data = objectMapper.readValue(jsonContent, ResumeData.class);
            log.info("Successfully parsed resume data: name={}, education={}, experience={}, projects={}",
                    data.getName(),
                    data.getEducation() != null ? data.getEducation().size() : 0,
                    data.getExperience() != null ? data.getExperience().size() : 0,
                    data.getProjects() != null ? data.getProjects().size() : 0);
            
            return data;
            
        } catch (Exception e) {
            log.error("Error extracting resume data: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Clean JSON response from AI (remove markdown code blocks, etc.)
     */
    private String cleanJsonResponse(String jsonContent) {
        if (jsonContent == null) return null;
        
        String cleaned = jsonContent.trim();
        
        // Remove markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        // Find the JSON object boundaries
        int startBrace = cleaned.indexOf('{');
        int endBrace = cleaned.lastIndexOf('}');
        
        if (startBrace != -1 && endBrace != -1 && endBrace > startBrace) {
            cleaned = cleaned.substring(startBrace, endBrace + 1);
        }
        
        return cleaned.trim();
    }
    
    /**
     * Legacy conversion method - used as fallback when structured approach fails
     */
    private String convertToLatexLegacy(String resumeContent, GenerateResumeRequest request) {
        log.info("Using legacy LaTeX conversion method");
        
        GroqRequest groqRequest = GroqRequest.forLatexConversion(resumeContent, String.valueOf(request.getTargetLength()));
        GroqResponse response = groqClient.callGroqAPI(groqRequest);

        if (response.isSuccess()) {
            log.info("Legacy LaTeX conversion successful");
            return response.getContent();
        } else {
            log.error("Legacy LaTeX conversion failed: {}", response.getErrorMessage());
            return generateLatexTemplate(resumeContent, request);
        }
    }

    /**
     * Calculate ATS compatibility score based on keyword matching and formatting
     */
    public double calculateATSCompatibilityScore(String resumeContent, JobAnalysisResponse jobAnalysis) {
        if (resumeContent == null || resumeContent.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        String lowerResume = resumeContent.toLowerCase();

        // Check for required skills (40% of score)
        List<String> requiredSkills = jobAnalysis.getRequiredSkills();
        if (requiredSkills != null && !requiredSkills.isEmpty()) {
            int matchedRequired = 0;
            for (String skill : requiredSkills) {
                if (lowerResume.contains(skill.toLowerCase())) {
                    matchedRequired++;
                }
            }
            score += (double) matchedRequired / requiredSkills.size() * 0.4;
        }

        // Check for preferred skills (20% of score)
        List<String> preferredSkills = jobAnalysis.getPreferredSkills();
        if (preferredSkills != null && !preferredSkills.isEmpty()) {
            int matchedPreferred = 0;
            for (String skill : preferredSkills) {
                if (lowerResume.contains(skill.toLowerCase())) {
                    matchedPreferred++;
                }
            }
            score += (double) matchedPreferred / preferredSkills.size() * 0.2;
        }

        // Check for primary keywords (25% of score)
        List<String> primaryKeywords = jobAnalysis.getKeywordsPrimary();
        if (primaryKeywords != null && !primaryKeywords.isEmpty()) {
            int matchedKeywords = 0;
            for (String keyword : primaryKeywords) {
                if (lowerResume.contains(keyword.toLowerCase())) {
                    matchedKeywords++;
                }
            }
            score += (double) matchedKeywords / primaryKeywords.size() * 0.25;
        }

        // Check for quantified achievements (15% of score)
        score += calculateQuantificationScore(resumeContent) * 0.15;

        return Math.min(1.0, score);
    }

    /**
     * Parse AI response into structured JobAnalysisResponse
     */
    private JobAnalysisResponse parseJobAnalysisFromAI(String aiResponse, String jobTitle, String companyName) {
        try {
            return JobAnalysisResponse.builder()
                .jobTitle(jobTitle)
                .companyName(companyName)
                .requiredSkills(extractSection(aiResponse, "REQUIRED_SKILLS"))
                .preferredSkills(extractSection(aiResponse, "PREFERRED_SKILLS"))
                .keywordsPrimary(extractSection(aiResponse, "KEY_KEYWORDS"))
                .experienceLevel(extractSingleValue(aiResponse, "EXPERIENCE_LEVEL", "MID"))
                .resumeOptimizationTips(extractSection(aiResponse, "OPTIMIZATION_TIPS"))
                .overallMatchPotential(0.7) // Default match potential
                .build();
        } catch (Exception e) {
            log.warn("Error parsing AI job analysis, using fallback", e);
            return createFallbackJobAnalysis(jobTitle, companyName, aiResponse);
        }
    }

    /**
     * Extract a section of items from AI response
     */
    private List<String> extractSection(String response, String sectionName) {
        List<String> items = new ArrayList<>();
        
        try {
            Pattern sectionPattern = Pattern.compile(sectionName + ":\\s*([\\s\\S]*?)(?=^[A-Z_]+:|$)", Pattern.MULTILINE);
            Matcher matcher = sectionPattern.matcher(response);
            
            if (matcher.find()) {
                String sectionContent = matcher.group(1).trim();
                String[] lines = sectionContent.split("\n");
                
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                        String item = line.replaceFirst("^[-•*]\\s*", "").trim();
                        if (!item.isEmpty() && item.length() > 2) {
                            items.add(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting section {}: {}", sectionName, e.getMessage());
        }
        
        // Fallback extraction if structured parsing fails
        if (items.isEmpty()) {
            items = extractByKeywordSearch(response, sectionName);
        }
        
        return items;
    }

    /**
     * Extract a single value from AI response
     */
    private String extractSingleValue(String response, String key, String fallback) {
        try {
            Pattern pattern = Pattern.compile(key + ":\\s*([A-Z]+)");
            Matcher matcher = pattern.matcher(response);
            
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.warn("Error extracting {}: {}", key, e.getMessage());
        }
        
        return fallback;
    }

    /**
     * Fallback extraction using keyword search
     */
    private List<String> extractByKeywordSearch(String response, String sectionName) {
        List<String> items = new ArrayList<>();
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("•") || line.startsWith("*")) {
                String item = line.replaceFirst("^[-•*]\\s*", "").trim();
                if (!item.isEmpty() && item.length() > 2) {
                    items.add(item);
                }
            }
        }
        
        // If still no items, provide defaults based on section
        if (items.isEmpty()) {
            items = getDefaultItemsForSection(sectionName);
        }
        
        return items;
    }

    /**
     * Get default items for a section when extraction fails
     */
    private List<String> getDefaultItemsForSection(String sectionName) {
        return switch (sectionName) {
            case "REQUIRED_SKILLS" -> List.of("Communication", "Problem-solving", "Teamwork", "Attention to detail");
            case "PREFERRED_SKILLS" -> List.of("Leadership", "Project management", "Time management");
            case "KEY_KEYWORDS" -> List.of("Professional", "Experienced", "Results-driven", "Collaborative");
            case "OPTIMIZATION_TIPS" -> List.of("Include relevant keywords", "Quantify achievements", "Use action verbs");
            default -> List.of();
        };
    }

    /**
     * Create fallback job analysis when AI processing fails
     */
    private JobAnalysisResponse createFallbackJobAnalysis(String jobTitle, String companyName, String jobPosting) {
        return JobAnalysisResponse.builder()
            .jobTitle(jobTitle)
            .companyName(companyName)
            .requiredSkills(extractBasicSkills(jobPosting))
            .preferredSkills(List.of("Leadership", "Project Management", "Communication"))
            .keywordsPrimary(extractBasicKeywords(jobTitle, companyName))
            .experienceLevel("MID")
            .resumeOptimizationTips(List.of("Match job keywords", "Quantify achievements", "Use action verbs"))
            .overallMatchPotential(0.5)
            .build();
    }

    /**
     * Extract basic skills from job posting using simple keyword matching
     */
    private List<String> extractBasicSkills(String jobPosting) {
        List<String> skills = new ArrayList<>();
        String[] commonSkills = {
            "communication", "leadership", "problem-solving", "teamwork", "project management",
            "analytical", "creative", "detail-oriented", "organizational", "time management"
        };
        
        String lowerJobPosting = jobPosting.toLowerCase();
        for (String skill : commonSkills) {
            if (lowerJobPosting.contains(skill)) {
                skills.add(Character.toUpperCase(skill.charAt(0)) + skill.substring(1));
            }
        }
        
        return skills.isEmpty() ? List.of("Communication", "Problem-solving", "Teamwork") : skills;
    }

    /**
     * Extract basic keywords from job title and company
     */
    private List<String> extractBasicKeywords(String jobTitle, String companyName) {
        List<String> keywords = new ArrayList<>();
        keywords.add(jobTitle.toLowerCase());
        keywords.add(companyName.toLowerCase());
        keywords.addAll(List.of("professional", "experienced", "skilled", "qualified"));
        return keywords;
    }

    /**
     * Build preferences string for AI prompt
     */
    private String buildPreferencesString(GenerateResumeRequest request) {
        StringBuilder preferences = new StringBuilder();
        preferences.append("Target Length: ").append(request.getTargetLength()).append(" page(s)\n");
        preferences.append("Include Experience: ").append(request.isIncludeExperience()).append("\n");
        preferences.append("Include Education: ").append(request.isIncludeEducation()).append("\n");
        preferences.append("Include Projects: ").append(request.isIncludeProjects()).append("\n");
        preferences.append("Include Skills: ").append(request.isIncludeSkills()).append("\n");
        preferences.append("Include Leadership: ").append(request.isIncludeLeadership()).append("\n");
        return preferences.toString();
    }

    /**
     * Format job analysis for AI prompt
     */
    private String formatJobAnalysisForPrompt(JobAnalysisResponse jobAnalysis) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Required Skills: ").append(String.join(", ", jobAnalysis.getRequiredSkills())).append("\n");
        analysis.append("Preferred Skills: ").append(String.join(", ", jobAnalysis.getPreferredSkills())).append("\n");
        analysis.append("Key Keywords: ").append(String.join(", ", jobAnalysis.getKeywordsPrimary())).append("\n");
        analysis.append("Experience Level: ").append(jobAnalysis.getExperienceLevel()).append("\n");
        analysis.append("Optimization Tips: ").append(String.join(", ", jobAnalysis.getResumeOptimizationTips()));
        return analysis.toString();
    }

    /**
     * Generate enhanced fallback when AI tailoring fails
     */
    private String generateEnhancedFallback(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request) {
        // Basic enhancement: add job-relevant keywords to master resume
        String enhanced = masterResume;
        
        // Add a note about the optimization
        String optimizationNote = String.format("""
            
            [RESUME OPTIMIZED FOR: %s at %s]
            This resume has been optimized for ATS compatibility and job requirements.
            Key skills highlighted: %s
            """, 
            jobAnalysis.getJobTitle(), 
            jobAnalysis.getCompanyName(),
            String.join(", ", jobAnalysis.getRequiredSkills().subList(0, Math.min(3, jobAnalysis.getRequiredSkills().size())))
        );
        
        return enhanced + optimizationNote;
    }

    /**
     * Generate LaTeX template using centralized LatexTemplateService when AI conversion fails
     */
    private String generateLatexTemplate(String resumeContent, GenerateResumeRequest request) {
        log.warn("Using fallback LaTeX template for target length: {} page(s)", request.getTargetLength());
        
        // Escape special characters and wrap in template
        String escapedContent = latexTemplateService.escapeLatex(resumeContent);
        
        String fallbackBody = String.format("""
            %% Target length: %d page(s)
            %% AI service unavailable - using escaped content
            
            \\begin{center}
                \\textbf{\\huge Resume Content}\\\\
                \\vspace{5pt}
                \\textit{Please regenerate for proper formatting}
            \\end{center}
            
            \\section{CONTENT}
            \\begin{itemize}[leftmargin=0.15in, label={}]
                \\item %s
            \\end{itemize}
            """, request.getTargetLength(), escapedContent);
            
        return latexTemplateService.wrapInTemplate(fallbackBody);
    }

    /**
     * Calculate quantification score based on numbers and metrics in resume
     */
    private double calculateQuantificationScore(String resumeContent) {
        // Look for patterns like "increased by 25%", "managed $1M budget", "led team of 5", etc.
        Pattern[] quantificationPatterns = {
            Pattern.compile("\\d+%"),                    // Percentages
            Pattern.compile("\\$\\d+[KkMmBb]?"),         // Money amounts
            Pattern.compile("\\d+\\s*(years?|months?)"), // Time periods
            Pattern.compile("\\d+\\s*(people|members|employees|clients)"), // Team/people sizes
            Pattern.compile("(increased|decreased|improved|reduced)\\s+by\\s+\\d+"), // Improvements
        };
        
        int quantificationCount = 0;
        for (Pattern pattern : quantificationPatterns) {
            Matcher matcher = pattern.matcher(resumeContent.toLowerCase());
            while (matcher.find()) {
                quantificationCount++;
            }
        }
        
        // Normalize score: assume good resume has 5-10 quantifications
        return Math.min(1.0, quantificationCount / 7.0);
    }

    /**
     * Generate processing notes for the user
     */
    private List<String> generateProcessingNotes(JobAnalysisResponse jobAnalysis, double atsScore) {
        List<String> notes = new ArrayList<>();
        
        if (atsScore >= 0.8) {
            notes.add("Excellent ATS compatibility score! Your resume is well-optimized for this position.");
        } else if (atsScore >= 0.6) {
            notes.add("Good ATS compatibility score. Consider adding more job-specific keywords for better optimization.");
        } else {
            notes.add("ATS compatibility could be improved. Try including more keywords from the job posting.");
        }
        
        if (jobAnalysis.getRequiredSkills().size() > 8) {
            notes.add("This position has many requirements. Focus on highlighting your strongest matching skills.");
        }
        
        if ("SENIOR".equals(jobAnalysis.getExperienceLevel()) || "EXECUTIVE".equals(jobAnalysis.getExperienceLevel())) {
            notes.add("This is a senior-level position. Emphasize leadership and strategic accomplishments.");
        }
        
        return notes;
    }
}
