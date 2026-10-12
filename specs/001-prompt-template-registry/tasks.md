# Tasks: Prompt Template Registry + Renderer

**Spec**: [`spec.md`](./spec.md) · **Plan**: [`plan.md`](./plan.md) · **Package**: `dev.ssec2.ptr`

Ordered, TDD-granular tasks. Each names the test written first, the files implemented, and the FRs
covered. Gate per task:

```bash
mvn -q -B test -Dtest=<TestClass>     # focused
mvn -q -B test                        # full suite (gate before each commit)
```

Dependency order: T1 (parser) and T2 (renderer) are pure and independent → T3 (entity+repo) →
T4 (service) depends on T1–T3 → T5 (DTOs+controller) + T6 (error handler) depend on T4 →
T7 wires end-to-end. Build increments are demoable from T4 onward.

---

## T1 — VariableParser (pure)  *(FR-002, FR-003)*

**Test first**: `domain/VariableParserTest`
- extracts distinct, insertion-ordered variables from a body (`"Hi {{name}} re {{name}} {{x}}"` →
  `[name, x]`).
- trims whitespace inside braces (`{{ name }}` → `name`).
- zero placeholders → empty list.
- malformed → `MalformedTemplateException`: unclosed `{{name`, invalid name `{{1bad}}`, empty
  `{{}}`.

**Implement**: `domain/VariableParser` (regex `\{\{\s*([A-Za-z_][A-Za-z0-9_]*)\s*\}\}`; scan for
stray `{{`/`}}` that don't form a valid placeholder → throw), `domain/MalformedTemplateException`.

**Done when**: `mvn -q -B test -Dtest=VariableParserTest` green.

---

## T2 — TemplateRenderer (pure)  *(FR-005, FR-006)*

**Test first**: `domain/TemplateRendererTest`
- replaces every occurrence single-pass (`"{{x}} and {{x}}"`, `{x:A}` → `"A and A"`).
- value containing `{{y}}` is inserted literally, never re-expanded.
- value containing regex-special `$`/`\` inserted literally (quoteReplacement).
- missing declared var → `VariableValidationException` with `missing=[...]`.
- unknown supplied key → `VariableValidationException` with `unknown=[...]`.
- both at once → exception lists both sets.
- zero-variable body with empty map → body unchanged.

**Implement**: `domain/TemplateRenderer.render(body, declaredVars, suppliedVars)` using
`Matcher.appendReplacement` + `Matcher.quoteReplacement`; `service/VariableValidationException`
(carries `missing`, `unknown` lists).

**Done when**: `mvn -q -B test -Dtest=TemplateRendererTest` green.

---

## T3 — Entity + repository  *(FR-004, FR-008)*

**Test first**: `repository/PromptTemplateRepositoryTest` (`@DataJpaTest`, H2)
- persist (name, v1) and (name, v2); `findTopByNameOrderByVersionDesc` returns v2.
- `findByNameOrderByVersionAsc` returns [v1, v2].
- `findMaxVersion(name)` returns 2; returns null/empty for unknown name.
- latest-per-name query returns each name once at its highest version.

**Implement**: `domain/PromptTemplate` (`@Entity`, unique `(name,version)`, `@PrePersist` sets
`createdAt`), `repository/PromptTemplateRepository` with the finders above (latest-per-name via
`@Query`).

**Done when**: `mvn -q -B test -Dtest=PromptTemplateRepositoryTest` green.

---

## T4 — Service  *(FR-001, FR-004, FR-005, FR-006, FR-007, FR-008)*

**Test first**: `service/PromptTemplateServiceTest` (Mockito-mocked repo; real parser+renderer)
- `register` new name → version 1, declaredVariables parsed.
- `register` existing name → version = max+1; prior version untouched (insert-only).
- `register` malformed body → propagates `MalformedTemplateException` (no save).
- `getLatest` unknown name → `TemplateNotFoundException`.
- `getVersions` unknown name → `TemplateNotFoundException`.
- `render` unknown name → `TemplateNotFoundException`.
- `render` happy path delegates to renderer, returns rendered + version used.
- `render` missing/unknown var → `VariableValidationException` surfaces.
- `list` returns latest-per-name.

**Implement**: `service/PromptTemplateService` (`@Service`), `service/TemplateNotFoundException`.

**Done when**: `mvn -q -B test -Dtest=PromptTemplateServiceTest` green.

---

## T5 — DTOs + controller  *(FR-001, FR-008, FR-009, FR-010)*

**Test first**: `web/PromptTemplateControllerTest` (`@WebMvcTest` + MockMvc, mocked service)
- `POST /api/templates` valid → `201`, JSON has name/version/declaredVariables.
- `POST` with blank name / oversized body → `400` (bean validation).
- `GET /api/templates` → `200` list.
- `GET /api/templates/{name}` → `200` latest; unknown → `404`.
- `GET /api/templates/{name}/versions` → `200` list.
- `POST /api/templates/{name}/render` valid → `200` rendered; missing/unknown var → `400`.

**Implement**: `web/dto/{CreateTemplateRequest,TemplateResponse,RenderRequest,RenderResponse}`,
`web/PromptTemplateController` (thin; `@Valid` on create).

**Done when**: `mvn -q -B test -Dtest=PromptTemplateControllerTest` green.

---

## T6 — Exception handler + ApiError  *(FR-007, FR-009, SC-004)*

**Test first**: `web/ApiExceptionHandlerTest`
- `TemplateNotFoundException` → 404 body with all standard fields.
- `VariableValidationException` → 400 body; `details` lists missing+unknown.
- `MalformedTemplateException` → 400 body.
- one shared `ApiError` shape across all three (timestamp, status, error, message, path, details).

**Implement**: `web/dto/ApiError`, `web/ApiExceptionHandler` (`@RestControllerAdvice`) mapping the
three domain exceptions + `MethodArgumentNotValidException` → the single `ApiError`.

**Done when**: `mvn -q -B test -Dtest=ApiExceptionHandlerTest` green.

---

## T7 — End-to-end wiring + verification  *(all FRs; SC-001..SC-004)*

- Full suite green: `mvn -q -B test`.
- `mvn spring-boot:run`; `curl` the five endpoints incl. a 404, a validation 400, and a
  missing-variable 400; capture request/response transcript into `evidence/` for the demo.
- Confirm no LLM/network dependency and in-memory H2 only.

**Done when**: full suite green + curl transcript captured.

---

## Definition of done (all tasks)

- Full `mvn -q -B test` green.
- Every FR-001..FR-010 exercised by at least one test; TRACEABILITY map flipped to done with test
  names.
- No external service/network/LLM dependency; runs on H2 only.
