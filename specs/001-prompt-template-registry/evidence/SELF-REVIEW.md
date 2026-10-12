# Self-Review — Prompt Template Registry + Renderer

C2 SSE evidence requires self-reviewing the AI-generated code/tests against the spec's acceptance
criteria **before** raising a PR. This is that review: each FR is checked against the test(s) that
prove it, followed by honest notes on decisions, trade-offs, and gaps.

## Acceptance-criteria verification

| FR | Criterion | Verified by | Verdict |
| --- | --- | --- | --- |
| FR-001 | Register → 201 + version + declaredVariables | `PromptTemplateControllerTest.create_returns201WithBody`, `PromptTemplateServiceTest.registerNewName_startsAtVersion1` | PASS |
| FR-002 | Parse `{{var}}` → distinct, ordered | `VariableParserTest` (order, trim, underscores/digits) | PASS |
| FR-003 | Malformed placeholder → 400, no row created | `VariableParserTest` (unclosed/`{{1bad}}`/`{{}}`/`{{na-me}}`), `PromptTemplateServiceTest.registerMalformedBody_throws_andNeverSaves`, `ApiExceptionHandlerTest.malformed_maps400` | PASS |
| FR-004 | Re-register → new immutable version | `PromptTemplateServiceTest.registerExistingName_incrementsVersion_andInsertsNewRow`, `PromptTemplateRepositoryTest` (latest/versions/maxVersion), live demo step 5 shows v1 unchanged after v2 | PASS |
| FR-005 | Render single literal pass, no re-expansion | `TemplateRendererTest.valueContainingPlaceholderIsNotReExpanded`, `.valueWithRegexSpecialCharsInsertedLiterally`, `.repeatedVariableReplacedEverywhere` | PASS |
| FR-006 | Missing/unknown → 400 listing both | `TemplateRendererTest.missingAndUnknownReportedTogether`, `PromptTemplateControllerTest.render_missingVariable_returns400WithDetails` | PASS |
| FR-007 | Unknown name → 404 standard body | `PromptTemplateServiceTest` (getLatest/getVersions/render unknown), `PromptTemplateControllerTest.getLatest_unknown_returns404`, `.render_unknownName_returns404` | PASS |
| FR-008 | List latest / get latest / list versions | `PromptTemplateRepositoryTest.latestPerName_*`, `PromptTemplateControllerTest` (list/get/versions) | PASS |
| FR-009 | Structured JSON; one error shape | `ApiExceptionHandlerTest.allErrorBodies_shareTheSameShape`, live demo steps 7–11 all return the same `ApiError` shape | PASS |
| FR-010 | Input bounds → 400 | `PromptTemplateControllerTest.create_blankName_returns400`, `.create_illegalNameChars_returns400` | PASS |

Success criteria SC-001..SC-004 are each covered by the above plus the live transcript in
[`CURL-DEMO.md`](./CURL-DEMO.md). Full suite: **50 tests, 0 failures**.

## Decisions & trade-offs (honest notes)

1. **`VariableValidationException` lives in `service/` but is thrown by `domain/TemplateRenderer`.**
   This is a deliberate but imperfect choice: the renderer is otherwise framework-free, yet it
   depends on a `service`-package exception, creating a domain→service compile dependency that
   points the "wrong" way. Acceptable for an app this size, but in a larger codebase I would move
   both `VariableValidationException` and `MalformedTemplateException` into a neutral
   `domain`-level package (or a dedicated `error` package) so the pure domain has no outward
   dependency. **Flagged, not fixed** — changing it now would churn imports across 4 files for no
   behavioral gain in a demo artifact.

2. **`declaredVariables` is derived on every read, not stored.** Chosen to prevent drift from the
   body (single source of truth). Trade-off: re-parses on each response. Negligible at these sizes;
   if it ever mattered, cache per (name,version) since versions are immutable.

3. **In-memory H2, no auth.** Intentional per spec Assumptions — keeps the artifact clone-and-run
   with zero setup. The JPA mapping is MySQL-mode and would port to a real MySQL unchanged. Not
   production-ready by design.

4. **`@MockBean` is used in the web-slice test.** Deprecated in Spring Boot 3.4+, but this project
   pins 3.3.4 where it is current. If bumped to 3.4+, migrate to `@MockitoBean`.

5. **Single-pass substitution via `Matcher.quoteReplacement`.** Verified that values containing
   `{{x}}`, `$`, or `\` are inserted literally and never re-expanded — the subtle correctness point
   of the whole feature, and the test that would most likely catch a regression.

6. **No pagination on list endpoints.** Out of scope per spec; fine for the demo, would be the first
   addition for real use.

## Gaps / not covered (by design)

- No concurrency test for two simultaneous registrations of the same name racing on `nextVersion`.
  The unique `(name, version)` constraint makes the loser fail rather than silently overwrite, which
  is the safe outcome, but a retry-on-conflict path is not implemented. Noted as a known limitation.
- No OpenAPI/Swagger doc. Not required by the spec.

## Conclusion

Every FR has at least one green test; the live run matches the spec; the one design smell
(exception package placement) is documented rather than hidden. Ready to raise the PR.
