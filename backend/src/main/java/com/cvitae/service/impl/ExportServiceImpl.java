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
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.cvitae.ai.GroqRequest;
import com.cvitae.ai.GroqResponse;
import com.cvitae.ai.GroqClient;
import com.cvitae.service.LatexTemplateService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private final ResumeRepository resumeRepository;
    private final WebClient webClient;
    private final GroqClient groqClient;
    private final LatexTemplateService latexTemplateService;

    @Value("${latex.service.url:http://localhost:8082}")
    private String latexServiceUrl;
    
    @Value("${latex.debug.enabled:true}")
    private boolean debugEnabled;
    
    @Value("${latex.debug.directory:/tmp/debug}")
    private String debugDirectory;

    @Override
    public String generateLatexCode(ExportRequest request) {
        log.info("Generating LaTeX code for resume: {}", request.getResumeId());
        
        // Return custom LaTeX code if provided
        if (request.getCustomLatexCode() != null && !request.getCustomLatexCode().isEmpty()) {
            return request.getCustomLatexCode();
        }
        
        // Try to find the resume in database
        UUID resumeUuid;
        try {
            resumeUuid = UUID.fromString(request.getResumeId());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for resume ID: {}", request.getResumeId());
            resumeUuid = null;
        }
        
        Resume resume = resumeUuid != null ? resumeRepository.findById(resumeUuid).orElse(null) : null;
        
        if (resume == null) {
            log.warn("Resume not found: {}, using fallback template", request.getResumeId());
            // Create a minimal resume object for fallback template
            resume = Resume.builder()
                .id(resumeUuid != null ? resumeUuid : UUID.randomUUID())
                .jobTitle("Professional")
                .companyName("Target Company")
                .build();
            return generateFallbackLatexTemplate(resume);
        }

        String latexCode = resume.getLatexCode();
        
        // Enhanced LaTeX processing and validation
        if (latexCode != null && !latexCode.trim().isEmpty()) {
            log.info("Found stored LaTeX code, length: {} characters", latexCode.length());
            
            // Try to clean and fix the LaTeX code before rejecting it
            String cleanedLatex = cleanAndValidateLatex(latexCode);
            if (cleanedLatex != null) {
                log.info("Using cleaned stored LaTeX code");
                return cleanedLatex;
            }
            
            // If cleaning failed, check if it's truly malformed
            if (isLatexMalformed(latexCode)) {
                log.warn("Stored LaTeX code contains malformed content, regenerating for resume: {}", request.getResumeId());
                latexCode = null; // Force regeneration
            } else {
                log.info("Using stored LaTeX code as-is");
                return latexCode;
            }
        }
        
        // Try to regenerate LaTeX from tailored resume content
        if (resume.getTailoredResume() != null && !resume.getTailoredResume().trim().isEmpty()) {
            log.info("Attempting to generate LaTeX from tailored resume content");
            try {
                String regeneratedLatex = regenerateLatexFromContent(resume.getTailoredResume(), resume);
                if (regeneratedLatex != null) {
                    log.info("Successfully regenerated LaTeX from content");
                    return regeneratedLatex;
                }
            } catch (Exception e) {
                log.warn("Failed to regenerate LaTeX from content: {}", e.getMessage());
            }
        }
        
        // Final fallback to template
        log.warn("No valid LaTeX code found for resume: {}, using fallback template", request.getResumeId());
        return generateFallbackLatexTemplate(resume);
    }

    @Override
    public Resource generatePdf(ExportRequest request) {
        log.info("Generating PDF for resume: {}", request.getResumeId());
        
        Path tempFile = null;
        String debugSession = null;
        try {
            String latexCode = generateLatexCode(request);
            
            // Save debug files if enabled
            if (debugEnabled) {
                debugSession = saveDebugFiles(latexCode, request.getResumeId());
                log.debug("Saved debug files for session: {}", debugSession);
            }
            
            // Call LaTeX service to compile PDF
            Map<String, Object> requestBody = Map.of(
                "latex", latexCode,
                "name", "resume",
                "paperSize", request.getPaperSize() != null ? request.getPaperSize() : "letter",
                "orientation", request.getOrientation() != null ? request.getOrientation() : "portrait"
            );

            final String finalDebugSession = debugSession; // Make final for lambda
            byte[] pdfBytes = webClient.post()
                .uri(latexServiceUrl + "/compile/pdf")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> response.bodyToMono(String.class)
                        .map(body -> {
                            // Save error response to debug files
                            if (debugEnabled && finalDebugSession != null) {
                                saveDebugError(finalDebugSession, body);
                            }
                            
                            // Parse detailed LaTeX error information
                            String enhancedError = enhanceLatexErrorMessage(body, finalDebugSession);
                            log.error("LaTeX compilation failed - Debug Session: {} - Error: {}", finalDebugSession, enhancedError);
                            
                            return new RuntimeException(enhancedError);
                        })
                )
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(60))
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
            
            // Save error to debug files
            if (debugEnabled && debugSession != null) {
                saveDebugError(debugSession, e.getMessage());
            }
            
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
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    response -> response.bodyToMono(String.class)
                        .map(body -> {
                            String enhancedError = enhanceLatexErrorMessage(body, null);
                            log.error("LaTeX image compilation failed - Error: {}", enhancedError);
                            return new RuntimeException(enhancedError);
                        })
                )
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(90))
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
                .onStatus(
                    status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> clientResponse.bodyToMono(String.class)
                        .map(body -> new RuntimeException("Health check failed: " + body))
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
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
     * Generate a fallback LaTeX template using centralized LatexTemplateService
     */
    private String generateFallbackLatexTemplate(Resume resume) {
        log.info("Generating fallback LaTeX template for resume: {}", resume.getId());
        
        String jobTitle = resume.getJobTitle() != null ? resume.getJobTitle() : "Professional";
        String companyName = resume.getCompanyName() != null ? resume.getCompanyName() : "Target Company";
        
        return latexTemplateService.wrapInTemplate(
            latexTemplateService.generateSampleBody(jobTitle, companyName)
        );
    }
    
    /**
     * Get the static LaTeX template from centralized service
     */
    private String getStaticLatexTemplate() {
        return latexTemplateService.getJakesTemplate();
    }
    
    /**
     * Clean and validate LaTeX code, attempting to fix common issues
     */
    private String cleanAndValidateLatex(String latexCode) {
        if (latexCode == null || latexCode.trim().isEmpty()) {
            return null;
        }
        
        String cleaned = latexCode.trim();
        
        // Remove common AI response patterns while preserving LaTeX
        String[] patterns = {
            "Here's the LaTeX code:", "Here is the LaTeX code:",
            "```latex", "```", "\\`\\`\\`latex", "\\`\\`\\`"
        };
        
        for (String pattern : patterns) {
            cleaned = cleaned.replace(pattern, "");
        }
        
        // Extract LaTeX content if wrapped
        if (cleaned.contains("%__BEGIN_LATEX__") && cleaned.contains("%__END_LATEX__")) {
            int start = cleaned.indexOf("%__BEGIN_LATEX__") + "%__BEGIN_LATEX__".length();
            int end = cleaned.indexOf("%__END_LATEX__");
            if (end > start) {
                cleaned = cleaned.substring(start, end).trim();
            }
        }
        
        // Ensure document structure
        if (!cleaned.contains("\\documentclass")) {
            log.debug("LaTeX missing documentclass, cannot clean");
            return null;
        }
        
        if (!cleaned.contains("\\begin{document}")) {
            log.debug("LaTeX missing begin document, cannot clean");
            return null;
        }
        
        if (!cleaned.contains("\\end{document}")) {
            log.debug("LaTeX missing end document, attempting to add");
            if (!cleaned.endsWith("\\end{document}")) {
                cleaned += "\n\\end{document}";
            }
        }
        
        log.debug("Successfully cleaned LaTeX code");
        return cleaned;
    }

    /**
     * Improved LaTeX malformed detection - less aggressive
     */
    private boolean isLatexMalformed(String latexCode) {
        if (latexCode == null || latexCode.trim().isEmpty()) {
            return true;
        }

        String trimmed = latexCode.trim();
        
        // Only check for obvious non-LaTeX content - be less aggressive
        String[] criticalMalformedPatterns = {
            "I'm sorry", "I cannot", "I'm unable to", "I can't",
            "Error:", "ERROR:", "Failed to", "Unable to process",
            "```json", "```html", "```css", "```javascript"
        };
        
        for (String pattern : criticalMalformedPatterns) {
            if (trimmed.contains(pattern)) {
                log.debug("Found critical malformed pattern '{}' in LaTeX code", pattern);
                return true;
            }
        }
        
        // Essential structure checks
        if (!trimmed.contains("\\documentclass")) {
            log.debug("LaTeX code doesn't contain \\documentclass");
            return true;
        }
        
        if (!trimmed.contains("\\begin{document}")) {
            log.debug("LaTeX code doesn't contain \\begin{document}");
            return true;
        }
        
        // Allow missing \end{document} as it can be added
        
        return false;
    }

    /**
     * Regenerate LaTeX from tailored resume content
     */
    private String regenerateLatexFromContent(String content, Resume resume) {
        try {
            // Create a simple conversion request
            GroqRequest latexRequest = GroqRequest.builder()
                .systemPrompt("""
                    Convert the following resume content to Jake's LaTeX template format.
                    
                    CRITICAL REQUIREMENTS:
                    - Start with %__BEGIN_LATEX__
                    - Use \\documentclass[letterpaper,11pt]{article}
                    - Include all necessary packages
                    - End with \\end{document}
                    - Close with %__END_LATEX__
                    - Use Jake's resume macros properly
                    """)
                .userPrompt("Resume Content to Convert:\n" + content)
                .maxTokens(4000)
                .temperature(0.2)
                .build();
                
            GroqResponse response = groqClient.callGroqAPI(latexRequest);
            
            if (response.isSuccess()) {
                String latexCode = response.getContent();
                String cleaned = cleanAndValidateLatex(latexCode);
                if (cleaned != null && !isLatexMalformed(cleaned)) {
                    return cleaned;
                }
            }
            
            log.warn("Failed to regenerate LaTeX via AI, falling back to template");
            return null;
            
        } catch (Exception e) {
            log.error("Error in LaTeX regeneration: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Save debug files for LaTeX troubleshooting
     * Returns a debug session ID for tracking
     */
    private String saveDebugFiles(String latexCode, String resumeId) {
        try {
            // Create debug directory if it doesn't exist
            Path debugDir = Paths.get(debugDirectory);
            if (!Files.exists(debugDir)) {
                Files.createDirectories(debugDir);
            }
            
            // Generate debug session ID with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String sessionId = String.format("resume_%s_%s", resumeId, timestamp);
            
            // Save the raw LaTeX code
            Path latexFile = debugDir.resolve(sessionId + "_generated.tex");
            Files.write(latexFile, latexCode.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Save metadata
            String metadata = String.format("""
                Debug Session: %s
                Resume ID: %s
                Timestamp: %s
                LaTeX File: %s
                LaTeX Code Length: %d characters
                
                Debug Directory Contents:
                - %s_generated.tex (raw LaTeX code)
                - %s_error.log (if compilation fails)
                - %s_metadata.txt (this file)
                
                To reproduce the error manually:
                cd %s
                latexmk -pdf -interaction=nonstopmode -halt-on-error -file-line-error %s_generated.tex
                """, 
                sessionId, resumeId, LocalDateTime.now(), latexFile.toString(), latexCode.length(),
                sessionId, sessionId, sessionId, debugDir.toString(), sessionId);
                
            Path metadataFile = debugDir.resolve(sessionId + "_metadata.txt");
            Files.write(metadataFile, metadata.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("Saved debug files for session: {} in directory: {}", sessionId, debugDir);
            return sessionId;
            
        } catch (Exception e) {
            log.warn("Failed to save debug files: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Save error information to debug files
     */
    private void saveDebugError(String sessionId, String errorMessage) {
        if (sessionId == null) return;
        
        try {
            Path debugDir = Paths.get(debugDirectory);
            Path errorFile = debugDir.resolve(sessionId + "_error.log");
            
            String errorLog = String.format("""
                LaTeX Compilation Error
                Session: %s
                Timestamp: %s
                
                Error Message:
                %s
                
                Suggested Actions:
                1. Check the generated .tex file for syntax errors
                2. Verify all custom macros are defined
                3. Look for unescaped special characters (&, $, %, _, {, })
                4. Check for lonely \\item commands outside list environments
                5. Verify document structure (\\documentclass, \\begin{document}, \\end{document})
                
                To debug manually:
                cd %s
                latexmk -pdf -interaction=nonstopmode -halt-on-error -file-line-error %s_generated.tex
                """, 
                sessionId, LocalDateTime.now(), errorMessage, debugDirectory, sessionId);
                
            Files.write(errorFile, errorLog.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            log.debug("Saved error log for debug session: {}", sessionId);
            
        } catch (Exception e) {
            log.warn("Failed to save debug error: {}", e.getMessage());
        }
    }

    /**
     * Enhance LaTeX error messages with detailed debugging information
     */
    private String enhanceLatexErrorMessage(String rawError, String debugSession) {
        if (rawError == null || rawError.trim().isEmpty()) {
            return "LaTeX compilation failed with unknown error";
        }
        
        try {
            StringBuilder enhanced = new StringBuilder();
            enhanced.append("🔥 LATEX COMPILATION ERROR\n");
            enhanced.append("═══════════════════════════════════════\n");
            
            if (debugSession != null) {
                enhanced.append("🔍 Debug Session: ").append(debugSession).append("\n");
                enhanced.append("📁 Debug Files: ").append(debugDirectory).append("/").append(debugSession).append("*\n");
            }
            
            // Parse JSON error response from LaTeX service
            if (rawError.startsWith("{")) {
                enhanced.append("\n📋 DETAILED ERROR ANALYSIS:\n");
                enhanced.append("─────────────────────────────────────\n");
                
                // Extract key error information
                if (rawError.contains("Undefined control sequence")) {
                    enhanced.append("❌ UNDEFINED MACROS DETECTED\n");
                    enhanced.append("   Problem: LaTeX cannot find required macro definitions\n");
                    enhanced.append("   Solution: Ensure all \\resumeItem, \\resumeSubheading macros are defined\n\n");
                }
                
                if (rawError.contains("Lonely \\item")) {
                    enhanced.append("❌ LONELY ITEMS DETECTED\n");
                    enhanced.append("   Problem: \\item commands found outside list environments\n");
                    enhanced.append("   Solution: Wrap items in \\resumeItemListStart...\\resumeItemListEnd\n\n");
                }
                
                if (rawError.contains("Missing \\begin{document}")) {
                    enhanced.append("❌ DOCUMENT STRUCTURE ERROR\n");
                    enhanced.append("   Problem: LaTeX document is missing \\begin{document}\n");
                    enhanced.append("   Solution: Check template structure and AI output format\n\n");
                }
                
                if (rawError.contains("Misplaced alignment tab character &")) {
                    enhanced.append("❌ UNESCAPED SPECIAL CHARACTERS\n");
                    enhanced.append("   Problem: & character not properly escaped\n");
                    enhanced.append("   Solution: Replace & with \\& in text content\n\n");
                }
                
                if (rawError.contains("Missing $ inserted")) {
                    enhanced.append("❌ MATH MODE ERRORS\n");
                    enhanced.append("   Problem: Unescaped $ characters or math syntax issues\n");
                    enhanced.append("   Solution: Escape $ as \\$ or fix math mode syntax\n\n");
                }
            }
            
            enhanced.append("🔧 DEBUGGING STEPS:\n");
            enhanced.append("─────────────────────\n");
            enhanced.append("1. Check console output for detailed LaTeX log\n");
            enhanced.append("2. Verify all macro definitions are included\n");
            enhanced.append("3. Look for unescaped special characters: & $ % _ { }\n");
            enhanced.append("4. Ensure proper document structure\n");
            enhanced.append("5. Check for lonely \\item commands\n\n");
            
            enhanced.append("📄 RAW ERROR RESPONSE:\n");
            enhanced.append("─────────────────────\n");
            enhanced.append(rawError);
            
            String result = enhanced.toString();
            
            // Also log to console for immediate visibility
            log.error("\n" + result);
            
            return result;
            
        } catch (Exception e) {
            log.warn("Failed to enhance error message: {}", e.getMessage());
            return "LaTeX compilation failed: " + rawError;
        }
    }
    
    @Override
    public Map<String, Object> getDebugInfo(String sessionId) {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            Path debugDir = Paths.get(debugDirectory);
            
            // Find all files for this session
            List<Path> sessionFiles = Files.list(debugDir)
                    .filter(path -> path.getFileName().toString().startsWith(sessionId))
                    .sorted()
                    .collect(Collectors.toList());
            
            if (sessionFiles.isEmpty()) {
                debugInfo.put("error", "No debug files found for session: " + sessionId);
                return debugInfo;
            }
            
            debugInfo.put("sessionId", sessionId);
            debugInfo.put("debugDirectory", debugDirectory);
            debugInfo.put("filesFound", sessionFiles.size());
            
            List<Map<String, Object>> files = new ArrayList<>();
            
            for (Path file : sessionFiles) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", file.getFileName().toString());
                fileInfo.put("size", Files.size(file));
                fileInfo.put("lastModified", Files.getLastModifiedTime(file).toString());
                
                // Read file content if it's a text file and small enough
                String fileName = file.getFileName().toString();
                if ((fileName.endsWith(".tex") || fileName.endsWith(".log") || fileName.endsWith(".txt")) 
                    && Files.size(file) < 50000) { // Limit to 50KB
                    try {
                        String content = Files.readString(file);
                        fileInfo.put("content", content);
                    } catch (Exception e) {
                        fileInfo.put("contentError", "Failed to read: " + e.getMessage());
                    }
                }
                
                files.add(fileInfo);
            }
            
            debugInfo.put("files", files);
            debugInfo.put("timestamp", LocalDateTime.now().toString());
            
        } catch (Exception e) {
            log.error("Failed to get debug info for session {}: {}", sessionId, e.getMessage());
            debugInfo.put("error", "Failed to retrieve debug info: " + e.getMessage());
        }
        
        return debugInfo;
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