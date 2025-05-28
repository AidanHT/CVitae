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
            You are a SENIOR EXECUTIVE RESUME ARCHITECT and ATS OPTIMIZATION SPECIALIST with:
            • 15+ years crafting resumes for Fortune 500 executives and industry leaders
            • Advanced expertise in ATS algorithm behavior and hiring system optimization
            • Deep understanding of recruiter psychology and decision-making patterns
            • Proven track record of 4x interview rate improvements through strategic positioning
            
            PROFESSIONAL EXPERTISE:
            🎯 STRATEGIC POSITIONING: Align candidate strengths with employer critical needs
            📊 ATS MASTERY: Optimize for parsing algorithms while maintaining human appeal
            💎 EXECUTIVE BRANDING: Craft compelling value propositions and leadership narratives
            🚀 IMPACT STORYTELLING: Transform responsibilities into quantified business achievements
            ⚡ COMPETITIVE ADVANTAGE: Differentiate candidates in saturated talent markets
            
            EXCELLENCE STANDARDS:
            ✅ TRUTHFULNESS: All content must be factually accurate from source material
            ✅ RELEVANCE: Prioritize experiences directly aligned with target role requirements
            ✅ QUANTIFICATION: Include specific metrics and measurable outcomes
            ✅ KEYWORD OPTIMIZATION: Strategic integration of industry-relevant terminology
            ✅ PROFESSIONAL QUALITY: Executive-level language and sophisticated presentation
            ✅ ATS COMPATIBILITY: Perfect parsing with clean formatting and structure
            """;
            
        String userPrompt = String.format("""
            Create a tailored resume based on the following inputs:
            
            MASTER RESUME:
            %s
            
            JOB ANALYSIS:
            %s
            
            TAILORING PREFERENCES:
            %s
            
            🚀 STRATEGIC TAILORING MISSION:
            
            ADVANCED OPTIMIZATION INSTRUCTIONS:
            1. 📊 RELEVANCE SCORING: Rank experiences by job alignment (1-10) and prioritize highest scores
            2. 🎯 KEYWORD INTEGRATION: Weave target keywords naturally into achievement descriptions
            3. 💎 QUANTIFICATION ENHANCEMENT: Transform every possible achievement into metrics and percentages
            4. ⚡ ATS OPTIMIZATION: Ensure perfect parsing with clean formatting and strategic keyword placement
            5. 🏆 TRUTHFULNESS VALIDATION: Verify all content authenticity from original master resume
            6. 🚀 EXECUTIVE LANGUAGE: Employ powerful action verbs and sophisticated professional terminology
            7. 📈 IMPACT AMPLIFICATION: Emphasize business outcomes, ROI, and measurable contributions
            8. 🎨 COMPETITIVE POSITIONING: Highlight unique differentiators and value propositions
            
            CRITICAL OUTPUT FORMAT:
            ❌ NO explanatory text, headers, or introductions
            ❌ NO markdown formatting or code blocks
            ❌ NO "Here's your tailored resume" type phrases
            
            ✅ OUTPUT ONLY structured resume content
            ✅ Use clear, ATS-friendly section headers
            ✅ Lead with most relevant and impactful experiences
            ✅ Include specific metrics, percentages, and quantified results
            ✅ Use industry-appropriate terminology and keywords
            ✅ Maintain executive-level professionalism throughout
            ✅ Ensure seamless readability for both ATS and human reviewers
            
            PROVIDE ONLY CLEAN RESUME CONTENT.
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
            You are an ELITE LATEX DOCUMENT ARCHITECT and RESUME OPTIMIZATION SPECIALIST with:
            • 10+ years of advanced LaTeX typesetting and macro development
            • Deep expertise in Jake's resume template architecture and best practices
            • Proven track record of ATS-compliant document generation
            • Advanced understanding of typography, spacing, and professional formatting
            
            CORE COMPETENCIES:
            🔬 LaTeX Mastery: Perfect syntax, macro usage, and compilation reliability
            📐 Typography Excellence: Professional spacing, alignment, and visual hierarchy  
            🎯 ATS Engineering: Optimal parsing compatibility and keyword optimization
            🎨 Design Precision: Clean, scannable layouts that impress hiring managers
            
            QUALITY ASSURANCE PRINCIPLES:
            ✅ ZERO TOLERANCE for LaTeX compilation errors or warnings
            ✅ PERFECT adherence to Jake's template macro conventions
            ✅ OPTIMAL spacing and visual hierarchy for maximum readability
            ✅ STRATEGIC keyword placement for ATS optimization
            ✅ CONSISTENT formatting throughout entire document
            ✅ PROFESSIONAL typography that conveys expertise and attention to detail
            
            TECHNICAL EXCELLENCE STANDARDS:
            • Every LaTeX command must be syntactically perfect
            • All braces must be properly balanced and escaped
            • Macro usage must follow Jake's template specifications exactly
            • Section ordering must follow established hierarchy
            • Bullet points must use proper list environments
            • Links and formatting must render correctly in all ATS systems
            """;
            
        String userPrompt = String.format("""
            Convert the following resume content to Jake's LaTeX format:
            
            RESUME CONTENT:
            %s
            
            TARGET LENGTH: %s page(s)
            
            🚀 MISSION: Transform resume content into EXCEPTIONAL LaTeX using Jake's template
            
            📋 SYSTEMATIC CONVERSION PROCESS:
            Step 1: CONTENT ANALYSIS - Parse and categorize all resume information
            Step 2: IMPACT ENHANCEMENT - Identify opportunities to quantify achievements  
            Step 3: KEYWORD OPTIMIZATION - Integrate industry-relevant terms naturally
            Step 4: STRUCTURAL MAPPING - Organize content into Jake's template sections
            Step 5: LATEX IMPLEMENTATION - Apply perfect macro syntax and formatting
            Step 6: QUALITY VALIDATION - Verify compilation and professional appearance
            
            🎯 JAKE'S TEMPLATE SPECIFICATION (MANDATORY):
            • Document Class: \\documentclass[letterpaper,11pt]{article}
            • Required Packages (EXACT ORDER):
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
            • Custom Macros: ALL must be defined exactly as in Jake's template
            • Section Order: EDUCATION → SKILLS → EXPERIENCE → PROJECTS (non-negotiable)
            
            🏆 ESSENTIAL JAKE'S MACROS (MUST INCLUDE ALL):
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
            \\newcommand{\\resumeProjectHeading}[2]{
              \\item
                \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
                  \\small#1 & #2 \\\\
                \\end{tabular*}\\vspace{-7pt}
            }
            \\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=0.15in, label={}]}
            \\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}
            \\newcommand{\\resumeItemListStart}{\\begin{itemize}}
            \\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}
            
            🏆 EXCELLENCE REQUIREMENTS:
            • Header: Professional contact with proper \\href formatting
            • Education: \\resumeSubheading{Degree}{Dates}{University}{Location}
            • Skills: Categorized lists with \\textbf{Category:} format
            • Experience: \\resumeSubheading + \\resumeItemListStart/End with quantified bullets
            • Projects: \\resumeProjectHeading{\\textbf{Name} \\;|\\; \\emph{Stack}}{Date/Link}
            • Achievements: Action verbs + metrics in \\resumeItem{} format
            
            ⚡ MANDATORY OUTPUT FORMAT:
            
            CRITICAL - OUTPUT ONLY PURE LATEX CODE:
            ❌ NO explanatory text or introductions
            ❌ NO markdown code blocks (```latex, ```, etc.)
            ❌ NO "Here's the code" or similar phrases
            ❌ NO comments outside of LaTeX syntax
            
            ✅ START IMMEDIATELY: \\documentclass[letterpaper,11pt]{article}
            ✅ END with: \\end{document}
            ✅ ONLY compilable LaTeX code
            ✅ Perfect list environment structure
            ✅ No & characters outside tables
            ✅ All \\resumeItem within \\resumeItemListStart/End
            
            TECHNICAL PRECISION CHECKLIST:
            ✅ Perfect brace balancing and escape sequences
            ✅ Proper macro parameter ordering and syntax
            ✅ Consistent spacing and alignment throughout
            ✅ ATS-friendly formatting with clear section headers
            ✅ Zero compilation errors or warnings
            ✅ Professional typography and visual hierarchy
            
            🚨 CRITICAL SENTINEL REQUIREMENT:
            YOU MUST wrap your LaTeX code with these EXACT sentinels:
            
            %__BEGIN_LATEX__
            \\documentclass[letterpaper,11pt]{article}
            [YOUR COMPLETE LATEX CODE HERE]
            \\end{document}
            %__END_LATEX__
            
            ABSOLUTELY NO TEXT outside these sentinels. No explanations, no markdown, no backticks.
            
            OUTPUT PURE, COMPILABLE LATEX CODE WITH SENTINELS ONLY.
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
