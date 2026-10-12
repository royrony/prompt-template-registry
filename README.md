# Prompt Template Registry + Renderer

A small, self-contained Spring Boot service that stores **versioned prompt templates** with named
`{{variables}}` and renders them against supplied values with strict validation. Built as C2
(AI-Integrated Competence Level) SSE evidence, entirely through a spec-driven (Speckit) flow.

It is intentionally dependency-light: an **in-memory H2** database means you can clone, build, run,
and demo it with nothing but a JDK and Maven — no API keys, no network, no external services.

## Why this feature

It maps directly onto the C2 *Advanced Prompt Engineering* topics — reusable prompt **templates with
variables**, enforced **structured output**, and validation that a filled prompt has no missing or
unknown variables — without needing a live LLM. The "AI" concern here is prompt construction, which
is exactly where structured, reusable prompt engineering lives.

## Run it

```bash
mvn spring-boot:run
# service on http://localhost:8080
```

## Build & test

```bash
mvn test            # unit + web-slice + context tests
mvn clean package   # build the jar
```

## Spec-driven artifacts

This project was built spec-first. The full chain lives under [`specs/`](./specs):

- `specs/001-prompt-template-registry/spec.md` — specification + acceptance criteria
- `specs/001-prompt-template-registry/plan.md` — technical plan
- `specs/001-prompt-template-registry/tasks.md` — TDD task breakdown
- `specs/001-prompt-template-registry/TRACEABILITY.md` — acceptance-criteria → code → test map
- `specs/001-prompt-template-registry/evidence/` — prompt-engineering + self-review logs

## API (summary — authoritative version in the spec)

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/templates` | Register a new template (name + body with `{{vars}}`) |
| `GET`  | `/api/templates` | List templates (latest version each) |
| `GET`  | `/api/templates/{name}` | Get the latest version of a template |
| `GET`  | `/api/templates/{name}/versions` | List all versions of a template |
| `POST` | `/api/templates/{name}/render` | Render the latest version with supplied variables |

Rendering fails with `400` if any declared variable is missing, or any supplied variable is unknown.
Unknown template name returns `404`. Error bodies share one standard shape.
