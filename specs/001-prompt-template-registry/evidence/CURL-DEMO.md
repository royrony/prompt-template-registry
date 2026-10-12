# End-to-End Demo Transcript

Captured against the running app (`mvn spring-boot:run` / `java -jar target/prompt-template-registry-0.1.0.jar`,
in-memory H2, port 8080). Reproduces the spec's user stories and every error path. Automated proof is
the 50-test suite (`mvn test`); this transcript is the human-facing demo.

```bash
BASE=http://localhost:8080/api/templates
```

## 1. Register a template (US1, FR-001) → 201

```bash
curl -i -X POST $BASE -H 'Content-Type: application/json' \
  -d '{"name":"greeting","body":"Hello {{name}}, welcome to {{product}}."}'
```
```
HTTP/1.1 201
{"name":"greeting","version":1,"body":"Hello {{name}}, welcome to {{product}}.",
 "declaredVariables":["name","product"],"createdAt":"2026-10-12T07:16:13.988035Z"}
```

## 2. Re-register same name → new immutable version (US3, FR-004) → 201 v2

```bash
curl -X POST $BASE -H 'Content-Type: application/json' \
  -d '{"name":"greeting","body":"Hi {{name}}!"}'
```
```json
{"name":"greeting","version":2,"body":"Hi {{name}}!","declaredVariables":["name"],
 "createdAt":"2026-10-12T07:16:14.042167Z"}
```

## 3. List templates — each name once at latest (FR-008)

```bash
curl $BASE
```
```json
[{"name":"greeting","version":2,"body":"Hi {{name}}!","declaredVariables":["name"], ...}]
```

## 4. Get latest (FR-008) → v2

```bash
curl $BASE/greeting
```
```json
{"name":"greeting","version":2,"body":"Hi {{name}}!","declaredVariables":["name"], ...}
```

## 5. Get all versions (FR-008) → v1 preserved unchanged + v2

```bash
curl $BASE/greeting/versions
```
```json
[{"name":"greeting","version":1,"body":"Hello {{name}}, welcome to {{product}}.",
  "declaredVariables":["name","product"], ...},
 {"name":"greeting","version":2,"body":"Hi {{name}}!","declaredVariables":["name"], ...}]
```

## 6. Render latest (US1, FR-005) → 200

```bash
curl -X POST $BASE/greeting/render -H 'Content-Type: application/json' \
  -d '{"variables":{"name":"Rony"}}'
```
```json
{"name":"greeting","version":2,"rendered":"Hi Rony!","variables":{"name":"Rony"}}
```

## 7. Render with a missing variable (US2, FR-006) → 400

```bash
curl -X POST $BASE/greeting/render -H 'Content-Type: application/json' -d '{"variables":{}}'
```
```json
{"timestamp":"...","status":400,"error":"Bad Request",
 "message":"Variable mismatch. Missing: [name].",
 "path":"/api/templates/greeting/render","details":["missing: name"]}
```

## 8. Render with an unknown variable (US2, FR-006) → 400

```bash
curl -X POST $BASE/greeting/render -H 'Content-Type: application/json' \
  -d '{"variables":{"name":"Rony","extra":"x"}}'
```
```json
{"timestamp":"...","status":400,"error":"Bad Request",
 "message":"Variable mismatch. Unknown: [extra].",
 "path":"/api/templates/greeting/render","details":["unknown: extra"]}
```

## 9. Unknown template (US4, FR-007) → 404

```bash
curl $BASE/does-not-exist
```
```json
{"timestamp":"...","status":404,"error":"Not Found",
 "message":"No template found with name 'does-not-exist'.",
 "path":"/api/templates/does-not-exist"}
```

## 10. Malformed placeholder at registration (FR-003) → 400

```bash
curl -X POST $BASE -H 'Content-Type: application/json' -d '{"name":"bad","body":"Hello {{1bad}}"}'
```
```json
{"timestamp":"...","status":400,"error":"Bad Request",
 "message":"Malformed placeholder at index 6: a '{{' must be a valid '{{name}}' ...",
 "path":"/api/templates"}
```

## 11. Invalid name — bean validation (FR-010) → 400

```bash
curl -X POST $BASE -H 'Content-Type: application/json' -d '{"name":"has space!","body":"x"}'
```
```json
{"timestamp":"...","status":400,"error":"Bad Request","message":"Request validation failed",
 "path":"/api/templates","details":["name: name may contain only letters, digits, '.', '_', '-'"]}
```

---

**Observations tying back to the spec:**
- v1 body is byte-identical in step 5 after v2 was created → immutable versioning (SC-003).
- The single `ApiError` shape appears across 400 (missing), 400 (unknown), 404, 400 (malformed),
  400 (bean validation) → one error contract (SC-004, FR-009).
- Render refused to produce a half-filled prompt in steps 7–8 → the feature's core value (SC-002).
- No LLM, no network, no external DB were involved → fully self-contained artifact.
