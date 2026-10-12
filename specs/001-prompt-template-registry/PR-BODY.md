# Prompt Template Registry + Renderer

Built entirely through a spec-driven (Speckit) flow as C2 (AI-Integrated Competence Level) SSE
evidence: spec → plan → tasks → AI-generated implementation (TDD) → self-review → this PR.

## What this is

A self-contained Spring Boot service that stores **versioned prompt templates** with named
`{{variables}}` and renders them against supplied values with strict missing/unknown-variable
validation. In-memory H2 — clone, `mvn test`, `mvn spring-boot:run`, no external setup.

## Spec artifacts (traceability)

- Spec: `specs/001-prompt-template-registry/spec.md`
- Plan: `specs/001-prompt-template-registry/plan.md`
- Tasks: `specs/001-prompt-template-registry/tasks.md`
- Traceability map: `specs/001-prompt-template-registry/TRACEABILITY.md`
- Prompt-engineering log: `specs/001-prompt-template-registry/evidence/PROMPTING.md`
- Self-review: `specs/001-prompt-template-registry/evidence/SELF-REVIEW.md`
- Live demo transcript: `specs/001-prompt-template-registry/evidence/CURL-DEMO.md`

## Acceptance criteria

- [x] FR-001 Register template → 201 with version + declaredVariables
- [x] FR-002 Parse `{{var}}` into distinct, ordered declared variables
- [x] FR-003 Malformed placeholder rejected (400); no row created
- [x] FR-004 Re-register creates a new immutable version
- [x] FR-005 Render in a single literal pass (no re-expansion)
- [x] FR-006 Missing/unknown variables → 400 listing both
- [x] FR-007 Unknown template name → 404 standard error body
- [x] FR-008 List latest / get latest / list versions
- [x] FR-009 Structured JSON; one standard error shape
- [x] FR-010 Input bounds enforced (400)

## Testing

`mvn clean test` → **50 tests, 0 failures**. Layers covered: pure domain (parser, renderer),
`@DataJpaTest` repository, Mockito service, `@WebMvcTest` controller, exception-handler unit test,
and a Spring context test. End-to-end behavior captured live in the demo transcript.

## Notes

- Single-tenant, in-memory H2, no auth — intentional for a self-contained practice/evidence app.
- One documented design smell (validation-exception package placement) is called out in the
  self-review rather than hidden.
