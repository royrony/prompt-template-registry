package dev.ssec2.ptr.service;

import dev.ssec2.ptr.domain.PromptTemplate;
import dev.ssec2.ptr.domain.TemplateRenderer;
import dev.ssec2.ptr.domain.VariableParser;
import dev.ssec2.ptr.repository.PromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates template registration, reads, and rendering over the repository and the pure domain
 * helpers ({@link VariableParser}, {@link TemplateRenderer}).
 *
 * <p>Registration is insert-only: a new version is always appended, never overwritten (FR-004).
 * The parser validates the body on register (malformed → 400) and the renderer validates variables
 * on render (missing/unknown → 400). Unknown names raise {@link TemplateNotFoundException} (404).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTemplateService {

    private final PromptTemplateRepository repository;

    /**
     * Register a new version of a template. Validates the body first (may throw
     * {@code MalformedTemplateException}) so nothing is persisted for a malformed template.
     */
    @Transactional
    public PromptTemplate register(String name, String body) {
        // Parse first - a malformed body must not create a row.
        VariableParser.parse(body);

        Integer currentMax = repository.findMaxVersion(name);
        int nextVersion = (currentMax == null ? 0 : currentMax) + 1;

        PromptTemplate saved = repository.save(new PromptTemplate(name, nextVersion, body));
        log.info("Registered template '{}' version {}", name, nextVersion);
        return saved;
    }

    /** Each template name once, at its latest version. */
    @Transactional(readOnly = true)
    public List<PromptTemplate> listLatest() {
        return repository.findLatestPerName();
    }

    /** Latest version of a name, or 404. */
    @Transactional(readOnly = true)
    public PromptTemplate getLatest(String name) {
        return repository.findTopByNameOrderByVersionDesc(name)
                .orElseThrow(() -> new TemplateNotFoundException(name));
    }

    /** All versions of a name (oldest first), or 404 if the name is unknown. */
    @Transactional(readOnly = true)
    public List<PromptTemplate> getVersions(String name) {
        List<PromptTemplate> versions = repository.findByNameOrderByVersionAsc(name);
        if (versions.isEmpty()) {
            throw new TemplateNotFoundException(name);
        }
        return versions;
    }

    /**
     * Render the latest version of a name against the supplied variables. 404 if the name is
     * unknown; the renderer raises {@code VariableValidationException} (400) on missing/unknown
     * variables.
     */
    @Transactional(readOnly = true)
    public RenderResult render(String name, Map<String, String> variables) {
        PromptTemplate latest = getLatest(name);
        List<String> declared = VariableParser.parse(latest.getBody());
        String rendered = TemplateRenderer.render(latest.getBody(), declared, variables);
        return new RenderResult(latest.getName(), latest.getVersion(), rendered,
                variables == null ? Map.of() : variables);
    }

    /** Convenience: declared variables for a stored template, derived from its body. */
    public List<String> declaredVariablesOf(PromptTemplate template) {
        return VariableParser.parse(template.getBody());
    }
}
