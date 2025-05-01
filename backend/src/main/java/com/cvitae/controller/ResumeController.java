package com.cvitae.controller;

import com.cvitae.dto.GenerateResumeRequest;
import com.cvitae.dto.ResumeResponse;
import com.cvitae.dto.AnalyzeJobRequest;
import com.cvitae.dto.JobAnalysisResponse;
import com.cvitae.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure properly for production
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/analyze-job")
    public ResponseEntity<JobAnalysisResponse> analyzeJob(@Valid @RequestBody AnalyzeJobRequest request) {
        log.info("Analyzing job posting for requirements extraction");
        JobAnalysisResponse response = resumeService.analyzeJobPosting(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    public ResponseEntity<ResumeResponse> generateResume(@Valid @RequestBody GenerateResumeRequest request) {
        log.info("Generating tailored resume for job posting");
        ResumeResponse response = resumeService.generateTailoredResume(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload-master")
    public ResponseEntity<String> uploadMasterResume(@RequestParam("file") MultipartFile file) {
        log.info("Uploading master resume file: {}", file.getOriginalFilename());
        String resumeText = resumeService.extractTextFromFile(file);
        return ResponseEntity.ok(resumeText);
    }

    @PostMapping("/upload-job")
    public ResponseEntity<String> uploadJobPosting(@RequestParam("file") MultipartFile file) {
        log.info("Uploading job posting file: {}", file.getOriginalFilename());
        String jobText = resumeService.extractTextFromFile(file);
        return ResponseEntity.ok(jobText);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResume(@PathVariable UUID id) {
        log.info("Retrieving resume with ID: {}", id);
        ResumeResponse resume = resumeService.getResume(id);
        return ResponseEntity.ok(resume);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResumeResponse> updateResume(
            @PathVariable UUID id, 
            @Valid @RequestBody GenerateResumeRequest request) {
        log.info("Updating resume with ID: {}", id);
        ResumeResponse response = resumeService.updateResume(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ResumeResponse>> getUserResumes(@PathVariable String userId) {
        log.info("Retrieving resumes for user: {}", userId);
        List<ResumeResponse> resumes = resumeService.getUserResumes(userId);
        return ResponseEntity.ok(resumes);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(@PathVariable UUID id) {
        log.info("Deleting resume with ID: {}", id);
        resumeService.deleteResume(id);
        return ResponseEntity.noContent().build();
    }
}
