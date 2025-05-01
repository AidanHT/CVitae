package com.cvitae.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalyzeJobRequest {
    
    @NotBlank(message = "Job posting content cannot be blank")
    private String jobPosting;
    
    private String jobTitle;
    private String companyName;
    private String jobUrl;
    private String sessionId;
}
