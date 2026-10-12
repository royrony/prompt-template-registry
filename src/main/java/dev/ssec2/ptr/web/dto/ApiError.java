package dev.ssec2.ptr.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * The single standard error body shape for every error path (spec FR-009, SC-004).
 * {@code details} is omitted from JSON when null (used only for validation errors).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, path,
                (details == null || details.isEmpty()) ? null : details);
    }
}
