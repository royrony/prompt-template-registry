package dev.ssec2.ptr.web.dto;

import dev.ssec2.ptr.service.RenderResult;

import java.util.Map;

/**
 * The rendered output returned to clients: which template/version was used, the substituted text,
 * and the variables applied.
 */
public record RenderResponse(
        String name,
        int version,
        String rendered,
        Map<String, String> variables
) {
    public static RenderResponse of(RenderResult result) {
        return new RenderResponse(result.name(), result.version(), result.rendered(), result.variables());
    }
}
