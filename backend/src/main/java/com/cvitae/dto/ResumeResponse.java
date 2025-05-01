package com.cvitae.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ResumeResponse {
    
    private UUID id;
    private String tailoredResume; // The generated resume content
    private String latexCode; // Generated LaTeX code
    private String jobTitle;
    private String companyName;
    private Integer targetLength;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status; // GENERATING, COMPLETED, ERROR
    
    // AI Analysis Results
    private JobAnalysisResponse jobAnalysis;
    private List<String> selectedExperiences;
    private List<String> selectedSkills;
    private List<String> optimizedKeywords;
    
    // Export Information
    private List<ExportInfo> availableExports;
    
    // Performance Metrics
    private Double atsCompatibilityScore;
    private Map<String, Object> optimizationMetrics;
    
    @Data
    @Builder
    public static class ExportInfo {
        private String format; // PDF, PNG, JPG, LATEX
        private String downloadUrl;
        private LocalDateTime generatedAt;
        private String status;
    }
}
