package dev.ssec2.ptr.web.dto;

import dev.ssec2.ptr.domain.PromptTemplate;

import java.time.Instant;
import java.util.List;

/**
 * A template version as returned to clients. {@code declaredVariables} is derived from the body,
 * not stored (spec FR-002).
 */
public record TemplateResponse(
        String name,
        int version,
        String body,
        List<String> declaredVariables,
        Instant createdAt
) {
    public static TemplateResponse of(PromptTemplate template, List<String> declaredVariables) {
        return new TemplateResponse(
                template.getName(),
                template.getVersion(),
                template.getBody(),
                declaredVariables,
                template.getCreatedAt());
    }
}
