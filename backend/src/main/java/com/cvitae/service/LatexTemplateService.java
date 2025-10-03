package com.cvitae.service;

import org.springframework.stereotype.Service;

/**
 * Centralized service for Jake's LaTeX resume template.
 * This eliminates duplication across GroqClient, ExportServiceImpl, and ResumeTailorService.
 * 
 * The template includes all required macro definitions for Jake's resume format,
 * ensuring consistency and preventing "undefined control sequence" errors.
 */
@Service
public class LatexTemplateService {
    
    /**
     * Get the complete Jake's LaTeX template with all macro definitions.
     * Use %__BODY__ as placeholder for the resume content.
     */
    public String getJakesTemplate() {
        return JAKES_LATEX_TEMPLATE;
    }
    
    /**
     * Wrap body content in Jake's LaTeX template.
     * 
     * @param bodyContent The resume content to wrap (sections, items, etc.)
     * @return Complete LaTeX document ready for compilation
     */
    public String wrapInTemplate(String bodyContent) {
        if (bodyContent == null || bodyContent.trim().isEmpty()) {
            return JAKES_LATEX_TEMPLATE.replace("%__BODY__", getFallbackBody());
        }
        return JAKES_LATEX_TEMPLATE.replace("%__BODY__", bodyContent.trim());
    }
    
    /**
     * Generate a fallback document when AI generation fails.
     * 
     * @return Complete LaTeX document with fallback notice
     */
    public String generateFallbackDocument() {
        return wrapInTemplate(getFallbackBody());
    }
    
    /**
     * Generate a sample resume body with placeholder content.
     * 
     * @param jobTitle Target job title
     * @param companyName Target company name
     * @return LaTeX body content
     */
    public String generateSampleBody(String jobTitle, String companyName) {
        String safeJobTitle = escapeLatex(jobTitle != null ? jobTitle : "Professional");
        String safeCompanyName = escapeLatex(companyName != null ? companyName : "Target Company");
        
        return String.format(SAMPLE_RESUME_BODY, safeJobTitle, safeCompanyName);
    }
    
    /**
     * Escape special LaTeX characters in user-provided text.
     * 
     * @param input Raw text that may contain special characters
     * @return Text safe for LaTeX compilation
     */
    public String escapeLatex(String input) {
        if (input == null) return "";
        
        return input
            .replace("\\", "\\textbackslash ")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("^", "\\textasciicircum ")
            .replace("~", "\\textasciitilde ");
    }
    
    /**
     * Get fallback body content for error cases
     */
    private String getFallbackBody() {
        return """
            %% HEADER SECTION
            \\begin{center}
                {\\textbf{\\Huge \\scshape Professional Resume}} \\\\ \\vspace{1pt}
                \\small AI Generation Notice
            \\end{center}
            
            \\section{NOTICE}
            \\begin{itemize}[leftmargin=0.15in, label={}]
                \\item \\textbf{Status:} This is a fallback document generated due to processing issues.
                \\item \\textbf{Action Required:} Please regenerate your resume to get the proper content.
                \\item \\textbf{Support:} If this issue persists, check your API configuration.
            \\end{itemize}
            
            \\section{TROUBLESHOOTING}
            \\begin{itemize}[leftmargin=0.15in, label={}]
                \\item Verify your Groq API key is properly configured
                \\item Check that your resume content doesn't contain special characters
                \\item Ensure all required fields are filled out
                \\item Try regenerating with different preferences
            \\end{itemize}
            """;
    }
    
    /**
     * Jake's LaTeX template with all required macro definitions.
     * This is the single source of truth for the resume template.
     */
    private static final String JAKES_LATEX_TEMPLATE = """
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
            \\setlength{\\headheight}{14pt}
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
            
            %% Education-specific subheading
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
            
            %% Alternative subheading styles
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
            
            %% Additional macros
            \\newcommand{\\resumeAward}[2]{
              \\item \\textbf{#1} \\hfill #2
            }
            
            \\newcommand{\\resumeCertification}[2]{
              \\item #1 \\hfill \\textit{#2}
            }
            
            %% Legacy macro for compatibility
            \\newcommand{\\resumeSubItem}[1]{\\resumeItem{#1}\\vspace{-4pt}}
            
            %% Safe text escaping helpers
            \\newcommand{\\safeampersand}{\\&}
            \\newcommand{\\safedollar}{\\$}
            \\newcommand{\\safepercent}{\\%}
            \\newcommand{\\safeunderscore}{\\_}
            
            %% ========== DOCUMENT CONTENT ==========
            \\begin{document}
            
            %__BODY__
            
            \\end{document}
            """;
    
    /**
     * Sample resume body template with placeholders
     */
    private static final String SAMPLE_RESUME_BODY = """
            %% HEADER SECTION
            \\begin{center}
                {\\textbf{\\Huge \\scshape Professional Resume}} \\\\ \\vspace{1pt}
                \\small Phone \\;|\\; \\href{mailto:email@example.com}{\\underline{email@example.com}} \\;|\\;
                \\href{https://linkedin.com/in/profile}{\\underline{linkedin.com/in/profile}} \\;|\\;
                \\href{https://github.com/username}{\\underline{github.com/username}}
            \\end{center}
            
            %% EDUCATION SECTION
            \\section{EDUCATION}
            \\resumeSubHeadingListStart
                \\resumeSubheading
                    {Bachelor of Science in Computer Science}{Expected May 2024}
                    {University Name}{City, State}
            \\resumeSubHeadingListEnd
            
            %% SKILLS SECTION
            \\section{TECHNICAL SKILLS}
            \\begin{itemize}[leftmargin=0.15in, label={}]
                \\small
                \\resumeSkillItem{Programming Languages}{Java, Python, JavaScript, TypeScript, SQL}
                \\resumeSkillItem{Frameworks \\& Libraries}{React, Node.js, Spring Boot, FastAPI}
                \\resumeSkillItem{Developer Tools}{Git, Docker, AWS, MongoDB, PostgreSQL}
                \\resumeSkillItem{Operating Systems}{Linux, macOS, Windows}
            \\end{itemize}
            
            %% EXPERIENCE SECTION
            \\section{EXPERIENCE}
            \\resumeSubHeadingListStart
                \\resumeSubheading
                    {%s}{January 2024 -- Present}
                    {%s}{City, State}
                \\resumeItemListStart
                    \\resumeItem{Developed and maintained enterprise applications using modern frameworks}
                    \\resumeItem{Collaborated with cross-functional teams to deliver high-quality software solutions}
                    \\resumeItem{Implemented best practices for code quality and performance optimization}
                    \\resumeItem{Reduced system response time by 40\\%% through database optimization}
                \\resumeItemListEnd
            \\resumeSubHeadingListEnd
            
            %% PROJECTS SECTION
            \\section{PROJECTS}
            \\resumeSubHeadingListStart
                \\resumeProjectHeading
                    {\\textbf{Professional Project} \\;|\\; \\emph{Java, Spring Boot, React, PostgreSQL}}{2024}
                \\resumeItemListStart
                    \\resumeItem{Built a full-stack web application with modern technologies}
                    \\resumeItem{Implemented secure authentication and authorization systems}
                    \\resumeItem{Achieved 99.9\\%% uptime through robust error handling and monitoring}
                \\resumeItemListEnd
            \\resumeSubHeadingListEnd
            """;
}
