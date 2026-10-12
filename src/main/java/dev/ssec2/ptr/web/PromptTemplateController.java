package dev.ssec2.ptr.web;

import dev.ssec2.ptr.domain.PromptTemplate;
import dev.ssec2.ptr.service.PromptTemplateService;
import dev.ssec2.ptr.service.RenderResult;
import dev.ssec2.ptr.web.dto.CreateTemplateRequest;
import dev.ssec2.ptr.web.dto.RenderRequest;
import dev.ssec2.ptr.web.dto.RenderResponse;
import dev.ssec2.ptr.web.dto.TemplateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST surface for the template registry. Thin: delegates all logic to {@link PromptTemplateService}
 * and maps domain objects to DTOs. Error handling is centralized in {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService service;

    /** FR-001: register a new template version. */
    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request) {
        PromptTemplate saved = service.register(request.name(), request.body());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /** FR-008: list each template name once at its latest version. */
    @GetMapping
    public List<TemplateResponse> list() {
        return service.listLatest().stream().map(this::toResponse).toList();
    }

    /** FR-008: latest version of a name (404 if unknown). */
    @GetMapping("/{name}")
    public TemplateResponse getLatest(@PathVariable String name) {
        return toResponse(service.getLatest(name));
    }

    /** FR-008: all versions of a name, oldest first (404 if unknown). */
    @GetMapping("/{name}/versions")
    public List<TemplateResponse> getVersions(@PathVariable String name) {
        return service.getVersions(name).stream().map(this::toResponse).toList();
    }

    /** FR-005/FR-006: render the latest version (400 on missing/unknown vars, 404 if unknown). */
    @PostMapping("/{name}/render")
    public RenderResponse render(@PathVariable String name, @RequestBody(required = false) RenderRequest request) {
        RenderResult result = service.render(name,
                request == null ? null : request.variables());
        return RenderResponse.of(result);
    }

    private TemplateResponse toResponse(PromptTemplate template) {
        return TemplateResponse.of(template, service.declaredVariablesOf(template));
    }
}
