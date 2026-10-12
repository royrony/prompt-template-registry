# Technical Plan: Prompt Template Registry + Renderer

**Spec**: [`spec.md`](./spec.md) · **Repo**: `prompt-template-registry` ·
**Base package**: `dev.ssec2.ptr` · **Stack**: Spring Boot 3.3.4, Java 17, H2 (in-memory), JPA.

## Approach

A classic layered Spring Boot slice: **controller → service → (parser + renderer) + repository →
entity**, with a `@ControllerAdvice` that renders one standard error body for 404 and 400. Two
pure, framework-free domain helpers carry the real logic — `VariableParser` (body → declared
variables, with malformed detection) and `TemplateRenderer` (substitution + missing/unknown
validation) — so the behavioral acceptance criteria are tested without Spring at all.

Immutability + versioning (FR-004) is enforced by never updating a row: registration computes
`nextVersion = max(version for name) + 1` and inserts a new `prompt_template` row. "Latest" reads
select the max version for a name.

## Package layout

```
dev.ssec2.ptr
├── PromptTemplateRegistryApplication
├── domain
│   ├── PromptTemplate                 (@Entity: id, name, version, body, createdAt)
│   ├── VariableParser                 (pure: body -> ordered distinct vars; throws on malformed)
│   ├── TemplateRenderer               (pure: (body, declaredVars, suppliedVars) -> rendered)
│   └── MalformedTemplateException     (RuntimeException -> 400)
├── repository
│   └── PromptTemplateRepository       (Spring Data JPA)
├── service
│   ├── PromptTemplateService          (register/list/get/getVersions/render orchestration)
│   ├── TemplateNotFoundException      (RuntimeException -> 404)
│   └── VariableValidationException    (RuntimeException -> 400; carries missing[] + unknown[])
├── web
│   ├── PromptTemplateController        (REST endpoints under /api/templates)
│   ├── dto
│   │   ├── CreateTemplateRequest       (name, body; bean-validation annotations)
│   │   ├── TemplateResponse            (name, version, body, declaredVariables, createdAt)
│   │   ├── RenderRequest               (variables: Map<String,String>)
│   │   └── RenderResponse              (name, version, rendered, variables)
│   └── ApiExceptionHandler             (@ControllerAdvice -> standard ApiError body)
└── web.dto.ApiError                    (timestamp, status, error, message, path, details[])
```

## Component → FR mapping

| FR | Component(s) | Mechanism |
| --- | --- | --- |
| FR-001 register → 201 + version + declaredVariables | `PromptTemplateController.create`, `PromptTemplateService.register`, `TemplateResponse` | POST maps request → service → 201 with body |
| FR-002 parse `{{var}}` | `VariableParser.parse(body)` | regex `\{\{\s*([A-Za-z_][A-Za-z0-9_]*)\s*\}\}`; distinct, insertion-ordered |
| FR-003 malformed → 400 | `VariableParser` throws `MalformedTemplateException`; `ApiExceptionHandler` → 400 | detect stray `{{` / `}}` not forming a valid placeholder |
| FR-004 new immutable version | `PromptTemplateService.register`, `PromptTemplateRepository` | `nextVersion = findMaxVersion(name)+1`; insert-only, no update path |
| FR-005 render single literal pass | `TemplateRenderer.render` | one regex `replaceAll` pass using supplied values; values inserted via `Matcher.quoteReplacement` (never re-scanned) |
| FR-006 missing/unknown → 400 both | `TemplateRenderer` → `VariableValidationException(missing, unknown)` | set-diff declared vs supplied; both lists in error |
| FR-007 unknown name → 404 | `PromptTemplateService` throws `TemplateNotFoundException`; handler → 404 | empty repository lookup |
| FR-008 list / get latest / versions | `PromptTemplateController` + `PromptTemplateService` + repo finders | `findLatestPerName`, `findTopByNameOrderByVersionDesc`, `findByNameOrderByVersionAsc` |
| FR-009 structured JSON + one error shape | all DTOs + `ApiError` + `ApiExceptionHandler` | Jackson; single `ApiError` builder used by every handler |
| FR-010 input bounds → 400 | `CreateTemplateRequest` bean validation + `@Valid`; handler maps `MethodArgumentNotValidException` | `@NotBlank`, `@Size`, `@Pattern` |

## Data model

`prompt_template` table (H2, MySQL mode):

| column | type | notes |
| --- | --- | --- |
| `id` | BIGINT PK auto | surrogate |
| `name` | VARCHAR(100) | part of unique key |
| `version` | INT | part of unique key; ≥1 |
| `body` | VARCHAR(10000) | raw template text |
| `created_at` | TIMESTAMP | set on persist |

Unique constraint `(name, version)`. `declaredVariables` is **not** persisted — it is derived from
`body` by `VariableParser` on read/response, keeping the stored row the single source of truth and
avoiding drift.

## Key design decisions

- **Pure domain helpers.** `VariableParser` and `TemplateRenderer` have no Spring/JPA dependency, so
  US1/US2 acceptance scenarios (the heart of the feature) are plain fast unit tests. This is the
  deepest-logic / lowest-framework boundary — easiest to test exhaustively.
- **Insert-only versioning.** No entity is ever mutated after creation (FR-004, SC-003). The service
  has no `update`/`save-over` path by construction.
- **Single-pass, quoted substitution.** Using `Matcher.appendReplacement` with
  `Matcher.quoteReplacement(value)` guarantees a value containing `{{x}}` or `$`/`\` is inserted
  literally and never re-expanded (FR-005, clarification).
- **One error shape.** `ApiError` is built in exactly one place per branch inside
  `ApiExceptionHandler`; controllers/services throw typed exceptions and never format errors
  themselves (FR-009, SC-004).
- **Derived, not stored, variables.** Prevents a stored `declaredVariables` from drifting out of
  sync with `body`.

## Testing strategy (previews Task breakdown)

- **Domain unit tests** (no Spring): `VariableParserTest`, `TemplateRendererTest` — cover parsing,
  malformed detection, substitution, repetition, missing/unknown diff, zero-variable case.
- **Service unit tests** (Mockito repo): `PromptTemplateServiceTest` — versioning increment,
  latest-wins, not-found, render orchestration delegating to renderer.
- **Web-slice tests** (`@WebMvcTest` + MockMvc, mocked service): `PromptTemplateControllerTest` —
  status codes + JSON shape for create(201), get/list/versions, render(200), 404, 400.
- **Error-shape test**: assert the one `ApiError` body across a 404, a validation 400, and a
  malformed-template 400.
- **Context test** (already present): `contextLoads`.

## Out of scope (from spec)

Auth/multi-tenant, persistence beyond in-memory, templating beyond `{{var}}` (no conditionals/loops),
and any LLM invocation.
