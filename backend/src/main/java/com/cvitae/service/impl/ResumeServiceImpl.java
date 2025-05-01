package com.cvitae.service.impl;

import com.cvitae.dto.*;
import com.cvitae.entity.Resume;
import com.cvitae.repository.ResumeRepository;
import com.cvitae.service.ResumeService;
import com.cvitae.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final GroqAIService groqAIService;

    @Override
    public JobAnalysisResponse analyzeJobPosting(AnalyzeJobRequest request) {
        log.info("Analyzing job posting for: {}", request.getJobTitle());
        
        try {
            return groqAIService.analyzeJobPosting(
                request.getJobPosting(),
                request.getJobTitle(),
                request.getCompanyName()
            );
        } catch (Exception e) {
            log.error("Error analyzing job posting", e);
            throw new RuntimeException("Failed to analyze job posting", e);
        }
    }

    @Override
    public ResumeResponse generateTailoredResume(GenerateResumeRequest request) {
        log.info("Generating tailored resume for: {}", request.getJobTitle());
        
        try {
            // 1. Analyze the job posting
            JobAnalysisResponse jobAnalysis = groqAIService.analyzeJobPosting(
                request.getJobPosting(),
                request.getJobTitle(),
                request.getCompanyName()
            );

            // 2. Generate tailored resume content
            String tailoredContent = groqAIService.generateTailoredResume(
                request.getMasterResume(),
                jobAnalysis,
                request
            );

            // 3. Convert to Jake's LaTeX format
            String latexCode = groqAIService.convertToJakesLatex(tailoredContent, request);

            // 4. Save to database
            Resume resume = Resume.builder()
                .id(UUID.randomUUID())
                .masterResume(request.getMasterResume())
                .jobPosting(request.getJobPosting())
                .jobTitle(request.getJobTitle())
                .companyName(request.getCompanyName())
                .tailoredResume(tailoredContent)
                .latexCode(latexCode)
                .targetLength(request.getTargetLength())
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            resume = resumeRepository.save(resume);

            // 5. Build response
            return ResumeResponse.builder()
                .id(resume.getId())
                .tailoredResume(tailoredContent)
                .latexCode(latexCode)
                .jobTitle(request.getJobTitle())
                .companyName(request.getCompanyName())
                .targetLength(request.getTargetLength())
                .status("COMPLETED")
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .jobAnalysis(jobAnalysis)
                .atsCompatibilityScore(calculateATSScore(tailoredContent, jobAnalysis))
                .availableExports(getAvailableExports(resume.getId()))
                .build();

        } catch (Exception e) {
            log.error("Error generating tailored resume", e);
            throw new RuntimeException("Failed to generate tailored resume", e);
        }
    }

    @Override
    public ResumeResponse updateResume(UUID id, GenerateResumeRequest request) {
        log.info("Updating resume with ID: {}", id);
        
        Resume existingResume = resumeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Resume not found"));

        // Regenerate the resume with new parameters
        ResumeResponse newResume = generateTailoredResume(request);
        
        // Update existing record
        existingResume.setTailoredResume(newResume.getTailoredResume());
        existingResume.setLatexCode(newResume.getLatexCode());
        existingResume.setUpdatedAt(LocalDateTime.now());
        
        resumeRepository.save(existingResume);
        
        return newResume;
    }

    @Override
    public ResumeResponse getResume(UUID id) {
        log.info("Retrieving resume with ID: {}", id);
        
        Resume resume = resumeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Resume not found"));

        return mapToResponse(resume);
    }

    @Override
    public List<ResumeResponse> getUserResumes(String userId) {
        log.info("Retrieving resumes for user: {}", userId);
        
        List<Resume> resumes = resumeRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return resumes.stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    public void deleteResume(UUID id) {
        log.info("Deleting resume with ID: {}", id);
        
        if (!resumeRepository.existsById(id)) {
            throw new RuntimeException("Resume not found");
        }
        
        resumeRepository.deleteById(id);
    }

    @Override
    public String extractTextFromFile(MultipartFile file) {
        log.info("Extracting text from file: {}", file.getOriginalFilename());
        
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                throw new RuntimeException("Invalid file");
            }

            String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            
            return switch (extension) {
                case ".pdf" -> extractTextFromPdf(file);
                case ".docx" -> extractTextFromDocx(file);
                case ".txt" -> new String(file.getBytes());
                default -> throw new RuntimeException("Unsupported file format: " + extension);
            };

        } catch (Exception e) {
            log.error("Error extracting text from file", e);
            throw new RuntimeException("Failed to extract text from file", e);
        }
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private ResumeResponse mapToResponse(Resume resume) {
        return ResumeResponse.builder()
            .id(resume.getId())
            .tailoredResume(resume.getTailoredResume())
            .latexCode(resume.getLatexCode())
            .jobTitle(resume.getJobTitle())
            .companyName(resume.getCompanyName())
            .targetLength(resume.getTargetLength())
            .status(resume.getStatus())
            .createdAt(resume.getCreatedAt())
            .updatedAt(resume.getUpdatedAt())
            .availableExports(getAvailableExports(resume.getId()))
            .build();
    }

    private Double calculateATSScore(String resumeContent, JobAnalysisResponse jobAnalysis) {
        // Simplified ATS score calculation
        // In practice, this would be more sophisticated
        
        if (jobAnalysis.getRequiredSkills() == null || jobAnalysis.getRequiredSkills().isEmpty()) {
            return 0.7; // Default score
        }

        long matchingSkills = jobAnalysis.getRequiredSkills().stream()
            .mapToLong(skill -> resumeContent.toLowerCase().contains(skill.toLowerCase()) ? 1 : 0)
            .sum();

        return Math.min(1.0, 0.5 + (matchingSkills * 0.1));
    }

    private List<ResumeResponse.ExportInfo> getAvailableExports(UUID resumeId) {
        return List.of(
            ResumeResponse.ExportInfo.builder()
                .format("LATEX")
                .status("AVAILABLE")
                .build(),
            ResumeResponse.ExportInfo.builder()
                .format("PDF")
                .status("AVAILABLE")
                .build(),
            ResumeResponse.ExportInfo.builder()
                .format("PNG")
                .status("AVAILABLE")
                .build()
        );
    }
}
