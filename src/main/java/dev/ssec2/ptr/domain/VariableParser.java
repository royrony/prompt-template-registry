package dev.ssec2.ptr.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure (no Spring/JPA) parser that extracts the declared {@code {{variable}}} placeholders from a
 * template body.
 *
 * <p>Valid placeholder: {@code {{ name }}} where {@code name} matches
 * {@code [A-Za-z_][A-Za-z0-9_]*} (surrounding whitespace tolerated and trimmed). Any {@code {{}}
 * opening that is not immediately followed by a valid name and closing {@code }}} is a malformed
 * template and raises {@link MalformedTemplateException} (spec FR-002, FR-003).
 */
public final class VariableParser {

    /** A well-formed placeholder: {{ optional-ws name optional-ws }}. */
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*}}");

    /** Any placeholder opening, used to detect openings the valid pattern did not consume. */
    private static final Pattern OPENING = Pattern.compile("\\{\\{");

    private VariableParser() {
    }

    /**
     * @return the distinct declared variable names, in first-appearance order (possibly empty).
     * @throws MalformedTemplateException if the body contains a malformed placeholder.
     */
    public static List<String> parse(String body) {
        if (body == null) {
            throw new MalformedTemplateException("Template body must not be null.");
        }

        // Collect valid placeholders and remember the spans they occupy.
        Set<String> ordered = new LinkedHashSet<>();
        boolean[] consumed = new boolean[body.length()];
        Matcher valid = PLACEHOLDER.matcher(body);
        while (valid.find()) {
            ordered.add(valid.group(1).trim());
            for (int i = valid.start(); i < valid.end(); i++) {
                consumed[i] = true;
            }
        }

        // Any "{{" opening not inside a consumed (valid) span is malformed.
        Matcher opening = OPENING.matcher(body);
        while (opening.find()) {
            if (!consumed[opening.start()]) {
                throw new MalformedTemplateException(
                        "Malformed placeholder at index " + opening.start()
                                + ": a '{{' must be a valid '{{name}}' with name matching "
                                + "[A-Za-z_][A-Za-z0-9_]*.");
            }
        }

        return new ArrayList<>(ordered);
    }
}
