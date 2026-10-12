package dev.ssec2.ptr.domain;

import dev.ssec2.ptr.service.VariableValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * T2 - pure renderer behavior (spec FR-005 substitution, FR-006 missing/unknown validation).
 */
class TemplateRendererTest {

    @Test
    void substitutesAllOccurrences() {
        String out = TemplateRenderer.render(
                "Hello {{name}}, welcome to {{product}}.",
                List.of("name", "product"),
                Map.of("name", "Rony", "product", "Frequency"));

        assertThat(out).isEqualTo("Hello Rony, welcome to Frequency.");
    }

    @Test
    void repeatedVariableReplacedEverywhere() {
        String out = TemplateRenderer.render("{{x}} and {{x}}", List.of("x"), Map.of("x", "A"));
        assertThat(out).isEqualTo("A and A");
    }

    @Test
    void valueContainingPlaceholderIsNotReExpanded() {
        String out = TemplateRenderer.render(
                "{{a}} then {{b}}",
                List.of("a", "b"),
                Map.of("a", "{{b}}", "b", "DONE"));
        // a's value "{{b}}" must stay literal, not become "DONE"
        assertThat(out).isEqualTo("{{b}} then DONE");
    }

    @Test
    void valueWithRegexSpecialCharsInsertedLiterally() {
        String out = TemplateRenderer.render(
                "cost is {{amount}}",
                List.of("amount"),
                Map.of("amount", "$5 \\ 50% off"));
        assertThat(out).isEqualTo("cost is $5 \\ 50% off");
    }

    @Test
    void zeroVariableBodyRendersToItself() {
        String out = TemplateRenderer.render("no vars here", List.of(), Map.of());
        assertThat(out).isEqualTo("no vars here");
    }

    @Test
    void whitespaceInsideBracesStillSubstitutes() {
        String out = TemplateRenderer.render("Hi {{ name }}", List.of("name"), Map.of("name", "R"));
        assertThat(out).isEqualTo("Hi R");
    }

    @Test
    void missingVariable_isRejectedAndNamed() {
        VariableValidationException ex = catchThrowableOfType(
                () -> TemplateRenderer.render(
                        "Hello {{name}} {{product}}",
                        List.of("name", "product"),
                        Map.of("name", "Rony")),
                VariableValidationException.class);

        assertThat(ex.getMissing()).containsExactly("product");
        assertThat(ex.getUnknown()).isEmpty();
    }

    @Test
    void unknownVariable_isRejectedAndNamed() {
        VariableValidationException ex = catchThrowableOfType(
                () -> TemplateRenderer.render(
                        "Hello {{name}}",
                        List.of("name"),
                        Map.of("name", "Rony", "extra", "Y")),
                VariableValidationException.class);

        assertThat(ex.getUnknown()).containsExactly("extra");
        assertThat(ex.getMissing()).isEmpty();
    }

    @Test
    void missingAndUnknownReportedTogether() {
        VariableValidationException ex = catchThrowableOfType(
                () -> TemplateRenderer.render(
                        "Hello {{name}} {{product}}",
                        List.of("name", "product"),
                        Map.of("name", "Rony", "extra", "Y")),
                VariableValidationException.class);

        assertThat(ex.getMissing()).containsExactly("product");
        assertThat(ex.getUnknown()).containsExactly("extra");
    }

    @Test
    void nullSuppliedMapTreatedAsEmpty() {
        assertThatThrownBy(() -> TemplateRenderer.render("Hi {{name}}", List.of("name"), null))
                .isInstanceOf(VariableValidationException.class);
    }
}
