package dev.ssec2.ptr.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T1 - pure parser behavior (spec FR-002 parsing, FR-003 malformed detection).
 */
class VariableParserTest {

    @Test
    void extractsDistinctVariablesInFirstAppearanceOrder() {
        assertThat(VariableParser.parse("Hi {{name}}, re {{name}} and {{product}}"))
                .containsExactly("name", "product");
    }

    @Test
    void trimsWhitespaceInsideBraces() {
        assertThat(VariableParser.parse("Hello {{  name  }}")).containsExactly("name");
    }

    @Test
    void bodyWithNoPlaceholders_declaresNoVariables() {
        assertThat(VariableParser.parse("just plain text, no vars")).isEmpty();
    }

    @Test
    void underscoreAndDigitsInNameAreValid() {
        assertThat(VariableParser.parse("{{first_name}} {{addr2}}"))
                .containsExactly("first_name", "addr2");
    }

    @Test
    void unclosedPlaceholder_isMalformed() {
        assertThatThrownBy(() -> VariableParser.parse("Hello {{name"))
                .isInstanceOf(MalformedTemplateException.class);
    }

    @Test
    void nameStartingWithDigit_isMalformed() {
        assertThatThrownBy(() -> VariableParser.parse("Hello {{1bad}}"))
                .isInstanceOf(MalformedTemplateException.class);
    }

    @Test
    void emptyPlaceholder_isMalformed() {
        assertThatThrownBy(() -> VariableParser.parse("Hello {{}}"))
                .isInstanceOf(MalformedTemplateException.class);
    }

    @Test
    void nameWithIllegalCharacter_isMalformed() {
        assertThatThrownBy(() -> VariableParser.parse("Hello {{na-me}}"))
                .isInstanceOf(MalformedTemplateException.class);
    }

    @Test
    void nullBody_isMalformed() {
        assertThatThrownBy(() -> VariableParser.parse(null))
                .isInstanceOf(MalformedTemplateException.class);
    }
}
