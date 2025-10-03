package com.cvitae.service;

import com.cvitae.dto.ResumeData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service that programmatically builds Jake's LaTeX resume from structured data.
 * This ensures perfect LaTeX syntax every time by using a fixed template
 * and only inserting properly escaped data.
 */
@Service
@Slf4j
public class LatexResumeBuilder {

    /**
     * Build a complete LaTeX resume from structured data using Jake's template.
     * 
     * @param data The structured resume data extracted by AI
     * @return Complete, compilable LaTeX document
     */
    public String buildResume(ResumeData data) {
        if (data == null) {
            log.warn("ResumeData is null, returning fallback template");
            return buildFallbackResume();
        }
        
        StringBuilder latex = new StringBuilder();
        
        // Add preamble (fixed - never changes)
        latex.append(getPreamble());
        
        // Add custom commands (fixed - never changes)
        latex.append(getCustomCommands());
        
        // Begin document
        latex.append("%-------------------------------------------\n");
        latex.append("%%%%%%  RESUME STARTS HERE  %%%%%%%%%%%%%%%%%%%%%%%%%%%%\n\n\n");
        latex.append("\\begin{document}\n\n");
        
        // SECTION ORDER MUST MATCH JAKE'S TEMPLATE EXACTLY:
        // 1. Header/Heading
        // 2. Education
        // 3. Experience
        // 4. Projects
        // 5. Technical Skills
        
        // Add header with contact info
        latex.append(buildHeader(data));
        
        // Add education section (FIRST after header per Jake's template)
        if (data.getEducation() != null && !data.getEducation().isEmpty()) {
            latex.append(buildEducationSection(data.getEducation()));
        }
        
        // Add experience section (SECOND per Jake's template)
        if (data.getExperience() != null && !data.getExperience().isEmpty()) {
            latex.append(buildExperienceSection(data.getExperience()));
        }
        
        // Add projects section (THIRD per Jake's template)
        if (data.getProjects() != null && !data.getProjects().isEmpty()) {
            latex.append(buildProjectsSection(data.getProjects()));
        }
        
        // Add technical skills section (LAST per Jake's template)
        if (data.getSkills() != null) {
            latex.append(buildSkillsSection(data.getSkills()));
        }
        
        // End document
        latex.append("\n%-------------------------------------------\n");
        latex.append("\\end{document}\n");
        
        return latex.toString();
    }
    
    /**
     * Jake's LaTeX preamble with all required packages
     */
    private String getPreamble() {
        return """
            %-------------------------
            % Resume in Latex
            % Author : Jake Gutierrez
            % Based off of: https://github.com/sb2nov/resume
            % License : MIT
            %------------------------
            
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
            \\fancyhf{} % clear all header and footer fields
            \\fancyfoot{}
            \\renewcommand{\\headrulewidth}{0pt}
            \\renewcommand{\\footrulewidth}{0pt}
            
            % Adjust margins
            \\addtolength{\\oddsidemargin}{-0.5in}
            \\addtolength{\\evensidemargin}{-0.5in}
            \\addtolength{\\textwidth}{1in}
            \\addtolength{\\topmargin}{-.5in}
            \\addtolength{\\textheight}{1.0in}
            
            \\urlstyle{same}
            
            \\raggedbottom
            \\raggedright
            \\setlength{\\tabcolsep}{0in}
            
            % Sections formatting
            \\titleformat{\\section}{
              \\vspace{-4pt}\\scshape\\raggedright\\large
            }{}{0em}{}[\\color{black}\\titlerule \\vspace{-5pt}]
            
            % Ensure that generate pdf is machine readable/ATS parsable
            \\pdfgentounicode=1
            
            """;
    }
    
    /**
     * Jake's custom LaTeX commands
     */
    private String getCustomCommands() {
        return """
            %-------------------------
            % Custom commands
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
            
            """;
    }
    
