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

            GroqResponse groqResponse = parseGroqResponse(response);
            
            // Apply LaTeX cleaning if this is a LaTeX generation request
            if (groqResponse.isSuccess() && isLatexGenerationRequest(request)) {
                String cleanedLatex = cleanLatexResponse(groqResponse.getContent());
                return GroqResponse.success(
                    cleanedLatex,
                    groqResponse.getTotalTokens(),
                    groqResponse.getPromptTokens(),
                    groqResponse.getCompletionTokens()
                );
            }
            
            return groqResponse;

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
            You are a STRATEGIC CAREER INTELLIGENCE ANALYST and ATS REVERSE-ENGINEERING EXPERT with:
            • 12+ years analyzing hiring patterns across 500+ companies
            • Advanced expertise in ATS algorithm behavior and keyword optimization
            • Deep understanding of recruiter psychology and decision-making processes
            • Proven track record of 3x interview rate improvements through strategic positioning
            
            ANALYTICAL FRAMEWORK:
            🔍 KEYWORD INTELLIGENCE: Identify hidden requirements and priority skills
            📊 COMPETITIVE ANALYSIS: Understand what separates top-tier candidates  
            🎯 ATS OPTIMIZATION: Reverse-engineer parsing algorithms for maximum visibility
            💼 STRATEGIC POSITIONING: Align candidate strengths with employer pain points
            🚀 IMPACT AMPLIFICATION: Transform responsibilities into quantified achievements
            
            ANALYTICAL METHODOLOGY:
            1. REQUIREMENT EXTRACTION: Parse explicit and implicit job requirements
            2. PRIORITY MAPPING: Identify must-have vs. nice-to-have qualifications
            3. KEYWORD ANALYSIS: Extract high-value terms and industry terminology
            4. COMPETITIVE POSITIONING: Determine unique value propositions
            5. GAP IDENTIFICATION: Highlight areas needing emphasis or de-emphasis
            6. STRATEGIC RECOMMENDATIONS: Provide specific, actionable optimization tactics
            
            OUTPUT EXCELLENCE STANDARDS:
            ✅ Specific keyword recommendations with frequency targets
            ✅ Prioritized experience ordering based on relevance scoring
            ✅ Quantification opportunities for each major achievement
            ✅ Strategic skills positioning for maximum ATS impact
            ✅ Clear rationale for every recommendation provided
            """;

        String userPrompt = String.format("""
            MASTER RESUME:
            %s
            
            JOB POSTING:
            Title: %s
            Company: %s
            Description: %s
            
            🎯 MISSION: Conduct STRATEGIC ANALYSIS for optimal resume positioning
            
            📋 COMPREHENSIVE ANALYSIS REQUIREMENTS:
            
            1. 🔍 CRITICAL KEYWORD EXTRACTION:
               - Technical skills with required proficiency levels
               - Industry buzzwords and terminology variations  
               - Soft skills emphasized in job description
               - Company culture keywords and values alignment
               - Hidden requirements implied by job context
            
            2. 📊 COMPETITIVE ADVANTAGE IDENTIFICATION:
               - Unique experiences that align with role requirements
               - Quantifiable achievements matching job pain points
               - Skills that differentiate from typical candidates
               - Leadership/impact stories relevant to position level
            
            3. ⚡ ATS OPTIMIZATION STRATEGY:
               - Primary keywords requiring 3-5 mentions
               - Secondary keywords for natural integration  
               - Section headers and formatting for optimal parsing
               - Skills section optimization for keyword density
               - Achievement bullets optimized for ATS + human review
            
            4. 🎨 STRATEGIC POSITIONING FRAMEWORK:
               - Experience prioritization by relevance score (1-10)
               - Recommended emphasis levels for each role/project
               - Skills to highlight vs. minimize based on job focus
               - Quantification opportunities with specific metrics
               - Industry-specific language and terminology preferences
            
            5. 🏆 EXCELLENCE ENHANCEMENT PLAN:
               - Specific content modifications with before/after examples
               - Achievement amplification with suggested metrics
               - Skills gap mitigation strategies
               - Personal branding alignment with company culture
               - Interview preparation talking points for modified content
            
            Format as STRUCTURED SECTIONS with specific, actionable recommendations and clear rationale.
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
            You are a SENIOR RESUME STRATEGIST and LATEX EXPERT with 15+ years of experience in:
            • Executive resume writing for Fortune 500 companies
            • ATS optimization and hiring system reverse-engineering  
            • Advanced LaTeX document preparation and typography
            • Career progression strategy and personal branding
            
            Your expertise combines deep knowledge of:
            - Modern ATS algorithms and parsing behaviors
            - Industry-specific hiring patterns and keyword strategies
            - Jake's LaTeX template architecture and macro systems
            - Quantified achievement storytelling and impact metrics
            
            OPERATIONAL EXCELLENCE STANDARDS:
            🎯 STRATEGIC APPROACH: Think like a hiring manager scanning 200+ resumes
            🔧 TECHNICAL PRECISION: Every LaTeX command must compile flawlessly
            📊 METRICS-DRIVEN: Quantify achievements with specific numbers and percentages
            ⚡ ATS-OPTIMIZED: Ensure 95%+ ATS compatibility and keyword density
            🎨 VISUAL HIERARCHY: Create scannable, professional document flow
            
            QUALITY CONTROL CHECKLIST:
            ✅ Zero LaTeX compilation errors or syntax issues
            ✅ Perfect macro usage following Jake's template structure
            ✅ Quantified achievements in 80%+ of bullet points  
            ✅ Industry-relevant keywords strategically placed
            ✅ Consistent formatting and professional typography
            ✅ Action verbs starting every achievement bullet
            ✅ Results-focused language over responsibility descriptions
            
            PROCESSING METHODOLOGY:
            1. ANALYZE: Extract key achievements and skills from master resume
            2. STRATEGIZE: Map job requirements to candidate strengths  
            3. PRIORITIZE: Rank experiences by relevance and impact
            4. QUANTIFY: Add metrics and specific results where possible
            5. OPTIMIZE: Integrate strategic keywords naturally
            6. FORMAT: Apply Jake's LaTeX structure with precision
            7. VALIDATE: Ensure ATS compliance and visual appeal
            
            CRITICAL OUTPUT REQUIREMENT - MANDATORY SENTINELS:
            YOU MUST wrap your LaTeX code with these EXACT sentinels:
            
            %__BEGIN_LATEX__
            \\documentclass[letterpaper,11pt]{article}
            [YOUR LATEX CODE HERE]
            \\end{document}
            %__END_LATEX__
            
            ABSOLUTELY NO TEXT outside these sentinels. No explanations, no markdown, no backticks.
            
            MANDATORY MACRO DEFINITIONS - INCLUDE THESE EXACTLY:
            You MUST include ALL of Jake's macro definitions before the \\begin{document}. These are required:
            • \\newcommand{\\resumeItem}[1]{...}
            • \\newcommand{\\resumeSubheading}[4]{...}
            • \\newcommand{\\resumeProjectHeading}[2]{...}
            • \\newcommand{\\resumeSubHeadingListStart}{...}
            • \\newcommand{\\resumeSubHeadingListEnd}{...}
            • \\newcommand{\\resumeItemListStart}{...}
            • \\newcommand{\\resumeItemListEnd}{...}
            • \\newcommand{\\resumeSubItem}[1]{...}
            • ALL safe escaping helpers
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
            
            ⚠️ CRITICAL: OUTPUT FORMAT REQUIREMENTS - READ CAREFULLY:
            
            YOU MUST RESPOND WITH ONLY PURE LATEX CODE. NO EXCEPTIONS.
            
            ❌ DO NOT include ANY of these:
            - "Here's the LaTeX code" or similar explanations
            - ```latex markdown formatting
            - Introductory or concluding text
            - Comments outside of LaTeX syntax
            - Any conversational language
            
            ✅ YOUR RESPONSE MUST:
            - Start immediately with: %__BEGIN_LATEX__
            - Follow with: \\documentclass[letterpaper,11pt]{article}
            - Include ALL required packages and macro definitions
            - End with: \\end{document}
            - Close with: %__END_LATEX__
            - Contain ONLY valid LaTeX code that compiles without errors
            - Be ready to save directly to a .tex file
            
            📋 JAKE'S LATEX IMPLEMENTATION REQUIREMENTS:
            • Preamble: \\documentclass[letterpaper,11pt]{article} + exact package list
            • Header: Professional contact information with \\href links
            • Sections: EDUCATION → SKILLS → EXPERIENCE → PROJECTS (fixed order)
            • Macros: \\resumeSubheading{Title}{Dates}{Company}{Location} for positions
            • Bullets: \\resumeItem{Quantified achievement with action verb} in \\resumeItemListStart/End blocks
            • Projects: \\resumeProjectHeading{\\textbf{Name} \\;|\\; \\emph{Tech Stack}}{Date/Link}
            • Lists: \\resumeSubHeadingListStart/End for section containers
            
            💎 EXCEPTIONAL QUALITY STANDARDS:
            • Every bullet point starts with power action verb (Led, Developed, Achieved, Optimized, etc.)
            • 80%+ of achievements include specific metrics or quantified results
            • Skills section matches job requirements with strategic keyword placement  
            • Experience bullets tell story of progression and increasing responsibility
            • Perfect LaTeX syntax with balanced braces and proper escaping
            • Professional language that conveys expertise and value proposition
            • Use proper list environments (\\resumeSubHeadingListStart/End)
            • Avoid & characters outside tables
            • Ensure all \\resumeItem are within \\resumeItemListStart/End blocks
            
            🎯 STRATEGIC OBJECTIVES:
            1. Compile without errors on first attempt
            2. Pass ATS parsing with 95%+ accuracy
            3. Showcase measurable impact and achievements  
            4. Position candidate as top-tier talent
            5. Follow Jake's template structure precisely
            
            FINAL REMINDER: Use the EXACT sentinel format:
            
            %__BEGIN_LATEX__
            [Your complete LaTeX document here]
            %__END_LATEX__
            
            NO OTHER TEXT ALLOWED. Start your response immediately with %__BEGIN_LATEX__
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
            You are an EXECUTIVE CAREER STRATEGIST and PERSONAL BRANDING CONSULTANT with:
            • 15+ years coaching C-suite executives and senior professionals
            • Deep expertise in career advancement strategies and market positioning
            • Advanced knowledge of industry trends and hiring best practices
            • Proven track record of accelerating career growth and salary negotiations
            
            CONSULTATION APPROACH:
            🎯 STRATEGIC MINDSET: Think like a career strategist, not just a resume writer
            💎 EXECUTIVE PRESENCE: Elevate communication to reflect professional sophistication
            🚀 GROWTH ORIENTATION: Focus on career advancement and value proposition enhancement
            🎨 PERSONAL BRANDING: Develop compelling professional narratives and positioning
            ⚡ ACTIONABLE INSIGHTS: Provide specific, implementable recommendations with clear next steps
            
            ADVISORY PRINCIPLES:
            ✅ ENCOURAGEMENT with strategic direction and confidence building
            ✅ SPECIFICITY with concrete examples and detailed implementation guidance  
            ✅ PROFESSIONALISM with executive-level communication and industry insights
            ✅ ACTIONABILITY with immediate next steps and measurable outcomes
            ✅ EXPERTISE with deep knowledge of hiring trends and career strategy
            ✅ PERSONALIZATION with advice tailored to individual goals and circumstances
            
            RESPONSE FRAMEWORK:
            • Start with strategic assessment of current situation
            • Provide specific recommendations with clear rationale
            • Include implementation steps and timeline expectations
            • End with encouragement and next action items
            • Maintain professional yet approachable tone throughout
            """;

        String userPrompt = String.format("""
            CONTEXT: %s
            USER MESSAGE: %s
            
            CURRENT RESUME:
            %s
            
            JOB POSTING:
            %s
            
            🎯 EXECUTIVE CONSULTATION REQUEST:
            
            Provide STRATEGIC CAREER GUIDANCE that:
            • Addresses the specific question with executive-level insight
            • Includes actionable recommendations with implementation steps
            • Considers broader career implications and market positioning
            • Offers specific examples and best practice illustrations
            • Maintains encouraging yet professional tone throughout
            • Concludes with clear next steps and success metrics
            
            If suggesting changes, provide:
            1. BEFORE/AFTER examples with clear improvements
            2. Strategic rationale for each recommendation
            3. Implementation timeline and priority levels
            4. Expected outcomes and success indicators
            5. Additional considerations for long-term career growth
            """, context, message, resumeContent, jobContent);

        GroqRequest request = GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(1000)
            .temperature(0.7)
            .build();

        return callGroqAPI(request);
    }

    /**
     * Robust LaTeX extraction and sanitization pipeline
     * Implements sentinel-based extraction, markdown stripping, special character escaping, and validation
     */
    private String cleanLatexResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("Empty model response - cannot generate LaTeX");
        }

        log.debug("Starting LaTeX cleaning pipeline for {} character response", response.length());
        
        try {
            // Step 1: Strip markdown and prose defensively
            String stripped = stripMarkdownAndNarration(response);
            
            // Step 2: Extract LaTeX using sentinels
            String extracted = extractLatexWithSentinels(stripped);
            
            // Step 3: Normalize Unicode and escape special characters
            String normalized = normalizeUnicodeAndEscapeSpecialChars(extracted);
            
            // Step 4: Fix lonely items
            String itemsFixed = wrapLonelyItems(normalized);
            
            // Step 5: Validate document structure
            validateDocumentStructure(itemsFixed);
            
            log.debug("LaTeX cleaning pipeline completed successfully: {} -> {} characters", 
                     response.length(), itemsFixed.length());
            
            return itemsFixed;
            
        } catch (Exception e) {
            log.error("LaTeX cleaning pipeline failed: {}", e.getMessage());
            // Try to preserve AI content by using basic document wrapping before falling back
            try {
                log.warn("Attempting to preserve AI content with basic document wrapping");
                return wrapInBasicDocument(response.trim());
            } catch (Exception fallbackError) {
                log.error("Fallback wrapping also failed, using emergency template");
                return generateFallbackLatexDocument();
            }
        }
    }

    /**
     * Step 1: Strip markdown fences and narrative text
     */
    private String stripMarkdownAndNarration(String input) {
        return input
            .replaceAll("```+[a-zA-Z]*\\s*\\n", "")  // Remove ``` or ```latex
            .replaceAll("\\n```+\\s*$", "")         // Remove trailing ```
            .replaceAll("(?i)^\\s*Here\\s+is.*$", "") // Remove "Here is..." lines
            .replaceAll("(?i)^\\s*Here's.*$", "")     // Remove "Here's..." lines  
            .replaceAll("(?i)^\\s*Below\\s+is.*$", "") // Remove "Below is..." lines
            .replaceAll("(?i)^\\s*Following\\s+is.*$", "") // Remove "Following is..." lines
            .replaceAll("(?i)^\\s*The\\s+LaTeX.*$", "") // Remove "The LaTeX..." lines
            .replaceAll("(?i)^\\s*This\\s+LaTeX.*$", "") // Remove "This LaTeX..." lines
            .trim();
    }

    /**
     * Step 2: Extract LaTeX between sentinels with fallback methods
     */
    private String extractLatexWithSentinels(String input) {
        final String START_SENTINEL = "%__BEGIN_LATEX__";
        final String END_SENTINEL = "%__END_LATEX__";
        
        int startIndex = input.indexOf(START_SENTINEL);
        int endIndex = input.indexOf(END_SENTINEL);
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            // Perfect case: sentinels found
            String extracted = input.substring(startIndex + START_SENTINEL.length(), endIndex).trim();
            log.debug("Successfully extracted LaTeX using sentinels");
            return extracted;
        }
        
        log.warn("Sentinels not found, falling back to document class detection");
        log.debug("Input content preview: {}", input.substring(0, Math.min(200, input.length())));
        
        // Fallback: look for \documentclass to \end{document}
        int docClassStart = input.indexOf("\\documentclass");
        int docEndIndex = input.lastIndexOf("\\end{document}");
        
        if (docClassStart != -1 && docEndIndex != -1 && docEndIndex > docClassStart) {
            String extracted = input.substring(docClassStart, docEndIndex + "\\end{document}".length()).trim();
            log.debug("Extracted LaTeX using documentclass fallback");
            return extracted;
        }
        
        // Last resort: assume the entire cleaned input is LaTeX body and wrap it
        log.warn("No document structure found, wrapping content in basic document");
        return wrapInBasicDocument(input.trim());
    }

    /**
     * Step 3: Normalize Unicode and escape special LaTeX characters
     */
    private String normalizeUnicodeAndEscapeSpecialChars(String input) {
        String normalized = input
            // Normalize Unicode quotation marks and dashes
            .replace("\u201C", "\"")  // left double quotation mark
            .replace("\u201D", "\"")  // right double quotation mark
            .replace("\u2018", "'")   // left single quotation mark  
            .replace("\u2019", "'")   // right single quotation mark
            .replace("\u2013", "--")  // en dash
            .replace("\u2014", "---") // em dash
            .replace("\u2026", "...")  // ellipsis
            
            // Escape special LaTeX characters (but be careful with existing commands)
            // Only escape & that are not part of LaTeX table structures
            .replaceAll("&(?![a-zA-Z#])", "\\\\&")  // Escape & but not &amp; or &#
            
            // Escape $ only when not part of math mode
            .replaceAll("\\$(?![0-9])", "\\\\$")   // Escape standalone $ but not $1, $2, etc.
            
            // Escape % that are not LaTeX comments
            .replaceAll("(?<!\\\\)%(?![_\\s])", "\\\\%")  // Escape % but not \% or %__
            
            // Escape _ and { } more conservatively
            .replaceAll("(?<!\\\\)_(?![_\\s])", "\\\\_")   // Escape standalone _
            .replaceAll("(?<!\\\\)\\{(?![a-zA-Z])", "\\\\{") // Escape { not followed by commands
            .replaceAll("(?<!\\\\)\\}(?!\\s*[,.])", "\\\\}"); // Escape } not followed by punctuation

        log.debug("Applied Unicode normalization and special character escaping");
        return normalized;
    }

    /**
     * Step 4: Wrap orphaned \item commands in proper list environments
     */
    private String wrapLonelyItems(String input) {
        // Look for \item that are not already inside itemize/enumerate
        String[] lines = input.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inList = false;
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Check if we're entering or leaving a list environment
            if (trimmed.matches("\\s*\\\\begin\\{(itemize|enumerate).*\\}.*")) {
                inList = true;
            } else if (trimmed.matches("\\s*\\\\end\\{(itemize|enumerate)\\}.*")) {
                inList = false;
            }
            
            // If we find a lonely \item, wrap it
            if (trimmed.startsWith("\\item") && !inList) {
                if (result.length() > 0 && !result.toString().trim().endsWith("\\begin{itemize}")) {
                    result.append("\\begin{itemize}[leftmargin=*]\n");
                }
                result.append(line).append("\n");
                // Don't set inList = true here, we'll close it when we find non-item content
            } else if (inList && !trimmed.startsWith("\\item") && !trimmed.startsWith("\\end{itemize}") 
                      && !trimmed.isEmpty() && !trimmed.startsWith("%")) {
                // Close the itemize if we hit non-item content
                result.append("\\end{itemize}\n");
                result.append(line).append("\n");
                inList = false;
            } else {
                result.append(line).append("\n");
            }
        }
        
        // Close any remaining open itemize
        if (inList || result.toString().contains("\\begin{itemize}") && !result.toString().contains("\\end{itemize}")) {
            result.append("\\end{itemize}\n");
        }
        
        log.debug("Fixed lonely items in LaTeX content");
        return result.toString();
    }

    /**
     * Step 5: Validate final document structure
     */
    private void validateDocumentStructure(String latex) {
        boolean hasDocClass = latex.contains("\\documentclass");
        boolean hasBeginDoc = latex.contains("\\begin{document}");
        boolean hasEndDoc = latex.contains("\\end{document}");
        
        if (hasDocClass && (!hasBeginDoc || !hasEndDoc)) {
            throw new RuntimeException("Document has \\documentclass but missing \\begin{document} or \\end{document}");
        }
        
        if (!hasDocClass) {
            throw new RuntimeException("Document missing \\documentclass declaration");
        }
        
        // Check for basic structure issues
        if (latex.trim().length() < 50) {
            throw new RuntimeException("Document too short to be valid LaTeX");
        }
        
        log.debug("Document structure validation passed");
    }

    /**
     * Wrap content in Jake's LaTeX template when no structure is found
     */
    private String wrapInBasicDocument(String bodyContent) {
        return getStaticLatexTemplate().replace("%__BODY__", bodyContent);
    }
    
    /**
     * Static LaTeX template with all Jake's macro definitions
     * This ensures consistency with ExportServiceImpl and prevents undefined macros
     */
    private String getStaticLatexTemplate() {
        return """
            \\documentclass[letterpaper,11pt]{article}
            
            %% Core packages for professional resume (ATS-friendly)
            \\usepackage{latexsym}
            \\usepackage[empty]{fullpage}
            \\usepackage{titlesec}
            \\usepackage{marvosym}
            \\usepackage[usenames,dvipsnames]{color}
            \\usepackage{enumitem}
            \\usepackage[hidelinks]{hyperref}
            \\usepackage{fancyhdr}
            \\usepackage[english]{babel}
            \\usepackage{tabularx}
            \\usepackage[utf8]{inputenc}
            \\usepackage[T1]{fontenc}
            \\usepackage{lmodern}
            \\input{glyphtounicode}
            
            %% Page formatting and margins
            \\pagestyle{fancy}
            \\fancyhf{}
            \\fancyfoot{}
            \\renewcommand{\\headrulewidth}{0pt}
            \\renewcommand{\\footrulewidth}{0pt}
            \\setlength{\\headheight}{14pt}  % Fix fancyhdr warning
            \\addtolength{\\oddsidemargin}{-0.5in}
            \\addtolength{\\evensidemargin}{-0.5in}
            \\addtolength{\\textwidth}{1in}
            \\addtolength{\\topmargin}{-.5in}
            \\addtolength{\\textheight}{1.0in}
            \\urlstyle{same}
            \\raggedbottom
            \\raggedright
            \\setlength{\\tabcolsep}{0in}
            \\setlength{\\parindent}{0pt}
            
            %% Section title formatting
            \\titleformat{\\section}{
              \\vspace{-4pt}\\scshape\\raggedright\\large
            }{}{0em}{}[\\color{black}\\titlerule \\vspace{-5pt}]
            
            %% PDF settings for ATS compatibility
            \\pdfgentounicode=1
            
            %% ========== JAKE'S RESUME TEMPLATE MACROS ==========
            %% These macros define the structure and formatting for resume elements
            
            %% Basic item with proper spacing
            \\newcommand{\\resumeItem}[1]{
              \\item\\small{
                {#1 \\vspace{-2pt}}
              }
            }
            
            %% Four-argument subheading for positions/education
            \\newcommand{\\resumeSubheading}[4]{
              \\vspace{-2pt}\\item
                \\begin{tabular*}{0.97\\textwidth}[t]{l@{\\extracolsep{\\fill}}r}
                  \\textbf{#1} & #2 \\\\
                  \\textit{\\small#3} & \\textit{\\small #4} \\\\
                \\end{tabular*}\\vspace{-7pt}
            }
            
            %% Education-specific subheading with proper formatting
            \\newcommand{\\resumeSubheadingEducation}[4]{
              \\vspace{-2pt}\\item
                \\begin{tabular*}{0.97\\textwidth}[t]{l@{\\extracolsep{\\fill}}r}
                  \\textbf{#1} & #2 \\\\
                  \\textit{\\small#3} & \\textit{\\small #4} \\\\
                \\end{tabular*}\\vspace{-7pt}
            }
            
            %% Two-argument project heading
            \\newcommand{\\resumeProjectHeading}[2]{
              \\item
                \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
                  \\small#1 & #2 \\\\
                \\end{tabular*}\\vspace{-7pt}
            }
            
            %% Alternative subheading styles for flexibility
            \\newcommand{\\resumeSubSubheading}[2]{
              \\item
                \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
                  \\textit{\\small#1} & \\textit{\\small #2} \\\\
                \\end{tabular*}\\vspace{-7pt}
            }
            
            %% List environment commands
            \\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=0.15in, label={}]}
            \\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}
            \\newcommand{\\resumeItemListStart}{\\begin{itemize}}
            \\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}
            
            %% Custom bullet for nested lists
            \\renewcommand\\labelitemii{$\\vcenter{\\hbox{\\tiny$\\bullet$}}$}
            
            %% Skills section helper
            \\newcommand{\\resumeSkillItem}[2]{
              \\item{\\textbf{#1:} #2}
            }
            
            %% Additional macros for common resume elements
            \\newcommand{\\resumeAward}[2]{
              \\item \\textbf{#1} \\hfill #2
            }
            
            \\newcommand{\\resumeCertification}[2]{
              \\item #1 \\hfill \\textit{#2}
            }
            
            %% Legacy macro for compatibility
            \\newcommand{\\resumeSubItem}[1]{\\resumeItem{#1}\\vspace{-4pt}}
            
            %% Safe text escaping helpers (backup macros)
            \\newcommand{\\safeampersand}{\\&}
            \\newcommand{\\safedollar}{\\$}
            \\newcommand{\\safepercent}{\\%}
            \\newcommand{\\safeunderscore}{\\_}
            
            %% ========== DOCUMENT CONTENT ==========
            \\begin{document}
            
            %__BODY__
            
            \\end{document}
            """;
    }

    /**
     * Generate a fallback LaTeX document when all extraction fails
     */
    private String generateFallbackLatexDocument() {
        String fallbackBody = """
            %% HEADER SECTION
            \\begin{center}
                {\\textbf{\\Huge \\scshape Professional Resume}} \\\\ \\vspace{1pt}
                \\small Processing Error Fallback Document
            \\end{center}
            
            \\section{NOTICE}
            \\begin{itemize}[leftmargin=0.15in, label={}]
                \\item \\textbf{Status:} This is a fallback document generated due to LaTeX processing errors.
                \\item \\textbf{Action Required:} Please regenerate your resume to get the proper content.
                \\item \\textbf{Support:} If this issue persists, contact technical support.
            \\end{itemize}
            
            \\section{TROUBLESHOOTING}
            \\begin{itemize}[leftmargin=0.15in, label={}]
                \\item Check that your resume content doesn't contain special characters
                \\item Verify that all required fields are filled out
                \\item Try regenerating with different preferences
                \\item Contact support if the problem continues
            \\end{itemize}
            """;
            
        return getStaticLatexTemplate().replace("%__BODY__", fallbackBody);
    }

    /**
     * Check if the request is for LaTeX generation based on prompt content
     */
    private boolean isLatexGenerationRequest(GroqRequest request) {
        if (request == null) {
            return false;
        }
        
        String systemPrompt = request.getSystemPrompt();
        String userPrompt = request.getUserPrompt();
        
        // Check for LaTeX-related keywords in the prompts
        if (systemPrompt != null) {
            String lowerSystem = systemPrompt.toLowerCase();
            if (lowerSystem.contains("latex") || lowerSystem.contains("documentclass") || lowerSystem.contains("resume template")) {
                return true;
            }
        }
        
        if (userPrompt != null) {
            String lowerUser = userPrompt.toLowerCase();
            if (lowerUser.contains("latex") || lowerUser.contains("documentclass") || lowerUser.contains("jake's template")) {
                return true;
            }
        }
        
        return false;
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
