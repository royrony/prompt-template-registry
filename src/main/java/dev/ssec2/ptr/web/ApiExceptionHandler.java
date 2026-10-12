package dev.ssec2.ptr.web;

import dev.ssec2.ptr.domain.MalformedTemplateException;
import dev.ssec2.ptr.service.TemplateNotFoundException;
import dev.ssec2.ptr.service.VariableValidationException;
import dev.ssec2.ptr.web.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates domain/validation exceptions into the one standard {@link ApiError} body
 * (spec FR-007, FR-009, SC-004). Every branch builds the error in exactly one place.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TemplateNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MalformedTemplateException.class)
    public ResponseEntity<ApiError> handleMalformed(MalformedTemplateException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(VariableValidationException.class)
    public ResponseEntity<ApiError> handleVariableValidation(VariableValidationException ex,
                                                             HttpServletRequest req) {
        List<String> details = new ArrayList<>();
        ex.getMissing().forEach(v -> details.add("missing: " + v));
        ex.getUnknown().forEach(v -> details.add("unknown: " + v));
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, details);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest req) {
        List<String> details = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> details.add(fe.getField() + ": " + fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Request validation failed", req, details);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest req, List<String> details) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message,
                req.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
