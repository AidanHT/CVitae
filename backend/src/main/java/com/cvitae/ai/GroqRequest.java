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
    
    /**
     * Escape % characters in user input to prevent String.format() errors.
     * This is necessary because resume content often contains percentages like "40%".
     */
    private static String escapeForFormat(String input) {
        if (input == null) return "";
        return input.replace("%", "%%");
    }
    
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
            """, escapeForFormat(jobTitle), escapeForFormat(companyName), escapeForFormat(jobPosting));
            
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
            """, escapeForFormat(masterResume), escapeForFormat(jobAnalysis), escapeForFormat(preferences));
            
        return GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(4000)
            .temperature(0.4)
            .build();
    }
    
    /**
     * Create a request for LaTeX conversion using Jake's exact template format
     */
    public static GroqRequest forLatexConversion(String resumeContent, String targetLength) {
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
            RESUME CONTENT TO CONVERT:
            %s
            
            TARGET LENGTH: %s page(s)
            
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
                \\resumeSubheading
                  {Blinn College}{Bryan, TX}
                  {Associate's in Liberal Arts}{Aug. 2014 -- May 2018}
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
                    \\resumeItem{Explored ways to visualize GitHub collaboration in a classroom setting}
                  \\resumeItemListEnd

                \\resumeSubheading
                  {Information Technology Support Specialist}{Sep. 2018 -- Present}
                  {Southwestern University}{Georgetown, TX}
                  \\resumeItemListStart
                    \\resumeItem{Communicate with managers to set up campus computers used on campus}
                    \\resumeItem{Assess and troubleshoot computer problems brought by students, faculty and staff}
                    \\resumeItem{Maintain upkeep of computers, classroom equipment, and 200 printers across campus}
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
                        \\resumeItem{Visualized GitHub data to show collaboration}
                        \\resumeItem{Used Celery and Redis for asynchronous tasks}
                      \\resumeItemListEnd
                  \\resumeProjectHeading
                      {\\textbf{Simple Paintball} $|$ \\emph{Spigot API, Java, Maven, TravisCI, Git}}{May 2018 -- May 2020}
                      \\resumeItemListStart
                        \\resumeItem{Developed a Minecraft server plugin to entertain kids during free time for a previous job}
                        \\resumeItem{Published plugin to websites gaining 2K+ downloads and an average 4.5/5-star review}
                        \\resumeItem{Implemented continuous delivery using TravisCI to build the plugin upon new a release}
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
            """, escapeForFormat(resumeContent), escapeForFormat(targetLength));
            
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
            """, escapeForFormat(context), escapeForFormat(message), escapeForFormat(resumeContent), escapeForFormat(jobContent));
            
        return GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(1500)
            .temperature(0.7)
            .build();
    }
    
    /**
     * Create a request to extract structured resume data as JSON.
     * This is used to parse raw resume text into structured fields that can be
     * programmatically inserted into Jake's LaTeX template.
     */
    public static GroqRequest forResumeExtraction(String resumeContent, String jobPosting, String jobTitle, String companyName) {
        String systemPrompt = """
            You are a RESUME DATA EXTRACTION SPECIALIST. Your ONLY job is to extract structured data 
            from resume text and output it as valid JSON.
            
            CRITICAL RULES:
            1. Output ONLY valid JSON - no markdown, no explanations, no text before or after
            2. Extract ALL information from the resume - do not skip anything
            3. **IMPORTANT**: If a field is NOT present in the resume, use an EMPTY STRING "" - 
               NEVER use placeholder text like "Not Specified", "N/A", "Unknown", or similar.
               Empty string "" means the field will be left blank in the output.
            4. For arrays, use empty array [] if no items exist
            5. Keep the original wording from the resume - do not rewrite or embellish
            6. For bullet points, preserve the original achievement descriptions
            7. Parse dates exactly as they appear (e.g., "May 2025 - August 2025", "Present")
            8. If dates are not available for a project or experience, use "" (empty string)
            
            You must output a JSON object that matches this EXACT structure.
            """;
            
        String userPrompt = String.format("""
            RESUME TEXT TO EXTRACT:
            ════════════════════════════════════════
            %s
            ════════════════════════════════════════
            
            TARGET JOB (for context - use to prioritize relevant experience):
            Job Title: %s
            Company: %s
            Job Description: %s
            
            ════════════════════════════════════════
            OUTPUT FORMAT - Return ONLY this JSON structure:
            ════════════════════════════════════════
            
            {
              "name": "Full Name",
              "phone": "(123) 456-7890",
              "email": "email@example.com",
              "linkedin": "linkedin.com/in/username",
              "linkedinUrl": "https://linkedin.com/in/username",
              "github": "github.com/username",
              "githubUrl": "https://github.com/username",
              "education": [
                {
                  "school": "University Name",
                  "location": "City, State/Province",
                  "degree": "Degree Type in Major (honors if any)",
                  "dates": "Start Date - End Date",
                  "highlights": ["GPA: X.XX", "Dean's List", "Relevant coursework"]
                }
              ],
              "experience": [
                {
                  "title": "Job Title",
                  "dates": "Start Date - End Date",
                  "company": "Company Name",
                  "location": "City, State/Province",
                  "bullets": [
                    "Achievement or responsibility with metrics",
                    "Another achievement with quantified impact"
                  ]
                }
              ],
              "projects": [
                {
                  "name": "Project Name",
                  "techStack": "Technology1, Technology2, Technology3",
                  "dates": "Date or Link",
                  "bullets": [
                    "What you built and its impact",
                    "Technical details and achievements"
                  ]
                }
              ],
              "skills": {
                "languages": "Python, Java, JavaScript, C++, SQL",
                "frameworks": "React, Node.js, Spring Boot, FastAPI",
                "developerTools": "Git, Docker, AWS, VS Code",
                "databases": "PostgreSQL, MongoDB, Redis",
                "libraries": "pandas, NumPy, TensorFlow"
              }
            }
            
            ════════════════════════════════════════
            EXTRACTION RULES:
            ════════════════════════════════════════
            
            1. CONTACT INFO:
               - Extract name, phone, email, LinkedIn, GitHub exactly as shown
               - For LinkedIn/GitHub, extract both display text and full URL if available
            
            2. EDUCATION:
               - List most recent first
               - Include honors, GPA, relevant coursework in highlights array
               - Keep degree format consistent: "Bachelor of Science in Computer Science"
            
            3. EXPERIENCE:
               - List most recent first
               - Keep original bullet point text - do not rewrite
               - Include ALL bullet points from the resume
               - Preserve any metrics/numbers exactly as written
            
            4. PROJECTS:
               - Extract project name and tech stack separately
               - Tech stack should be comma-separated technologies
               - Include all description bullets
               - If no dates/links exist, use "" (empty string) - NEVER "Not Specified"
            
            5. SKILLS:
               - Group into categories: languages, frameworks, developerTools, databases, libraries
               - If a category doesn't fit, put in the most appropriate one
               - Keep as comma-separated strings
            
            6. MISSING DATA:
               - For ANY field not found in the resume, use "" (empty string)
               - NEVER write "Not Specified", "N/A", "Unknown", "None", or any placeholder text
               - Empty strings will be rendered as blank in the final resume
            
            OUTPUT THE JSON NOW (no markdown code blocks, just raw JSON):
            """, 
            escapeForFormat(resumeContent),
            escapeForFormat(jobTitle != null ? jobTitle : ""),
            escapeForFormat(companyName != null ? companyName : ""),
            escapeForFormat(jobPosting != null ? jobPosting : "")
        );
            
        return GroqRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .maxTokens(4000)
            .temperature(0.1) // Very low temperature for consistent extraction
            .build();
    }
}
