# Prompt-Engineering Evidence Log

C2 SSE evidence. Captures the prompt-engineering techniques applied while driving the Speckit flow
through an AI agent — annotating **chaining**, **structured output**, and **caching-aware
prompting** (the *Advanced Prompt Engineering* course topics).

The chain is the flow itself; each stage's output is the next stage's input (deterministic
chaining, not an autonomous agent loop):

```
/specify  ->  /plan  ->  /tasks  ->  implement (per task, TDD)  ->  self-review  ->  PR
   |            |           |              |                            |
 spec.md     plan.md    tasks.md     src + tests                 SELF-REVIEW.md
```

---

## Stage 1 — `/specify` (spec authoring)

**Technique: caching-aware prompting.** A stable, reusable context prefix is placed first and kept
byte-identical across every stage, so the provider can cache the shared prefix; only the
stage-specific instruction varies at the tail. The stable prefix for this project is:

- `README.md` (feature intent + planned API surface)
- The Speckit `spec.md` section schema (template shape)
- The fixed feature decisions (versioned templates, `{{var}}` syntax, strict render validation,
  structured JSON)

**Technique: structured output.** The spec was requested against a fixed section schema rather than
free prose, so it is diffable and each requirement is addressable:
`Feature Specification` header → `Clarifications` → `User Scenarios & Testing` → `Requirements`
(FR-nnn) → `Key Entities` → `Success Criteria` (SC-nnn) → `Assumptions`. Acceptance scenarios are
written Given/When/Then so each maps 1:1 to a test later.

**Prompt used (verbatim intent):**

> Using the planned API in `README.md` and the Speckit spec template schema, write `spec.md` for a
> **Prompt Template Registry + Renderer**. Encode these as explicit, testable acceptance criteria:
> templates have a unique `name` + `body` with `{{variable}}` placeholders; the distinct placeholder
> set is `declaredVariables`; re-registering a name creates a new immutable version (start at 1);
> render replaces every occurrence in a single literal pass (values are not re-expanded); render
> fails 400 if any declared variable is missing OR any supplied key is unknown, reporting **both**
> sets; malformed placeholders are rejected 400 at registration; unknown template name → 404;
> everything is structured JSON with one shared error shape; enforce input bounds. Follow the
> template section order exactly, number requirements FR-nnn and success criteria SC-nnn, and record
> every resolved ambiguity under Clarifications.

**Output:** [`../spec.md`](../spec.md) — FR-001..FR-010, four user stories (register/render,
validation, versioning, not-found) with Given/When/Then acceptance scenarios, SC-001..SC-004, and
seven clarifications pinning down variable syntax, versioning semantics, single-pass substitution,
and input bounds.

**Why structured output here:** the Given/When/Then acceptance scenarios make the spec → test
mapping mechanical — each becomes a named unit or web-slice test in Tasks 5–9.
