package com.cvitae.service.impl;

import com.cvitae.ai.ResumeTailorService;
import com.cvitae.ai.ResumeTailoringResult;
import com.cvitae.dto.*;
import com.cvitae.entity.Resume;
import com.cvitae.repository.ResumeRepository;
import com.cvitae.service.ResumeService;
import com.cvitae.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final GroqAIService groqAIService;
    private final ResumeTailorService resumeTailorService;

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
            // Use the new AI orchestration service for complete workflow
            ResumeTailoringResult tailoringResult = resumeTailorService.tailorResume(
                request.getMasterResume(),
                request.getJobPosting(),
                request
            );
            
            if (!tailoringResult.isSuccess()) {
                throw new RuntimeException("Resume tailoring failed: " + tailoringResult.getErrorMessage());
            }

            // Save to database
            Resume resume = Resume.builder()
                .id(UUID.randomUUID())
                .masterResume(request.getMasterResume())
                .jobPosting(request.getJobPosting())
                .jobTitle(request.getJobTitle())
                .companyName(request.getCompanyName())
                .tailoredResume(tailoringResult.getTailoredContent())
                .latexCode(tailoringResult.getLatexCode())
                .targetLength(request.getTargetLength())
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

            resume = resumeRepository.save(resume);

            // Build response with enhanced analytics
            return ResumeResponse.builder()
                .id(resume.getId())
                .tailoredResume(tailoringResult.getTailoredContent())
                .latexCode(tailoringResult.getLatexCode())
                .jobTitle(request.getJobTitle())
                .companyName(request.getCompanyName())
                .targetLength(request.getTargetLength())
                .status("COMPLETED")
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .jobAnalysis(tailoringResult.getJobAnalysis())
                .atsCompatibilityScore(tailoringResult.getAtsCompatibilityScore())
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
                case ".txt" -> extractTextFromTxt(file);
                default -> throw new RuntimeException("Unsupported file format: " + extension);
            };

        } catch (Exception e) {
            log.error("Error extracting text from file", e);
            throw new RuntimeException("Failed to extract text from file", e);
        }
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Better text ordering
            String text = stripper.getText(document);
            return cleanText(text);
        }
    }

    private String extractTextFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            // Clean up common encoding issues and normalize line breaks
            return text.replaceAll("\\r\\n", "\n")
                      .replaceAll("\\r", "\n")
                      .replaceAll("\\u00A0", " ") // Non-breaking space
                      .replaceAll("\\u2019", "'") // Right single quotation mark
                      .replaceAll("\\u201C", "\"") // Left double quotation mark
                      .replaceAll("\\u201D", "\"") // Right double quotation mark
                      .replaceAll("\\u2013", "-") // En dash
                      .replaceAll("\\u2014", "--") // Em dash
                      .replaceAll("\\u2022", "•") // Bullet point
                      .trim();
        }
    }

    private String extractTextFromTxt(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        
        // Try to detect encoding and read with proper charset
        try {
            // First try UTF-8
            String text = new String(bytes, "UTF-8");
            // Check if it contains replacement characters (invalid UTF-8)
            if (!text.contains("\uFFFD")) {
                return cleanText(text);
            }
        } catch (Exception e) {
            log.debug("UTF-8 decoding failed, trying other encodings");
        }
        
        // Try Windows-1252 (common for Windows documents)
        try {
            String text = new String(bytes, "Windows-1252");
            return cleanText(text);
        } catch (Exception e) {
            log.debug("Windows-1252 decoding failed, trying ISO-8859-1");
        }
        
        // Fallback to ISO-8859-1 (never fails)
        String text = new String(bytes, "ISO-8859-1");
        return cleanText(text);
    }
    
    private String cleanText(String text) {
        // Normalize line breaks and clean up common issues
        return text.replaceAll("\\r\\n", "\n")
                  .replaceAll("\\r", "\n")
                  .replaceAll("\\u00A0", " ") // Non-breaking space
                  .replaceAll("\\t", "    ") // Replace tabs with spaces
                  .replaceAll("[ \\t]+$", "") // Remove trailing whitespace
                  .replaceAll("\\n{3,}", "\n\n") // Limit consecutive newlines
                  .trim();
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
