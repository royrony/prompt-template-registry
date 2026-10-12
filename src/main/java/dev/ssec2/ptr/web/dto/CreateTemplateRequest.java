package dev.ssec2.ptr.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to register a new template version. Bean-validation enforces the input bounds from the
 * spec (FR-010): name 1-100 chars matching {@code [A-Za-z0-9._-]+}; body 1-10,000 chars.
 */
public record CreateTemplateRequest(

        @NotBlank(message = "name must not be blank")
        @Size(min = 1, max = 100, message = "name must be 1-100 characters")
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "name may contain only letters, digits, '.', '_', '-'")
        String name,

        @NotBlank(message = "body must not be blank")
        @Size(min = 1, max = 10_000, message = "body must be 1-10000 characters")
        String body
) {
}
