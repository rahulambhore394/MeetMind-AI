package com.meetmind.meetmind_backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        return buildErrorResponse(ex, ex.getReason(), ex.getStatusCode(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        
        return buildErrorResponse(ex, "Validation Failed", HttpStatus.BAD_REQUEST, request, errors);
    }

    @ExceptionHandler({RuntimeException.class, IllegalArgumentException.class})
    public ResponseEntity<Object> handleBusinessException(Exception ex, WebRequest request) {
        return buildErrorResponse(ex, ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex, WebRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.error("Unhandled exception [ID: {}]: {}", requestId, ex.getMessage(), ex);
        
        return buildErrorResponse(ex, "An internal server error occurred. Reference ID: " + requestId, 
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<Object> buildErrorResponse(Exception ex, String message, 
                                                      org.springframework.http.HttpStatusCode status, WebRequest request) {
        return buildErrorResponse(ex, message, status, request, null);
    }

    private ResponseEntity<Object> buildErrorResponse(Exception ex, String message, 
                                                      HttpStatus status, WebRequest request, 
                                                      Map<String, String> validationErrors) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        body.put("requestId", org.slf4j.MDC.get("requestId"));
        
        if (validationErrors != null) {
            body.put("validationErrors", validationErrors);
        }

        return new ResponseEntity<>(body, status);
    }

    private ResponseEntity<Object> buildErrorResponse(Exception ex, String message, 
                                                      org.springframework.http.HttpStatusCode status, 
                                                      WebRequest request, 
                                                      Map<String, String> validationErrors) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        body.put("requestId", org.slf4j.MDC.get("requestId"));
        
        if (validationErrors != null) {
            body.put("validationErrors", validationErrors);
        }

        return new ResponseEntity<>(body, status);
    }
}
