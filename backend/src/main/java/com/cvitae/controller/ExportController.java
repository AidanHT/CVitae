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
        return ResponseEntity.ok(latexCode);
    }

    @PostMapping("/pdf")
    public ResponseEntity<Resource> exportPdf(@Valid @RequestBody ExportRequest request) {
        log.info("Exporting resume to PDF format");
        Resource pdfResource = exportService.generatePdf(request);
        
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
        Resource imageResource = exportService.generateImage(request, format);
        
        MediaType mediaType = switch (format.toLowerCase()) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        String.format("attachment; filename=\"resume.%s\"", format))
                .body(imageResource);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadExport(@PathVariable UUID id) {
        log.info("Downloading export with ID: {}", id);
        Resource resource = exportService.getExportFile(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export\"")
                .body(resource);
    }
}
