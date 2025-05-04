package com.cvitae.service;

import com.cvitae.dto.ExportRequest;
import org.springframework.core.io.Resource;

import java.util.Map;

/**
 * Service interface for exporting resumes in various formats
 */
public interface ExportService {
    
    /**
     * Generate LaTeX code for a resume
     * @param request Export configuration
     * @return LaTeX source code
     */
    String generateLatexCode(ExportRequest request);
    
    /**
     * Generate PDF from resume
     * @param request Export configuration
     * @return PDF file resource
     */
    Resource generatePdf(ExportRequest request);
    
    /**
     * Generate image from resume
     * @param request Export configuration
     * @param format Image format (png, jpg)
     * @return Image file resource
     */
    Resource generateImage(ExportRequest request, String format);
    
    /**
     * Get existing export file
     * @param resumeId Resume ID
     * @param format Export format
     * @return File resource
     */
    Resource getExportFile(String resumeId, String format);
    
    /**
     * Get export service health status
     * @return Health status information
     */
    Map<String, Object> getExportHealthStatus();
    
    /**
     * Validate that an exported file is valid and non-empty
     * @param resource File resource to validate
     * @param format Expected format
     * @return true if valid
     */
    boolean validateExportedFile(Resource resource, String format);
}
