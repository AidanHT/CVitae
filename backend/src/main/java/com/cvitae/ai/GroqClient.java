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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cvitae.service.LatexTemplateService;

/**
 * Enhanced Groq API client with better error handling, retry logic, and structured responses
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroqClient {

    /**
     * Escape % characters in user input to prevent String.format() errors.
     */
    private static String escapeForFormat(String input) {
        if (input == null) return "";
        return input.replace("%", "%%");
    }

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.model:llama3-8b-8192}")
    private String groqModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final LatexTemplateService latexTemplateService;

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
            """, escapeForFormat(masterResume), escapeForFormat(jobTitle), escapeForFormat(companyName), escapeForFormat(jobPosting));

        GroqRequest request = GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(4000)
            .temperature(0.3) // Lower temperature for more consistent analysis
            .build();

        return callGroqAPI(request);
    }

    /**
     * Generate tailored resume content with LaTeX formatting using Jake's exact template
     */
    public GroqResponse generateTailoredResume(String masterResume, String analysisResults, Map<String, Object> preferences) {
        String systemPrompt = """
            You are an ELITE LATEX DOCUMENT ARCHITECT specializing in Jake Gutierrez's resume template.
            Your ONLY job is to convert resume content into Jake's EXACT LaTeX format.
            
            You have PERFECT knowledge of:
            • Jake's resume template structure and all custom macros
            • LaTeX syntax, brace balancing, and escape sequences
            • ATS-compliant formatting that parses correctly
            
            CRITICAL RULES:
            1. Follow Jake's template EXACTLY - no deviations
            2. Use the EXACT macro definitions provided
            3. Follow the EXACT section order: Education → Experience → Projects → Technical Skills
            4. Output ONLY pure LaTeX code wrapped in sentinels
            5. NEVER add explanations, markdown, or text outside the LaTeX
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
            
            ════════════════════════════════════════════════════════════════════
            JAKE'S RESUME TEMPLATE - YOU MUST PRODUCE OUTPUT IN THIS EXACT FORMAT
            ════════════════════════════════════════════════════════════════════
            
            Below is the COMPLETE working template. Replace the example content with
            the user's resume data while keeping ALL formatting EXACTLY the same.
            
            %%__BEGIN_LATEX__
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
            \\input{glyphtounicode}

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

            \\titleformat{\\section}{
              \\vspace{-4pt}\\scshape\\raggedright\\large
            }{}{0em}{}[\\color{black}\\titlerule \\vspace{-5pt}]

            \\pdfgentounicode=1

            %%--- Custom Commands (REQUIRED - DO NOT MODIFY) ---
            \\newcommand{\\resumeItem}[1]{
              \\item\\small{
                {#1 \\vspace{-2pt}}
              }
            }

            \\newcommand{\\resumeSubheading}[4]{
              \\vspace{-2pt}\\item
                \\begin{tabular*}{0.97\\textwidth}[t]{l@{\\extracolsep{\\fill}}r}
                  \\textbf{#1} & #2 \\\\
                  \\textit{\\small#3} & \\textit{\\small #4} \\\\
                \\end{tabular*}\\vspace{-7pt}
            }

            \\newcommand{\\resumeSubSubheading}[2]{
                \\item
                \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
                  \\textit{\\small#1} & \\textit{\\small #2} \\\\
                \\end{tabular*}\\vspace{-7pt}
            }

            \\newcommand{\\resumeProjectHeading}[2]{
                \\item
                \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
                  \\small#1 & #2 \\\\
                \\end{tabular*}\\vspace{-7pt}
            }

            \\newcommand{\\resumeSubItem}[1]{\\resumeItem{#1}\\vspace{-4pt}}

            \\renewcommand\\labelitemii{$\\vcenter{\\hbox{\\tiny$\\bullet$}}$}

            \\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=0.15in, label={}]}
            \\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}
            \\newcommand{\\resumeItemListStart}{\\begin{itemize}}
            \\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}

            \\begin{document}

            %%----------HEADING----------
            \\begin{center}
                \\textbf{\\Huge \\scshape Jake Ryan} \\\\ \\vspace{1pt}
                \\small 123-456-7890 $|$ \\href{mailto:x@x.com}{\\underline{jake@su.edu}} $|$ 
                \\href{https://linkedin.com/in/...}{\\underline{linkedin.com/in/jake}} $|$
                \\href{https://github.com/...}{\\underline{github.com/jake}}
            \\end{center}

            %%-----------EDUCATION-----------
            \\section{Education}
              \\resumeSubHeadingListStart
                \\resumeSubheading
                  {Southwestern University}{Georgetown, TX}
                  {Bachelor of Arts in Computer Science, Minor in Business}{Aug. 2018 -- May 2021}
              \\resumeSubHeadingListEnd

            %%-----------EXPERIENCE-----------
            \\section{Experience}
              \\resumeSubHeadingListStart
                \\resumeSubheading
                  {Undergraduate Research Assistant}{June 2020 -- Present}
                  {Texas A\\&M University}{College Station, TX}
                  \\resumeItemListStart
                    \\resumeItem{Developed a REST API using FastAPI and PostgreSQL to store data from learning management systems}
                    \\resumeItem{Developed a full-stack web application using Flask, React, PostgreSQL and Docker to analyze GitHub data}
                  \\resumeItemListEnd
              \\resumeSubHeadingListEnd

            %%-----------PROJECTS-----------
            \\section{Projects}
                \\resumeSubHeadingListStart
                  \\resumeProjectHeading
                      {\\textbf{Gitlytics} $|$ \\emph{Python, Flask, React, PostgreSQL, Docker}}{June 2020 -- Present}
                      \\resumeItemListStart
                        \\resumeItem{Developed a full-stack web application using with Flask serving a REST API with React as the frontend}
                        \\resumeItem{Implemented GitHub OAuth to get data from user's repositories}
                      \\resumeItemListEnd
                \\resumeSubHeadingListEnd

            %%-----------TECHNICAL SKILLS-----------
            \\section{Technical Skills}
             \\begin{itemize}[leftmargin=0.15in, label={}]
                \\small{\\item{
                 \\textbf{Languages}{: Java, Python, C/C++, SQL (Postgres), JavaScript, HTML/CSS, R} \\\\
                 \\textbf{Frameworks}{: React, Node.js, Flask, JUnit, WordPress, Material-UI, FastAPI} \\\\
                 \\textbf{Developer Tools}{: Git, Docker, TravisCI, Google Cloud Platform, VS Code, Visual Studio, PyCharm, IntelliJ, Eclipse} \\\\
                 \\textbf{Libraries}{: pandas, NumPy, Matplotlib}
                }}
             \\end{itemize}

            \\end{document}
            %%__END_LATEX__
            
            ════════════════════════════════════════════════════════════════════
            CONTENT MAPPING INSTRUCTIONS
            ════════════════════════════════════════════════════════════════════
            
            MAP USER'S DATA TO EACH SECTION:
            
            1. HEADING: Replace with user's name, phone, email, LinkedIn, GitHub
               - Use \\textbf{\\Huge \\scshape NAME}
               - Separate contact items with $|$
               - Use \\href{URL}{\\underline{display text}} for links
            
            2. EDUCATION: For each degree/school:
               - \\resumeSubheading{School}{Location}{Degree, Major}{Dates}
               - Most recent education first
            
            3. EXPERIENCE: For each job:
               - \\resumeSubheading{Title}{Dates}{Company}{Location}
               - \\resumeItemListStart
               - \\resumeItem{Achievement with metrics}
               - \\resumeItemListEnd
               - ESCAPE & as \\& in company names (e.g., Texas A\\&M)
            
            4. PROJECTS: For each project:
               - \\resumeProjectHeading{\\textbf{Name} $|$ \\emph{Tech Stack}}{Date or Link}
               - \\resumeItemListStart
               - \\resumeItem{Description with impact}
               - \\resumeItemListEnd
            
            5. TECHNICAL SKILLS: Use this EXACT format:
               \\begin{itemize}[leftmargin=0.15in, label={}]
                  \\small{\\item{
                   \\textbf{Languages}{: item1, item2, item3} \\\\
                   \\textbf{Frameworks}{: item1, item2, item3} \\\\
                   \\textbf{Developer Tools}{: item1, item2, item3} \\\\
                   \\textbf{Libraries}{: item1, item2, item3}
                  }}
               \\end{itemize}
               
               NOTE: The colon and items are OUTSIDE \\textbf{}, format is:
               \\textbf{Category}{: items}
            
            ════════════════════════════════════════════════════════════════════
            CRITICAL VALIDATION CHECKLIST
            ════════════════════════════════════════════════════════════════════
            
            Before outputting, verify:
            ✅ Starts with %%__BEGIN_LATEX__
            ✅ Ends with %%__END_LATEX__
            ✅ All & escaped as \\&
            ✅ All %% escaped as \\%% (except in comments)
            ✅ All # escaped as \\#
            ✅ All $ used only for math mode separators ($|$)
            ✅ \\resumeItem ONLY inside \\resumeItemListStart/End
            ✅ Technical Skills uses plain \\begin{itemize}, NOT \\resumeItemListStart
            ✅ All braces balanced
            ✅ No markdown or explanatory text
            
            OUTPUT THE COMPLETE LATEX DOCUMENT NOW:
            """, 
            escapeForFormat(masterResume), 
            escapeForFormat(analysisResults),
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
            .temperature(0.2)
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
            """, escapeForFormat(context), escapeForFormat(message), escapeForFormat(resumeContent), escapeForFormat(jobContent));

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
     * 
     * IMPROVED: More resilient extraction that preserves AI content whenever possible
     */
    private String cleanLatexResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            log.error("Empty model response - cannot generate LaTeX");
            return generateFallbackLatexDocument();
        }

        log.info("=== LATEX CLEANING PIPELINE START ===");
        log.info("Input response length: {} characters", response.length());
        log.debug("Original AI response preview (first 500 chars): {}", 
                response.substring(0, Math.min(500, response.length())));
        
        String result = response;
        
        try {
            // Step 1: Strip markdown and prose defensively
            result = stripMarkdownAndNarration(result);
            log.debug("After stripping markdown (length {})", result.length());
            
            // Step 2: Extract LaTeX using multiple fallback methods
            result = extractLatexWithSentinels(result);
            log.debug("After LaTeX extraction (length {})", result.length());
            
            // Step 3: Normalize Unicode (but DON'T over-escape LaTeX commands)
            result = normalizeUnicodeOnly(result);
            log.debug("After Unicode normalization (length {})", result.length());
            
            // Step 4: Ensure document structure exists
            result = ensureDocumentStructure(result);
            log.debug("After ensuring document structure (length {})", result.length());
            
            // Step 5: Fix orphan \resumeItem calls that are outside list environments
            result = fixOrphanResumeItems(result);
            log.debug("After fixing orphan resumeItems (length {})", result.length());
            
            // Validate the result has minimum content
            if (result.trim().length() < 100) {
                log.warn("Result too short ({}), using wrapped original content", result.length());
                result = wrapInBasicDocument(stripMarkdownAndNarration(response));
            }
            
            log.info("LaTeX cleaning pipeline completed successfully: {} -> {} characters", 
                     response.length(), result.length());
            log.info("=== LATEX CLEANING PIPELINE END ===");
            
            return result;
            
        } catch (Exception e) {
            log.error("LaTeX cleaning pipeline failed: {}", e.getMessage());
            
            // Progressive fallback: try to preserve as much AI content as possible
            try {
                // First try: wrap the stripped content in document structure
                String stripped = stripMarkdownAndNarration(response);
                if (stripped.contains("\\section") || stripped.contains("\\resumeItem") || stripped.contains("\\begin{")) {
                    log.warn("Preserving AI content with document wrapper");
                    return wrapInBasicDocument(stripped);
                }
                
                // Second try: extract any LaTeX-looking content
                String latexContent = extractAnyLatexContent(response);
                if (latexContent != null && latexContent.length() > 100) {
                    log.warn("Extracted partial LaTeX content");
                    return ensureDocumentStructure(latexContent);
                }
                
            } catch (Exception fallbackError) {
                log.error("All fallback methods failed: {}", fallbackError.getMessage());
            }
            
            log.error("Using emergency fallback template");
            return generateFallbackLatexDocument();
        }
    }
    
    /**
     * Try to extract any LaTeX-looking content from the response
     */
    private String extractAnyLatexContent(String input) {
        // Look for content between \documentclass and \end{document}
        int docClassIndex = input.indexOf("\\documentclass");
        int endDocIndex = input.lastIndexOf("\\end{document}");
        
        if (docClassIndex != -1 && endDocIndex != -1 && endDocIndex > docClassIndex) {
            return input.substring(docClassIndex, endDocIndex + "\\end{document}".length());
        }
        
        // Look for content between \begin{document} and \end{document}
        int beginDocIndex = input.indexOf("\\begin{document}");
        if (beginDocIndex != -1 && endDocIndex != -1 && endDocIndex > beginDocIndex) {
            String body = input.substring(beginDocIndex + "\\begin{document}".length(), endDocIndex);
            return wrapInBasicDocument(body.trim());
        }
        
        return null;
    }
    
    /**
     * Normalize Unicode characters only - don't touch LaTeX commands
     */
    private String normalizeUnicodeOnly(String input) {
        return input
            // Normalize Unicode quotation marks and dashes
            .replace("\u201C", "``")   // left double quotation mark -> LaTeX style
            .replace("\u201D", "''")   // right double quotation mark -> LaTeX style
            .replace("\u2018", "`")    // left single quotation mark
            .replace("\u2019", "'")    // right single quotation mark
            .replace("\u2013", "--")   // en dash
            .replace("\u2014", "---")  // em dash
            .replace("\u2026", "...")  // ellipsis
            .replace("\u00A0", " ")    // non-breaking space
            .replace("\u2022", "$\\bullet$"); // bullet point
    }
    
    /**
     * Ensure the document has proper LaTeX structure
     */
    private String ensureDocumentStructure(String input) {
        boolean hasDocClass = input.contains("\\documentclass");
        boolean hasBeginDoc = input.contains("\\begin{document}");
        boolean hasEndDoc = input.contains("\\end{document}");
        
        // If it's a complete document, return as-is
        if (hasDocClass && hasBeginDoc && hasEndDoc) {
            return input;
        }
        
        // If it has document class but missing begin/end, try to fix
        if (hasDocClass && !hasBeginDoc) {
            // Find where packages end and add \begin{document}
            int lastUsepackage = input.lastIndexOf("\\usepackage");
            if (lastUsepackage != -1) {
                int lineEnd = input.indexOf("\n", lastUsepackage);
                if (lineEnd != -1) {
                    input = input.substring(0, lineEnd + 1) + "\n\\begin{document}\n" + input.substring(lineEnd + 1);
                }
            }
        }
        
        if (!hasEndDoc) {
            input = input.trim() + "\n\\end{document}";
        }
        
        // If no document structure at all, wrap it
        if (!input.contains("\\documentclass")) {
            return wrapInBasicDocument(input);
        }
        
        return input;
    }

    /**
     * Fix orphan \resumeItem calls that are outside proper list environments.
     * This handles cases where the AI incorrectly uses \resumeItem without wrapping
     * it in \resumeItemListStart/\resumeItemListEnd.
     * 
     * For skills sections: converts \resumeItem{X} to plain \item{X}
     * For other sections: wraps consecutive orphan items with \resumeItemListStart/End
     */
    private String fixOrphanResumeItems(String input) {
        if (input == null || !input.contains("\\resumeItem")) {
            return input;
        }
        
        log.debug("Checking for orphan \\resumeItem calls");
        
        String result = input;
        
        // First, handle the skills section specially - convert \resumeItem to \item
        result = fixSkillsSectionItems(result);
        
        // Then, wrap any remaining orphan \resumeItem calls with proper list environment
        result = wrapOrphanResumeItems(result);
        
        return result;
    }
    
    /**
     * In the SKILLS section, \resumeItem should be plain \item instead.
     * This converts \resumeItem{X} to \item{X} within skills sections.
     */
    private String fixSkillsSectionItems(String input) {
        // Find the skills section boundaries
        Pattern skillsSectionPattern = Pattern.compile(
            "(\\\\section\\{(?:SKILLS|Skills|TECHNICAL SKILLS|Technical Skills)[^}]*\\})" +
            "(.*?)" +
            "(\\\\section\\{|\\\\end\\{document\\})",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = skillsSectionPattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String sectionHeader = matcher.group(1);
            String sectionContent = matcher.group(2);
            String nextSection = matcher.group(3);
            
            // Check if skills section has orphan \resumeItem (not inside \resumeItemListStart/End)
            if (sectionContent.contains("\\resumeItem") && 
                !sectionContent.contains("\\resumeItemListStart")) {
                
                log.info("Found orphan \\resumeItem in skills section, converting to \\item");
                
                // Convert \resumeItem{X} to \item{X} in this section
                String fixedContent = sectionContent.replaceAll(
                    "\\\\resumeItem\\{",
                    "\\\\item{"
                );
                
                // Ensure the section has proper itemize environment
                if (!fixedContent.contains("\\begin{itemize}")) {
                    // Find where the items start and wrap them
                    fixedContent = wrapItemsInItemize(fixedContent);
                }
                
                matcher.appendReplacement(sb, 
                    Matcher.quoteReplacement(sectionHeader + fixedContent + nextSection));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Wrap loose \item calls in an itemize environment
     */
    private String wrapItemsInItemize(String content) {
        // Find consecutive \item calls that aren't in an itemize environment
        Pattern looseItemsPattern = Pattern.compile(
            "((?:\\s*\\\\item\\{[^}]*\\}\\s*)+)",
            Pattern.DOTALL
        );
        
        Matcher matcher = looseItemsPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String items = matcher.group(1);
            // Check if already inside an itemize
            int pos = matcher.start();
            String before = content.substring(0, pos);
            int lastBeginItemize = before.lastIndexOf("\\begin{itemize}");
            int lastEndItemize = before.lastIndexOf("\\end{itemize}");
            
            // If not inside itemize, wrap it
            if (lastBeginItemize <= lastEndItemize) {
                String wrapped = "\n\\begin{itemize}[leftmargin=0.15in, label={}]\n\\small\n" + 
                                items.trim() + 
                                "\n\\end{itemize}\n";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(wrapped));
                log.debug("Wrapped loose items in itemize environment");
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(items));
            }
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Wrap orphan \resumeItem calls (outside skills section) with \resumeItemListStart/End
     */
    private String wrapOrphanResumeItems(String input) {
        // Find \resumeItem calls that are not inside \resumeItemListStart/End
        String[] lines = input.split("\n");
        StringBuilder result = new StringBuilder();
        
        boolean inItemList = false;
        boolean needsListStart = false;
        int orphanItemCount = 0;
        StringBuilder orphanItems = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();
            
            // Track list environment state
            if (trimmedLine.contains("\\resumeItemListStart")) {
                inItemList = true;
                // If we have pending orphan items, flush them first
                if (orphanItemCount > 0) {
                    result.append("\\resumeItemListStart\n");
                    result.append(orphanItems);
                    result.append("\\resumeItemListEnd\n");
                    orphanItems = new StringBuilder();
                    orphanItemCount = 0;
                }
                result.append(line).append("\n");
                continue;
            }
            
            if (trimmedLine.contains("\\resumeItemListEnd")) {
                inItemList = false;
                result.append(line).append("\n");
                continue;
            }
            
            // Check for orphan \resumeItem
            if (trimmedLine.startsWith("\\resumeItem{") && !inItemList) {
                orphanItems.append(line).append("\n");
                orphanItemCount++;
                log.debug("Found orphan \\resumeItem at line {}", i + 1);
                continue;
            }
            
            // If we have orphan items and hit a non-resumeItem line, flush them
            if (orphanItemCount > 0 && !trimmedLine.startsWith("\\resumeItem{")) {
                log.info("Wrapping {} orphan \\resumeItem calls with list environment", orphanItemCount);
                result.append("\\resumeItemListStart\n");
                result.append(orphanItems);
                result.append("\\resumeItemListEnd\n");
                orphanItems = new StringBuilder();
                orphanItemCount = 0;
            }
            
            result.append(line).append("\n");
        }
        
        // Handle any remaining orphan items at the end
        if (orphanItemCount > 0) {
            log.info("Wrapping {} remaining orphan \\resumeItem calls", orphanItemCount);
            // Insert before \end{document}
            String resultStr = result.toString();
            int endDocPos = resultStr.lastIndexOf("\\end{document}");
            if (endDocPos > 0) {
                return resultStr.substring(0, endDocPos) +
                       "\\resumeItemListStart\n" +
                       orphanItems +
                       "\\resumeItemListEnd\n" +
                       resultStr.substring(endDocPos);
            } else {
                result.append("\\resumeItemListStart\n");
                result.append(orphanItems);
                result.append("\\resumeItemListEnd\n");
            }
        }
        
        return result.toString();
    }

    /**
     * Strip markdown fences and narrative text from AI response
     */
    private String stripMarkdownAndNarration(String input) {
        String result = input;
        
        // Remove markdown code fences (```latex, ```, etc.)
        result = result.replaceAll("```+\\s*latex\\s*\\n", "");
        result = result.replaceAll("```+\\s*tex\\s*\\n", "");
        result = result.replaceAll("```+\\s*\\n", "");
        result = result.replaceAll("\\n```+\\s*$", "");
        result = result.replaceAll("^```+\\s*", "");
        
        // Remove common AI narration lines (multiline mode)
        String[] narrativePatterns = {
            "(?m)^\\s*Here\\s+is\\s+the\\s+LaTeX.*$",
            "(?m)^\\s*Here's\\s+the\\s+LaTeX.*$",
            "(?m)^\\s*Below\\s+is\\s+the\\s+LaTeX.*$",
            "(?m)^\\s*The\\s+following\\s+is.*$",
            "(?m)^\\s*I've\\s+created.*$",
            "(?m)^\\s*This\\s+LaTeX\\s+code.*$"
        };
        
        for (String pattern : narrativePatterns) {
            result = result.replaceAll(pattern, "");
        }
        
        return result.trim();
    }

    /**
     * Extract LaTeX between sentinels with multiple fallback methods
     */
    private String extractLatexWithSentinels(String input) {
        // Method 1: Look for our custom sentinels
        final String START_SENTINEL = "%__BEGIN_LATEX__";
        final String END_SENTINEL = "%__END_LATEX__";
        
        int startIndex = input.indexOf(START_SENTINEL);
        int endIndex = input.indexOf(END_SENTINEL);
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String extracted = input.substring(startIndex + START_SENTINEL.length(), endIndex).trim();
            log.info("Extracted LaTeX using sentinels (length: {})", extracted.length());
            return extracted;
        }
        
        // Method 2: Look for complete document structure
        int docClassStart = input.indexOf("\\documentclass");
        int docEndIndex = input.lastIndexOf("\\end{document}");
        
        if (docClassStart != -1 && docEndIndex != -1 && docEndIndex > docClassStart) {
            String extracted = input.substring(docClassStart, docEndIndex + "\\end{document}".length()).trim();
            log.info("Extracted LaTeX using documentclass markers (length: {})", extracted.length());
            return extracted;
        }
        
        // Method 3: Look for \begin{document} ... \end{document}
        int beginDocStart = input.indexOf("\\begin{document}");
        if (beginDocStart != -1 && docEndIndex != -1 && docEndIndex > beginDocStart) {
            // Extract body and wrap with template
            String body = input.substring(beginDocStart + "\\begin{document}".length(), docEndIndex).trim();
            log.info("Extracted document body, wrapping with template (length: {})", body.length());
            return wrapInBasicDocument(body);
        }
        
        // Method 4: If it contains LaTeX commands, wrap it
        if (containsLatexCommands(input)) {
            log.info("Input contains LaTeX commands, wrapping with document structure");
            return wrapInBasicDocument(input.trim());
        }
        
        // Method 5: Return as-is and let ensureDocumentStructure handle it
        log.warn("No clear LaTeX structure found, returning stripped input");
        return input.trim();
    }
    
    /**
     * Check if the input contains recognizable LaTeX commands
     */
    private boolean containsLatexCommands(String input) {
        String[] latexIndicators = {
            "\\section", "\\subsection", "\\resumeItem", "\\resumeSubheading",
            "\\begin{", "\\end{", "\\textbf{", "\\textit{", "\\item",
            "\\href{", "\\small", "\\large", "\\centering"
        };
        
        for (String indicator : latexIndicators) {
            if (input.contains(indicator)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wrap content in Jake's LaTeX template when no structure is found
     * Uses centralized LatexTemplateService
     */
    private String wrapInBasicDocument(String bodyContent) {
        return latexTemplateService.wrapInTemplate(bodyContent);
    }

    /**
     * Generate a fallback LaTeX document when all extraction fails
     * Uses centralized LatexTemplateService
     */
    private String generateFallbackLatexDocument() {
        return latexTemplateService.generateFallbackDocument();
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
        boolean configured = groqApiKey != null && 
               !groqApiKey.trim().isEmpty() && 
               !groqApiKey.equals("your-groq-api-key") &&
               !groqApiKey.startsWith("${");
        
        log.info("=== GROQ API KEY CHECK ===");
        log.info("API Key null: {}", groqApiKey == null);
        log.info("API Key empty: {}", groqApiKey != null && groqApiKey.trim().isEmpty());
        log.info("API Key is placeholder: {}", groqApiKey != null && groqApiKey.equals("your-groq-api-key"));
        log.info("API Key starts with ${}: {}", groqApiKey != null && groqApiKey.startsWith("${"));
        log.info("API Key configured: {}", configured);
        if (groqApiKey != null) {
            log.info("API Key length: {}", groqApiKey.length());
            log.info("API Key prefix: {}", groqApiKey.substring(0, Math.min(10, groqApiKey.length())));
        }
        log.info("=== END API KEY CHECK ===");
        
        return configured;
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
