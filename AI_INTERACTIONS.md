# AI interaction log

Per the team's shared requirement to log AI interactions and mark affected
code with `// AI-assisted: [description]`. Core UI and business logic is
written by hand; entries below are the exception, each noted as such.

Add an entry per session/PR, newest first.

---

## 2026-10-19 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build the app shell: a list of screens and a
navigator with a back stack, a top bar showing connection state, pending
uploads and the signed-in user, and the main window that joins them.

**What it produced** on `feature/ui-app-shell`:

- `lavs.ui.Screen`, `NavigatorState`, `ScreenRegistry`, `Refreshable`: the
  navigation rules (parameters, a back stack that keeps each built view,
  reset on login/logout, "not built yet" placeholders, a view that fails to
  build leaves the current screen alone), with no JavaFX, and unit tests.
- `Navigator` (thin JavaFX control over those rules).
- `StatusFeed`, `FixedStatusFeed` (TEMPORARY), `TopBarState`, `TopBar`:
  "Online / Offline" with "All changes uploaded" or "N waiting to upload",
  "Signed in: J. Smith", a default title per screen, hidden on Login. The
  text and style decisions are in `TopBarState` and unit tested.
- `MainWindow`; `LavsApp` now builds the shell and starts on a TEMPORARY
  login stand-in; the sample `main-window.fxml` was removed.

**Marking:** each new file carries an `// AI-assisted` comment.

---

## 2026-10-19 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build the helper that runs every service call off
the UI thread and shows a loading state, testable without starting JavaFX.

**What it produced** on `feature/ui-background-task`:

- `lavs.ui.BackgroundTasks`: takes a background executor and a UI-thread
  runner; `run(work, onStart, onDone, onError)` returns a handle whose
  `cancel()` means no callback is ever made. 100% branch tested with
  queues standing in for threads (`BackgroundTasksTests`).
- `lavs.ui.LoadingState`: the rules for the busy button text, disabled
  button, Cancel button and skeleton rows. Reads get Cancel and skeletons;
  saves do not, because saves still upload through the sync queue
  (`LoadingStateTests`).
- `LoadingView` and `SkeletonRows`: thin JavaFX controls with no branches
  that copy the state onto the screen.

**Marking:** each new file carries an `// AI-assisted` comment.

---

## 2026-10-19 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build the shared LAVS stylesheet from the Figma.

**What it produced** on `feature/ui-stylesheet`:

