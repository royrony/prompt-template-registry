package dev.ssec2.ptr.domain;

import dev.ssec2.ptr.service.VariableValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure (no Spring/JPA) renderer. Substitutes every {@code {{variable}}} occurrence in a body with
 * its supplied value in a single literal pass - values are inserted verbatim and never re-scanned
 * for further placeholders (spec FR-005).
 *
 * <p>Before substituting it enforces an exact match between declared and supplied variables: any
 * declared variable with no value, or any supplied key that is not declared, aborts the render with
 * a {@link VariableValidationException} naming both sets (spec FR-006).
 */
public final class TemplateRenderer {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*}}");

    private TemplateRenderer() {
    }

    public static String render(String body, List<String> declaredVariables,
                                Map<String, String> suppliedVariables) {
        Map<String, String> supplied = suppliedVariables == null ? Map.of() : suppliedVariables;

        validate(declaredVariables, supplied);

        Matcher matcher = PLACEHOLDER.matcher(body);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String value = supplied.get(name);
            // quoteReplacement so a value containing $ or \ (or {{...}}) is inserted literally.
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static void validate(List<String> declaredVariables, Map<String, String> supplied) {
        List<String> declared = declaredVariables == null ? List.of() : declaredVariables;

        List<String> missing = new ArrayList<>();
        for (String name : declared) {
            if (!supplied.containsKey(name)) {
                missing.add(name);
            }
        }

        List<String> unknown = new ArrayList<>();
        for (String key : supplied.keySet()) {
            if (!declared.contains(key)) {
                unknown.add(key);
            }
        }

        if (!missing.isEmpty() || !unknown.isEmpty()) {
            throw new VariableValidationException(missing, unknown);
        }
    }
}
