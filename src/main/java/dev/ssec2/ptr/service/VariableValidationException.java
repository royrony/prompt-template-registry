package dev.ssec2.ptr.service;

import lombok.Getter;

import java.util.List;

/**
 * Thrown when a render request's supplied variables do not exactly match the template's declared
 * variables. Carries both problem sets so the caller sees everything wrong at once (spec FR-006):
 * {@code missing} = declared-but-not-supplied, {@code unknown} = supplied-but-not-declared.
 * Surfaced as HTTP 400.
 */
@Getter
public class VariableValidationException extends RuntimeException {

    private final List<String> missing;
    private final List<String> unknown;

    public VariableValidationException(List<String> missing, List<String> unknown) {
        super(buildMessage(missing, unknown));
        this.missing = List.copyOf(missing);
        this.unknown = List.copyOf(unknown);
    }

    private static String buildMessage(List<String> missing, List<String> unknown) {
        StringBuilder sb = new StringBuilder("Variable mismatch.");
        if (!missing.isEmpty()) {
            sb.append(" Missing: ").append(missing).append('.');
        }
        if (!unknown.isEmpty()) {
            sb.append(" Unknown: ").append(unknown).append('.');
        }
        return sb.toString();
    }
}
