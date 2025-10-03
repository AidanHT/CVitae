package com.cvitae.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured data model for resume content extracted by AI.
 * This is used as an intermediate format between raw resume text
 * and the final LaTeX output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResumeData {
    
    // Header information
    private String name;
    private String phone;
    private String email;
    private String linkedin;
    private String linkedinUrl;
    private String github;
    private String githubUrl;
    private String website;
    private String websiteUrl;
    
    // Resume sections
    private List<Education> education;
    private List<Experience> experience;
    private List<Project> projects;
    private Skills skills;
    
    /**
     * Education entry (degree, school, etc.)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Education {
        private String school;
        private String location;
        private String degree;
        private String dates;
        private List<String> highlights; // Optional GPA, honors, etc.
    }
    
    /**
     * Work experience entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Experience {
        private String title;
        private String dates;
        private String company;
        private String location;
        private List<String> bullets; // Achievement bullet points
    }
    
    /**
     * Project entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Project {
        private String name;
        private String techStack;
        private String dates; // Can also be a link
        private List<String> bullets; // Description bullet points
    }
    
    /**
     * Technical skills grouped by category
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Skills {
        private String languages;
        private String frameworks;
        private String developerTools;
        private String libraries;
        private String databases;
        private String other; // Any additional categories
    }
}