    /**
     * Build the header section with name and contact info
     */
    private String buildHeader(ResumeData data) {
        StringBuilder header = new StringBuilder();
        header.append("%-----------HEADING-----------\n");
        header.append("\\begin{center}\n");
        header.append("    \\textbf{\\Huge \\scshape ").append(escapeLatex(data.getName())).append("} \\\\ \\vspace{1pt}\n");
        header.append("    \\small ");
        
        // Build contact line with $|$ separators
        StringBuilder contacts = new StringBuilder();
        
        if (data.getPhone() != null && !data.getPhone().isEmpty()) {
            contacts.append(escapeLatex(data.getPhone()));
        }
        
        if (data.getEmail() != null && !data.getEmail().isEmpty()) {
            if (contacts.length() > 0) contacts.append(" $|$ ");
            contacts.append("\\href{mailto:").append(data.getEmail()).append("}{\\underline{").append(escapeLatex(data.getEmail())).append("}}");
        }
        
        if (data.getLinkedin() != null && !data.getLinkedin().isEmpty()) {
            if (contacts.length() > 0) contacts.append(" $|$\n    ");
            String linkedinUrl = data.getLinkedinUrl() != null ? data.getLinkedinUrl() : "https://linkedin.com/in/" + data.getLinkedin();
            contacts.append("\\href{").append(linkedinUrl).append("}{\\underline{").append(escapeLatex(data.getLinkedin())).append("}}");
        }
        
        if (data.getGithub() != null && !data.getGithub().isEmpty()) {
            if (contacts.length() > 0) contacts.append(" $|$\n    ");
            String githubUrl = data.getGithubUrl() != null ? data.getGithubUrl() : "https://github.com/" + data.getGithub();
            contacts.append("\\href{").append(githubUrl).append("}{\\underline{").append(escapeLatex(data.getGithub())).append("}}");
        }
        
        header.append(contacts);
        header.append("\n\\end{center}\n\n");
        
        return header.toString();
    }
    
    /**
     * Build the education section
     */
    private String buildEducationSection(List<ResumeData.Education> education) {
        StringBuilder section = new StringBuilder();
        section.append("%-----------EDUCATION-----------\n");
        section.append("\\section{Education}\n");
        section.append("  \\resumeSubHeadingListStart\n");
        
        for (ResumeData.Education edu : education) {
            section.append("    \\resumeSubheading\n");
            section.append("      {").append(escapeLatex(edu.getSchool())).append("}{").append(escapeLatex(edu.getLocation())).append("}\n");
            section.append("      {").append(escapeLatex(edu.getDegree())).append("}{").append(escapeLatex(edu.getDates())).append("}\n");
            
            // Add highlights if present (GPA, honors, etc.)
            if (edu.getHighlights() != null && !edu.getHighlights().isEmpty()) {
                section.append("    \\resumeItemListStart\n");
                for (String highlight : edu.getHighlights()) {
                    section.append("      \\resumeItem{").append(escapeLatex(highlight)).append("}\n");
                }
                section.append("    \\resumeItemListEnd\n");
            }
        }
        
        section.append("  \\resumeSubHeadingListEnd\n\n");
        return section.toString();
    }
    
    /**
     * Build the experience section
     */
    private String buildExperienceSection(List<ResumeData.Experience> experiences) {
        StringBuilder section = new StringBuilder();
        section.append("%-----------EXPERIENCE-----------\n");
        section.append("\\section{Experience}\n");
        section.append("  \\resumeSubHeadingListStart\n");
        
        for (ResumeData.Experience exp : experiences) {
            section.append("    \\resumeSubheading\n");
            section.append("      {").append(escapeLatex(exp.getTitle())).append("}{").append(escapeLatex(exp.getDates())).append("}\n");
            section.append("      {").append(escapeLatex(exp.getCompany())).append("}{").append(escapeLatex(exp.getLocation())).append("}\n");
            
            if (exp.getBullets() != null && !exp.getBullets().isEmpty()) {
                section.append("      \\resumeItemListStart\n");
                for (String bullet : exp.getBullets()) {
                    section.append("        \\resumeItem{").append(escapeLatex(bullet)).append("}\n");
                }
                section.append("      \\resumeItemListEnd\n");
            }
            section.append("\n");
        }
        
        section.append("  \\resumeSubHeadingListEnd\n\n");
        return section.toString();
    }
    
    /**
     * Build the projects section
     */
    private String buildProjectsSection(List<ResumeData.Project> projects) {
        StringBuilder section = new StringBuilder();
        section.append("%-----------PROJECTS-----------\n");
        section.append("\\section{Projects}\n");
        section.append("    \\resumeSubHeadingListStart\n");
        
        for (ResumeData.Project proj : projects) {
            section.append("      \\resumeProjectHeading\n");
            section.append("          {\\textbf{").append(escapeLatex(proj.getName())).append("}");
            if (proj.getTechStack() != null && !proj.getTechStack().isEmpty()) {
                section.append(" $|$ \\emph{").append(escapeLatex(proj.getTechStack())).append("}");
            }
            section.append("}{").append(escapeLatex(proj.getDates())).append("}\n");
            
            if (proj.getBullets() != null && !proj.getBullets().isEmpty()) {
                section.append("          \\resumeItemListStart\n");
                for (String bullet : proj.getBullets()) {
                    section.append("            \\resumeItem{").append(escapeLatex(bullet)).append("}\n");
                }
                section.append("          \\resumeItemListEnd\n");
            }
        }
        
        section.append("    \\resumeSubHeadingListEnd\n\n");
        return section.toString();
    }
    
