package com.cvitae.controller;

import com.cvitae.dto.ExportRequest;
import com.cvitae.service.ExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ExportController {

    private final ExportService exportService;

    @PostMapping("/latex")
    public ResponseEntity<String> exportLatex(@Valid @RequestBody ExportRequest request) {
        log.info("Exporting resume to LaTeX format");
        String latexCode = exportService.generateLatexCode(request);
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume.tex\"")
                .body(latexCode);
    }

    @PostMapping("/pdf")
    public ResponseEntity<Resource> exportPdf(@Valid @RequestBody ExportRequest request) {
        log.info("Exporting resume to PDF format");
        Resource pdfResource = exportService.generatePdf(request);
        
        // Validate that the PDF was generated correctly
        if (!exportService.validateExportedFile(pdfResource, "pdf")) {
            throw new RuntimeException("Generated PDF file is invalid or empty");
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume.pdf\"")
                .body(pdfResource);
    }

    @PostMapping("/image")
    public ResponseEntity<Resource> exportImage(
            @Valid @RequestBody ExportRequest request,
            @RequestParam(defaultValue = "png") String format) {
        log.info("Exporting resume to {} format", format.toUpperCase());
        
        // Validate format
        if (!format.matches("(?i)(png|jpg|jpeg)")) {
            throw new RuntimeException("Unsupported image format: " + format + ". Supported formats: PNG, JPG, JPEG");
        }
        
        Resource imageResource = exportService.generateImage(request, format);
        
        // Validate that the image was generated correctly
        if (!exportService.validateExportedFile(imageResource, format)) {
            throw new RuntimeException("Generated image file is invalid or empty");
        }
        
        MediaType mediaType = switch (format.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    String.format("attachment; filename=\"resume.%s\"", format.toLowerCase()))
                .body(imageResource);
    }

    @GetMapping("/formats")
    public ResponseEntity<Map<String, Object>> getAvailableFormats() {
        log.info("Getting available export formats");
        
        Map<String, Object> formats = Map.of(
            "latex", Map.of(
                "name", "LaTeX Source Code",
                "description", "Raw LaTeX code for custom editing",
                "mimeType", "text/plain",
                "extension", "tex"
            ),
            "pdf", Map.of(
                "name", "PDF Document", 
                "description", "Professional PDF format",
                "mimeType", "application/pdf",
                "extension", "pdf"
            ),
            "png", Map.of(
                "name", "PNG Image",
                "description", "High-quality image format",
                "mimeType", "image/png",
                "extension", "png"
            ),
            "jpg", Map.of(
                "name", "JPEG Image",
                "description", "Compressed image format",
                "mimeType", "image/jpeg",
                "extension", "jpg"
            )
        );
        
        return ResponseEntity.ok(formats);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> exportHealth() {
        log.info("Checking export service health");
        
        Map<String, Object> health = exportService.getExportHealthStatus();
        boolean isHealthy = (Boolean) health.getOrDefault("healthy", false);
        
        return isHealthy ? 
            ResponseEntity.ok(health) : 
            ResponseEntity.status(503).body(health);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadExport(@PathVariable UUID id) {
        log.info("Downloading export with ID: {}", id);
        Resource resource = exportService.getExportFile(id.toString(), "pdf"); // Default to PDF
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.pdf\"")
                .body(resource);
    }
}