# Quality Report (draft)

Draft started 22 October 2026 from `main` at `9989505`. Sections marked **TODO** still need filling
in before submission. Every number here can be reproduced with the commands given in that section.

## 1. How we check quality

| Check | Where | Runs |
|---|---|---|
| Unit and integration tests (JUnit 5, Mockito) | `mainframe-interface-main/src/test/java` | every build |
| Acceptance tests, one per GIVEN/WHEN/THEN | `src/test/java/lavs/acceptance` | every build |
| 95% branch coverage gate (JaCoCo) | `pom.xml` | every build; the build fails below it |
| Layer rules (`ui` → `logic` → `data`) | `LayerRulesTests` | every build |
| CI: `mvnw clean verify` on Windows | `.github/workflows` | every PR and every push to `main` |
| Code metrics: coupling, complexity, cohesion | `./mvnw -Pmetrics validate` (CK 0.7.0) | on demand: baseline and final |

Run everything with `./mvnw clean verify` from `mainframe-interface-main`. The coverage report is
written to `target/site/jacoco/index.html` and CI uploads it as an artifact on every run.

## 2. Test results

836 tests in 83 test classes, 0 failures, 0 errors, 0 skipped. They take about 10 seconds.

| Package | Test classes | Tests |
|---|---|---|
| `lavs` (layer rules) | 1 | 11 |
| `lavs.acceptance` | 3 | 9 |
| `lavs.data` | 7 | 65 |
| `lavs.logic` | 30 | 433 |
| `lavs.model` | 9 | 35 |
| `lavs.ui` | 28 | 202 |
| `lavs.validation` | 3 | 66 |
| `mainframe.client` | 2 | 15 |

Notable kinds of test besides plain unit tests:

- **Against a fake mainframe.** Services run against `FakeConnection`, which returns scripted replies
  and throws on any request nobody scripted. A test can't pass by accident because the mainframe was
  never asked. It can also go offline (`setIsConnected`) and answer slowly (`setDelay`).
- **Offline queue integration.** `SyncIntegrationTests` runs the real SQLite queue on a temp file:
  saves made offline, restarts, and upload order once the connection is back.
- **Screens.** `lavs.ui` tests cover the JavaFX-free text and state helpers (e.g. `TopBarState`,
  `ContactText`). `FxScreenSmokeTests` loads the FXML screens so a broken `fx:id` or binding fails
  the build.
- **Layer rules.** `LayerRulesTests` reads the source and fails if `ui` imports `Connection`, the
  message classes or JDBC, or if `logic` imports JavaFX or JDBC. `LavsApp` is the one exception,
  because it builds the services.

## 3. Coverage

The gate is 95% **branch** coverage over the code that holds the logic. Two packages are left out,
and the reasons are written in `pom.xml`:

- `lavs/ui/**` is JavaFX controllers and views. Their logic is pulled out into JavaFX-free helpers,
  which sit in `ui` but are unit tested anyway (see above).
- `mainframe/client/**` is the client library we were given.

| Package | Branch coverage | Line coverage |
|---|---|---|
| `lavs.data` | 94.7% (126/133) | 95.5% (386/404) |
| `lavs.logic` | 97.5% (820/841) | 99.4% (1736/1746) |
| `lavs.model` | 100.0% (34/34) | 100.0% (324/324) |
| `lavs.validation` | 96.2% (50/52) | 96.8% (60/62) |
| **Gated total** | **97.2% (1030/1060)** | **98.8% (2506/2536)** |

**TODO:** update these numbers from the final `main` before submission, and attach the JaCoCo HTML
report (CI artifact `jacoco-coverage-report`).

## 4. Acceptance tests

Each acceptance criterion in the proposal is one test, and its `@DisplayName` is the criterion's own
wording, so the test report reads as the acceptance criteria. All 9 pass.

| Story | Criterion | Test |
|---|---|---|
| US6 Create a loan | No mailing address, or no phone or email → stopped and told what's missing | `US6AcceptanceTests` (2 tests: missing both / missing contact only) |
| US6 | Empty or invalid field → that field highlighted, nothing else lost | `US6AcceptanceTests` |
| US6 | All valid → created with status New, summary and repayments shown | `US6AcceptanceTests` |
| US9 Change a loan's status | Every borrower reachable → status becomes Pending | `US9AcceptanceTests` |
| US9 | A co-borrower missing details → stopped, told who needs updating | `US9AcceptanceTests` |
| US11 Losing the connection | Offline save → kept on the laptop, shown as waiting to upload | `US11AcceptanceTests` |
| US11 | Changes waiting, app closed and reopened → still there | `US11AcceptanceTests` |
| US11 | Connection back → uploaded automatically, in order | `US11AcceptanceTests` |

US6 and US9 run the real services against `FakeConnection`. US11 runs the app's own wiring
(`SyncSessions`: per-user SQLite file, upload queue, gateway) on a real file, and only the mainframe
is faked.

Writing the US9 tests turned up a gap: only the main borrower was checked before a loan went to
Pending. That was fixed in #51 and every co-borrower is now checked, with an error that names them.

**TODO:** acceptance tests for the remaining stories, if the marking guide asks for all of them.

## 5. Code metrics

Measured with CK 0.7.0 (`./mvnw -Pmetrics validate`). Full baseline:
[`docs/metrics/baseline-2026-10-22.md`](metrics/baseline-2026-10-22.md).

| Metric | Baseline (22 Oct, `ff950b2`) | Final | Change |
|---|---|---|---|
| Classes / methods | 161 / 1142 | **TODO** | |
| Avg CBO (coupling) | 6.26 | **TODO** | |
| Avg WMC (complexity) | 11.93 | **TODO** | |
| Avg LCOM* (cohesion) | 0.47 | **TODO** | |
| Avg method complexity | 1.71 | **TODO** | |
| Methods over 10 | 4 (max 16) | **TODO** | |

What the baseline shows:

- **Coupling** is highest in `ui`, as expected. `LavsApp` (CBO 46) builds every service, so it is
  the one place everything meets. The data layer is loosely coupled (avg CBO 2.9).
- **Complexity:** four methods are over the plan's limit of 10: `QueuedWrites.send` (16),
  `CustomerEligibilityService.check` (15), `PendingRecordReads.apply` (12) and `SyncProcessor.send`
  (12). They are the refactoring targets.
- **Cohesion:** controllers have high LCOM* (0.84 to 0.92) because each handler uses only some of the
  controller's fields. That's normal for FXML controllers. Model classes score 0.75 because
  getter/setter pairs share no fields.

**TODO:** re-run at the end, fill in the Final column, and say what each refactor changed.

## 6. Other checks

| Check | Status |
|---|---|
| Clean clone on Windows 10+ (fresh clone, empty Maven cache, `mvnw clean verify`) | Done 21 Oct; **TODO** redo on the final `main` |
| Responsiveness with a slow network (`FakeConnection.setDelay`) | **TODO** |
| Walkthrough of each story as the MMM | **TODO** |
| Third-party licences | **TODO** |

## 7. Known limitations

- Branch protection on `main` is being set up. Until then, CI must be green before merging. On 22 Oct
  two PRs that each passed CI on their own (#50 and #51) broke `main` together. It was fixed within
  the hour in #53. Branch protection with "require branches to be up to date" prevents this.
- **TODO:** anything the walkthrough or responsiveness check finds.
