package dev.ssec2.ptr.service;

import dev.ssec2.ptr.domain.MalformedTemplateException;
import dev.ssec2.ptr.domain.PromptTemplate;
import dev.ssec2.ptr.repository.PromptTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * T4 - service orchestration with a mocked repository and the REAL parser/renderer
 * (spec FR-001, FR-004, FR-005, FR-006, FR-007, FR-008).
 */
@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private PromptTemplateRepository repository;

    @InjectMocks
    private PromptTemplateService service;

    @Test
    void registerNewName_startsAtVersion1() {
        when(repository.findMaxVersion("greeting")).thenReturn(null);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromptTemplate result = service.register("greeting", "Hello {{name}}");

        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(service.declaredVariablesOf(result)).containsExactly("name");
    }

    @Test
    void registerExistingName_incrementsVersion_andInsertsNewRow() {
        when(repository.findMaxVersion("greeting")).thenReturn(2);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.register("greeting", "Hello {{name}} v3");

        ArgumentCaptor<PromptTemplate> captor = ArgumentCaptor.forClass(PromptTemplate.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(3);
        assertThat(captor.getValue().getBody()).isEqualTo("Hello {{name}} v3");
    }

    @Test
    void registerMalformedBody_throws_andNeverSaves() {
        assertThatThrownBy(() -> service.register("bad", "Hello {{1bad}}"))
                .isInstanceOf(MalformedTemplateException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void getLatestUnknownName_throwsNotFound() {
        when(repository.findTopByNameOrderByVersionDesc("missing")).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.getLatest("missing"))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    void getVersionsUnknownName_throwsNotFound() {
        when(repository.findByNameOrderByVersionAsc("missing")).thenReturn(List.of());
        assertThatThrownBy(() -> service.getVersions("missing"))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    void renderUnknownName_throwsNotFound() {
        when(repository.findTopByNameOrderByVersionDesc("missing")).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> service.render("missing", Map.of()))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    void renderHappyPath_substitutesAndReportsVersion() {
        PromptTemplate stored = new PromptTemplate("greeting", 2, "Hello {{name}}");
        when(repository.findTopByNameOrderByVersionDesc("greeting"))
                .thenReturn(java.util.Optional.of(stored));

        RenderResult result = service.render("greeting", Map.of("name", "Rony"));

        assertThat(result.name()).isEqualTo("greeting");
        assertThat(result.version()).isEqualTo(2);
        assertThat(result.rendered()).isEqualTo("Hello Rony");
    }

    @Test
    void renderMissingVariable_throwsValidation() {
        PromptTemplate stored = new PromptTemplate("greeting", 1, "Hello {{name}} {{product}}");
        when(repository.findTopByNameOrderByVersionDesc("greeting"))
                .thenReturn(java.util.Optional.of(stored));

        assertThatThrownBy(() -> service.render("greeting", Map.of("name", "Rony")))
                .isInstanceOf(VariableValidationException.class);
    }

    @Test
    void listLatest_returnsRepositoryLatestPerName() {
        PromptTemplate g = new PromptTemplate("greeting", 2, "g");
        PromptTemplate f = new PromptTemplate("farewell", 1, "f");
        when(repository.findLatestPerName()).thenReturn(List.of(f, g));

        assertThat(service.listLatest()).containsExactly(f, g);
    }
}
