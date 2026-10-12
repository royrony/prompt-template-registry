package dev.ssec2.ptr.web.dto;

import java.util.Map;

/**
 * Request to render a template: the variable values to substitute. A null/absent map is treated as
 * an empty map (valid only for a zero-variable template).
 */
public record RenderRequest(Map<String, String> variables) {
}
