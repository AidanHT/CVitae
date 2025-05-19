package com.cvitae.service.impl;

import com.cvitae.dto.ExportRequest;
import com.cvitae.entity.Resume;
import com.cvitae.repository.ResumeRepository;
import com.cvitae.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private final ResumeRepository resumeRepository;
    private final WebClient webClient;

    @Value("${latex.service.url:http://localhost:8082}")
    private String latexServiceUrl;

    @Override
    public String generateLatexCode(ExportRequest request) {
        log.info("Generating LaTeX code for resume: {}", request.getResumeId());
        
        // Return custom LaTeX code if provided
        if (request.getCustomLatexCode() != null && !request.getCustomLatexCode().isEmpty()) {
            return request.getCustomLatexCode();
        }
        
        // Try to find the resume in database
        Resume resume = resumeRepository.findById(request.getResumeId()).orElse(null);
        
        if (resume == null) {
            log.warn("Resume not found: {}, using fallback template", request.getResumeId());
            // Create a minimal resume object for fallback template
            resume = Resume.builder()
                .id(request.getResumeId())
                .jobTitle("Professional")
                .companyName("Target Company")
                .build();
            return generateFallbackLatexTemplate(resume);
        }

        String latexCode = resume.getLatexCode();
        
        // If no LaTeX code exists, generate fallback template
        if (latexCode == null || latexCode.trim().isEmpty()) {
            log.warn("No LaTeX code found for resume: {}, using fallback template", request.getResumeId());
            latexCode = generateFallbackLatexTemplate(resume);
        }

        return latexCode;
    }

    @Override
    public Resource generatePdf(ExportRequest request) {
        log.info("Generating PDF for resume: {}", request.getResumeId());
        
        Path tempFile = null;
        try {
            String latexCode = generateLatexCode(request);
            
            // Call LaTeX service to compile PDF
            Map<String, Object> requestBody = Map.of(
                "latex", latexCode,
                "name", "resume",
                "paperSize", request.getPaperSize() != null ? request.getPaperSize() : "letter",
                "orientation", request.getOrientation() != null ? request.getOrientation() : "portrait"
            );

            byte[] pdfBytes = webClient.post()
                .uri(latexServiceUrl + "/compile/pdf")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(30))
                .block();

            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("Failed to generate PDF - empty response from LaTeX service");
            }

            // Save PDF to temporary file
            tempFile = Files.createTempFile("cvitae_resume_", ".pdf");
            Files.write(tempFile, pdfBytes);

            log.info("Generated PDF file: {} (size: {} bytes)", tempFile, pdfBytes.length);
            return new FileSystemResource(tempFile.toFile());

        } catch (Exception e) {
            log.error("Error generating PDF", e);
            cleanupTempFile(tempFile);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource generateImage(ExportRequest request, String format) {
        log.info("Generating {} image for resume: {}", format.toUpperCase(), request.getResumeId());
        
        Path tempFile = null;
        try {
            String latexCode = generateLatexCode(request);
            
            // Call LaTeX service to compile image
            Map<String, Object> requestBody = Map.of(
                "latex", latexCode,
                "name", "resume",
                "format", format.toLowerCase(),
                "dpi", request.getDpi() != null ? request.getDpi() : 300,
                "backgroundColor", request.getBackgroundColor() != null ? request.getBackgroundColor() : "white",
                "highQuality", request.getHighQuality() != null ? request.getHighQuality() : true
            );

            byte[] imageBytes = webClient.post()
                .uri(latexServiceUrl + "/compile/image")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(45))
                .block();

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("Failed to generate image - empty response from LaTeX service");
            }

            // Save image to temporary file
            tempFile = Files.createTempFile("cvitae_resume_", "." + format.toLowerCase());
            Files.write(tempFile, imageBytes);

            log.info("Generated {} image: {} (size: {} bytes)", format.toUpperCase(), tempFile, imageBytes.length);
            return new FileSystemResource(tempFile.toFile());

        } catch (Exception e) {
            log.error("Error generating {} image", format, e);
            cleanupTempFile(tempFile);
            throw new RuntimeException("Failed to generate " + format + " image: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource getExportFile(String resumeId, String format) {
        log.info("Getting export file for resume: {} in format: {}", resumeId, format);
        
        try {
            // In a production system, this would retrieve from a file storage system
            // For now, return a placeholder implementation
            throw new RuntimeException("Export file retrieval not yet implemented - would retrieve " + 
                format + " file for resume " + resumeId);
            
        } catch (Exception e) {
            log.error("Error retrieving export file", e);
            throw new RuntimeException("Failed to retrieve export file", e);
        }
    }

    @Override
    public Map<String, Object> getExportHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check LaTeX service connectivity
            boolean latexServiceHealthy = checkLatexServiceHealth();
            
            // Check temporary directory access
            boolean tempDirAccessible = checkTempDirectoryAccess();
            
            boolean overallHealthy = latexServiceHealthy && tempDirAccessible;
            
            health.put("healthy", overallHealthy);
            health.put("status", overallHealthy ? "healthy" : "unhealthy");
            health.put("timestamp", LocalDateTime.now());
            health.put("components", Map.of(
                "latex_service", Map.of(
                    "healthy", latexServiceHealthy,
                    "url", latexServiceUrl
                ),
                "temp_directory", Map.of(
                    "healthy", tempDirAccessible,
                    "path", System.getProperty("java.io.tmpdir")
                )
            ));
            
        } catch (Exception e) {
            log.error("Error checking export service health", e);
            health.put("healthy", false);
            health.put("status", "error");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
        }
        
        return health;
    }

    @Override
    public boolean validateExportedFile(Resource resource, String format) {
        try {
            if (resource == null || !resource.exists()) {
                log.warn("Export file does not exist");
                return false;
            }
            
            long fileSize = resource.contentLength();
            if (fileSize <= 0) {
                log.warn("Export file is empty (size: {})", fileSize);
                return false;
            }
            
            // Basic format-specific validation
            switch (format.toLowerCase()) {
                case "pdf":
                    return validatePdfFile(resource);
                case "png":
                case "jpg":
                case "jpeg":
                    return validateImageFile(resource, format);
                default:
                    return fileSize > 0; // Basic size check for other formats
            }
            
        } catch (Exception e) {
            log.error("Error validating exported file: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkLatexServiceHealth() {
        try {
            String response = webClient.get()
                .uri(latexServiceUrl + "/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            
            return response != null && response.contains("healthy");
            
        } catch (Exception e) {
            log.warn("LaTeX service health check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkTempDirectoryAccess() {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            
            // Try to create and delete a test file
            Path testFile = Files.createTempFile(tempDir, "cvitae_test_", ".tmp");
            Files.delete(testFile);
            
            return true;
            
        } catch (Exception e) {
            log.warn("Temporary directory access check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean validatePdfFile(Resource resource) {
        try {
            // Check if file starts with PDF header
            byte[] header = new byte[4];
            int bytesRead = resource.getInputStream().read(header);
            
            if (bytesRead < 4) {
                return false;
            }
            
            String headerStr = new String(header);
            return headerStr.equals("%PDF");
            
        } catch (Exception e) {
            log.warn("PDF validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean validateImageFile(Resource resource, String format) {
        try {
            // Basic image validation - check file headers
            byte[] header = new byte[8];
            int bytesRead = resource.getInputStream().read(header);
            
            if (bytesRead < 4) {
                return false;
            }
            
            switch (format.toLowerCase()) {
                case "png":
                    // PNG signature: 89 50 4E 47 0D 0A 1A 0A
                    return bytesRead >= 4 && 
                           header[0] == (byte) 0x89 && header[1] == 0x50 && 
                           header[2] == 0x4E && header[3] == 0x47;
                case "jpg":
                case "jpeg":
                    // JPEG signature: FF D8 FF
                    return bytesRead >= 3 &&
                           header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && 
                           header[2] == (byte) 0xFF;
                default:
                    return true; // Unknown format, assume valid
            }
            
        } catch (Exception e) {
            log.warn("Image validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate a fallback LaTeX template when no LaTeX code exists
     */
    private String generateFallbackLatexTemplate(Resume resume) {
        log.info("Generating fallback LaTeX template for resume: {}", resume.getId());
        
        // Use basic information from the resume if available
        String jobTitle = resume.getJobTitle() != null ? resume.getJobTitle() : "Professional";
        String companyName = resume.getCompanyName() != null ? resume.getCompanyName() : "Target Company";
        
        return String.format("""
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
                
                %% Adjust margins
                \\addtolength{\\oddsidemargin}{-0.5in}
                \\addtolength{\\evensidemargin}{-0.5in}
                \\addtolength{\\textwidth}{1in}
                \\addtolength{\\topmargin}{-.5in}
                \\addtolength{\\textheight}{1.0in}
                
                \\urlstyle{same}
                \\raggedbottom
                \\raggedright
                \\setlength{\\tabcolsep}{0in}
                
                %% Sections formatting
                \\titleformat{\\section}{
                  \\vspace{-4pt}\\scshape\\raggedright\\large
                }{}{0em}{}[\\color{black}\\titlerule \\vspace{-5pt}]
                
                %% Custom commands
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
                
                \\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=0.15in, label={}]}
                \\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}
                \\newcommand{\\resumeItemListStart}{\\begin{itemize}}
                \\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}
                
                \\begin{document}
                
                %%----------HEADING----------
                \\begin{center}
                    \\textbf{\\Huge \\scshape Your Name} \\\\ \\vspace{1pt}
                    \\small Your Phone $|$ \\href{mailto:your.email@example.com}{\\underline{your.email@example.com}} $|$ 
                    \\href{https://linkedin.com/in/yourprofile}{\\underline{linkedin.com/in/yourprofile}} $|$
                    \\href{https://github.com/yourusername}{\\underline{github.com/yourusername}}
                \\end{center}
                
                %%----------PROFESSIONAL SUMMARY----------
                \\section{Professional Summary}
                Experienced professional seeking opportunities as %s at %s. 
                Skilled in various technologies and methodologies with a proven track record of delivering results.
                
                %%----------EXPERIENCE----------
                \\section{Experience}
                \\resumeSubHeadingListStart
                    \\resumeSubheading
                      {Your Job Title}{Date Range}
                      {Company Name}{Location}
                      \\resumeItemListStart
                        \\resumeItem{Accomplished significant results through strategic initiatives}
                        \\resumeItem{Collaborated with cross-functional teams to deliver projects}
                        \\resumeItem{Developed and implemented solutions that improved efficiency}
                      \\resumeItemListEnd
                \\resumeSubHeadingListEnd
                
                %%----------EDUCATION----------
                \\section{Education}
                \\resumeSubHeadingListStart
                    \\resumeSubheading
                      {Your University}{Graduation Date}
                      {Degree in Field of Study}{Location}
                \\resumeSubHeadingListEnd
                
                %%----------TECHNICAL SKILLS----------
                \\section{Technical Skills}
                \\begin{itemize}[leftmargin=0.15in, label={}]
                    \\small{\\item{
                     \\textbf{Languages}{: Programming languages and technologies} \\\\
                     \\textbf{Frameworks}{: Frameworks and libraries} \\\\
                     \\textbf{Tools}{: Development tools and platforms} \\\\
                     \\textbf{Databases}{: Database systems and query languages}
                    }}
                \\end{itemize}
                
                \\end{document}
                """, jobTitle, companyName);
    }
    
    /**
     * Clean up temporary files to prevent memory leaks
     */
    private void cleanupTempFile(Path tempFile) {
        try {
            if (tempFile != null && Files.exists(tempFile)) {
                Files.delete(tempFile);
                log.debug("Cleaned up temporary file: {}", tempFile);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup temporary file {}: {}", tempFile, e.getMessage());
        }
    }
}