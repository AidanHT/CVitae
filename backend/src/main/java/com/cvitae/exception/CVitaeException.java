package com.cvitae.exception;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Custom application exception for CVitae-specific errors
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class CVitaeException extends RuntimeException {
    private final HttpStatus status;
    private final String error;
    private final Map<String, Object> details;

    public CVitaeException(HttpStatus status, String error, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.error = error;
        this.details = details;
    }

    public static CVitaeException badRequest(String message) {
        return new CVitaeException(HttpStatus.BAD_REQUEST, "Bad Request", message, null);
    }

    public static CVitaeException notFound(String message) {
        return new CVitaeException(HttpStatus.NOT_FOUND, "Not Found", message, null);
    }

    public static CVitaeException serviceUnavailable(String message) {
        return new CVitaeException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", message, null);
    }
}
