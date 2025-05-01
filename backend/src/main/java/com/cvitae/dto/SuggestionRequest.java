package com.cvitae.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SuggestionRequest {
    
    private UUID resumeId;
    private String resumeContent;
    private String jobPostingContent;
    private String suggestionType; // CONTENT_IMPROVEMENT, KEYWORD_OPTIMIZATION, FORMAT_ENHANCEMENT
    private String sessionId;
}
