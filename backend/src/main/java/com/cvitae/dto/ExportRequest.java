package com.cvitae.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ExportRequest {
    
    @NotNull(message = "Resume ID cannot be null")
    private String resumeId;
    
    private String customLatexCode; // If user wants to use custom LaTeX
    
    // Export options
    private Map<String, Object> exportOptions; // Format-specific options
    
    // PDF options
    private String paperSize = "A4"; // A4, Letter, etc.
    private String orientation = "portrait"; // portrait, landscape
    private Integer dpi = 300; // For image exports
    
    // Image options
    private String backgroundColor = "white";
    private Boolean highQuality = true;
    
    private String userId; // For tracking
}
