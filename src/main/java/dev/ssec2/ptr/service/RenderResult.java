package dev.ssec2.ptr.service;

import java.util.Map;

/**
 * Outcome of rendering a template: which template/version was used, the fully substituted text,
 * and the variables that were applied. Produced on read; never persisted.
 */
public record RenderResult(String name, int version, String rendered, Map<String, String> variables) {
}
