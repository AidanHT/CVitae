package com.cvitae.service.impl;

import com.cvitae.dto.ExportRequest;
import com.cvitae.entity.Resume;
import com.cvitae.repository.ResumeRepository;
import com.cvitae.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportServiceImpl implements ExportService {

    private final ResumeRepository resumeRepository;
    private final WebClient webClient;

    @Value("${latex.service.url:http://localhost:8082}")
    private String latexServiceUrl;

    @Override
    public String generateLatexCode(ExportRequest request) {
        log.info("Generating LaTeX code for resume: {}", request.getResumeId());
        
        Resume resume = resumeRepository.findById(request.getResumeId())
            .orElseThrow(() -> new RuntimeException("Resume not found"));

        // Return existing LaTeX code or custom code if provided
        if (request.getCustomLatexCode() != null && !request.getCustomLatexCode().isEmpty()) {
            return request.getCustomLatexCode();
        }

        return resume.getLatexCode();
    }

    @Override
    public Resource generatePdf(ExportRequest request) {
        log.info("Generating PDF for resume: {}", request.getResumeId());
        
        try {
            String latexCode = generateLatexCode(request);
            
            // Call LaTeX service to compile PDF
            Map<String, Object> requestBody = Map.of(
                "latex", latexCode,
                "name", "resume",
                "paperSize", request.getPaperSize(),
                "orientation", request.getOrientation()
            );

            byte[] pdfBytes = webClient.post()
                .uri(latexServiceUrl + "/compile/pdf")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

            if (pdfBytes == null) {
                throw new RuntimeException("Failed to generate PDF");
            }

            // Save PDF to temporary file
            Path tempFile = Files.createTempFile("resume_", ".pdf");
            Files.write(tempFile, pdfBytes);

            return new FileSystemResource(tempFile.toFile());

        } catch (Exception e) {
            log.error("Error generating PDF", e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    @Override
    public Resource generateImage(ExportRequest request, String format) {
        log.info("Generating {} image for resume: {}", format.toUpperCase(), request.getResumeId());
        
        try {
            String latexCode = generateLatexCode(request);
            
            // Call LaTeX service to compile image
            Map<String, Object> requestBody = Map.of(
                "latex", latexCode,
                "name", "resume",
                "format", format.toLowerCase(),
                "dpi", request.getDpi(),
                "backgroundColor", request.getBackgroundColor(),
                "highQuality", request.getHighQuality()
            );

            byte[] imageBytes = webClient.post()
                .uri(latexServiceUrl + "/compile/image")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();

            if (imageBytes == null) {
                throw new RuntimeException("Failed to generate image");
            }

            // Save image to temporary file
            Path tempFile = Files.createTempFile("resume_", "." + format.toLowerCase());
            Files.write(tempFile, imageBytes);

            return new FileSystemResource(tempFile.toFile());

        } catch (Exception e) {
            log.error("Error generating image", e);
            throw new RuntimeException("Failed to generate image", e);
        }
    }

    @Override
    public Resource getExportFile(UUID exportId) {
        log.info("Retrieving export file: {}", exportId);
        
        // This is a simplified implementation
        // In practice, you'd want to store export metadata and file paths
        
        try {
            Path exportPath = Paths.get(System.getProperty("java.io.tmpdir"), exportId.toString());
            if (!Files.exists(exportPath)) {
                throw new RuntimeException("Export file not found");
            }
            
            return new FileSystemResource(exportPath.toFile());
            
        } catch (Exception e) {
            log.error("Error retrieving export file", e);
            throw new RuntimeException("Failed to retrieve export file", e);
        }
    }
}
