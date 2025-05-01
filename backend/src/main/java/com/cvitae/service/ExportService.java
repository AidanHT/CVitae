package com.cvitae.service;

import com.cvitae.dto.ExportRequest;
import org.springframework.core.io.Resource;

import java.util.UUID;

public interface ExportService {
    
    /**
     * Generate LaTeX code for a resume
     */
    String generateLatexCode(ExportRequest request);
    
    /**
     * Generate PDF from resume
     */
    Resource generatePdf(ExportRequest request);
    
    /**
     * Generate image from resume (PNG, JPG, etc.)
     */
    Resource generateImage(ExportRequest request, String format);
    
    /**
     * Get an existing export file
     */
    Resource getExportFile(UUID exportId);
}