    /**
     * Build the technical skills section using Jake's exact format
     */
    private String buildSkillsSection(ResumeData.Skills skills) {
        StringBuilder section = new StringBuilder();
        section.append("%\n%-----------PROGRAMMING SKILLS-----------\n");
        section.append("\\section{Technical Skills}\n");
        section.append(" \\begin{itemize}[leftmargin=0.15in, label={}]\n");
        section.append("    \\small{\\item{\n");
        
        StringBuilder skillLines = new StringBuilder();
        
        if (skills.getLanguages() != null && !skills.getLanguages().isEmpty()) {
            skillLines.append("     \\textbf{Languages}{: ").append(escapeLatex(skills.getLanguages())).append("}");
        }
        
        if (skills.getFrameworks() != null && !skills.getFrameworks().isEmpty()) {
            if (skillLines.length() > 0) skillLines.append(" \\\\\n");
            skillLines.append("     \\textbf{Frameworks}{: ").append(escapeLatex(skills.getFrameworks())).append("}");
        }
        
        if (skills.getDeveloperTools() != null && !skills.getDeveloperTools().isEmpty()) {
            if (skillLines.length() > 0) skillLines.append(" \\\\\n");
            skillLines.append("     \\textbf{Developer Tools}{: ").append(escapeLatex(skills.getDeveloperTools())).append("}");
        }
        
        if (skills.getDatabases() != null && !skills.getDatabases().isEmpty()) {
            if (skillLines.length() > 0) skillLines.append(" \\\\\n");
            skillLines.append("     \\textbf{Databases}{: ").append(escapeLatex(skills.getDatabases())).append("}");
        }
        
        if (skills.getLibraries() != null && !skills.getLibraries().isEmpty()) {
            if (skillLines.length() > 0) skillLines.append(" \\\\\n");
            skillLines.append("     \\textbf{Libraries}{: ").append(escapeLatex(skills.getLibraries())).append("}");
        }
        
        if (skills.getOther() != null && !skills.getOther().isEmpty()) {
            if (skillLines.length() > 0) skillLines.append(" \\\\\n");
            skillLines.append("     \\textbf{Other}{: ").append(escapeLatex(skills.getOther())).append("}");
        }
        
        section.append(skillLines);
        section.append("\n    }}\n");
        section.append(" \\end{itemize}\n\n");
        
        return section.toString();
    }
    
    /**
     * Sanitize input - treat placeholder text as empty
     * This catches cases where AI outputs "Not Specified", "N/A", etc.
     */
    private String sanitizeInput(String input) {
        if (input == null) return "";
        
        String trimmed = input.trim();
        
        // List of placeholder patterns to treat as empty
        String lowerTrimmed = trimmed.toLowerCase();
        if (lowerTrimmed.equals("not specified") ||
            lowerTrimmed.equals("n/a") ||
            lowerTrimmed.equals("na") ||
            lowerTrimmed.equals("none") ||
            lowerTrimmed.equals("unknown") ||
            lowerTrimmed.equals("null") ||
            lowerTrimmed.equals("undefined") ||
            lowerTrimmed.equals("-") ||
            lowerTrimmed.equals("--") ||
            lowerTrimmed.equals("tbd") ||
            lowerTrimmed.equals("to be determined") ||
            lowerTrimmed.startsWith("not available") ||
            lowerTrimmed.startsWith("no ") ||
            trimmed.isEmpty()) {
            return "";
        }
        
        return trimmed;
    }
    
    /**
     * Escape special LaTeX characters in user-provided text
     */
    private String escapeLatex(String input) {
        if (input == null) return "";
        
        // First sanitize to remove placeholder text
        String sanitized = sanitizeInput(input);
        if (sanitized.isEmpty()) return "";
        
        return sanitized
            .replace("\\", "\\textbackslash ")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("^", "\\textasciicircum ")
            .replace("~", "\\textasciitilde ")
            // Handle common Unicode characters
            .replace("\u2013", "--")   // en dash
            .replace("\u2014", "---")  // em dash
            .replace("\u201C", "``")   // left double quote
            .replace("\u201D", "''")   // right double quote
            .replace("\u2018", "`")    // left single quote
            .replace("\u2019", "'")    // right single quote
            .replace("\u2026", "...");  // ellipsis
    }
    
    /**
     * Build a fallback resume when data is missing
     */
    private String buildFallbackResume() {
        return getPreamble() + getCustomCommands() + """
            \\begin{document}
            
            \\begin{center}
                \\textbf{\\Huge \\scshape Resume} \\\\ \\vspace{1pt}
                \\small Please provide your resume content to generate a formatted PDF.
            \\end{center}
            
            \\section{Notice}
            \\begin{itemize}[leftmargin=0.15in, label={}]
                \\item This is a placeholder document.
                \\item Please submit your resume content to generate a properly formatted PDF.
            \\end{itemize}
            
            \\end{document}
            """;
    }
}
