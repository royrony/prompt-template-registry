package dev.ssec2.ptr.domain;

/**
 * Thrown when a template body contains a malformed placeholder - unclosed braces, or a brace pair
 * whose contents are not a valid variable name. Surfaced as HTTP 400 at registration time
 * (spec FR-003); no template is created.
 */
public class MalformedTemplateException extends RuntimeException {
    public MalformedTemplateException(String message) {
        super(message);
    }
}
