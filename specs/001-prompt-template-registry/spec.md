# Feature Specification: Prompt Template Registry + Renderer

**Feature Branch**: `001-prompt-template-registry`

**Created**: 2026-10-12

**Status**: Draft

**Input**: User description: "I want a small service where I can register reusable prompt templates
that contain named `{{variables}}`, keep every edit as a new immutable version, and render a
template by supplying values for its variables. Rendering must refuse to produce a half-filled
prompt: if a declared variable has no value, or a supplied value names a variable the template
doesn't declare, that's an error, not a silent pass. Everything returns structured JSON."

**Related**: C2 (AI-Integrated Competence Level) SSE evidence. Produced through a Speckit-driven
flow — see [`TRACEABILITY.md`](./TRACEABILITY.md). Maps to the *Advanced Prompt Engineering* topic
(reusable prompt templates with variables, enforced structured output).

## Clarifications

### Session 2026-10-12

- Q: What makes a prompt "template" here — free text with placeholders? → A: A template has a unique
  `name` and a `body` string. Placeholders use double-brace syntax `{{variableName}}`. The set of
  distinct placeholder names in the body is the template's **declared variables**.
- Q: What does "versioned" mean operationally? → A: Registering a `name` that already exists does
  not overwrite — it creates a new, higher **version** of that name. Every version is immutable once
  created. "The template" for reads/renders means the **latest** version unless a specific version
  is requested. Version numbers start at 1 and increment by 1 per name.
- Q: What is a valid variable name? → A: `[A-Za-z_][A-Za-z0-9_]*` (letter/underscore start, then
  alphanumerics/underscores). Whitespace inside the braces around the name is tolerated and trimmed
  (`{{ name }}` ≡ `{{name}}`). A brace pair that doesn't match this pattern is a malformed template
  and is rejected at registration (400).
- Q: On render, how strict is the variable match? → A: Exact set match required. Every declared
  variable MUST have a supplied value (missing → 400) and every supplied key MUST correspond to a
  declared variable (unknown → 400). Both problems are reported together, not one-at-a-time.
- Q: Can the same variable appear multiple times in a body? → A: Yes. Every occurrence is replaced
  with the same supplied value. It counts once in the declared-variable set.
- Q: Are variable values themselves scanned for `{{...}}`? → A: No. Substitution is single-pass and
  literal — a value that happens to contain `{{x}}` is inserted as-is and never re-expanded. This
  avoids injection surprises and keeps rendering deterministic.
- Q: What are the bounds on inputs? → A: `name` 1–100 chars matching `[A-Za-z0-9._-]+`; `body`
  1–10,000 chars. A template may declare 0 variables (a constant prompt), which renders by supplying
  an empty variables object.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register and render a reusable prompt (Priority: P1)

A practitioner registers a prompt once as a named template with `{{variables}}`, then renders it
many times with different values instead of copy-pasting and hand-editing free text each time.

**Why this priority**: This is the core loop — without register + render the service has no reason
to exist.

**Independent Test**: Register a template with a known body and variable set; render it with a full
set of values; confirm the returned text has every placeholder replaced and the response reports the
template name and version used.

**Acceptance Scenarios**:

1. **Given** no template named `greeting`, **When** a client POSTs `{name:"greeting",
   body:"Hello {{name}}, welcome to {{product}}."}`, **Then** the response is `201` with
   `version=1` and `declaredVariables=["name","product"]`.
2. **Given** template `greeting` v1 exists, **When** a client renders it with
   `{name:"Rony", product:"Frequency"}`, **Then** the response is `200` with
   `rendered="Hello Rony, welcome to Frequency."` and reports `name="greeting"`, `version=1`.
3. **Given** a template body repeats a variable (`"{{x}} and {{x}}"`), **When** it is rendered with
   `{x:"A"}`, **Then** the result is `"A and A"` and `x` appears once in `declaredVariables`.
4. **Given** a template that declares no variables, **When** it is rendered with an empty variables
   object, **Then** the body is returned unchanged with `200`.

---

### User Story 2 - Be stopped from producing a half-filled or wrong prompt (Priority: P1)

A practitioner must not be able to render a prompt with a variable left unfilled, or with a stray
value that doesn't belong — either would silently produce a wrong prompt that only shows its damage
downstream in an LLM call.

**Why this priority**: The validation is the feature's whole value proposition over naive string
formatting; a silent half-fill is the exact failure mode being designed out.

**Independent Test**: Render a template once omitting a declared variable, and once including an
extra undeclared key; confirm each is rejected with `400` and a body that names the offending
variables.

**Acceptance Scenarios**:

1. **Given** template `greeting` declaring `name` and `product`, **When** it is rendered with only
   `{name:"Rony"}`, **Then** the response is `400` and the error names `product` as missing.
2. **Given** the same template, **When** it is rendered with
   `{name:"Rony", product:"X", extra:"Y"}`, **Then** the response is `400` and the error names
   `extra` as unknown.
3. **Given** both problems at once (a missing declared variable and an unknown supplied key),
   **When** rendered, **Then** the response is `400` and the error lists both the missing and the
   unknown variable names.
4. **Given** a registration whose body contains a malformed placeholder (e.g. `{{1bad}}` or an
   unclosed `{{name`), **When** it is POSTed, **Then** the response is `400` and no template is
   created.

