package com.cvitae.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler that ensures all API responses return structured JSON
 * with proper trace IDs for debugging and monitoring.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Unhandled exception [TraceID: {}]: {}", traceId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .traceId(traceId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred. Please contact support with trace ID: " + traceId)
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.error("Runtime exception [TraceID: {}]: {}", traceId, ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .traceId(traceId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Request Processing Error")
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.warn("Validation error [TraceID: {}]: {}", traceId, ex.getMessage());
        
        Map<String, Object> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse error = ErrorResponse.builder()
            .traceId(traceId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Invalid input data provided")
            .path(request.getDescription(false).replace("uri=", ""))
            .details(fieldErrors)
            .build();
            
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeException(MaxUploadSizeExceededException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.warn("File upload size exceeded [TraceID: {}]: {}", traceId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .traceId(traceId)
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
            .error("File Too Large")
            .message("The uploaded file exceeds the maximum size limit of 10MB")
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
            
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(CVitaeException.class)
    public ResponseEntity<ErrorResponse> handleCVitaeException(CVitaeException ex, WebRequest request) {
        String traceId = generateTraceId();
        log.warn("Application exception [TraceID: {}]: {}", traceId, ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .traceId(traceId)
            .timestamp(LocalDateTime.now())
            .status(ex.getStatus().value())
            .error(ex.getError())
            .message(ex.getMessage())
            .path(request.getDescription(false).replace("uri=", ""))
            .details(ex.getDetails())
            .build();
            
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
