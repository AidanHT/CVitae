package com.cvitae.dto;

import lombok.Data;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class JobAnalysisResponse {
    
    private String jobTitle;
    private String companyName;
    private String department;
    private String experienceLevel; // ENTRY, MID, SENIOR, EXECUTIVE
    
    // Key Requirements
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private List<String> requiredExperience;
    private List<String> preferredExperience;
    
    // Keywords and Phrases
    private List<String> keywordsPrimary; // Must-have keywords
    private List<String> keywordsSecondary; // Nice-to-have keywords
    private List<String> actionVerbs; // Recommended action verbs
    
    // Job Details
    private String jobType; // FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP
    private String remoteType; // REMOTE, HYBRID, ON_SITE
    private String salaryRange;
    private List<String> benefits;
    
    // Industry Information
    private String industry;
    private String companySize;
    private List<String> competitorKeywords;
    
    // AI Recommendations
    private List<String> resumeOptimizationTips;
    private Map<String, Double> skillPriorityScores; // Skill -> Priority Score (0-1)
    private Double overallMatchPotential; // How well this could match a typical resume
    
    // Content Analysis
    private Map<String, Object> contentAnalysis; // Additional metadata from AI analysis
}
