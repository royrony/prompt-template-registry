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
| Plan | [`plan.md`](./plan.md) | pending |
| Tasks | [`tasks.md`](./tasks.md) | pending |
| Prompt-engineering log | [`evidence/PROMPTING.md`](./evidence/PROMPTING.md) | in progress |
| Self-review log | [`evidence/SELF-REVIEW.md`](./evidence/SELF-REVIEW.md) | pending |
| PR | (link once raised) | pending |

## Acceptance-criteria → implementation map

Populated in the plan (Task 3) and checked off as code/tests land.

| FR | Description | Component | Test | Status |
| --- | --- | --- | --- | --- |
| FR-001 | Register template → 201 + version + declaredVariables | _TBD plan_ | | pending |
| FR-002 | Parse `{{var}}` → declaredVariables | _TBD plan_ | | pending |
| FR-003 | Reject malformed placeholder (400) | _TBD plan_ | | pending |
| FR-004 | Re-register → new immutable version | _TBD plan_ | | pending |
| FR-005 | Render latest, single literal pass | _TBD plan_ | | pending |
| FR-006 | Missing/unknown vars → 400 listing both | _TBD plan_ | | pending |
| FR-007 | Unknown name → 404 standard body | _TBD plan_ | | pending |
| FR-008 | List all / get latest / list versions | _TBD plan_ | | pending |
| FR-009 | Structured JSON; one error shape | _TBD plan_ | | pending |
| FR-010 | Input bounds enforced (400) | _TBD plan_ | | pending |
