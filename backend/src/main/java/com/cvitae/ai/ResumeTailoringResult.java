package com.cvitae.ai;

import com.cvitae.dto.JobAnalysisResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result object containing all outputs from the resume tailoring process
 */
@Data
@Builder
public class ResumeTailoringResult {
    
    private boolean success;
    private String errorMessage;
    
    // Core outputs
    private JobAnalysisResponse jobAnalysis;
    private String tailoredContent;
    private String latexCode;
    
    // Quality metrics
    private double atsCompatibilityScore;
    private List<String> processingNotes;
    
    // Processing metadata
    private int totalTokensUsed;
    private long processingTimeMs;
    
    /**
     * Check if all required outputs are available
     */
    public boolean hasCompleteResults() {
        return success && 
               jobAnalysis != null && 
               tailoredContent != null && 
               !tailoredContent.trim().isEmpty() &&
               latexCode != null && 
               !latexCode.trim().isEmpty();
    }
    
    /**
     * Get a summary of the tailoring process
     */
    public String getSummary() {
        if (!success) {
            return "Resume tailoring failed: " + errorMessage;
        }
        
        return String.format("""
            Resume tailoring completed successfully:
            • Job: %s at %s
            • ATS Compatibility Score: %.1f%%
            • Experience Level: %s
            • Key Skills Matched: %d
            • Processing Notes: %d items
            """,
            jobAnalysis.getJobTitle(),
            jobAnalysis.getCompanyName(),
            atsCompatibilityScore * 100,
            jobAnalysis.getExperienceLevel(),
            jobAnalysis.getRequiredSkills().size(),
            processingNotes != null ? processingNotes.size() : 0
        );
    }
}