- `lavs.css`: colour tokens as looked-up colours on `.root`, Segoe UI
  typography (Segoe UI Semibold for Figma's semibold text), and the style
  classes for every shared component.
- `LavsStylesheetTests`: checks that every token used is defined, that no
  one-off hex colours sit outside `.root`, and that every component class
  exists. Runs without starting JavaFX.
- `LavsApp` now loads the stylesheet at 1280x800; `main-window.fxml` has a
  TEMPORARY row of sample buttons and tags for checking the styles by eye.

**Marking:** `lavs.css` and `LavsStylesheetTests` carry an `// AI-assisted`
comment. The `LavsApp` and FXML edits were small.

---

## 2026-10-18 - Aum, Claude (Opus 5.5) - fixes from pre-push testing

**Prompt (summarised):** test the local store and sync work before pushing and
fix anything that could fail.

**What it changed** on `feature/local-store-sync`:

- `SyncService.close()` waits for the sync thread, so saves already queued
  reach the local file and the SQLite file is released (an open file stops
  Windows CI deleting test temp folders).
- `SyncService` no longer leaves a screen waiting forever after an unexpected
  upload error; the change stays queued and the result comes back as WAITING.
- `SyncProcessor` never sends a change that still has the temporary number of
  a paused or rejected record, including indirect cases (changes to a new loan
  whose new customer is paused or rejected), and resumes those changes when
  the MMM resolves the duplicate.
- New tests for each fix in `SyncServiceTests` and `SyncProcessorTests`.

**Marking:** the changed code carries `// AI-assisted` comments.

---

## 2026-10-18 - Aum, Claude (Opus 5.5)

**Prompt (summarised):** assist with Phase 2 local store and sync — result
types, the background service, and tests.

**What it produced** on `feature/local-store-sync`:

- `lavs.data`: `SendResult`, `DuplicateCheckResult`, `UploadGateway` (the
  interface Scott's gateway will implement, marked TEMPORARY).
- `lavs.logic.SyncService` - single background sync thread; queuing,
  draining, future completion and listener notification.
- Unit tests for all of the above, plus tests for the data store and the
  sync processor that Aum wrote: `LocalDatabaseTests`,
  `SqlitePendingChangeStoreTests`, `CategoryConstantStoreTests`,
  `ResultTypesTests`, `InMemoryChangeQueue` (test helper),
  `FakeUploadGateway` (scripted test helper), `SyncProcessorTests`,
  `SyncServiceTests`, `TemporaryNumbersTests`, `PendingChangeTests`.
- `sqlite-jdbc` dependency added to `pom.xml`.

**Marking:** each file that carries a `// AI-assisted` comment. Generating
these tests was agreed by the team as an exception to the hand-written rule.

---

---

## 2026-10-15 - Aum, Claude (Opus 5.5)

**Prompt (summarised):** generate unit tests for the Phase 1 model classes
in `lavs.model`.

**What it produced** on `feature/model-classes`:

- Unit tests for `Customer`, `Address`, `PhoneNumber`, `Email`, `Employer`,
  `Loan` and `LoanCoborrower` (`src/test/java/lavs/model`).

**Marking:** each test file carries a `// AI-assisted` comment. Generating
these tests was agreed by the team as an exception to the hand-written rule.

---

## 2026-10-11 - Gabeliu, Claude Code (Sonnet 5)

**Prompt (summarised):** work through the sprint backlog (build/test/CI
setup, validation framework, eligibility check, a test fake that can go
offline mid-test, US8 and US9 service logic), resolve the main-borrower/
guarantor question if possible, and don't push or open a PR without
checking first.

**What it produced**, each on its own local branch off `main`, none pushed:

- `chore/build-test-ci-setup` - Maven wrapper (mvnw/mvnw.cmd), Mockito,
  JaCoCo (95% branch-coverage gate on the team's own code), excludes the
  starter tests that call the live mainframe server from the default run,
  `.github/workflows/ci.yml`, and a build/test/run README section.
- `shared/validation-framework` - `lavs.validation`: `FieldError`,
  `ValidationResult`, `Validators` (required/date/phone/email/currency/
  postcode).
- `shared/eligibility-check` - `lavs.logic.EligibilityChecker` (mailing
  address + phone or email).
- `test/fake-connection-offline` - added the "goes offline mid-test ->
  send() returns a rejection" behaviour to `mainframe.client.FakeConnection`
  (whose scripted-response design originates from Yu Tokuda's unmerged
  `feature/us10-constants` branch - needs reconciling when that merges).
- `feature/us8-loan-coborrowers` - `lavs.logic.LoanBorrowerService`: add/
  remove/update a co-borrower or guarantor via `loan.AddCoborrower`/
  `loan.DeleteCoborrower` (update = remove then re-add). Service logic and
  tests only - the screen is blocked on US6 and a UI framework choice.
- `feature/us9-submit-for-verification` - `lavs.logic.LoanStatusService`:
  submit a loan for verification (-> Pending) via `loan.UpdateStatus`,
  gated on `EligibilityChecker`.
- `docs/ai-interaction-log` - this file.

**Left unresolved, on purpose:** whether a main borrower with no employer
must have a guarantor before a loan can become Pending. The notes suggest
blocking submission on it, but that is flagged as an open question for
James on Ed in this session rather than decided - see the doc comments on
`LoanStatusService`/`EligibilityChecker`.

**Marking:** every file these branches add carries a `// AI-assisted`
comment (or, for the brought-in `FakeConnection.java`, a note on exactly
which lines are new) per the team's rule that core UI/business logic is
written by hand - these backlog cards were the stated exception for this
session.