---

### User Story 3 - Evolve a prompt safely with versions (Priority: P2)

A practitioner improves a prompt over time but needs old versions to remain available and immutable,
so a change can't retroactively break a caller that depended on the previous wording.

**Why this priority**: Versioning is what makes the registry trustworthy for reuse; valuable but the
register/render/validate core (US1, US2) is usable without reading history.

**Independent Test**: Register the same name twice with different bodies; confirm the second
registration creates version 2, the latest read returns v2, and v1 is still retrievable and
unchanged.

**Acceptance Scenarios**:

1. **Given** template `greeting` v1 exists, **When** a client POSTs `greeting` again with a new
   body, **Then** the response is `201` with `version=2`, and v1 remains readable unchanged.
2. **Given** `greeting` has v1 and v2, **When** a client GETs `/api/templates/greeting`, **Then**
   the latest (v2) is returned.
3. **Given** `greeting` has v1 and v2, **When** a client GETs `/api/templates/greeting/versions`,
   **Then** both versions are listed with their version numbers and bodies.
4. **Given** several distinct template names exist, **When** a client GETs `/api/templates`, **Then**
   each name is listed once at its latest version.

---

### User Story 4 - Get a clear error for an unknown template (Priority: P2)

A client that references a template name that was never registered needs a clear `404`, not an empty
or misleading success.

**Independent Test**: GET and render a name that doesn't exist; confirm both return `404` with the
standard error body.

**Acceptance Scenarios**:

1. **Given** no template named `missing`, **When** a client GETs `/api/templates/missing`, **Then**
   the response is `404` with the standard error body.
2. **Given** no template named `missing`, **When** a client renders `missing`, **Then** the response
   is `404` with the standard error body.

---

### Edge Cases

- Leading/trailing whitespace inside braces (`{{ name }}`) is trimmed; the declared variable is
  `name`.
- A body of only text and no placeholders declares zero variables and renders to itself.
- Duplicate supplied keys cannot occur (JSON object keys are unique); last-wins is a non-issue.
- Variable values are inserted literally and never re-scanned for placeholders (single-pass).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a client register a prompt template with a `name` and a `body`,
  returning `201` with the assigned `version` and the template's `declaredVariables`.
- **FR-002**: The system MUST parse the body for `{{variable}}` placeholders using the name pattern
  `[A-Za-z_][A-Za-z0-9_]*` (whitespace inside braces trimmed), and expose the distinct set as
  `declaredVariables`.
- **FR-003**: The system MUST reject (`400`) a registration whose body contains a malformed
  placeholder (unclosed braces or a name not matching the pattern) and MUST NOT create a template in
  that case.
- **FR-004**: Registering an existing `name` MUST create a new version (previous highest + 1) rather
  than overwriting; version numbering starts at `1` per name. Every stored version MUST be immutable.
- **FR-005**: The system MUST render the latest version of a named template by replacing every
  placeholder occurrence with the supplied value for that variable, in a single literal pass (values
  are not re-expanded).
- **FR-006**: Rendering MUST fail with `400` if any declared variable has no supplied value
  (missing) or any supplied key is not a declared variable (unknown); the error MUST enumerate both
  the missing and the unknown variable names.
- **FR-007**: The system MUST return `404` with the standard error body when a referenced template
  name does not exist (on get, versions, or render).
- **FR-008**: The system MUST provide reads: list all templates (each name once at latest version),
  get the latest version of a name, and list all versions of a name.
- **FR-009**: All responses (success and error) MUST be structured JSON; error bodies MUST share one
  shape (`timestamp`, `status`, `error`, `message`, `path`, and for validation a `details` list).
- **FR-010**: Input bounds MUST be enforced: `name` 1–100 chars matching `[A-Za-z0-9._-]+`; `body`
  1–10,000 chars; violations return `400` with the standard error body.

### Key Entities

- **PromptTemplate (version)**: One immutable version of a named template — `name`, `version`,
  `body`, `declaredVariables` (derived from the body), `createdAt`. Uniqueness on `(name, version)`.
- **RenderResult**: The outcome of a render — `name`, `version`, `rendered` text, and the
  `variables` that were applied.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A practitioner can register a template and render it with a full variable set and
  receive the fully substituted prompt, with zero placeholders remaining, in a single call each.
- **SC-002**: 100% of renders that omit a declared variable or include an unknown variable are
  rejected with `400` and a message that names the offending variables — none silently succeed.
- **SC-003**: Re-registering a name never destroys or mutates a prior version; every previously
  created version remains byte-identical and retrievable.
- **SC-004**: Every endpoint returns structured JSON, and every error path returns the one standard
  error shape (verified across 404, validation-400, and malformed-template-400).

## Assumptions

- Single-tenant, no authentication — this is a self-contained practice/evidence app, not a
  production multi-user service. Access control is out of scope.
- In-memory H2 persistence is sufficient; data need not survive a restart for the demo. (The JPA
  mapping would work unchanged against MySQL.)
- Template bodies are plain text; no templating constructs beyond `{{variable}}` substitution
  (no conditionals, loops, or partials) are in scope.
- Rendering does not call any LLM. The service produces the final prompt string; sending it to a
  model is a separate concern deliberately left out so the artifact runs with no external
  dependencies.
- "Latest version" is the highest version number for a name.
