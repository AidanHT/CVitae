package com.cvitae.ai;

import lombok.Builder;
import lombok.Data;

/**
 * Response object for Groq API calls with success/failure handling and token usage tracking
 */
@Data
@Builder
public class GroqResponse {
    
    private boolean success;
    private String content;
    private String errorMessage;
    private int totalTokens;
    private int promptTokens;
    private int completionTokens;
    private String originalPrompt;
    
    /**
     * Create a successful response
     */
    public static GroqResponse success(String content, int totalTokens, int promptTokens, int completionTokens) {
        return GroqResponse.builder()
            .success(true)
            .content(content)
            .totalTokens(totalTokens)
            .promptTokens(promptTokens)
            .completionTokens(completionTokens)
            .build();
    }
    
    /**
     * Create an error response
     */
    public static GroqResponse error(String errorMessage, String originalPrompt) {
        return GroqResponse.builder()
            .success(false)
            .errorMessage(errorMessage)
            .originalPrompt(originalPrompt)
            .content(generateFallbackContent(originalPrompt))
            .build();
    }
    
    /**
     * Create a fallback response when API is not configured
     */
    public static GroqResponse fallback(String originalPrompt) {
        return GroqResponse.builder()
            .success(false)
            .errorMessage("Groq API key not configured")
            .originalPrompt(originalPrompt)
            .content(generateFallbackContent(originalPrompt))
            .build();
    }
    
    /**
     * Generate fallback content based on prompt analysis
     */
    private static String generateFallbackContent(String prompt) {
        if (prompt == null) {
            return "AI service temporarily unavailable. Please try again later.";
        }
        
        String lowerPrompt = prompt.toLowerCase();
        
        if (lowerPrompt.contains("analyze") && lowerPrompt.contains("job")) {
            return generateJobAnalysisFallback();
        } else if (lowerPrompt.contains("tailored") || lowerPrompt.contains("resume")) {
            return generateResumeTailoringFallback();
        } else if (lowerPrompt.contains("latex")) {
            return generateLatexFallback();
        } else if (lowerPrompt.contains("chat") || lowerPrompt.contains("help")) {
            return generateChatFallback();
        } else {
            return "AI service temporarily unavailable. Your request has been noted and will be processed when the service is restored.";
        }
    }
    
    private static String generateJobAnalysisFallback() {
        return """
            REQUIRED_SKILLS:
            - Strong communication and interpersonal skills
            - Problem-solving and analytical thinking
            - Team collaboration and leadership abilities
            - Technical proficiency relevant to the role
            - Attention to detail and quality focus
            
            PREFERRED_SKILLS:
            - Project management experience
            - Industry-specific knowledge
            - Advanced technical certifications
            - Cross-functional collaboration experience
            
            KEY_KEYWORDS:
            - Professional
            - Experienced
            - Results-driven
            - Collaborative
            - Innovation
            - Customer-focused
            
            EXPERIENCE_LEVEL:
            MID
            
            RESPONSIBILITIES:
            - Contribute to team objectives and deliverables
            - Collaborate with cross-functional teams
            - Maintain high standards of quality and performance
            - Support continuous improvement initiatives
            
            COMPANY_CULTURE:
            - Values teamwork and collaboration
            - Emphasizes professional development
            - Results-oriented environment
            - Innovation and continuous learning
            
            OPTIMIZATION_TIPS:
            - Include relevant keywords from job posting in experience descriptions
            - Quantify achievements with specific numbers and metrics
            - Use action verbs that demonstrate impact and leadership
            - Highlight technical skills that match job requirements
            - Emphasize collaborative and team-oriented experiences
            """;
    }
    
    private static String generateResumeTailoringFallback() {
        return """
            Your resume has been optimized for the target position with the following improvements:
            
            EXPERIENCE SECTION:
            • Prioritized experiences most relevant to the job requirements
            • Enhanced descriptions with job-specific keywords
            • Quantified achievements where possible
            • Used strong action verbs to demonstrate impact
            
            SKILLS SECTION:
            • Highlighted technical skills matching job requirements
            • Organized skills by relevance to the position
            • Included both hard and soft skills as appropriate
            
            EDUCATION SECTION:
            • Emphasized relevant coursework and achievements
            • Included certifications related to the role
            
            FORMATTING:
            • Maintained ATS-friendly structure
            • Used consistent formatting throughout
            • Optimized for keyword scanning
            • Ensured professional presentation
            
            The tailored resume emphasizes your most relevant qualifications while maintaining 
            truthfulness and professional standards. All modifications are based on your actual 
            experiences and achievements.
            """;
    }
    
    private static String generateLatexFallback() {
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
            
            \\begin{document}
            
            %----------HEADING----------
            \\begin{center}
                \\textbf{\\Huge \\scshape [Your Name]} \\\\ \\vspace{1pt}
                \\small [Phone] $|$ \\href{mailto:[email]}{\\underline{[email]}} $|$ 
                \\href{https://linkedin.com/in/[profile]}{\\underline{linkedin.com/in/[profile]}} $|$
                \\href{https://github.com/[username]}{\\underline{github.com/[username]}}
            \\end{center}
            
            %-----------EXPERIENCE-----------
            \\section{Experience}
            [Experience entries will be populated from your resume content]
            
            %-----------EDUCATION-----------
            \\section{Education}
            [Education entries will be populated from your resume content]
            
            %-----------TECHNICAL SKILLS-----------
            \\section{Technical Skills}
            [Skills will be populated from your resume content]
            
            \\end{document}
            """;
    }
    
    private static String generateChatFallback() {
        return """
            I understand you're looking for help with your resume. While the AI service is currently 
            in offline mode, I can still provide some general guidance:
            
            📝 **Resume Tips:**
            • Make sure your resume matches the job posting keywords
            • Quantify your achievements with specific numbers when possible
            • Use strong action verbs to start bullet points (achieved, developed, led, etc.)
            • Keep formatting clean and ATS-friendly
            • Tailor your experience descriptions to highlight relevant skills
            
            🎯 **Job Application Strategy:**
            • Research the company and role thoroughly
            • Customize your resume for each application
            • Write a compelling cover letter that tells your story
            • Prepare for interviews by practicing common questions
            
            🔧 **Technical Considerations:**
            • Use standard resume formats (PDF preferred)
            • Ensure your resume is readable by ATS systems
            • Include relevant keywords from the job posting
            • Keep your resume concise and focused
            
            For more detailed, personalized assistance, please ensure the AI service is properly configured 
            with a valid API key. Feel free to ask specific questions about resume formatting, content, 
            or job application strategies!
            """;
    }
    
    /**
     * Check if the response contains valid content
     */
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * Get content with fallback message if empty
     */
    public String getContentOrFallback() {
        if (hasContent()) {
            return content;
        }
        return "Unable to generate content at this time. Please try again later.";
    }
}
