package dev.ssec2.ptr.service;

/**
 * Thrown when a referenced template name does not exist. Surfaced as HTTP 404 (spec FR-007).
 */
public class TemplateNotFoundException extends RuntimeException {
    public TemplateNotFoundException(String name) {
        super("No template found with name '" + name + "'.");
    }
}
