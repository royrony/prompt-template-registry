# Traceability — Prompt Template Registry + Renderer (C2 SSE Evidence)

Links the C2 evidence chain together: spec → plan → tasks → code → self-review → PR. The index a
reviewer (mentor) uses to navigate the artifact.

## Context

- **Program**: AI Adoption — C2 Certification (AI-Integrated Competence Level)
- **Role**: SSE
- **SSE evidence requirement**: *"Build a feature in your practice app entirely through an
  OpenSpec/Speckit-driven flow — spec, task breakdown, AI-generated implementation, self-review,
  PR — with the spec artifact and traceability visible."*
- **Practice app / repo**: `prompt-template-registry` (standalone, self-contained, in-memory H2)
- **Spec-first tooling**: Speckit-format artifacts under `specs/001-prompt-template-registry/`
- **AI flow**: deterministic chain `/specify → /plan → /tasks → implement (TDD) → self-review → PR`

## Feature summary

A versioned prompt-template registry: register templates with `{{variables}}`, render the latest
version against supplied values with strict missing/unknown-variable validation, keep every edit as
an immutable new version. Structured JSON throughout. No LLM, no network, no external DB.

## Artifact index

| Stage | Artifact | Status |
| --- | --- | --- |
| Spec | [`spec.md`](./spec.md) | done |
| Plan | [`plan.md`](./plan.md) | done |
| Tasks | [`tasks.md`](./tasks.md) | done |
| Prompt-engineering log | [`evidence/PROMPTING.md`](./evidence/PROMPTING.md) | done |
| Self-review log | [`evidence/SELF-REVIEW.md`](./evidence/SELF-REVIEW.md) | pending |
| Demo transcript | [`evidence/CURL-DEMO.md`](./evidence/CURL-DEMO.md) | done |
| PR | (link once raised) | pending |

## Acceptance-criteria → implementation map

Populated in the plan (Task 3) and checked off as code/tests land.

| FR | Description | Component | Test | Status |
| --- | --- | --- | --- | --- |
| FR-001 | Register template → 201 + version + declaredVariables | `PromptTemplateController.create` + `PromptTemplateService.register` + `TemplateResponse` | `PromptTemplateServiceTest` (done); `PromptTemplateControllerTest` create-201 (done) | done |
| FR-002 | Parse `{{var}}` → declaredVariables | `VariableParser.parse` | `VariableParserTest` (done) | done |
| FR-003 | Reject malformed placeholder (400) | `VariableParser` → `MalformedTemplateException` + `ApiExceptionHandler` | `VariableParserTest` (done); `PromptTemplateServiceTest` (done); `ApiExceptionHandlerTest` (done) | done |
| FR-004 | Re-register → new immutable version | `PromptTemplateService.register` + `PromptTemplateRepository` | `PromptTemplateServiceTest` (done); `PromptTemplateRepositoryTest` (done) | done |
| FR-005 | Render latest, single literal pass | `TemplateRenderer.render` + `PromptTemplateService.render` | `TemplateRendererTest` (done); `PromptTemplateServiceTest` (done) | done |
| FR-006 | Missing/unknown vars → 400 listing both | `TemplateRenderer` → `VariableValidationException` + `ApiExceptionHandler` | `TemplateRendererTest` (done); `PromptTemplateControllerTest` render-400 (done) | done |
| FR-007 | Unknown name → 404 standard body | `PromptTemplateService` → `TemplateNotFoundException` + `ApiExceptionHandler` | `PromptTemplateServiceTest` (done); `PromptTemplateControllerTest` 404 (done) | done |
| FR-008 | List all / get latest / list versions | `PromptTemplateController` + service + repo finders | `PromptTemplateRepositoryTest` (done); `PromptTemplateServiceTest` (done); `PromptTemplateControllerTest` (done) | done |
| FR-009 | Structured JSON; one error shape | DTOs + `ApiError` + `ApiExceptionHandler` | `PromptTemplateControllerTest` 400/404 (done); `ApiExceptionHandlerTest` (done) | done |
| FR-010 | Input bounds enforced (400) | `CreateTemplateRequest` bean validation + `ApiExceptionHandler` | `PromptTemplateControllerTest` blank/illegal-name 400 (done) | done |
| FR-009 | Structured JSON; one error shape | DTOs + `ApiError` + `ApiExceptionHandler` | error-shape test across 404/400/400 | pending |
| FR-010 | Input bounds enforced (400) | `CreateTemplateRequest` bean validation + handler | controller validation-400 | pending |
