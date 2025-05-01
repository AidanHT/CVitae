package com.cvitae.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GenerateResumeRequest {
    
    @NotBlank(message = "Master resume content cannot be blank")
    private String masterResume;
    
    @NotBlank(message = "Job posting content cannot be blank")
    private String jobPosting;
    
    private String jobTitle;
    private String companyName;
    
    @NotNull(message = "Resume length must be specified")
    private Integer targetLength; // 1 for 1-page, 2 for 2-page, etc.
    
    private List<String> priorityExperiences; // User-specified experiences to prioritize
    private List<String> prioritySkills; // User-specified skills to prioritize
    
    private Map<String, Object> customizationPreferences; // Additional customization options
    
    private String sessionId; // For chat context
    private String userId; // For future user management
    
    // Resume sections to include/exclude
    private boolean includeEducation = true;
    private boolean includeExperience = true;
    private boolean includeProjects = true;
    private boolean includeSkills = true;
    private boolean includeLeadership = true;
    private boolean includeCertifications = false;
    private boolean includeVolunteering = false;
}
