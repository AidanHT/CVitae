package com.cvitae.dto;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
public class SuggestionResponse {
    
    private String id;
    private String title;
    private String description;
    private String priority; // HIGH, MEDIUM, LOW
    private String category; // CONTENT, KEYWORDS, FORMAT, STRUCTURE
    private String suggestedChange;
    private String reasoning;
    private Double impactScore; // 0-1 score indicating expected impact
    private boolean requiresUserAction;
    private boolean isApplicable; // Whether this suggestion applies to current resume
    private Map<String, Object> metadata; // Additional context
}
