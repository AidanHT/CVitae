package com.cvitae.service;

import com.cvitae.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResumeService {
    
    /**
     * Analyze a job posting to extract requirements and keywords
     */
    JobAnalysisResponse analyzeJobPosting(AnalyzeJobRequest request);
    
    /**
     * Generate a tailored resume based on master resume and job analysis
     */
    ResumeResponse generateTailoredResume(GenerateResumeRequest request);
    
    /**
     * Update an existing resume
     */
    ResumeResponse updateResume(UUID id, GenerateResumeRequest request);
    
    /**
     * Retrieve a resume by ID
     */
    ResumeResponse getResume(UUID id);
    
    /**
     * Get all resumes for a user
     */
    List<ResumeResponse> getUserResumes(String userId);
    
    /**
     * Delete a resume
     */
    void deleteResume(UUID id);
    
    /**
     * Extract text from uploaded files (PDF, DOCX, TXT)
     */
    String extractTextFromFile(MultipartFile file);
}
