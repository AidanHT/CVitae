package com.cvitae.ai;

import com.cvitae.dto.GenerateResumeRequest;
import com.cvitae.dto.JobAnalysisResponse;
import com.cvitae.exception.CVitaeException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enhanced Groq AI service with comprehensive error handling, retries, and mock mode
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedGroqAIService implements GroqAIService {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama3-8b-8192}")
    private String groqModel;

    @Value("${groq.mock.enabled:false}")
    private boolean mockModeEnabled;

    @Value("${groq.timeout.request:30}")
    private int requestTimeoutSeconds;

    @Value("${groq.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${groq.retry.delay-seconds:2}")
    private int retryDelaySeconds;

    @Qualifier("groqWebClient")
    private final WebClient groqWebClient;
    
    private final ObjectMapper objectMapper;
    
    private final AtomicReference<GroqAIHealthStatus> lastHealthStatus = new AtomicReference<>();

    @Override
    public JobAnalysisResponse analyzeJobPosting(String jobPosting, String jobTitle, String companyName) {
        log.info("Analyzing job posting for: {} at {}", jobTitle, companyName);
        
        if (mockModeEnabled) {
            return generateMockJobAnalysis(jobTitle, companyName);
        }
        
        if (!isApiKeyConfigured()) {
            log.warn("Groq API key not configured, using fallback analysis");
            return generateFallbackJobAnalysis(jobTitle, companyName);
        }

        try {
            String prompt = buildJobAnalysisPrompt(jobPosting, jobTitle, companyName);
            String aiResponse = callGroqAPIWithRetry(prompt);
            return parseJobAnalysisResponse(aiResponse, jobTitle, companyName);
            
        } catch (Exception e) {
            log.error("Error analyzing job posting", e);
            updateHealthStatus(false, "Job analysis failed: " + e.getMessage());
            return generateFallbackJobAnalysis(jobTitle, companyName);
        }
    }

    @Override
    public String generateTailoredResume(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request) {
        log.info("Generating tailored resume for: {}", request.getJobTitle());
        
        if (mockModeEnabled) {
            return generateMockTailoredResume(masterResume, request);
        }
        
        if (!isApiKeyConfigured()) {
            log.warn("Groq API key not configured, using fallback generation");
            return generateFallbackResume(masterResume, request);
        }

        try {
            String prompt = buildResumeGenerationPrompt(masterResume, jobAnalysis, request);
            return callGroqAPIWithRetry(prompt);
            
        } catch (Exception e) {
            log.error("Error generating tailored resume", e);
            updateHealthStatus(false, "Resume generation failed: " + e.getMessage());
            return generateFallbackResume(masterResume, request);
        }
    }

    @Override
    public String processChatMessage(String message, String context, String resumeContent, String jobContent) {
        log.info("Processing chat message for resume optimization");
        
        if (mockModeEnabled) {
            return generateMockChatResponse(message);
        }
        
        if (!isApiKeyConfigured()) {
            log.warn("Groq API key not configured, using fallback chat response");
            return generateFallbackChatResponse(message);
        }

        try {
            String prompt = buildChatPrompt(message, context, resumeContent, jobContent);
            return callGroqAPIWithRetry(prompt);
            
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            updateHealthStatus(false, "Chat processing failed: " + e.getMessage());
            return generateFallbackChatResponse(message);
        }
    }

    @Override
    public boolean isServiceAvailable() {
        return isApiKeyConfigured() && !mockModeEnabled;
    }

    @Override
    public GroqAIHealthStatus getHealthStatus() {
        GroqAIHealthStatus status = lastHealthStatus.get();
        if (status == null) {
            // Perform initial health check
            performHealthCheck();
            status = lastHealthStatus.get();
        }
        return status;
    }

    private String callGroqAPIWithRetry(String prompt) {
        long startTime = System.currentTimeMillis();
        
        return groqWebClient.post()
                .bodyValue(buildRequestBody(prompt))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofSeconds(retryDelaySeconds))
                    .filter(this::shouldRetry)
                    .doBeforeRetry(retrySignal -> 
                        log.warn("Retrying Groq API call, attempt {}/{}", 
                            retrySignal.totalRetries() + 1, maxRetryAttempts)))
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.debug("Groq API call successful in {}ms", duration);
                    updateHealthStatus(true, "API call successful", (int) duration);
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Groq API call failed after {}ms: {}", duration, error.getMessage());
                    updateHealthStatus(false, "API call failed: " + error.getMessage());
                })
                .map(this::parseGroqResponse)
                .block();
    }

    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            // Retry on server errors (5xx) and rate limits (429), but not on client errors (4xx)
            return ex.getStatusCode().is5xxServerError() || ex.getStatusCode().value() == 429;
        }
        // Retry on timeout and connection errors
        return throwable instanceof java.util.concurrent.TimeoutException ||
               throwable instanceof java.net.ConnectException;
    }

    private String parseGroqResponse(String response) {
        try {
            JsonNode responseNode = objectMapper.readTree(response);
            JsonNode choices = responseNode.path("choices");
            
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText();
            }
            
            throw new CVitaeException(HttpStatus.BAD_GATEWAY, "Invalid API Response", 
                "Groq API returned unexpected response format", null);
                
        } catch (Exception e) {
            log.error("Error parsing Groq response: {}", e.getMessage());
            throw new CVitaeException(HttpStatus.BAD_GATEWAY, "API Parsing Error", 
                "Failed to parse AI response", null);
        }
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        return Map.of(
            "model", groqModel,
            "messages", List.of(
                Map.of("role", "system", "content", "You are an expert resume writer and career advisor with deep knowledge of ATS systems."),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 4000,
            "temperature", 0.7,
            "stream", false
        );
    }

    private boolean isApiKeyConfigured() {
        return groqApiKey != null && 
               !groqApiKey.trim().isEmpty() && 
               !groqApiKey.equals("your-groq-api-key") &&
               !groqApiKey.startsWith("${");
    }

    private void performHealthCheck() {
        try {
            if (mockModeEnabled) {
                updateHealthStatus(true, "Mock mode enabled", 0);
                return;
            }
            
            if (!isApiKeyConfigured()) {
                updateHealthStatus(false, "API key not configured");
                return;
            }

            // Perform simple API test
            String testResponse = callGroqAPIWithRetry("Hello, please respond with 'OK' if you can process this message.");
            boolean healthy = testResponse != null && testResponse.toLowerCase().contains("ok");
            
            updateHealthStatus(healthy, healthy ? "API responding normally" : "API test failed");
            
        } catch (Exception e) {
            updateHealthStatus(false, "Health check failed: " + e.getMessage());
        }
    }

    private void updateHealthStatus(boolean available, String message) {
        updateHealthStatus(available, message, null);
    }

    private void updateHealthStatus(boolean available, String message, Integer responseTime) {
        GroqAIHealthStatus status = GroqAIHealthStatus.builder()
                .available(available)
                .apiKeyConfigured(isApiKeyConfigured())
                .status(available ? "healthy" : "unhealthy")
                .message(message)
                .lastChecked(LocalDateTime.now())
                .lastResponseTimeMs(responseTime)
                .mockMode(mockModeEnabled)
                .build();
                
        lastHealthStatus.set(status);
    }

    // Mock and fallback methods would be implemented here
    private JobAnalysisResponse generateMockJobAnalysis(String jobTitle, String companyName) {
        // Implementation for mock job analysis
        return JobAnalysisResponse.builder()
                .jobTitle(jobTitle)
                .companyName(companyName)
                .analysisNotes("Mock analysis for " + jobTitle + " at " + companyName)
                .build();
    }

    private JobAnalysisResponse generateFallbackJobAnalysis(String jobTitle, String companyName) {
        // Implementation for fallback job analysis
        return generateMockJobAnalysis(jobTitle, companyName);
    }

    private String generateMockTailoredResume(String masterResume, GenerateResumeRequest request) {
        return "Mock tailored resume for " + request.getJobTitle() + 
               " based on the provided master resume.";
    }

    private String generateFallbackResume(String masterResume, GenerateResumeRequest request) {
        return generateMockTailoredResume(masterResume, request);
    }

    private String generateMockChatResponse(String message) {
        return "Mock response to: " + message + 
               ". This is a placeholder response for development/testing.";
    }

    private String generateFallbackChatResponse(String message) {
        return generateMockChatResponse(message);
    }

    // Prompt building methods would be implemented here
    private String buildJobAnalysisPrompt(String jobPosting, String jobTitle, String companyName) {
        return String.format("Analyze this job posting for %s at %s: %s", jobTitle, companyName, jobPosting);
    }

    private String buildResumeGenerationPrompt(String masterResume, JobAnalysisResponse jobAnalysis, GenerateResumeRequest request) {
        return String.format("Generate a tailored resume for %s based on: %s", request.getJobTitle(), masterResume);
    }

    private String buildChatPrompt(String message, String context, String resumeContent, String jobContent) {
        return String.format("Context: %s. User message: %s", context, message);
    }

    private JobAnalysisResponse parseJobAnalysisResponse(String aiResponse, String jobTitle, String companyName) {
        // Implementation for parsing AI response into JobAnalysisResponse
        return generateMockJobAnalysis(jobTitle, companyName);
    }
}
