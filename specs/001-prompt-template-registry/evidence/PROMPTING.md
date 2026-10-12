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


---

## Stage 2 — `/plan` (technical plan)

**Technique: chaining.** Input is exactly Stage 1's output ([`spec.md`](../spec.md)). Every FR is
carried forward and assigned a concrete Spring Boot component plus a planned test; no component is
introduced that doesn't trace to an FR.

**Technique: caching-aware prompting.** Same stable prefix (README + Speckit schema + fixed feature
decisions) kept byte-identical; only the "map FRs onto components" instruction and the confirmed
stack facts (Spring Boot 3.3.4, package `dev.ssec2.ptr`, H2) vary at the tail.

**Technique: structured output.** Requested as fixed sections — Approach, Package layout,
Component→FR table, Data model, Key design decisions, Testing strategy, Out of scope — so each FR
has a row and the plan is skimmable.

**Prompt used (verbatim intent):**

> Chain from `spec.md`. Produce `plan.md` mapping every FR onto concrete `dev.ssec2.ptr` components:
> a `PromptTemplate` JPA entity (insert-only versioning), a repository, two **pure** domain helpers
> `VariableParser` and `TemplateRenderer` (no Spring, so the core logic is unit-tested directly), a
> service, REST DTOs, a controller under `/api/templates`, and a `@ControllerAdvice` emitting one
> `ApiError` shape. Decide how substitution avoids re-expansion, how versioning stays immutable, and
> whether `declaredVariables` is stored or derived — justify each. Emit an FR→component→test table.

**Output:** [`../plan.md`](../plan.md). Key decisions recorded there:
- `declaredVariables` is **derived** from `body` on read, never stored — no drift (FR-002).
- **Insert-only** versioning: `nextVersion = max(version)+1`; no update path exists (FR-004, SC-003).
- Single-pass substitution via `Matcher.appendReplacement` + `quoteReplacement` so values are
  inserted literally and never re-scanned (FR-005).
- One `ApiError` built in exactly one place per branch in `ApiExceptionHandler` (FR-009, SC-004).
- Pure `VariableParser` / `TemplateRenderer` isolate the behavioral core for framework-free tests.

**Chain integrity check:** every FR-001..FR-010 has a component and a planned test in the
TRACEABILITY map before any code is written.


---

## Stage 3 — `/tasks` (task breakdown)

**Technique: chaining.** Input is [`plan.md`](../plan.md). Each task cites the FRs it covers and the
component the plan assigned, so the task list is a mechanical projection of the plan — a new design
decision in tasks would be a chain break.

**Technique: structured output.** Fixed per-task schema — *Test first* → *Implement* → *Done when* —
so each task is independently verifiable and strictly TDD (the test precedes the code).

**Technique: caching-aware prompting.** Same stable prefix; only the plan + "emit TDD tasks"
instruction varied at the tail.

**Prompt used (verbatim intent):**

> Chain from `plan.md`. Emit `tasks.md` as an ordered, TDD-granular list: pure `VariableParser` →
> pure `TemplateRenderer` → entity+repository → service → DTOs+controller → exception handler →
> end-to-end. For each task give the test to write first, the files to implement, the FRs covered,
> and a `mvn -q -B test` done-when. Order by dependency; mark which tasks are independent.

**Output:** [`../tasks.md`](../tasks.md) — T1 parser, T2 renderer (both pure/independent), T3
entity+repo (`@DataJpaTest`), T4 service (Mockito), T5 DTOs+controller (`@WebMvcTest`), T6 exception
handler + `ApiError`, T7 end-to-end + curl capture. Definition-of-done requires full-suite green and
every FR exercised.

**Chain integrity check:** every FR-001..FR-010 appears in at least one task's coverage list and
every task's first test maps back to a spec acceptance scenario — closing the
spec → plan → tasks chain before any production code is written.
